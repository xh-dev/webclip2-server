#!/bin/bash

# 確保腳本在出錯時立即停止
set -e

echo "--- [1/4] 初始化環境變數 ---"

# 1. 提取 build.sbt 中的版本號 (對應 Jenkins 的 C_VERSION)
C_VERSION=$(grep -E "^[ ]*version[ ]*:=[ ]*\"([^\"]+)\"$" build.sbt | sed -e "s/version[ ]*:=[ ]*\"\(.*\)\"/\1/g")

# 2. 獲取 Git 資訊 (若在 GitHub Actions 或本地有 Git 環境)
# 如果環境變數不存在，則嘗試從 git 指令獲取
GIT_BRANCH=${GITHUB_REF_NAME:-$(git rev-parse --abbrev-ref HEAD)}
GIT_COMMIT=${GITHUB_SHA:-$(git rev-parse HEAD)}

echo "專案版本: $C_VERSION"
echo "目前分支: $GIT_BRANCH"
echo "Commit ID: $GIT_COMMIT"

echo "--- [2/4] 執行 sbt assembly ---"
# 執行編譯，產生 JAR 檔
sbt assembly

echo "--- [3/4] 執行 Docker Build ---"
# 定義映像檔名稱
IMAGE_NAME="xethhung/webclip2-server"

# 執行 Docker 建置，並傳入 build-arg
docker build \
  --build-arg branchName="$GIT_BRANCH" \
  --build-arg commitId="$GIT_COMMIT" \
  --build-arg C_VERSION="$C_VERSION" \
  -t "$IMAGE_NAME:latest" \
  -t "$IMAGE_NAME:$C_VERSION" \
  .

echo "--- [4/4] 建置完成 ---"
echo "已產生映像檔: $IMAGE_NAME:latest 與 $IMAGE_NAME:$C_VERSION"