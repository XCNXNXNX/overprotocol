param(
    [switch]$Online,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs = @('build')
)
$ErrorActionPreference = 'Stop'
$jdkPath = if ($env:OVERPROTOCOL_JAVA_HOME) { $env:OVERPROTOCOL_JAVA_HOME } else { 'C:\Program Files\Java\latest\jdk-21' }
if (-not (Test-Path -LiteralPath "$jdkPath\bin\java.exe")) {
    throw 'Set OVERPROTOCOL_JAVA_HOME to a Java 21 JDK directory.'
}
$previousJavaHome = $env:JAVA_HOME
$effectiveArgs = @($GradleArgs)
if (-not $Online -and $effectiveArgs -notcontains '--offline') {
    $effectiveArgs += '--offline'
}
try {
    $env:JAVA_HOME = $jdkPath
    Push-Location (Split-Path -Parent $PSScriptRoot)
    try {
        & .\gradlew.bat @effectiveArgs --console=plain
        $resultCode = $LASTEXITCODE
    } finally { Pop-Location }
} finally { $env:JAVA_HOME = $previousJavaHome }
exit $resultCode
