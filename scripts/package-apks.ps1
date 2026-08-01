$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot "dist\debug"

$packages = @(
    @{
        Source = "app\build\outputs\apk\retail\debug\app-retail-debug.apk"
        Target = "CatatToko-Retail.apk"
    },
    @{
        Source = "app\build\outputs\apk\wholesale\debug\app-wholesale-debug.apk"
        Target = "CatatToko-Grosir.apk"
    },
    @{
        Source = "app\build\outputs\apk\culinary\debug\app-culinary-debug.apk"
        Target = "CatatToko-Kuliner.apk"
    }
)

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

foreach ($package in $packages) {
    $sourcePath = Join-Path $projectRoot $package.Source
    $targetPath = Join-Path $outputDirectory $package.Target

    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "APK belum tersedia: $sourcePath"
    }

    Copy-Item -LiteralPath $sourcePath -Destination $targetPath -Force
    $hash = (Get-FileHash -LiteralPath $targetPath -Algorithm SHA256).Hash
    Write-Output "$($package.Target) | SHA256 $hash"
}
