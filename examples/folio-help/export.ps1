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
# Note: Adjust 'folioxml-test' if you use a different image tag or name (e.g., imazen/folioxml)
# Use `cmd /c` to ensure Docker command runs correctly, especially with volume paths on Windows
cmd /c "docker run --rm -v ""$ExampleDir`:/data"" folioxml-test -config $ConfigPathContainer -export folio_help"


Write-Host "Docker command finished." 