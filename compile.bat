@echo off
echo Starting build...
cd /d C:\Users\AZ\Documents\Plugin\1.21.8\DragonGestion
C:\Users\AZ\Documents\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests
echo Build finished
pause