param(
    [string]$QuestionBankPath
)

$ErrorActionPreference = 'Stop'
$buildDirectory = Join-Path ([System.IO.Path]::GetTempPath()) 'millionaire-mind-swing-ui-build'
New-Item -ItemType Directory -Force -Path $buildDirectory | Out-Null

$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' -File |
    Where-Object { $_.Name -ne 'ConsoleUI.java' } |
    Select-Object -ExpandProperty FullName)
& javac --release 17 -d $buildDirectory $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\Question Bank.csv') `
    -Destination (Join-Path $buildDirectory 'questions.csv') -Force

Push-Location -LiteralPath $PSScriptRoot
try {
    if ($QuestionBankPath) {
        & java -cp $buildDirectory millionairemind.SwingUI $QuestionBankPath
    } else {
        & java -cp $buildDirectory millionairemind.SwingUI
    }
    exit $LASTEXITCODE
} finally {
    Pop-Location
}