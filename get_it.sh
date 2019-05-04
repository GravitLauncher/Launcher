#!/bin/sh
git clone https://github.com/GravitLauncher/Launcher.git
cd Launcher
sed -i 's/git@github.com:/https:\/\/github.com\//' .gitmodules
git submodule sync
git submodule update --init --recursive
