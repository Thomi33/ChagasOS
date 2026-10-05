#!/usr/bin/env bash
# CHAGASOS - compilar y ejecutar
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -encoding UTF-8 -cp "lib/jlayer.jar" -d out $(find src -name "*.java")
echo "ChagasOS compilado con exito."
exec java -cp "out:lib/jlayer.jar" chagasos.ChagasOS
