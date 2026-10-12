#!/usr/bin/env bash
# ==============================================================================
# Auto Version Bump, Commit, and Push Script for LR Mass Downloader
# ==============================================================================

set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

echo "🔍 Checking repository status..."

# 1. Read current version from version.properties
PROPS_FILE="version.properties"
if [ ! -f "$PROPS_FILE" ]; then
  cat <<EOF > "$PROPS_FILE"
major=1
minor=0
patch=0
build=100
versionName=1.0.0
lastUpdated=$(date +%Y-%m-%d)
EOF
fi

MAJOR=$(grep -E "^major=" "$PROPS_FILE" | cut -d'=' -f2)
MINOR=$(grep -E "^minor=" "$PROPS_FILE" | cut -d'=' -f2)
PATCH=$(grep -E "^patch=" "$PROPS_FILE" | cut -d'=' -f2)
BUILD=$(grep -E "^build=" "$PROPS_FILE" | cut -d'=' -f2)

MAJOR=${MAJOR:-1}
MINOR=${MINOR:-0}
PATCH=${PATCH:-0}
BUILD=${BUILD:-100}

# 2. Increment patch and build
NEW_PATCH=$((PATCH + 1))
NEW_BUILD=$((BUILD + 1))
NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"
TODAY=$(date +%Y-%m-%d)

echo "📦 Upgrading Version: ${MAJOR}.${MINOR}.${PATCH} (Build ${BUILD}) ➔ ${NEW_VERSION} (Build ${NEW_BUILD})"

cat <<EOF > "$PROPS_FILE"
major=${MAJOR}
minor=${MINOR}
patch=${NEW_PATCH}
build=${NEW_BUILD}
versionName=${NEW_VERSION}
lastUpdated=${TODAY}
EOF

# 3. Commit changes
git add version.properties
git add -A

COMMIT_MSG="${1:-Release v${NEW_VERSION} (Build ${NEW_BUILD}): Automated version increment}"
git commit -m "$COMMIT_MSG" || echo "No changes to commit."

# 4. Create local version tag
git tag -f "v${NEW_VERSION}" || true

echo "✅ Created commit and tag v${NEW_VERSION}!"

# 5. Push if remote is configured
if git remote | grep -q "origin"; then
  echo "🚀 Pushing to remote origin (branch main and tags)..."
  git push origin main --tags || git push origin master --tags || echo "⚠️ Push failed. Please check your git remote permissions."
  echo "🎉 Successfully pushed to GitHub! GitHub Actions will now automatically build and publish APK v${NEW_VERSION}."
else
  echo "ℹ️ No remote 'origin' configured yet."
  echo "To connect to your GitHub repository and enable automatic 1-click APK releases:"
  echo "  git remote add origin https://github.com/tapos-linx/LR_Mass_Downloader.git"
  echo "  git push -u origin main --tags"
fi
