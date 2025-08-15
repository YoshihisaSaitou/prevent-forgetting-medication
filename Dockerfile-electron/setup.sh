#!/usr/bin/env bash
set -e

# カレントディレクトリに package.json が無ければ初期化
if [ ! -f package.json ]; then
  echo "package.json が無いので初期セットアップを実行します"
  yarn init -y

  # Electron とビルダーを追加
  yarn add --dev electron electron-builder

  # 必要最低限の script を追記（Yarn 1.x 用）
  npx --yes json -I -f package.json -e \
    'this.scripts={"electron:dev":"electron .","electron:make":"electron-builder"}'
fi

# 依存関係をインストール
yarn install --frozen-lockfile

# ビルド or 開発サーバを起動（docker‑compose 側で渡す）
exec "$@"
