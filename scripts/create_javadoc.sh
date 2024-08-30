
# :NOTE: Лучше еще добавлять #!/bin/bash в начале скрипта

TEST_REPO_NAME="java-advanced-2024"
#TEST_REPO_NAME="shared"
TEST_REPO_PATH="./../../$TEST_REPO_NAME"
MY_REPO="./../java-solutions/info/kgeorgiy/ja/matveev"
ARTIFACT_PREFIX="$TEST_REPO_PATH/artifacts/*"

# :NOTE: лучше явно куказывать ссылку на javadoc стандартной библиотеки ввашей версии

javadoc \
  -private \
  -author \
  -d ./../javadoc $MY_REPO/implementor/*.java $MY_REPO/iterative/*.java \
  -cp "$ARTIFACT_PREFIX"
