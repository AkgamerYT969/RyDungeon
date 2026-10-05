#!/usr/bin/env bash
set -e

echo "RyDungeon 1.1.0 - Paper 1.21.11 - Made By TheRynzo"
mvn clean package

mkdir -p dist
cp -f target/RyDungeon.jar dist/RyDungeon.jar

echo "BUILD SUCCESS: dist/RyDungeon.jar"
