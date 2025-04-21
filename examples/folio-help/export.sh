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
# Note: Adjust 'folioxml-test' if you use a different image tag or name (e.g., imazen/folioxml)
docker run --rm -v "$EXAMPLE_DIR:/data" folioxml-test -config $CONFIG_PATH_CONTAINER -export folio_help

echo "Docker command finished." 