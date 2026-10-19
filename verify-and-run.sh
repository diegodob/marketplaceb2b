#!/usr/bin/env sh
set -e
./gradlew clean test
exec ./gradlew bootRun
