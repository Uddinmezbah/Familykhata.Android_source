$ErrorActionPreference = "Stop"

$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repo

if (git status --porcelain) {
    throw "Working tree is not clean. Commit or intentionally preserve changes before recovery backup."
}

$branch = (git branch --show-current).Trim()
$sha = (git rev-parse HEAD).Trim()
$shortSha = (git rev-parse --short HEAD).Trim()
$remote = (git remote get-url origin).Trim()

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $HOME "Documents\HisabiKhata_Backups"
$backup = Join-Path $backupRoot $stamp

New-Item -ItemType Directory -Force -Path $backup | Out-Null

$bundle = Join-Path $backup "HisabiKhata-FULL-HISTORY-$shortSha.bundle"
$sourceZip = Join-Path $backup "HisabiKhata-SOURCE-$branch-$shortSha.zip"
$metadata = Join-Path $backup "BACKUP-INFO.txt"
$hashes = Join-Path $backup "SHA256.txt"

git fetch --all --prune
if ($LASTEXITCODE -ne 0) {
    throw "git fetch failed"
}

git bundle create $bundle --all
if ($LASTEXITCODE -ne 0) {
    throw "Git bundle creation failed"
}

git bundle verify $bundle
if ($LASTEXITCODE -ne 0) {
    throw "Git bundle verification failed"
}

git archive --format=zip --output=$sourceZip HEAD
if ($LASTEXITCODE -ne 0) {
    throw "Source ZIP creation failed"
}

@"
HISABI KHATA DEVELOPER RECOVERY BACKUP

Created: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Repository: $remote
Branch: $branch
Commit: $sha
Commit message: $(git log -1 --pretty=%s)

FULL HISTORY:
$(Split-Path $bundle -Leaf)

SOURCE SNAPSHOT:
$(Split-Path $sourceZip -Leaf)

IMPORTANT:
Signing keys, passwords, Play Console credentials and other secrets
must be backed up separately and must never be committed into Git.
"@ | Set-Content $metadata -Encoding UTF8

Get-FileHash $bundle -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash)  $(Split-Path $_.Path -Leaf)" } |
    Set-Content $hashes

Get-FileHash $sourceZip -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash)  $(Split-Path $_.Path -Leaf)" } |
    Add-Content $hashes

Write-Host ""
Write-Host "=== HISABI KHATA RECOVERY BACKUP COMPLETE ===" -ForegroundColor Green
Write-Host "Location: $backup"
Write-Host "Branch:   $branch"
Write-Host "Commit:   $sha"
Get-ChildItem $backup | Select-Object Name,Length
