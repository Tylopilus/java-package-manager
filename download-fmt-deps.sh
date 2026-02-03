#!/bin/bash
# Download missing Palantir Java Format dependencies

set -e

JPM_DIR="$HOME/.jpm"
LIB_DIR="$JPM_DIR/lib"

echo "==> Downloading Palantir Java Format dependencies..."

# Download palantir-java-format-spi (contains FormatterException)
SPI_VERSION="2.50.0"
SPI_JAR="palantir-java-format-spi-${SPI_VERSION}.jar"
SPI_URL="https://repo1.maven.org/maven2/com/palantir/javaformat/palantir-java-format-spi/${SPI_VERSION}/${SPI_JAR}"

if [ ! -f "$LIB_DIR/$SPI_JAR" ]; then
    echo "  -> Downloading $SPI_JAR..."
    curl -sL "$SPI_URL" -o "$LIB_DIR/$SPI_JAR"
fi

# Download Guava (required by Palantir)
GUAVA_VERSION="33.0.0-jre"
GUAVA_JAR="guava-${GUAVA_VERSION}.jar"
GUAVA_URL="https://repo1.maven.org/maven2/com/google/guava/guava/${GUAVA_VERSION}/${GUAVA_JAR}"

if [ ! -f "$LIB_DIR/$GUAVA_JAR" ]; then
    echo "  -> Downloading $GUAVA_JAR..."
    curl -sL "$GUAVA_URL" -o "$LIB_DIR/$GUAVA_JAR"
fi

# Download Guava failureaccess
FAILUREACCESS_VERSION="1.0.2"
FAILUREACCESS_JAR="failureaccess-${FAILUREACCESS_VERSION}.jar"
FAILUREACCESS_URL="https://repo1.maven.org/maven2/com/google/guava/failureaccess/${FAILUREACCESS_VERSION}/${FAILUREACCESS_JAR}"

if [ ! -f "$LIB_DIR/$FAILUREACCESS_JAR" ]; then
    echo "  -> Downloading $FAILUREACCESS_JAR..."
    curl -sL "$FAILUREACCESS_URL" -o "$LIB_DIR/$FAILUREACCESS_JAR"
fi

echo "==> Dependencies downloaded successfully!"
