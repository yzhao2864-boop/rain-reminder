param([string]$Abi = 'all', [string]$SdkRoot = '', [string]$KeyStore = '', [string]$Manifest = "$PSScriptRoot/AndroidManifest.xml", [string]$ExtraSource = '', [string]$OutputApk = '')
$ErrorActionPreference='Stop'
if(!$SdkRoot){$SdkRoot=$env:ANDROID_SDK_ROOT}
if(!$SdkRoot){$SdkRoot=$env:ANDROID_HOME}
if(!$SdkRoot){throw 'Set ANDROID_SDK_ROOT or pass -SdkRoot.'}
$localRoot=Join-Path $PSScriptRoot '../.local'
if(!$KeyStore){$KeyStore=Join-Path $localRoot 'development.keystore'}
if(!$OutputApk){$OutputApk=Join-Path $localRoot 'RainReminder-0.9.36-test.apk'}
New-Item -ItemType Directory -Force (Split-Path $KeyStore -Parent),(Split-Path $OutputApk -Parent) | Out-Null
$SdkRoot=(Resolve-Path $SdkRoot).Path
$tools=Join-Path $SdkRoot 'build-tools/35.0.0'
$android=Join-Path $SdkRoot 'platforms/android-35/android.jar'
$build=Join-Path $localRoot ('build/'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force "$build/classes","$build/dex","$build/gen" | Out-Null
function Check {if($LASTEXITCODE -ne 0){throw "Build command failed: $LASTEXITCODE"}}
& "$tools/aapt2.exe" compile --dir "$PSScriptRoot/res" -o "$build/resources.zip"; Check
& "$tools/aapt2.exe" link --custom-package cn.local.rainreminder -o "$build/base.apk" --manifest $Manifest -I $android --java "$build/gen" -A "$PSScriptRoot/assets" "$build/resources.zip"; Check
$sources=@(Get-ChildItem "$PSScriptRoot/src","$build/gen" -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
$libraries=@(Get-ChildItem "$PSScriptRoot/libs" -Filter '*.jar' -ErrorAction SilentlyContinue | ForEach-Object {$_.FullName})
if($ExtraSource){$sources+= (Resolve-Path $ExtraSource).Path}
& javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$android;$tools/core-lambda-stubs.jar" -classpath ($libraries -join ';') -d "$build/classes" $sources; Check
& jar "-J-Djava.io.tmpdir=$build" cf "$build/classes.jar" -C "$build/classes" .; Check
& "$tools/d8.bat" --lib $android --min-api 28 --output "$build/dex" "$build/classes.jar" $libraries; Check
Copy-Item "$build/base.apk" "$build/unsigned.apk" -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$build/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build/dex/classes.dex",'classes.dex') | Out-Null
 foreach($entry in @($zip.Entries | Where-Object {$_.FullName.Contains('\')})){
  $name=$entry.FullName.Replace('\','/');$memory=[IO.MemoryStream]::new();$inputStream=$entry.Open();$inputStream.CopyTo($memory);$inputStream.Dispose();$entry.Delete();$replacement=$zip.CreateEntry($name);$outputStream=$replacement.Open();$memory.Position=0;$memory.CopyTo($outputStream);$outputStream.Dispose();$memory.Dispose()
 }
 foreach($file in (Get-ChildItem "$PSScriptRoot/native" -Recurse -Filter '*.so' -ErrorAction SilentlyContinue)){
  if($Abi -ne 'all' -and $file.Directory.Name -ne $Abi){continue}
  $entry='lib/'+$file.Directory.Name+'/'+$file.Name
  [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,$entry) | Out-Null
 }
}finally{$zip.Dispose()}
& "$tools/zipalign.exe" -f -p 4 "$build/unsigned.apk" "$build/aligned.apk"; Check
if(!(Test-Path $KeyStore)){
 & keytool -genkeypair -keystore $KeyStore -storepass android -keypass android -alias rain-test -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Rain Reminder Local Test'; Check
}
& "$tools/apksigner.bat" sign --ks $KeyStore --ks-key-alias rain-test --ks-pass pass:android --key-pass pass:android --out $OutputApk "$build/aligned.apk"; Check
& "$tools/apksigner.bat" verify --verbose $OutputApk; Check
Write-Output 'Build and APK signature verification passed.'
