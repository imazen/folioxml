# Stage 1: Cache External Dependencies
FROM maven:3.8.4-jdk-8 AS deps

WORKDIR /app

COPY examples/folio-help/input ./files/folio-help

# Copy only the root POM and module directories required by Maven
# to resolve the full dependency graph for dependency:go-offline.
COPY pom.xml .
COPY core ./core
COPY commandline ./commandline
COPY diff_match_patch ./diff_match_patch
COPY contrib ./contrib
# Add other module directories if they are part of the build


RUN echo "Building application..."
RUN mvn package assembly:single -pl commandline -am -U -B -fae


WORKDIR /app

# Define a mount point for user data (config, input, output)
VOLUME /data

# Copy only the built fat JAR from the builder stage.
RUN cp /app/commandline/target/folioxml-commandline-jar-with-dependencies.jar /app/folioxml.jar

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "/app/folioxml.jar"]
