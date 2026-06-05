# Dump the UUMS PostgreSQL database to a timestamped SQL file.
# Requires pg_dump on PATH (ships with PostgreSQL).
#
# Usage:
#   .\scripts\dump-database.ps1
#   .\scripts\dump-database.ps1 -Format custom
#   $env:PGPASSWORD = "your-password"; .\scripts\dump-database.ps1

param(
    [string]$DbHost = "localhost",
    [int]$Port = 5432,
    [string]$Database = "uums_db",
    [string]$User = "postgres",
    [string]$OutputDir = "backups",
    [ValidateSet("plain", "custom")]
    [string]$Format = "plain"
)

$ErrorActionPreference = "Stop"

function Resolve-PgDump {
    $command = Get-Command pg_dump -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $versions = 18, 17, 16, 15, 14, 13
    foreach ($version in $versions) {
        $candidate = "C:\Program Files\PostgreSQL\$version\bin\pg_dump.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "pg_dump not found. Add PostgreSQL bin to PATH or install PostgreSQL."
}

$pgDump = Resolve-PgDump
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$extension = if ($Format -eq "custom") { "dump" } else { "sql" }
$outputFile = Join-Path $OutputDir "${Database}_${timestamp}.${extension}"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot
$outputPath = Join-Path $projectRoot $OutputDir

if (-not (Test-Path $outputPath)) {
    New-Item -ItemType Directory -Path $outputPath | Out-Null
}

$outputFile = Join-Path $outputPath "${Database}_${timestamp}.${extension}"

if (-not $env:PGPASSWORD) {
    $env:PGPASSWORD = Read-Host "Enter PostgreSQL password for user '$User'"
}

$pgDumpArgs = @(
    "-h", $DbHost,
    "-p", $Port,
    "-U", $User,
    "-d", $Database,
    "--no-owner",
    "--no-acl"
)

if ($Format -eq "custom") {
    $pgDumpArgs += @("-F", "c", "-f", $outputFile)
} else {
    $pgDumpArgs += @("-F", "p", "-f", $outputFile)
}

Write-Host "Dumping '$Database' from ${DbHost}:${Port} ..."

& $pgDump @pgDumpArgs

if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump failed with exit code $LASTEXITCODE"
}

$fileSizeKb = [math]::Round((Get-Item $outputFile).Length / 1KB, 1)
Write-Host "Done. Backup saved to:"
Write-Host "  $outputFile"
Write-Host "  Size: ${fileSizeKb} KB"
