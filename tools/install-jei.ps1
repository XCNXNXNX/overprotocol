$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$modsDir = Join-Path $projectRoot 'run-client\mods'
$fileName = 'jei-1.21.1-neoforge-19.21.0.247.jar'
$target = Join-Path $modsDir $fileName
$expectedHash = '413680B163B2F4477409512E393B7F33133C4C395D55BBDF877791008B3EF702'
New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
if ((Test-Path -LiteralPath $target) -and (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -eq $expectedHash) {
    Write-Output "JEI is already installed and verified: $target"
    exit 0
}
$staging = "$target.download"
Invoke-WebRequest -Uri "https://mediafilez.forgecdn.net/files/5846/880/$fileName" -OutFile $staging
if ((Get-FileHash -LiteralPath $staging -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'JEI checksum mismatch. The download has not been installed.'
}
Move-Item -LiteralPath $staging -Destination $target -Force
Write-Output "Installed JEI: $target. Restart the development client to load it."
