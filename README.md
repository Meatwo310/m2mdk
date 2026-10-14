# m2mdk

Minecraft Mod開発用のGradleプラグインです。

`sandbox/`は、CIとローカルでプラグインの変更を検証するためのプロジェクトです。各プラットフォームをサブプロジェクトとして構成し、Modのビルド、メタデータ生成、実行時の読み込みを確認します。現在はMinecraft 1.21.1 / NeoForgeを対象にしています。

## 開発と検証

Java 21を用意し、リポジトリのルートで実行します。

```sh
./gradlew build
./gradlew -p sandbox :neoforge:build --configuration-cache
```

| コマンド | 検証対象 |
| --- | --- |
| `./gradlew build` | プラグインのビルドとTestKitによる機能テスト |
| `./gradlew -p sandbox :neoforge:build --configuration-cache` | 実際のMinecraft / NeoForge依存を使ったModのコンパイル、メタデータ生成、JARへの組み込み |

ルートの`build`には`functionalTest`も含まれます。Gradle TestKitで複数サブプロジェクトのメタデータ分離、初期値、実行タスク、configuration cacheの再利用、不正なMod IDの拒否を確認します。

`sandbox`は、プラグインを利用側の構成で検証するための独立したGradleビルドです。`pluginManagement.includeBuild("..")`で開発中のプラグインを読み込み、各プラットフォームのサブプロジェクトに適用します。現在は`:neoforge`のみです。

[CIワークフロー](.github/workflows/ci.yml)は、`main`へのpush、Pull Request、手動実行に対応しています。Ubuntu / Java 21でプラグインの機能テストとsandboxのビルドを実行し、生成JARにModのエントリクラスと展開済みメタデータが入っていることを検証します。ビルド成功だけでは確認できないMod ID・バージョン・依存範囲もチェックします。

sandboxの`build`には`verifyModJar`が含まれます。JARの検証のみを実行する場合は、次のコマンドを使用してください。必要なJARもタスクの依存関係からビルドされます。

```sh
./gradlew -p sandbox :neoforge:verifyModJar --configuration-cache
```

実行構成やMod読み込みに影響する変更は、ローカルでクライアントを起動し、検証用Modが読み込まれることを確認します。

```sh
./gradlew -p sandbox :neoforge:runClient
```

Gradle WrapperのZIPとJARは公式のSHA-256と照合しています。プラグインの依存は`gradle/verification-metadata.xml`でSHA-256を検証します。これは取得済みアーティファクトの固定であり、上流コード全体やMinecraft実行時の依存の監査を意味しません。

## 利用側の設定

```kotlin
plugins {
    id("net.meatwo310.m2mdk.neoforge")
}

group = "com.example"
version = "1.0.0"

m2mdk {
    modId.set("examplemod")
    modName.set("Example Mod")
    license.set("All Rights Reserved")
}

neoForge {
    version = "21.1.259"
}
```

ModDevGradle 2.0.148を依存として含み、自動適用します。Java 21のtoolchain、UTF-8でのコンパイル、`main`ソースセットの登録、client/serverの実行構成を設定します。上流の`neoForge {}`やGradle標準DSLで調整できます。

`m2mdk.modId`は必須です。Mod名の初期値はMod ID、ライセンスの初期値は`All Rights Reserved`です。`@Mod`のIDも同じ値にしてください。

`src/main/templates/META-INF/neoforge.mods.toml`を利用側に用意します。`generateModMetadata`が`${mod_id}`、`${mod_name}`、`${mod_license}`、`${mod_version}`を展開し、生成リソースをビルドとIDE同期に組み込みます。バージョンは適用先プロジェクトの`version`から取得します。MinecraftとNeoForgeの依存範囲はテンプレートで管理してください。通常のリソース側に同名のTOMLを置かないでください。

検証用Modは[sandbox/neoforge](sandbox/neoforge)にあります。プラグインの変更を検証できる最小限の構成を維持し、機能追加や不具合修正に応じて検証ケースを追加します。利用方法の具体例としても参照できます。

実行ディレクトリは適用先の`run/`です。`runServer`を使用する場合はMinecraftのEULAを確認し、同意する場合にのみ自身で設定してください。

プラグイン実装は現在ひとつのプロジェクトにまとめています。共通コードの結合、Mixin、Config、Modの公開処理は今後の対象です。
