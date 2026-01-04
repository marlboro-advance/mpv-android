#!/bin/bash
# This script is used by JitPack to set up the build environment

# Make script executable
chmod +x gradlew

echo "Java version:"
java -version

# Set correct permissions
chmod -R +x buildscripts

echo "Setup complete"