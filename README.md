# ASR — Todo & Habit App

> وَالْعَصْرِ ﴿١﴾ إِنَّ الْإِنسَانَ لَفِي خُسْرٍ ﴿٢﴾ إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ ﴿٣﴾
>
> *"By the time, indeed mankind is in loss, except for those who believe and do righteous deeds and advise each other to truth and advise each other to patience."*
> — Surah Al-Asr (103)

Simple, intuitive task and habit tracker for Android.

- **Tasks** — one-time todos with due dates, hierarchical subtasks, completion tracking
- **Habits** — recurring (daily/weekly/monthly) with frequency counts, streaks, monthly calendar history
- **Today dashboard** — combined view of due tasks + today's habits, undo snackbar, celebration on full completion
- **Tags** — colored labels via many-to-many junction tables, filter across tasks and habits
- **Filter & search** — query by title/description, filter by tag or date on every tab
- **Import/Export** — full JSON export/import with schema versioning
- **Reminders** — per-item alarm via Android `AlarmManager` + notification
- **Sound effects** — completion sound with pitch rising on streak
- **Theme** — light/dark/system, persisted

## Credits

Architecture and patterns drawn from two excellent open-source projects:

- **[Grit](https://github.com/shub39/grit)** by Shubham Gorai — KMP/Compose Multiplatform structure, UDF/MVI-lite architecture, Koin compiler plugin DI, Room database layout, custom vector drawables pattern
- **[WHPH](https://github.com/anomalyco/whph)** — data model design (hierarchical tasks, recurring habits, junction-table tags), NixOS dev environment approach, export/import schema versioning
- **Completion sound effect** — "Level Up" by [UNIVERSFIELD](https://pixabay.com/users/universfield-28281460/) from [Pixabay](https://pixabay.com/sound-effects/level-up-191997/)

## Quick Start (NixOS)

```bash
direnv allow                         # Enter Nix dev shell
./scripts/gradlew assembleDebug      # Build debug APK (patches AAPT2 for NixOS)
./scripts/gradlew installDebug       # Build + install on connected Android device with USB debugging
./scripts/gradlew :desktopApp:run    # Run desktop version for development
./scripts/gradlew test               # Run tests
```

> **USB debug:** Enable Developer options & USB debugging on your Android device, connect via USB, confirm with `adb devices`. The dev shell provides `adb` via `android-tools` and auto-patches it for NixOS.

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
| Tests | kotlin.test (commonTest + jvmTest) |

## License

MIT
