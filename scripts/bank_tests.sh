#!/bin/bash

LIB_PATH=".\..\lib"
SOLUTIONS_PATH=".\..\java-solutions"

javac "$SOLUTIONS_PATH\info\kgeorgiy\ja\matveev\bank\BankTests.java" \
-cp "$LIB_PATH\*;$SOLUTIONS_PATH" \
-d "."

java -cp "$LIB_PATH\*;." info.kgeorgiy.ja.matveev.bank.BankTests
EXIT_CODE=$?
rm -r info
echo $EXIT_CODE
exit $EXIT_CODE