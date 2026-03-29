# HardcoreEngine_VR_HM1

## Java Version

This repository is now pinned to Java 1.8.

- Required JDK: Java 1.8
- Verified runtime: Eclipse Adoptium jdk-8.0.482.8-hotspot
- Source directory: src
- Output directory: bin

The project previously ran under JDK 18 and JDK 21, but the repository configuration has been updated so that compilation and execution target Java 1.8 explicitly.

## VS Code Configuration

VS Code workspace settings are defined in .vscode/settings.json.

Current Java-related configuration:

- java.configuration.runtimes -> JavaSE-1.8
- java.project.sourcePaths -> src
- java.project.outputPath -> bin
- java.debug.settings.onBuildFailureProceed -> true

If your local JDK 1.8 installation path is different, update the path field in .vscode/settings.json accordingly.

## Build

Use JDK 1.8 to compile the project:

```powershell
Set-Location 'd:\project\虚拟现实作业_物理引擎'
& 'C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot\bin\javac.exe' -source 1.8 -target 1.8 -encoding UTF-8 -d bin src/homework1/*.java
```

## Run

Use JDK 1.8 to run the main class:

```powershell
Set-Location 'd:\project\虚拟现实作业_物理引擎'
& 'C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot\bin\java.exe' -cp bin homework1.Main
```

At startup, the program prints the actual Java version in use so you can confirm it is running on Java 1.8.

## Switching From JDK 18 Or JDK 21 To JDK 1.8

If the project was previously compiled or launched with a higher JDK, old class files or VS Code Java cache entries can cause version mismatch errors.

Recommended cleanup steps:

1. Rebuild the project with JDK 1.8.
2. Run Java: Clean Java Language Server Workspace in VS Code.
3. Reload the VS Code window if the Java extension still picks up stale cached classes.

Typical failure symptom when stale high-version classes remain:

- UnsupportedClassVersionError
- class file version 61 or 65 found while Java 1.8 only supports up to version 52

## Notes

- The main source entry point is src/homework1/Main.java.
- The repository's checked-in VS Code Java settings now document Java 1.8 explicitly.
- If this repository is opened on another machine, install JDK 1.8 first and then adjust the runtime path if needed.