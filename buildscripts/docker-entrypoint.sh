#!/bin/bash
set -e

echo "== mpv-android Build Environment =="

if [ -t 0 ]; then
    INTERACTIVE=1
else
    INTERACTIVE=0
fi

clean_builds() {
    echo "Cleaning build directories..."
    cd /home/mpvbuilder/mpv-android/buildscripts/deps

    for dir in */; do
        if [ -d "${dir}" ]; then
            echo "Cleaning ${dir}..."
            rm -rf "${dir}/_build" "${dir}/_build-arm64" "${dir}/_build-x64" "${dir}/_build-x86"
        fi
    done

    echo "Build directories cleaned."
}

build_project() {
    echo "Downloading dependencies..."
    cd /home/mpvbuilder/mpv-android/buildscripts
    ./download.sh

    echo "Building mpv-android..."

    ./buildall.sh --arch armv7l mpv
    ./buildall.sh --arch arm64 mpv
    ./buildall.sh --arch x86 mpv
    ./buildall.sh --arch x86_64 mpv

    ./buildall.sh

    echo "Build completed successfully!"
    echo "The build artifacts should be available in your mounted project directory."
}

show_help() {
    echo "mpv-android Docker build environment"
    echo ""
    echo "Available commands:"
    echo "  build       - Download dependencies and build the project"
    echo "  download    - Only download dependencies"
    echo "  clean       - Clean all build directories"
    echo "  build-clean - Clean build directories and then build"
    echo "  shell       - Start an interactive shell"
    echo "  help        - Show this help message"
    echo ""
    echo "Example: docker run -v /path/to/mpv-android:/home/mpvbuilder/mpv-android mpv-android-builder build"
}

if [ $# -eq 0 ]; then
    if [ $INTERACTIVE -eq 1 ]; then
        echo "No command specified. Starting a shell."
        exec /bin/bash
    else
        show_help
        exit 1
    fi
else
    case "$1" in
        build)
            build_project
            ;;
        download)
            cd /home/mpvbuilder/mpv-android/buildscripts
            ./download.sh
            ;;
        clean)
            clean_builds
            ;;
        build-clean)
            clean_builds
            build_project
            ;;
        shell)
            exec /bin/bash
            ;;
        help)
            show_help
            ;;
        *)
            echo "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
fi

if [ $INTERACTIVE -eq 1 ] && [ "$1" != "shell" ]; then
    echo "Build process completed. Starting a shell to allow further interaction."
    exec /bin/bash
fi
