#Requires -Version 7.0
# Draws the default honour guard player skin (64x64, vanilla player layout) plus the 16x16 item
# icon.  Only the hat overlay, the tunic, the belt and the boots differ from a plain player skin,
# so a real player skin can be dropped in at runtime and will look correct.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$entityDir = "$projectRoot\src\main\resources\assets\overprotocol\textures\entity"
$itemDir = "$projectRoot\src\main\resources\assets\overprotocol\textures\item"
New-Item -ItemType Directory -Path $entityDir, $itemDir -Force | Out-Null

$navy = '#26314F'; $navyDark = '#1B2340'; $navyLite = '#31406A'
$trouser = '#1A2340'; $trouserDark = '#131A30'
$gold = '#C9A227'; $goldLite = '#F0D264'; $goldDark = '#8A6C14'
$skin = '#E2AC7C'; $skinDark = '#C58C5A'
$hair = '#2A2018'; $black = '#141418'; $blackLite = '#22222A'
$white = '#E9E9E1'; $whiteDark = '#C9C9BF'
$visor = '#101014'

$script:cache = @{}
function C([string]$hex) {
    if (-not $script:cache.ContainsKey($hex)) { $script:cache[$hex] = [System.Drawing.ColorTranslator]::FromHtml($hex) }
    return $script:cache[$hex]
}
# Paints a w x h block at (x0,y0); the painter returns a hex colour or $null to leave the pixel alone.
function Paint($bmp, [int]$x0, [int]$y0, [int]$w, [int]$h, [scriptblock]$fn) {
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $hex = & $fn $x $y
            if ($hex) { $bmp.SetPixel($x0 + $x, $y0 + $y, (C $hex)) }
        }
    }
}

