#!/bin/bash

TEST_REPO_NAME="java-advanced-2024"
#TEST_REPO_NAME="shared"
TEST_REPO_PATH="./../../$TEST_REPO_NAME"
GK_IMPLEMENTOR_PATH="./../../$TEST_REPO_NAME/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor"
MY_REPO="./../java-solutions/info/kgeorgiy/ja/matveev"

javac -d . $MY_REPO/implementor/*.java \
  $GK_IMPLEMENTOR_PATH/Impler.java \
  $GK_IMPLEMENTOR_PATH/JarImpler.java \
  $GK_IMPLEMENTOR_PATH/ImplerException.java \
  -cp $TEST_REPO_PATH/artifacts/info.kgeorgiy.java.advanced.implementor.jar
jar cfm Implementor.jar MANIFEST.MF info
rm -r info
