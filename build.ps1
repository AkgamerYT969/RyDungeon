$ErrorActionPreference = "Stop"

Write-Host "RyDungeon 1.1.0 - Paper 1.21.11 - Made By TheRynzo"
mvn clean package

New-Item -ItemType Directory -Force -Path "dist" | Out-Null
Copy-Item "target/RyDungeon.jar" "dist/RyDungeon.jar" -Force

Write-Host "BUILD SUCCESS: dist/RyDungeon.jar"