$skinBmp = [System.Drawing.Bitmap]::new(64, 64)
try {
    # ---- head: 8x8x8, front is the face -------------------------------------
    Paint $skinBmp 8 8 8 8 {
        param($x, $y)
        if ($y -eq 0) { return $hair }
        if ($y -eq 1) { return $hair }
        if ($y -eq 2) { if ($x -le 0 -or $x -ge 7) { return $hair }; return $skin }
        if ($y -eq 3) {
            if ($x -eq 1) { return $whiteDark }; if ($x -eq 2) { return '#2B2B33' }
            if ($x -eq 5) { return '#2B2B33' };   if ($x -eq 6) { return $whiteDark }
            return $skin
        }
        if ($y -eq 4) { if ($x -eq 4) { return $skinDark }; return $skin }
        if ($y -eq 5) { return $skin }
        if ($y -eq 6) { if ($x -eq 3 -or $x -eq 4) { return '#8A5442' }; return $skin }
        return $skinDark
    }
    Paint $skinBmp 0 8 8 8 {   # right side of the head
        param($x, $y)
        if ($y -le 1) { return $hair }
        if ($x -eq 0 -and $y -le 4) { return $hair }
        return $skin
    }
    Paint $skinBmp 16 8 8 8 { param($x, $y) if ($y -le 1) { return $hair }; if ($x -eq 7 -and $y -le 4) { return $hair }; return $skin }
    Paint $skinBmp 24 8 8 8 { param($x, $y) if ($y -le 3) { return $hair }; return $skinDark }
    Paint $skinBmp 8 0 8 8 { $hair }
    Paint $skinBmp 16 0 8 8 { $skinDark }

    # ---- hat overlay: peaked cap on the top two rows, gold band under it -----
    # rows 0-1 crown, row 2 gold band, row 3 the black visor the peaked cap is known for
    $capSide = { param($x, $y) if ($y -le 1) { return $navy }; if ($y -eq 2) { return $gold }; if ($y -eq 3) { return $visor }; return $null }
    Paint $skinBmp 40 8 8 8 $capSide
    Paint $skinBmp 32 8 8 8 $capSide
    Paint $skinBmp 48 8 8 8 $capSide
    Paint $skinBmp 56 8 8 8 { param($x, $y) if ($y -le 1) { return $navy }; if ($y -eq 2) { return $goldDark }; if ($y -eq 3) { return $visor }; return $null }
    Paint $skinBmp 40 0 8 8 { param($x, $y) if ($x -eq 0 -or $x -eq 7 -or $y -eq 0 -or $y -eq 7) { return $navyDark }; return $navy }

    # ---- body: double breasted tunic, white belt ----------------------------
    $tunic = {
        param($x, $y)
        if ($y -eq 0) { return $gold }
        if ($y -ge 8 -and $y -le 9) { return $white }
        if ($y -ge 10) { return $navyDark }
        if ($x -eq 2 -or $x -eq 5) { if ($y % 2 -eq 0) { return $goldLite }; return $gold }
        if ($x -eq 3 -or $x -eq 4) { return $navyDark }
        return $navy
    }
    Paint $skinBmp 20 20 8 12 $tunic
    Paint $skinBmp 32 20 8 12 { param($x, $y) if ($y -eq 0) { return $gold }; if ($y -ge 8 -and $y -le 9) { return $whiteDark }; if ($y -ge 10) { return $navyDark }; if ($x -eq 3 -or $x -eq 4) { return $navyDark }; return $navy }
    Paint $skinBmp 16 20 4 12 { param($x, $y) if ($y -eq 0) { return $gold }; if ($y -ge 8 -and $y -le 9) { return $white }; if ($y -ge 10) { return $navyDark }; return $navy }
    Paint $skinBmp 28 20 4 12 { param($x, $y) if ($y -eq 0) { return $gold }; if ($y -ge 8 -and $y -le 9) { return $white }; if ($y -ge 10) { return $navyDark }; return $navy }
    Paint $skinBmp 20 16 8 4 { $gold }
    Paint $skinBmp 28 16 8 4 { $navyDark }

    # ---- arms: sleeve, gold cuff, white glove -------------------------------
    $sleeve = {
        param($x, $y)
        if ($y -eq 0) { return $gold }
        if ($y -eq 9) { return $gold }
        if ($y -ge 10) { return $white }
        if ($x -eq 1) { return $navyLite }
        return $navy
    }
    foreach ($ox in 40, 44, 48, 52) { Paint $skinBmp $ox 20 4 12 $sleeve }
    foreach ($ox in 32, 36, 40, 44) { Paint $skinBmp $ox 52 4 12 $sleeve }
    Paint $skinBmp 44 16 4 4 { $gold }
    Paint $skinBmp 48 16 4 4 { $goldDark }
    Paint $skinBmp 36 48 4 4 { $gold }
    Paint $skinBmp 40 48 4 4 { $goldDark }

    # ---- legs: trousers, gold seam, boots -----------------------------------
    $legFront = {
        param($x, $y)
        if ($y -le 7) { return $trouser }
        if ($y -eq 8) { return $gold }
        if ($y -eq 9) { return $blackLite }
        return $black
    }
    Paint $skinBmp 4 20 4 12 $legFront
    Paint $skinBmp 20 52 4 12 $legFront
    Paint $skinBmp 8 20 4 12 { param($x, $y) if ($y -le 7) { return $trouserDark }; if ($y -eq 8) { return $goldDark }; return $black }
    Paint $skinBmp 24 52 4 12 { param($x, $y) if ($y -le 7) { return $trouserDark }; if ($y -eq 8) { return $goldDark }; return $black }
    Paint $skinBmp 12 20 4 12 { param($x, $y) if ($y -le 7) { return $trouserDark }; if ($y -eq 8) { return $goldDark }; return $black }
    Paint $skinBmp 28 52 4 12 { param($x, $y) if ($y -le 7) { return $trouserDark }; if ($y -eq 8) { return $goldDark }; return $black }
    Paint $skinBmp 0 20 4 12 { param($x, $y) if ($y -le 7) { if ($x -eq 1) { return $gold }; return $trouser }; if ($y -eq 8) { return $goldDark }; return $black }
    Paint $skinBmp 24 52 4 12 { param($x, $y) if ($y -le 7) { if ($x -eq 2) { return $gold }; return $trouser }; if ($y -eq 8) { return $goldDark }; return $black }
    Paint $skinBmp 4 16 4 4 { $trouser }
    Paint $skinBmp 8 16 4 4 { $black }
    Paint $skinBmp 20 48 4 4 { $trouser }
    Paint $skinBmp 24 48 4 4 { $black }

    $skinBmp.Save("$entityDir\honor_guard.png", [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $skinBmp.Dispose() }

# ---- 16x16 inventory icon ---------------------------------------------------
$icon = [System.Drawing.Bitmap]::new(16, 16)
try {
    Paint $icon 0 0 16 16 { $null }
    Paint $icon 5 1 6 6 { param($x, $y) if ($y -le 1) { return $navy }; if ($y -eq 2) { return $gold }; if ($y -eq 3 -and ($x -eq 1 -or $x -eq 4)) { return '#2B2B33' }; return $skin }
    Paint $icon 7 7 2 5 { param($x, $y) if ($y -le 0) { return $gold }; if ($y -ge 3) { return $white }; return $navy }
    Paint $icon 4 7 3 5 { param($x, $y) if ($y -le 0) { return $gold }; if ($y -ge 3) { return $white }; return $navy }
    Paint $icon 9 7 3 5 { param($x, $y) if ($y -le 0) { return $gold }; if ($y -ge 3) { return $white }; return $navy }
    Paint $icon 5 12 3 4 { param($x, $y) if ($y -le 1) { return $trouser }; return $black }
    Paint $icon 8 12 3 4 { param($x, $y) if ($y -le 1) { return $trouser }; return $black }
    $icon.Save("$itemDir\honor_guard.png", [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $icon.Dispose() }

# 16x16 particle sprite: breaking the statue scatters terrain particles from this texture
$blockDir = "$projectRoot\src\main\resources\assets\overprotocol\textures\block"
$particle = [System.Drawing.Bitmap]::new(16, 16)
try {
    Paint $particle 0 0 16 16 {
        param($x, $y)
        if (($x + $y) % 4 -eq 0) { return $navyDark }
        if (($x * 3 + $y) % 11 -eq 0) { return $gold }
        if (($x + $y) % 7 -eq 0) { return '#1B2340' }
        return $navy
    }
    $particle.Save("$blockDir\honor_guard_particle.png", [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $particle.Dispose() }

$preview = [System.Drawing.Bitmap]::new(256, 256)
$graphics = [System.Drawing.Graphics]::FromImage($preview)
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$src = [System.Drawing.Bitmap]::new("$entityDir\honor_guard.png")
try { $graphics.DrawImage($src, 0, 0, 256, 256) } finally { $src.Dispose(); $graphics.Dispose() }
$preview.Save("$projectRoot\art\guard-skin-preview.png", [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
Write-Output 'Generated the guard skin (64x64) and the item icon (16x16)'