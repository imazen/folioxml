# Get the directory where the script is located
$ScriptDir = $PSScriptRoot
$ExampleDir = $ScriptDir # examples/folio-help
$ConfigPathHost = Join-Path -Path $ExampleDir -ChildPath "config.yaml"
$ExportDirHost = Join-Path -Path $ExampleDir -ChildPath "export"
$IndexDirHost = Join-Path -Path $ExampleDir -ChildPath "index"

# Define config path inside the container (relative to the /data mount point)
$ConfigPathContainer = "/data/config.yaml"

# Create output directories on the host if they don't exist
if (-not (Test-Path -Path $ExportDirHost)) {
    New-Item -ItemType Directory -Path $ExportDirHost | Out-Null
    Write-Host "Created directory: $ExportDirHost"
}
if (-not (Test-Path -Path $IndexDirHost)) {
    New-Item -ItemType Directory -Path $IndexDirHost | Out-Null
    Write-Host "Created directory: $IndexDirHost"
}

# --- Run Docker Command ---
Write-Host "Running Docker container..."

# Mount the entire examples/folio-help directory to /data in the container
# Assumes config.yaml exists in ExampleDir
# Note: Adjust 'folioxml-test' or 'imazen/folioxml' if you use a different image tag or name
# Use `cmd /c` to ensure Docker command runs correctly, especially with volume paths on Windows
cmd /c "docker run --rm -v ""$ExampleDir`:/data"" imazen/folioxml:latest -config $ConfigPathContainer -export folio_help"

$ExitCode = $LASTEXITCODE
Write-Host "Docker command finished with exit code $ExitCode."

# --- Find latest log file --- 
$LatestExportDir = Get-ChildItem -Path $ExportDirHost -Directory | Sort-Object -Property LastWriteTime -Descending | Select-Object -First 1

if ($LatestExportDir -ne $null) {
    $LogFile = Join-Path -Path $LatestExportDir.FullName -ChildPath "log.txt"
    if (Test-Path -Path $LogFile -PathType Leaf) {
        Write-Host "Export process finished. Check the log file for details or errors:"
        Write-Host "  Get-Content -Path ""$LogFile"""
        # Optionally display the last few lines:
        # Get-Content -Path $LogFile -Tail 5
    } else {
        Write-Host "Export process finished. Could not find log file at expected location: $LogFile"
    }
} else {
    Write-Host "Export process finished. Could not determine the latest export directory in $ExportDirHost to find the log file."
}

# Exit with the same code as the docker command
exit $ExitCode 