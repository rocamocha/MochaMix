## IDE context switching (short)

The `ide-context.gradle` script generates an IDE-only classpath for the selected adapter so `common/` has correct autocompletion and error checking without pulling the entire multi-project configuration into the IDE.

Recommended flow:

1. Select project/adapter:

```powershell
.\switch-project.bat <project> <adapter>
# e.g. .\switch-project.bat mochamix v1_21_1
```

2. Generate IDE context:

```powershell
.\gradlew -b ide-context.gradle generateContext
```

3. Reload your IDE project (IntelliJ: Reload Gradle Project; VS Code: Java/Gradle refresh).

Notes:

- The script reads the adapter property from `gradle.properties`.
- It uses `compileOnly` dependencies so the IDE can resolve version-specific APIs without changing the build.
- Use the existing `switch` tooling rather than editing `gradle.properties` directly when possible.

If you need a one-off change, editing `gradle.properties` is acceptable. After editing, regenerate the IDE context as above.