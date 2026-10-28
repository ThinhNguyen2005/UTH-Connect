# Fallback bootstrap when the standard official gradle-wrapper.jar is not bundled.
# Requires internet on first launch and downloads an official Gradle distribution.
$ErrorActionPreference = 'Stop'
$version = '8.11.1'
$cache = Join-Path $env:USERPROFILE ".gradle\uth-connect-bootstrap"
$gradle = Join-Path $cache "gradle-$version\bin\gradle.bat"
if (-not (Test-Path $gradle)) {
    New-Item -Path $cache -ItemType Directory -Force | Out-Null
    $zip = Join-Path $cache "gradle-$version-bin.zip"
    Write-Host "Downloading Gradle $version from services.gradle.org..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $cache -Force
    Remove-Item $zip -Force
}
& $gradle @args
exit $LASTEXITCODE
