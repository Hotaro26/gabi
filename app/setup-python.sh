#!/bin/bash
wget -q https://github.com/indygreg/python-build-standalone/releases/download/20240224/cpython-3.11.8+20240224-x86_64-unknown-linux-gnu-install_only.tar.gz
tar -xzf cpython-3.11.8+20240224-x86_64-unknown-linux-gnu-install_only.tar.gz
sed -i 's/version = "3.11"/version = "3.11"\n        buildPython(project.rootProject.file("python\/bin\/python3.11").absolutePath)/' app/build.gradle.kts
