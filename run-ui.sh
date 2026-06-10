#!/usr/bin/env zsh
set -e

export JAVA_HOME="/Users/heartvillegas/Library/Java/JavaVirtualMachines/temurin-21.0.10/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./.tools/apache-maven-3.9.11/bin/mvn javafx:run
