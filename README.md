# ASR — Todo & Habit App

Simple, intuitive task and habit tracker for Android.

## Credits

Architecture and patterns drawn from two excellent open-source projects:

- **[Grit](https://github.com/shub39/grit)** by Shubham Gorai — KMP/Compose Multiplatform structure, UDF/MVI-lite architecture, Koin compiler plugin DI, Room database layout, custom vector drawables pattern
- **[WHPH](https://github.com/anomalyco/whph)** — data model design (hierarchical tasks, recurring habits, junction-table tags), NixOS dev environment approach, export/import schema versioning

## Quick Start (NixOS)

```bash
direnv allow                         # Enter Nix dev shell
./scripts/gradlew assembleDebug      # Build debug APK (patches AAPT2 for NixOS)
./gradlew :desktopApp:run            # Run desktop version for development
./scripts/gradlew test               # Run tests
```

## Architecture

```
shared/core   — Domain models, repository interfaces
shared/ui     — ViewModels, Compose UI (4 tabs)
androidApp    — Room DB, DI, Glance widget, export/import
desktopApp    — JVM desktop entry for development
```

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.4.0 |
| UI | Compose Multiplatform 1.11.1 + Material 3 |
| DI | Koin 4.2.2 (compiler plugin) |
| Database | Room 3 |
| Date/Time | kotlinx-datetime + kotlin.time.Clock.System |
| File I/O | FileKit |
| Widgets | Glance |
| Tests | kotlin.test (31 tests) |

## License

MIT
