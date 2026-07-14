# ASR — Todo & Habit App

> وَالْعَصْرِ ﴿١﴾ إِنَّ الْإِنسَانَ لَفِي خُسْرٍ ﴿٢﴾ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ ﴿٣﴾
>
> *"By the time, indeed mankind is in loss, except for those who believe and do righteous deeds and advise each other to truth and advise each other to patience."*
> — Surah Al-Asr (103)

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
