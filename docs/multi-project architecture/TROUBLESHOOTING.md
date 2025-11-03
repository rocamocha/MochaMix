# MochaMix Multi-Project Troubleshooting Guide
# Troubleshooting — quick reference

Short, common fixes to get you back to building and testing quickly.

Build errors
- "package ... does not exist": ensure sources are under `projects/<project>/common/src/main/java/` and run `.\gradlew -b ide-context.gradle generateContext`.
- "Project directory not found": verify `projects/<name>` exists and spelling matches. Create directory or add project to tooling lists.
- Duplicate classes after merges: check precedence (override → common → base) and remove duplicates, then `.\n+\gradlew clean`.

IDE issues
- IDE shows red but build succeeds: regenerate IDE context and refresh the IDE (IntelliJ: Reload Gradle Project; VS Code: Java/Gradle refresh).
- Wrong/missing source paths: create the missing `common/src/main/java` directories and regenerate context.

Tooling and switching
- Invalid project/adapter: ensure you use `v1_21_1` style adapter names. Update `switch-project.bat` validation lists if adding projects.
- `gradle.properties` not updating: check file permissions or edit manually, then regenerate context.

Useful commands (PowerShell)

```powershell
.\switch-project.bat <project> <adapter>      # switch active project/adapter
.\gradlew -b ide-context.gradle generateContext # regenerate IDE-only classpath
.\gradlew clean build --info
dir projects
```

When reporting issues include:
- current `.\switch-project.bat` output
- `.\n+\gradlew -b ide-context.gradle generateContext` logs
- `.\n+\gradlew clean build --info` output
- a `tree projects /f` or `dir projects` listing

If you need further help, paste the short logs above and the active project/adapter values.