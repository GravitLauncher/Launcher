# Repository guidance

## Code changes

- Keep code clean, readable, and consistent with the surrounding codebase.
- Follow existing project abstractions and module boundaries. Prefer extending the established design over duplicating behavior or bypassing abstractions.
- Consider performance in hot paths and avoid unnecessary work, allocations, and I/O.
- Apply security checks at trust boundaries: validate inputs, enforce authorization, and handle secrets and errors safely.
- Do not create AI-generated documentation in the project directory.
- If a change requires breaking compatibility, state that explicitly in its description and explain the impact. A breaking change can be preferable to a messy hack or abstraction bypass; use common sense and balance the tradeoffs.

## Build and verification

- Prefer the installed `gradle` command when available and its version is 9.x or newer.
- If Gradle is unavailable, or the installed version is lower than 9.x, use the repository's `./gradlew` wrapper.
- Use `gradle installDist` (or `./gradlew installDist` when using the wrapper) to create executables.
- Use `gradle test` (or `./gradlew test` when using the wrapper) to run tests.
- Run the relevant build, checks, and tests for the change when practical, and report what was run and any failures.

## Supported Minecraft versions

- Minecraft 1.7.10: Forge.
- Minecraft 1.12.2: Forge.
- Minecraft 1.16.5: Forge and Fabric.
- Minecraft 1.17 and newer: Vanilla, Forge, NeoForge, and Fabric.
- Quilt support is experimental and has not been tested.

## Testing

Tests and commands may take a long time. Run tests or execute project commands only when the user has explicitly requested them.

### Run tests without a graphical interface

Build the distribution with `gradle installDist` (or `./gradlew installDist` when using the wrapper). From the installation directory, run:

```bash
ADDRESS=localhost PROJECTNAME=MineCraft bin/launchserver --prepare
```

To test commands, pass each command as a separate `--run` argument:

```bash
ADDRESS=localhost PROJECTNAME=MineCraft bin/launchserver --prepare \
  --run "command1 arg1 arg2" \
  --run "command2 arg1 arg2"
```

### Set up a development environment for manual testing

Use this setup only when the user requests manual testing. Rebuild the distribution with `gradle installDist` (or `./gradlew installDist`) after each source update. The other setup steps are needed only once.

1. Create a working directory outside any Git repository. In the commands below, `WORKDIR` is this directory and `YOUR_SOURCE_CODE_DIR` is the absolute path to this project's checkout.
2. Create `start.sh` in the working directory with this content. On Windows, create an equivalent batch file that sets `APP_HOME` and forwards all arguments to `launchserver`.

   ```bash
   #!/bin/bash
   export APP_HOME=YOUR_SOURCE_CODE_DIR/build/install/launchserver
   exec YOUR_SOURCE_CODE_DIR/build/install/launchserver/bin/launchserver "$@"
   ```

3. Make sure you have a built LauncherRuntime checkout from <https://github.com/GravitLauncher/LauncherRuntime>. Download and build it if needed.
4. Create a symlink from `RUNTIME_SOURCE_CODE/runtime` to `WORKDIR/runtime`.
5. Create a symlink from `RUNTIME_SOURCE_CODE/build/libs/JavaRuntime*.jar` to `WORKDIR/JavaRuntime.jar`.
6. Start in prepare mode:

   ```bash
   ADDRESS=localhost PROJECTNAME=MineCraft bin/launchserver --prepare
   ```

7. Add the JavaRuntime launcher module:

   ```bash
   bin/launchserver --prepare --run "modules launcher-load JavaRuntime.jar"
   ```

8. Load useful modules and generate a certificate:

   ```bash
   bin/launchserver --prepare \
     --run "modules load MirrorHelper" \
     --run "modules load GenerateCertificate" \
     --run "generatecertificate"
   ```

9. To start on Windows, download <https://github.com/GravitLauncher/LauncherPrestarter/releases/latest/download/Prestarter.exe> to `WORKDIR/Prestarter.exe`, then run:

   ```bash
   bin/launchserver --prepare \
     --run "modules load Prestarter" \
     --run "modules load OpenSSLSignCode"
   ```

10. To build or start a client, apply the workspace and install the client. Replace the example client name, Minecraft version, and loader as needed:

    ```bash
    bin/launchserver --prepare --run "applyworkspace"
    bin/launchserver --prepare --run "installclient testClient 1.7.10 FORGE"
    ```

    For Minecraft 1.12.2 and newer with Forge or NeoForge, download the installer using the exact Forge or NeoForge version:

    ```bash
    bin/launchserver --prepare --run "downloadinstaller 1.21.1 NEOFORGE <exact-forge-or-neoforge-version>"
    ```

11. Build the client side:

    ```bash
    bin/launchserver --prepare --run "build"
    ```

12. To make the client work, run LaunchServer as a daemon. It may need an interactive terminal, such as a Docker interactive session, `screen`, or `tmux`.

### Common testing issues

- Check that LaunchServer is not already running. Do not start it twice.
- Do not run `--prepare` while a daemon is running. Send commands to the daemon instead.
- If a file fails to download, or a download is very slow or times out, tell the user. The cause may be an internet connection, proxy, or blocked download site.

## Commits and pull requests

- Prefix commit subjects with one of: `[FEATURE]`, `[FIX]`, `[REFACTOR]`, or `[DOCS]`, according to the change. Use `[ANY]` for merge commits and dependency updates.
- For commits created with AI assistance, include a `Co-authored-by` trailer identifying the AI co-author.
- PR text must be reviewed by a human before the pull request is submitted or published.
