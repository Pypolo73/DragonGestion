# PowerShell build script for DragonGestion
$ErrorActionPreference = "Stop"
$projectDir = "C:\Users\AZ\Documents\Plugin\1.21.8\DragonGestion"
$mavenBin = "C:\Users\AZ\Documents\apache-maven-3.9.6\bin\mvn.cmd"

Set-Location $projectDir

Write-Host "Building DragonGestion..."

& $mavenBin clean package -DskipTests 2>&1

if (Test-Path "target\admincore-0.1.0-SNAPSHOT.jar") {
    Write-Host "BUILD SUCCESS: target\admincore-0.1.0-SNAPSHOT.jar created"
    Copy-Item "target\admincore-0.1.0-SNAPSHOT.jar" "versions\DragonGestion-1.1.54\DragonGestion-1.1.54.jar" -Force -ErrorAction SilentlyContinue
    Write-Host "Copied to versions\"
} else {
    Write-Host "BUILD FAILED - no JAR found"
    Get-ChildItem target -Filter *.jar -ErrorAction SilentlyContinue
}