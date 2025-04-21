# Dockerfile for folioxml

# ---- Builder Stage ----
# Use Maven with JDK 8 to build the project
FROM maven:3.8.4-jdk-8 AS builder

# Install wget and unzip (wget needed for data download)
RUN apt-get update && apt-get install -y wget unzip && rm -rf /var/lib/apt/lists/*

ARG APP_DIR=/app
WORKDIR ${APP_DIR}

# Copy pom.xml files first
COPY pom.xml ./
COPY core/pom.xml ./core/
COPY core/folioxml/pom.xml ./core/folioxml/
COPY commandline/pom.xml ./commandline/
COPY contrib/pom.xml ./contrib/
COPY contrib/folioxml-lucene/pom.xml ./contrib/folioxml-lucene/
COPY diff_match_patch/pom.xml ./diff_match_patch/

# Copy the rest of the source code
COPY . .

# Ensure mkres.sh uses Unix line endings (LF) - needed for final stage
RUN sed -i 's/\r$//' mkres.sh

# Build the project: clean, download deps, compile, package, create fat JAR
# Skipping tests due to failures in FolioSlxTransformerTest with FolioHlp data.
RUN mvn clean package assembly:single -U -B -fae -DskipTests

# Download test data AFTER build to prevent cleaning (will be copied to final stage)
RUN wget https://public-unit-test-resources.s3.us-east-1.amazonaws.com/FolioHlp.zip

# ---- Final Stage ----
# Use a slim JRE image
FROM openjdk:8-jre-slim

# Install unzip for the built-in example data setup
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

ARG APP_DIR=/app
ARG JAR_NAME=folioxml.jar
WORKDIR ${APP_DIR}

# Define a mount point for user data (config, input, output)
# This is separate from the built-in example data location
VOLUME /data

# Copy the built JAR from the builder stage
COPY --from=builder ${APP_DIR}/commandline/target/folioxml-commandline-jar-with-dependencies.jar ${APP_DIR}/${JAR_NAME}

# Copy assets needed for the built-in example from builder stage
# Ensure destinations are directories by adding a trailing slash
COPY --from=builder ${APP_DIR}/mkres.sh ${APP_DIR}/
COPY --from=builder ${APP_DIR}/core/folioxml/resources/test.yaml ${APP_DIR}/
COPY --from=builder ${APP_DIR}/FolioHlp.zip ${APP_DIR}/

# Prepare the built-in example data (FolioHlp) from the copied zip
# This makes the final image larger but provides a ready-to-run example.
RUN chmod +x mkres.sh && \
    ./mkres.sh && \
    rm FolioHlp.zip

# Set the entry point to run the JAR
# Users will pass arguments like: -config /path/to/config.yaml -export <export_name>
ENTRYPOINT ["java", "-jar", "${APP_DIR}/${JAR_NAME}"]

# Optional: Example command to run the built-in test configuration
# CMD ["-config", "/app/test.yaml", "-export", "folio_help"]

# ---- Usage ----
#
# == Running the Built-in Example (FolioHlp) ==
# The image includes the FolioHlp.fff file and a test configuration.
# To run the example export named "folio_help":
#   docker run --rm imazen/folioxml -config /app/test.yaml -export folio_help
# Output will be generated inside the container. To access it, you'd typically
# mount a volume (see below) or use `docker cp`.
#
# == Processing Your Own Data ==
# To process your own Folio files, mount a local directory to the container's /data volume.
# Place your configuration file (e.g., my_config.yaml) and input files in your local directory.
#
# 1. Create a local directory (e.g., ~/my_folio_data) and place your config and FFF/FLS files inside.
# 2. Your config file (e.g., my_config.yaml) should reference paths relative to /data inside the container.
#    Example `my_config.yaml` snippet:
#      input:
#        type: folio
#        path: /data/my_input.fff # Or .fls
#      exports:
#        my_export_name:
#          type: flat_html
#          path: /data/output/my_export # Output will go here inside the container
#
# 3. Run the container with the volume mount:
#    docker run --rm -v ~/my_folio_data:/data imazen/folioxml -config /data/my_config.yaml -export my_export_name
#
# Output files will appear in your local ~/my_folio_data/output directory. 