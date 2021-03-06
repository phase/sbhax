#!/bin/bash

echo "Deploying SBHS"

# Prepare files
BUILD_DIR="sbhs_builds"
HASH=`git describe --tags`
FULL_HASH=`git rev-parse $HASH`

# Clone the repo
BRANCH="build"
rm -rf .git/
git clone -b $BRANCH https://github.com/phase/sbhs.git $BUILD_DIR
cd $BUILD_DIR

# Setup user
USER="Jadon Fowler"
EMAIL="jadonflower@gmail.com"
git config user.name $USER
git config user.email $EMAIL

# Create an orphan branch if the build branch doesn't exist
if [ "`git branch --list $BRANCH`" ]
then
    git checkout -B $BRANCH
else
    git checkout --orphan $BRANCH
    git rm --cached $(git ls-files)
    rm -rf ./*
    # pesky hidden files
    find . -type f -name ".[^.]*" -delete
fi

cp ../build/libs/sbhs.jar ./sbhs-${HASH}.jar
# keep latest version as 'sbhs.jar'
rm -f ./sbhs.jar
cp ../build/libs/sbhs.jar ./sbhs.jar

# Push the files
git add -A
git commit -s \
    -m "Add build for ${HASH}" \
    -m "Build for https://github.com/phase/sbhs/commit/${FULL_HASH}" \
    -m "Auto-generated by my awesome deploy script."
git push --quiet https://${GITHUB_TOKEN}@github.com/phase/sbhs.git/ build > /dev/null 2>&1
cd ../

