param([string]$Notes='版本更新', [string]$Apk='', [string]$Output='')
$ErrorActionPreference='Stop'
[xml]$manifest=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'AndroidManifest.xml') -Raw
$releaseVersion=$manifest.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
if(!$Apk){$Apk=Join-Path $PSScriptRoot "../RainReminder-$releaseVersion-test.apk"}
if(!$Output){$Output=Join-Path $PSScriptRoot '../app-update.json'}
$packageFile=Get-Item -LiteralPath $Apk
$packageHash=(Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash.ToLowerInvariant()
$asset=[ordered]@{name="RainReminder-$releaseVersion-test.apk";browser_download_url="https://github.com/yzhao2864-boop/rain-reminder/releases/download/v$releaseVersion/RainReminder-$releaseVersion-test.apk";size=$packageFile.Length;digest="sha256:$packageHash"}
$release=[ordered]@{tag_name="v$releaseVersion";body=$Notes;assets=@($asset)}
$metadataText=[ordered]@{schema=1;releases=@($release)} | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($Output,$metadataText,[Text.UTF8Encoding]::new($false))
Write-Output "Prepared update metadata: $Output"
