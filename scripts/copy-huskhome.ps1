$sourceCommon = "C:\Users\AZ\Documents\Plugin\1.21.8\DragonGestion\temp\HuskHomes\common\src\main\java\net\william278\huskhomes"
$sourcePaper = "C:\Users\AZ\Documents\Plugin\1.21.8\DragonGestion\temp\HuskHomes\paper\src\main\java\net\william278\huskhomes"
$targetBase = "C:\Users\AZ\Documents\Plugin\1.21.8\DragonGestion\src\main\java\fr\dragon\admincore\teleportation\husk"

function Copy-AndAdaptFiles {
    param($sourceDir)
    
    Get-ChildItem -Path $sourceDir -Recurse -Filter "*.java" | ForEach-Object {
        $relativePath = $_.DirectoryName.Replace($sourceDir, "").TrimStart("\")
        $targetDir = Join-Path $targetBase $relativePath
        
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        
        $content = Get-Content $_.FullName -Raw
        $content = $content -replace 'package net\.william278\.huskhomes\.', 'package fr.dragon.admincore.teleportation.husk.'
        $content = $content -replace 'net\.william278\.huskhomes\.', 'fr.dragon.admincore.teleportation.husk.'
        
        $targetFile = Join-Path $targetDir $_.Name
        Set-Content -Path $targetFile -Value $content -NoNewline
        Write-Host "Copied: $($_.Name)"
    }
}

Write-Host "Copying common module..."
Copy-AndAdaptFiles $sourceCommon

Write-Host "Copying paper module..."
Copy-AndAdaptFiles $sourcePaper

Write-Host "Done!"
