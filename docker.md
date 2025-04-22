# Docker Build Strategy Notes for FolioXML

This document captures the current state and reasoning behind the Docker setup for this project.

## Current Setup (As of Last Update)

1.  **`Dockerfile` (Single-Stage Build):**
    *   Starts `FROM maven:3.8.4-jdk-8`.
    *   Copies specific source directories (`core`, `commandline`, `lucene`, `diff_match_patch`, `contrib`) and the root `pom.xml` into `/app`. These all depend on each other, we can't do a dependency-only build.
    *   **Copies example input** (`examples/folio-help/input`) into `/app/files/folio-help` within the image (likely for tests during build or default examples).
    *   Runs `mvn package assembly:single -pl commandline -am ...` to build the application fat JAR.
    *   Copies the fat JAR to `/app/folioxml.jar` using `RUN cp ...` within the same stage.
    *   Defines `VOLUME /data` for runtime data mounting.
    *   Sets `ENTRYPOINT` to run the application JAR.
    *   **Result:** The final image (e.g., tagged `imazen/folioxml:latest` or `imazen/folioxml:latest`) contains the full JDK, Maven, the copied source code, build artifacts (including the `/target` directories), the final fat JAR, and the copied example input data.
    *   **Caching:** Docker layer caching is limited. Changes to any copied source file or the `pom.xml` will invalidate the layer containing the `mvn package` command and subsequent layers.

2.  **`Dockerfile.dev`:**
    *   Starts `FROM imazen/folioxml:latest` (or whatever the main `Dockerfile` image is tagged as).
    *   Sets `WORKDIR /app`.
    *   Defines `VOLUME /data` and `VOLUME /root/.m2`.
    *   Sets `CMD ["bash"]` for interactive use.
    *   **Result:** This image is essentially the same as the main build image but defaults to an interactive bash shell. It provides an environment with JDK, Maven, and the source/JAR *as they existed when the base image was built*.

3.  **`.devcontainer/devcontainer.json`:**
    *   Configures GitHub Codespaces.
    *   Specifies that the Codespace container should be built using `../Dockerfile.dev`.
    *   Installs Docker-in-Docker (to allow building/running Docker within the Codespace) and common utilities.
    *   Installs relevant VS Code extensions (Docker, YAML, ShellCheck).
    *   Sets up workspace mounting.

4.  **Development Workflow (`export.sh`/`export.ps1`):**
    *   These scripts are intended to be run from an example project directory (e.g., `examples/folio-help`).
    *   They use `docker run` with the image specified by `$DEV_IMAGE` (e.g., `imazen/folioxml:latest` or the tag derived from `Dockerfile.dev`).
    *   They **mount the host's FolioXML repository root** over `/app` in the container. This *overlays* the source code that was baked into the image with the live code from the host workspace.
    *   They mount the current example project directory to `/data`.
    *   They optionally mount the host's `.m2` cache to `/root/.m2`.
    *   They execute `mvn clean package assembly:single ... && java -jar ...` *inside* the container. This build uses the JDK/Maven from the image but operates on the *mounted source code* from the host.

## Key Implications & Considerations

*   **Single Image:** There's effectively one main image build defined by `Dockerfile`. `Dockerfile.dev` just repackages it for interactive use.
*   **No Separate Dependency Caching:** The multi-stage approach to cache external dependencies separately has been removed. The build compiles everything in one go.
*   **Image Contents:** The primary image is large as it contains the build environment (JDK, Maven), source code, and build outputs.
*   **Example Data in Image:** The main `Dockerfile` now copies some example input data directly into the image.
*   **Dev Workflow:** The dev scripts overlay the live source code onto the `/app` directory of a container running the main image, then perform a full build inside that container. 