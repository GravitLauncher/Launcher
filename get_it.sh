#!/bin/sh
git clone https://github.com/GravitLauncher/Launcher.git
sed -i 's/git@github.com:/https:\/\/github.com\//' .gitmodules
git submodule update --init --recursive
