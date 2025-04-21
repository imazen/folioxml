#!/bin/bash

# Get the absolute path to the directory containing this script
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
EXAMPLE_DIR="$SCRIPT_DIR" # examples/folio-help
CONFIG_PATH_HOST="$EXAMPLE_DIR/config.yaml"
EXPORT_DIR_HOST="$EXAMPLE_DIR/export"
INDEX_DIR_HOST="$EXAMPLE_DIR/index"

# Define config path inside the container (relative to the /data mount point)
CONFIG_PATH_CONTAINER="/data/config.yaml"

# Create output directories on the host if they don't exist
mkdir -p "$EXPORT_DIR_HOST"
mkdir -p "$INDEX_DIR_HOST"

# --- Run Docker Command ---
echo "Running Docker container..."

# Mount the entire examples/folio-help directory to /data in the container
# Assumes config.yaml exists in EXAMPLE_DIR
# Note: Adjust 'folioxml-test' or 'imazen/folioxml' if you use a different image tag or name
docker run --rm -v "$EXAMPLE_DIR:/data" imazen/folioxml:latest -config $CONFIG_PATH_CONTAINER -export folio_help

EXIT_CODE=$?
echo "Docker command finished with exit code $EXIT_CODE."

# --- Find latest log file --- 
LATEST_EXPORT_DIR=$(ls -td -- "$EXPORT_DIR_HOST"/*/ | head -n 1)

if [[ -d "$LATEST_EXPORT_DIR" ]]; then
    LOG_FILE="$LATEST_EXPORT_DIR/log.txt"
    if [[ -f "$LOG_FILE" ]]; then
        echo "Export process finished. Check the log file for details or errors:"
        echo "  cat \"$LOG_FILE\""
    else
        echo "Export process finished. Could not find log file at expected location: $LOG_FILE"
    fi
else
    echo "Export process finished. Could not determine the latest export directory in $EXPORT_DIR_HOST to find the log file."
fi

exit $EXIT_CODE 