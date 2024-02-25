#!/bin/sh

version=$(curl -Ls -o /dev/null -w "%{url_effective}" https://github.com/rife2/bld/releases/latest)
version=${version##*/}
filepath=$(mktemp -u -t "bld-$version-XXXXXX.jar")

echo "Downloading bld v$version..."
echo

curl -L -s "https://github.com/rife2/bld/releases/download/$version/bld-$version.jar" > "$filepath"

echo "Welcome to bld v$version."
java -jar "$filepath" upgrade

rm -f "$filepath"