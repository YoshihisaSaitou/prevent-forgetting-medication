## 初期設定

「docker-compose.yml.example」をコピーして「docker-compose.yml」を作成する。
「.env.example」をコピーして「.env」を作成する。
「.env」の「HOST_IP」にPCのIPアドレスを設定し、「EXPO_TOKEN」にExpoの管理画面で取得したAccess tokensを設定する。
Expoの管理画面は下記からログインできる。
https://expo.dev/

## React Nativeのインストールコマンド例

初回のみ必要なので、通常は実施不要。

```
npx create-expo-app@latest <プロジェクト名>
```

## EAS（Expo Application Services）の設定

初回のみ必要なので、通常は実施不要。

```
eas build:configure
```

## React NativeコンテナでのExpoの起動コマンド

```
npx expo start
```

## ネイティブモジュールの作成コマンド

app.json / app.config.* と導入済みパッケージ・Config Plugin をもとに、ネイティブの ios/ と android/ プロジェクトを生成・同期します。Expo はこの仕組みを Continuous Native Generation (CNG) と呼び、同コマンドはその中核となる。
Expo Go だけでは動かない ネイティブモジュール（例：VisionCamera など） を使う／Dev Client を作る／ネイティブ設定を変更したい（Info.plist、AndroidManifest.xml 等）時に実行する。

```
npx expo prebuild
```

## AndroidのAAB/APKファイルとiOSのIPAファイルの作成コマンド

「--local」を付けないと有料のクラウド上で実行される。
「--local」は1回の実行で1プラットフォームのみビルド可能なので、Allを選択するとエラーになる。
先にeasコマンドのインストールやアカウントのセットアップが必要、詳細は下記を参照する。
https://docs.expo.dev/build/setup/?utm_source=chatgpt.com

```
本番配布用
eas build --local -p android
eas build --local -p ios

開発配布用
eas build --local -p android --profile development
eas build --local -p ios --profile development

検証配布用
eas build --local -p android --profile preview
eas build --local -p ios --profile preview
```

## ストアへの提出フロー

- Android：eas build --local -p android --profile production → 生成された .aab を Play Console にアップロード。
- iOS：eas build --local -p ios --profile production → 生成された .ipa を Xcode Organizer / Transporter で App Store Connect にアップロード。
