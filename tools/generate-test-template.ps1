$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDir = "$projectRoot\src\main\resources\data\overprotocol\structure"
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
$buffer = [System.IO.MemoryStream]::new()
$writer = [System.IO.BinaryWriter]::new($buffer)
function Write-Int([int]$value) {
    $bytes = [BitConverter]::GetBytes($value)
    if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($bytes) }
    $writer.Write($bytes)
}
function Write-String([string]$value) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($value)
    $writer.Write([byte]($bytes.Length -shr 8))
    $writer.Write([byte]($bytes.Length -band 255))
    $writer.Write($bytes)
}
function Write-Tag([byte]$type, [string]$name) {
    $writer.Write($type)
    Write-String $name
}
try {
    Write-Tag 10 ''
    Write-Tag 3 'DataVersion'; Write-Int 3955
    Write-Tag 9 'size'; $writer.Write([byte]3); Write-Int 3
    Write-Int 5; Write-Int 4; Write-Int 5
    Write-Tag 9 'palette'; $writer.Write([byte]10); Write-Int 1
    Write-Tag 8 'Name'; Write-String 'minecraft:air'; $writer.Write([byte]0)
    Write-Tag 9 'blocks'; $writer.Write([byte]10); Write-Int 0
    Write-Tag 9 'entities'; $writer.Write([byte]10); Write-Int 0
    $writer.Write([byte]0)
    $writer.Flush()
    $file = [System.IO.File]::Create("$targetDir\test_empty.nbt")
    $gzip = [System.IO.Compression.GZipStream]::new($file, [System.IO.Compression.CompressionLevel]::Optimal)
    try { $gzip.Write($buffer.ToArray(), 0, [int]$buffer.Length) }
    finally { $gzip.Dispose(); $file.Dispose() }
} finally { $writer.Dispose(); $buffer.Dispose() }
Write-Output 'Generated a 5 x 4 x 5 empty GameTest structure.'
