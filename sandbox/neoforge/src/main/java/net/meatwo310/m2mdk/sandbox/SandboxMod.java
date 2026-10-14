package net.meatwo310.m2mdk.sandbox;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;

@Mod(SandboxMod.MOD_ID)
public final class SandboxMod {
    public static final String MOD_ID = "m2mdk_sandbox";

    public SandboxMod() {
        LogUtils.getLogger().info("m2mdk sandbox mod loaded: {}", MOD_ID);
    }
}
