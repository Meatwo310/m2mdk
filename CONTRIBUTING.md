# コントリビューションガイド

## 開発環境と構成

Java 21を用意し、GradleはリポジトリのWrapper（`./gradlew`）を使用してください。
Javaのインストールや環境変数の設定を省きたい場合は、任意で[Nixの開発シェル](README.md#nixで環境を用意する任意)も利用できます。

- `src/main/java/`: Gradleプラグインの実装
- `src/test/java/`: Gradle TestKitによる機能テスト
- `sandbox/`: CIとローカルでプラグインの変更を検証するための独立したビルド。`includeBuild("..")`で開発中のプラグインを利用します。
- `sandbox/neoforge/`: Minecraft 1.21.1 / NeoForgeの検証用Modサブプロジェクト

利用方法は[README.md](README.md)を参照してください。

## 変更と検証

変更は目的ごとにまとめ、適用先プロジェクト内で設定が完結するようにしてください。プラグイン内で特定のプロジェクト名や兄弟プロジェクトを決め打ちしないでください。利用者がGradle標準DSLや`neoForge {}`で設定を調整できるようにしてください。

プラグインを変更した場合は、次のコマンドでビルドと機能テストを実行してください。

```sh
./gradlew build
./gradlew -p sandbox :neoforge:build --configuration-cache
```

ルートの`build`には`functionalTest`が含まれます。サブプロジェクト間のメタデータ分離、初期値、実行タスク、configuration cacheの再利用、不正なMod IDの拒否を確認します。

sandboxのビルドでは、実際のMinecraft / NeoForge依存を使ったModのコンパイル、メタデータ生成、JARへの組み込みを検証します。検証用Modは最小限の構成を維持し、機能追加や不具合修正に応じて検証ケースを追加してください。

[CIワークフロー](.github/workflows/ci.yml)はUbuntu / Java 21で上記のビルドを実行します。sandboxの`check`と`build`には`verifyModJar`が含まれ、生成JARのエントリクラスとメタデータを検証します。検証のみを実行する場合は、次のコマンドを使用してください。sandboxの期待値を変更した場合は、`sandbox/neoforge/build.gradle.kts`内の検証タスクの期待値も更新してください。

```sh
./gradlew -p sandbox :neoforge:verifyModJar --configuration-cache
```

実行構成やMod読み込みに影響する変更は、クライアントを起動して確認してください。

```sh
./gradlew -p sandbox :neoforge:runClient
```

ドキュメントのみの変更ではビルドは不要です。いずれの場合も`git diff --check`で差分の空白エラーを確認してください。生成物、キャッシュ、実行ディレクトリはコミットに含めないでください。

### Nix設定を変更する場合

`flake.nix`や`flake.lock`を変更した場合は、`nix flake check --all-systems`で全対応構成の設定を検証し、
`nix develop --command ./gradlew build`と
`nix develop --command ./gradlew -p sandbox :neoforge:build --configuration-cache`で
開発シェルからビルドできることを確認してください。
Nixpkgsを更新する場合は`nix flake update nixpkgs`を実行し、`flake.lock`もコミットします。

## コミットメッセージ

**コミットメッセージは[Conventional Commits 1.0.0](https://www.conventionalcommits.org/ja/v1.0.0/)に従ってください。**

```text
<type>[(scope)][!]: <description>

[任意の本文]

[任意のフッター]
```

`type`は小文字に統一し、説明は変更内容がわかる短い文章にしてください。`scope`は任意です。付ける場合は`neoforge`、`metadata`、`sandbox`など変更対象を表してください。

| type | 用途 |
| --- | --- |
| `feat` | 新機能 |
| `fix` | 不具合修正 |
| `docs` | ドキュメント |
| `refactor` | 機能を変えないコード整理 |
| `test` | テストの追加・修正 |
| `build` | ビルド設定・依存関係 |
| `ci` | CI設定 |
| `perf` | 性能改善 |
| `style` | 動作に影響しない書式変更 |
| `chore` | その他の保守作業 |

例:

```text
feat(neoforge): add default client and server runs
fix(metadata): use the target project's mod version
docs: add contribution guidelines
```

破壊的変更は`!`または`BREAKING CHANGE:`フッターで明示し、変更内容と移行方法を説明してください。

```text
feat(metadata)!: rename the mod metadata extension

BREAKING CHANGE: Replace the old extension name with m2mdk in build scripts.
```

## Pull Request

変更の目的、変更後の挙動、実行した検証とその結果を説明してください。未実施の検証がある場合は理由も記載してください。利用方法や公開DSLを変更した場合は、関連するドキュメントとsandboxも更新してください。
