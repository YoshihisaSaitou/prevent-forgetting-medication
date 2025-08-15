FROM node:22-bookworm AS base

# 必要なシステムパッケージをインストール
# watchmanはファイル変更の監視に必要
RUN apt-get update && apt-get install -y \
  bash \
  git \
  openssh-client \
  gettext-base \
  watchman \
  openjdk-17-jdk \
  && rm -rf /var/lib/apt/lists/*

# easコマンドをグローバルにインストール
RUN npm install -g eas-cli

# アプリ名を環境変数として設定
#ARG APP_NAME=PreventForgettingMedicationSpApp

#ARG APP_NAME
#RUN echo $APP_NAME
#RUN echo ${APP_NAME}

# 作業ディレクトリをReact Nativeのインストール場所に設定
#WORKDIR /


# APP_NAMEが存在しない場合のみ create-expo-app を実行
#RUN if [ ! -d "$APP_NAME" ]; then \
#      # React Nativeの依存関係をインストール
#      npx create-expo-app@latest "$APP_NAME" --yes; \
#    fi

# テンプレートをコピー（Dockerfile と同じ階層に配置しておく）
#COPY credentials.json.example /tmp/credentials.json.example

# 置換して配置
#RUN envsubst < /tmp/credentials.json.example > "/${APP_NAME}/credentials.json" && \
#    rm /tmp/credentials.json.example

# 作業ディレクトリをプロジェクト直下に設定
#WORKDIR "/${APP_NAME}"

# 依存解決に必要な最小ファイルを先にコピー（キャッシュ有効化のため）
#COPY package*.json ./

# 依存をインストール（npmのキャッシュを使いたければ BuildKit の cache mount を併用）
#RUN npm install

# 残りのソースをコピー
#COPY "/${APP_NAME}" .

# アプリケーションのポートを公開
#EXPOSE 8081
# 19000: Expo Metro Bundler (Redirect)
# 19001: Expo Metro Bundler
# 19002: Expo DevTools
#EXPOSE 19000 19001 19002

# コンテナ起動時のデフォルトコマンド
#CMD ["npx", "expo", "start", "--tunnel"]