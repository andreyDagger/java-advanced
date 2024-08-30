#!/bin/bash

LIB_PATH=".\..\lib"
SOLUTIONS_PATH=".\..\java-solutions"

javac "$SOLUTIONS_PATH\info\kgeorgiy\ja\matveev\bank\BankTest.java" \
-cp "$LIB_PATH\*;$SOLUTIONS_PATH" \
-d "."

java -jar "$LIB_PATH\junit-platform-console-standalone-1.10.2.jar" \
 -cp "." \
 --select-class info.kgeorgiy.ja.matveev.bank.BankTest

EXIT_CODE=$?
rm -r info
echo $EXIT_CODE
exit $EXIT_CODE