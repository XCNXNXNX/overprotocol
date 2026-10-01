$ErrorActionPreference = 'Stop'
# Rebuild from the selected image_gen originals. The legacy checkerboard generator is retired.
$pythonPath = if ($env:OVERPROTOCOL_PYTHON) { $env:OVERPROTOCOL_PYTHON } elseif (Test-Path -LiteralPath 'C:\Python\Python314\python.exe') { 'C:\Python\Python314\python.exe' } else { 'python' }
& $pythonPath (Join-Path $PSScriptRoot 'prepare-materials.py')
if ($LASTEXITCODE -ne 0) { throw 'Could not rebuild material textures; Python and Pillow are required.' }
