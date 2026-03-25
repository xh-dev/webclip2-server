#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# --- [1/4] Initialize Environment Variables ---

# Extract version from build.sbt (Jenkins C_VERSION logic)
C_VERSION=$(grep -E "^[ ]*version[ ]*:=[ ]*\"([^\"]+)\"$" build.sbt | sed -e "s/version[ ]*:=[ ]*\"\(.*\)\"/\1/g")

# Get Git Branch and Commit ID
# Fallback to git commands if environment variables are not set
GIT_BRANCH=${GITHUB_REF_NAME:-$(git rev-parse --abbrev-ref HEAD)}
GIT_COMMIT=${GITHUB_SHA:-$(git rev-parse HEAD)}

echo "Project Version: $C_VERSION"
echo "Branch: $GIT_BRANCH"
echo "Commit: $GIT_COMMIT"

# --- [2/4] Run sbt assembly ---

sbt assembly

# --- [3/4] Docker Build ---

IMAGE_NAME="xethhung/webclip2-server"

# Build with arguments matching your Jenkins setup
docker build \
  --build-arg branchName="$GIT_BRANCH" \
  --build-arg commitId="$GIT_COMMIT" \
  --build-arg C_VERSION="$C_VERSION" \
  -t "$IMAGE_NAME:latest" \
  -t "$IMAGE_NAME:$C_VERSION" \
  .

# --- [4/4] Completion ---

echo "Build complete: $IMAGE_NAME:latest and $IMAGE_NAME:$C_VERSION"