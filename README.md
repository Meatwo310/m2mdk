# m2mdk

Minecraft Mod開発用のGradleプラグインです。現在のsandboxはMinecraft 1.21.1 / NeoForgeに対応しています。

## 開発

Java 21を用意し、リポジトリのルートで実行します。

```sh
./gradlew build
./gradlew -p sandbox :neoforge:build
./gradlew -p sandbox :neoforge:runClient
```

`sandbox`は独立したGradleビルドです。`pluginManagement.includeBuild("..")`でローカルのプラグインを読み込み、プラットフォームごとのサブプロジェクトに適用します。現在は`:neoforge`のみです。

ルートの`build`には`functionalTest`も含まれます。Gradle TestKitで複数サブプロジェクトのメタデータ分離、初期値、実行タスク、configuration cacheの再利用、不正なMod IDの拒否を確認します。

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

サンプルは[sandbox/neoforge](sandbox/neoforge)にあります。実行ディレクトリは適用先の`run/`です。`runServer`を使用する場合はMinecraftのEULAを確認し、同意する場合にのみ自身で設定してください。

プラグイン実装は現在ひとつのプロジェクトにまとめています。共通コードの結合、Mixin、Config、Modの公開処理は今後の対象です。
