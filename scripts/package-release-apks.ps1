param(
    [string]$ApkSignerPath = ""
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$outputDirectory = Join-Path $projectRoot "dist\release"

function Resolve-ApkSigner {
    if ($ApkSignerPath) {
        if (-not (Test-Path -LiteralPath $ApkSignerPath)) {
            throw "apksigner tidak ditemukan: $ApkSignerPath"
        }
        return $ApkSignerPath
    }

    $sdkRoots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME) |
        Where-Object { $_ -and (Test-Path -LiteralPath $_) } |
        Select-Object -Unique

    foreach ($sdkRoot in $sdkRoots) {
        $buildToolsRoot = Join-Path $sdkRoot "build-tools"
        if (-not (Test-Path -LiteralPath $buildToolsRoot)) {
            continue
        }
        $candidate = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
            Sort-Object Name -Descending |
            ForEach-Object {
                $windowsPath = Join-Path $_.FullName "apksigner.bat"
                $unixPath = Join-Path $_.FullName "apksigner"
                if (Test-Path -LiteralPath $windowsPath) { $windowsPath }
                elseif (Test-Path -LiteralPath $unixPath) { $unixPath }
            } |
            Select-Object -First 1
        if ($candidate) {
            return $candidate
        }
    }

    throw "apksigner tidak ditemukan. Isi ANDROID_SDK_ROOT atau parameter -ApkSignerPath."
}

$packages = @(
    @{ Flavor = "retail"; Target = "CatatToko-Retail.apk" },
    @{ Flavor = "wholesale"; Target = "CatatToko-Grosir.apk" },
    @{ Flavor = "culinary"; Target = "CatatToko-Kuliner.apk" }
)

$validatedPackages = @()
foreach ($package in $packages) {
    $releaseDirectory = Join-Path $projectRoot "app\build\outputs\apk\$($package.Flavor)\release"
    $unsignedPath = Join-Path $releaseDirectory "app-$($package.Flavor)-release-unsigned.apk"
    $signedPath = Join-Path $releaseDirectory "app-$($package.Flavor)-release.apk"

    if (-not (Test-Path -LiteralPath $signedPath)) {
        if (Test-Path -LiteralPath $unsignedPath) {
            throw "APK $($package.Flavor) masih unsigned. Jalankan build dengan seluruh CATATTOKO_RELEASE_* terisi."
        }
        throw "APK release belum tersedia: $signedPath"
    }
    $validatedPackages += @{ Source = $signedPath; Target = $package.Target }
}

$apkSigner = Resolve-ApkSigner
foreach ($package in $validatedPackages) {
    $verification = (& $apkSigner verify --verbose --print-certs $package.Source 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw "Signature APK tidak valid: $($package.Source)`n$verification"
    }
    if ($verification -match "Android Debug") {
        throw "APK release memakai debug certificate dan diblokir: $($package.Source)"
    }
    $certificateDigests = @(
        [regex]::Matches(
            $verification,
            "(?im)certificate SHA-256 digest:\s*([0-9a-f]{64})\s*$"
        ) |
            ForEach-Object { $_.Groups[1].Value.ToUpperInvariant() } |
            Select-Object -Unique
    )
    if ($certificateDigests.Count -ne 1) {
        throw "Identitas certificate APK tidak tunggal atau tidak terbaca: $($package.Source)"
    }
    $package.CertificateSha256 = $certificateDigests[0]
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
foreach ($package in $validatedPackages) {
    $targetPath = Join-Path $outputDirectory $package.Target
    Copy-Item -LiteralPath $package.Source -Destination $targetPath -Force
    $hash = (Get-FileHash -LiteralPath $targetPath -Algorithm SHA256).Hash
    Write-Output "$($package.Target) | SIGNED | CERT_SHA256 $($package.CertificateSha256) | SHA256 $hash"
}
