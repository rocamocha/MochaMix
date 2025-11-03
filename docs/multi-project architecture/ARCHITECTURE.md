## MochaMix — architecture at-a-glance

MochaMix is a project-centric multi-module repo where each "project" contains shared code (common) plus one or more version adapters (v1_21_1, v1_21_5, ...). The build merges sources from these locations with a clear precedence so you can maintain version-specific overrides while keeping shared code in one place.

Minimal layout (high level):

```
MochaMix/
├── projects/
│   └── <project>/
│       ├── common/            # shared sources and resources
│       ├── v1_21_1/           # base adapter (version-specific overrides)
│       └── v1_21_5/           # override adapter
└── run/                       # shared run directories (client/server per version)
```

Source precedence (merge order, highest → lowest):

- adapter override (e.g. `projects/<project>/v1_21_5/src/main/java`)
- common (`projects/<project>/common/src/main/java`)
- base adapter (e.g. `projects/<project>/v1_21_1/src/main/java`)

Key files and commands (Windows PowerShell):

``powershell
.\switch-project.bat <project> <adapter>   # change active project/adapter
.\gradlew -b ide-context.gradle generateContext  # generate IDE context for current adapter
.\gradlew clean build
```

Notes:
- `gradle.properties` holds the active project/adapter used by the tooling.
- `ide-context.gradle` creates a compile-only IDE classpath for the selected adapter so the common module has correct autocompletion.
- `run/client/<adapter>/` and `run/server/<adapter>/` are shared environments used to test multiple projects together.

See `ADDING_PROJECTS.md` for the minimal steps to add a project.