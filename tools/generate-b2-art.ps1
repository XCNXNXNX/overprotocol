# Original icon and neutral mesh palette. Geometry remains editable in client/B2Mesh.java.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetDir = Join-Path (Split-Path -Parent $PSScriptRoot) 'src/main/resources/assets/overprotocol/textures'
New-Item -ItemType Directory -Force -Path (Join-Path $assetDir 'entity'),(Join-Path $assetDir 'item') | Out-Null
$palette = [Drawing.Bitmap]::new(4,4)
for ($x=0; $x -lt 4; $x++) { for ($y=0; $y -lt 4; $y++) { $palette.SetPixel($x,$y,[Drawing.Color]::White) } }
$palette.Save((Join-Path $assetDir 'entity/b2_palette.png'), [Drawing.Imaging.ImageFormat]::Png)
$palette.Dispose()
$sprite = [Drawing.Bitmap]::new(64,64)
$canvas = [Drawing.Graphics]::FromImage($sprite)
$canvas.Clear([Drawing.Color]::Transparent)
$outline = @(@(0,-10.45),@(26.06,7.85),@(22.41,10.45),@(13.07,3.46),@(6.4,8.38),@(3.9,6.58),@(0,9.2),@(-3.9,6.58),@(-6.4,8.38),@(-13.07,3.46),@(-22.41,10.45),@(-26.06,7.85))
$points = [Drawing.Point[]]@($outline | ForEach-Object { [Drawing.Point]::new([int](32+$_[0]*1.12),[int](32+$_[1]*1.12)) })
$skin = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(255,88,97,108))
$panel = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(255,105,115,127))
$glass = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(255,35,66,84))
$pen = [Drawing.Pen]::new([Drawing.Color]::FromArgb(255,32,39,48),1)
$canvas.FillPolygon($skin,$points); $canvas.DrawPolygon($pen,$points)
$canvas.FillEllipse($panel,30,23,4,16)
$canvas.FillRectangle($glass,31,23,2,2)
$canvas.FillEllipse($panel,26,27,3,10); $canvas.FillEllipse($panel,35,27,3,10)
$canvas.FillRectangle($glass,26,27,3,1); $canvas.FillRectangle($glass,35,27,3,1)
$canvas.FillRectangle($glass,26,38,3,2); $canvas.FillRectangle($glass,35,38,3,2)
$sprite.Save((Join-Path $assetDir 'item/b2_spirit.png'),[Drawing.Imaging.ImageFormat]::Png)
$canvas.Dispose(); $sprite.Dispose(); $skin.Dispose(); $panel.Dispose(); $glass.Dispose(); $pen.Dispose()
