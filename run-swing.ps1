param(
    [string]$QuestionBankPath
)

$ErrorActionPreference = 'Stop'
$buildDirectory = Join-Path ([System.IO.Path]::GetTempPath()) 'millionaire-mind-swing-ui-build'
New-Item -ItemType Directory -Force -Path $buildDirectory | Out-Null

$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Recurse -Filter '*.java' -File |
    Where-Object { $_.Name -ne 'ConsoleUI.java' } |
    Select-Object -ExpandProperty FullName)
& javac --release 17 -d $buildDirectory $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\Question Bank.csv') `
    -Destination (Join-Path $buildDirectory 'questions.csv') -Force

Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\bg.wav') `
    -Destination (Join-Path $buildDirectory 'bg.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\suspense.WAV') `
    -Destination (Join-Path $buildDirectory 'checkpoint-suspense.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\q12.WAV') `
    -Destination (Join-Path $buildDirectory 'gameplay-theme.wav') -Force

Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\splash.gif') `
    -Destination (Join-Path $buildDirectory 'splash.gif') -Force

Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\splash.wav') `
    -Destination (Join-Path $buildDirectory 'splash.wav') -Force

New-Item -ItemType Directory -Force -Path (Join-Path $buildDirectory 'sfx') | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\niera_sound_5.wav') `
    -Destination (Join-Path $buildDirectory 'sfx\niera_sound_5.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\niera_sound_2.wav') `
    -Destination (Join-Path $buildDirectory 'sfx\niera_sound_2.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\select_005.wav') `
    -Destination (Join-Path $buildDirectory 'sfx\select_005.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\confirmation_004.wav') `
    -Destination (Join-Path $buildDirectory 'sfx\confirmation_004.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\error_006.wav') `
    -Destination (Join-Path $buildDirectory 'sfx\error_006.wav') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\wrong.WAV') `
    -Destination (Join-Path $buildDirectory 'sfx\wrong.WAV') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\regular winning.WAV') `
    -Destination (Join-Path $buildDirectory 'sfx\regular winning.WAV') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\supreme victory.WAV') `
    -Destination (Join-Path $buildDirectory 'sfx\supreme victory.WAV') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'resources\sfx\Clapping Sound Effects.WAV') `
    -Destination (Join-Path $buildDirectory 'sfx\Clapping Sound Effects.WAV') -Force

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
