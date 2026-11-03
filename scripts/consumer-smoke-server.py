#!/usr/bin/env python3
"""Boot a disposable dedicated server, confirm Mod loading, then stop cleanly."""
import argparse
import os
from pathlib import Path
import queue
import subprocess
import threading
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("consumer", type=Path, help="Disposable consumer checkout; never use a development world")
parser.add_argument("loader", choices=("fabric", "neoforge"))
parser.add_argument("--minecraft", default="26.1.2")
parser.add_argument("--accept-eula", action="store_true", required=True,
                    help="Accept https://aka.ms/MinecraftEULA for this test server")
args = parser.parse_args()
ROOT = args.consumer.resolve()
run = ROOT / f"{args.minecraft}/{args.loader}/run"
run.mkdir(parents=True, exist_ok=True)
# Never replace an existing server's EULA, settings, or world.
for name, content in {
    "eula.txt": "eula=true\n",
    "server.properties": "online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nlevel-name=smoke-world\nview-distance=2\nsimulation-distance=2\n",
}.items():
    target = run / name
    if not target.exists():
        target.write_text(content)
assert "eula=true" in (run / "eula.txt").read_text(), "Existing EULA has not been accepted"
props = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines() if "=" in line)
mod_id = props["modId"]
log = ROOT / f"build/reports/smoke-{args.loader}.log"
log.parent.mkdir(parents=True, exist_ok=True)
wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
command = [wrapper, f":{args.minecraft}-{args.loader}:runServer", "--console=plain", "--max-workers=2"]
process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True, bufsize=1, start_new_session=True)
lines = queue.Queue()


def read_output():
    for line in process.stdout:
        lines.put(line)
    lines.put(None)


threading.Thread(target=read_output, daemon=True).start()
loaded = False
started = False
stopped = False
deadline = time.monotonic() + 300
try:
    with log.open("w") as stream:
        while time.monotonic() < deadline:
            try:
                line = lines.get(timeout=1)
            except queue.Empty:
                continue
            if line is None:
                break
            stream.write(line)
            stream.flush()
            loaded |= f"Initialized {mod_id}" in line
            if 'Done (' in line and not stopped:
                started = True
                process.stdin.write("stop\n")
                process.stdin.flush()
                stopped = True
                deadline = time.monotonic() + 60
        else:
            raise TimeoutError(f"Server did not finish in time; see {log}")
    result = process.wait(timeout=15)
    assert loaded and started and stopped and result == 0, f"Server smoke test failed; see {log}"
    print(f"Verified {args.loader}: Mod loaded, dedicated server started and stopped cleanly")
finally:
    if process.poll() is None:
        if os.name != "nt":
            import signal
            os.killpg(process.pid, signal.SIGTERM)
        else:
            process.terminate()
        process.wait(timeout=15)
