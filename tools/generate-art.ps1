#Requires -Version 7.0
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$pattern = Get-Content -LiteralPath "$projectRoot\art\carpet-pattern.json" -Raw | ConvertFrom-Json -AsHashtable
$colors = Get-Content -LiteralPath "$projectRoot\art\dye-palette.json" -Raw | ConvertFrom-Json -AsHashtable
$textureDir = "$projectRoot\src\main\resources\assets\overprotocol\textures\block"
New-Item -ItemType Directory -Path $textureDir -Force | Out-Null
function Shade([System.Drawing.Color]$color, [double]$factor) {
    [System.Drawing.Color]::FromArgb([int][Math]::Clamp($color.R * $factor,0,255),
        [int][Math]::Clamp($color.G * $factor,0,255), [int][Math]::Clamp($color.B * $factor,0,255))
}
foreach ($name in $colors.Keys) {
    $base = [System.Drawing.ColorTranslator]::FromHtml($colors[$name])
    foreach ($kind in @('carpet','cloth','skirt')) {
        $bitmap = [System.Drawing.Bitmap]::new(16,16)
        try {
            for ($y=0; $y -lt 16; $y++) {
                for ($x=0; $x -lt 16; $x++) {
                    if ($kind -eq 'carpet') {
                        $symbol = [string]$pattern.pixels[$y][$x]
                        if ($name -eq 'red' -or $symbol -in @('g','G','s')) {
                            $pixel = [System.Drawing.ColorTranslator]::FromHtml($pattern.palette[$symbol])
                        } else {
                            $factor = switch -CaseSensitive ($symbol) { 'r' {1.0}; 'R' {1.06}; 'd' {0.86} }
                            $pixel = Shade $base $factor
                        }
                    } elseif ($kind -eq 'cloth') {
                        $factor = if (($x+$y)%2 -eq 0) {1.02} else {0.98}
                        $pixel = Shade $base $factor
                    } else {
                        $factor = @(0.80,0.90,1.04,1.06)[$x%4]
                        if ($y -eq 12) { $factor *= 0.90 }
                        $pixel = Shade $base $factor
                    }
                    $bitmap.SetPixel($x,$y,$pixel)
                }
            }
            $filename = if ($kind -eq 'carpet') { if ($name -eq 'red') {'ceremonial_carpet'} else {"${name}_ceremonial_carpet"} } else {"${kind}_${name}"}
            $bitmap.Save("$textureDir\$filename.png",[System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $bitmap.Dispose() }
    }
}
Write-Output 'Generated 16 carpet palettes, 16 cloth surfaces and 16 perimeter skirts.'
