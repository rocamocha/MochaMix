# Adding new projects (quick-start)

Use this file for a minimal, actionable workflow to add a new project to MochaMix.

Minimum layout (example `logical-loadouts`):

```
projects/logical-loadouts/
├── common/src/main/java/
└── v1_21_1/src/main/java/
```

Steps

1. Create directories (PowerShell):

```powershell
mkdir projects\logical-loadouts\common\src\main\java -Force
mkdir projects\logical-loadouts\v1_21_1\src\main\java -Force
```

2. Add a minimal `build.gradle` to each adapter folder (usually empty — root build supplies common settings).

3. Add `fabric.mod.json` and any resources under `common/src/main/resources/`.

4. (Optional) Add your project to any validation lists in `switch-project.bat` or `ide-context.gradle` if you use explicit lists.

5. Switch and test:

```powershell
.\switch-project.bat logical-loadouts v1_21_1
.\gradlew -b ide-context.gradle generateContext
.\gradlew clean build
```

Best practices

- Keep shared code in `common/` and only add adapter-specific files where behavior or APIs differ.
- For overrides, add only the files that change — merge order is: override adapter → common → base adapter.
- Use the `mochamix` project as a template when in doubt.

If you need a full walkthrough with examples and scripts, open `ADDING_PROJECTS.md` for the longer reference.
