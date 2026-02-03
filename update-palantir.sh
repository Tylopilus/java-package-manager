#!/bin/bash
# Update Palantir Java Format to latest version

set -e

JPM_DIR="$HOME/.jpm"
LIB_DIR="$JPM_DIR/lib"

echo "==> Updating Palantir Java Format to latest version..."

# Latest version from maven
PALANTIR_VERSION="2.86.0"
PALANTIR_JAR="palantir-java-format-${PALANTIR_VERSION}.jar"
PALANTIR_URL="https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format/${PALANTIR_VERSION}/${PALANTIR_JAR}"

# Download latest Palantir Java Format
if [ ! -f "$LIB_DIR/$PALANTIR_JAR" ]; then
    echo "  -> Downloading palantir-java-format ${PALANTIR_VERSION}..."
    curl -sL "$PALANTIR_URL" -o "$LIB_DIR/$PALANTIR_JAR"
fi

# Download matching SPI version
PALANTIR_SPI_JAR="palantir-java-format-spi-${PALANTIR_VERSION}.jar"
PALANTIR_SPI_URL="https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format-spi/${PALANTIR_VERSION}/${PALANTIR_SPI_JAR}"

if [ ! -f "$LIB_DIR/$PALANTIR_SPI_JAR" ]; then
    echo "  -> Downloading palantir-java-format-spi ${PALANTIR_VERSION}..."
    curl -sL "$PALANTIR_SPI_URL" -o "$LIB_DIR/$PALANTIR_SPI_JAR"
fi

# Remove old version
rm -f "$LIB_DIR/palantir-java-format-2.50.0.jar" "$LIB_DIR/palantir-java-format-spi-2.50.0.jar" 2>/dev/null || true

echo "==> Updated successfully to version ${PALANTIR_VERSION}!"
