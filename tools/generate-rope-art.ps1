#Requires -Version 7.0
# Generates the twisted red velvet rope texture used by the ceremonial rope barrier.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$textureDir = "$projectRoot\src\main\resources\assets\overprotocol\textures\block"
New-Item -ItemType Directory -Path $textureDir -Force | Out-Null

$palette = @('#C4283C', '#A8142A', '#A8142A', '#8A0F20', '#A8142A', '#6E0A18')
$bitmap = [System.Drawing.Bitmap]::new(16, 16)
try {
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $bitmap.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($palette[($x + $y) % 6]))
        }
    }
    $bitmap.Save("$textureDir\rope_red.png", [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $bitmap.Dispose() }

$preview = [System.Drawing.Bitmap]::new(128, 128)
$graphics = [System.Drawing.Graphics]::FromImage($preview)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$src = [System.Drawing.Bitmap]::new("$textureDir\rope_red.png")
try { $graphics.DrawImage($src, 0, 0, 128, 128) } finally { $src.Dispose(); $graphics.Dispose() }
New-Item -ItemType Directory -Path "$projectRoot\art" -Force | Out-Null
$preview.Save("$projectRoot\art\rope-texture-preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
Write-Output 'Generated rope_red.png'
