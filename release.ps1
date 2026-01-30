$versionFile = "version.json"
$gradleFile = "app/build.gradle.kts"

# 1. Read current version from version.json
$json = Get-Content $versionFile | Out-String | ConvertFrom-Json
$oldVersionCode = $json.versionCode
$newVersionCode = $oldVersionCode + 1
$newVersionName = "1.$newVersionCode"

Write-Host "Upgrading from Version $oldVersionCode to $newVersionCode..."

# 2. Update version.json
$json.versionCode = $newVersionCode
$json.versionName = $newVersionName
$json | ConvertTo-Json | Set-Content $versionFile

# 3. Update build.gradle.kts
$gradleContent = Get-Content $gradleFile
$gradleContent = $gradleContent -replace "versionCode = $oldVersionCode", "versionCode = $newVersionCode"
$gradleContent = $gradleContent -replace "versionName = `"$($json.versionName)`"", "versionName = `"$newVersionName`"" # Simple check, might need refinement if strings vary
$gradleContent | Set-Content $gradleFile

# 4. Build Release
Write-Host "Building Release APK..."
./gradlew assembleRelease

Write-Host "Build Complete!"
Write-Host "New Version: $newVersionName (Code: $newVersionCode)"
Write-Host "DONT FORGET: Git Push your changes so the app can see the new version.json!"
