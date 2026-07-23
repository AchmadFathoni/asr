# AGENTS.md

## ASR — Todo & Habit App

### Quick Start (NixOS)
```bash
./scripts/gradlew assembleDebug  # Build APK (auto-bootstraps env on NixOS)
./scripts/gradlew :desktopApp:run  # Run desktop app for development
```
`scripts/gradlew` is self-contained on NixOS — no `nix develop` or `direnv` needed.
`direnv` is optional (faster env loading for interactive use).

### Technology Stack
- **Language:** Kotlin 2.4.0
- **UI:** Compose Multiplatform 1.11.1 + Material 3
- **Build:** Gradle 9.6.0 with Kotlin DSL
- **DI:** Koin 4.2.2 (compiler plugin, annotations)
- **Database:** Room 3.0.0
- **Navigation:** State-based (flat: 3 tabs + settings)
- **Serialization:** kotlinx-serialization-json
- **Date/Time:** kotlin.time.Clock.System + kotlinx-datetime (see Design Decision #2)
- **File Dialogs:** FileKit (Android) / java.io.File (desktop)

### Architecture: UDF/MVI-lite
```
UI → onAction(Sealed Action) → ViewModel → Repository (interface)
UI ← StateFlow<State> ← ViewModel ← Repository (Flow) ← Room
```

### Module Structure
```
:shared/core   — Domain models, repository interfaces, common interfaces
:shared/ui     — ViewModels, Compose UI, navigation
:androidApp    — Room DB, platform actuals, DI, notifications, FileKit file pickers
:desktopApp    — JSON-file storage stubs, DI, desktop entry point (Main.kt)
```

### Commands
| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew installDebug` | Build + install on connected Android device |
| `./gradlew test :shared:core:jvmTest :shared:ui:jvmTest` | Run all unit tests (Android + core + UI) |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew installRelease` | Build + sign + install release on connected device |
| `./gradlew :desktopApp:run` | Run desktop app (via Compose Multiplatform) |
| `./scripts/gradlew` | Script that auto-bootstraps NixOS env and patches AAPT2 |

**Debug vs Release on device:** `installDebug` installs `com.asr.debug` (separate app, safe for testing). `installRelease` installs `com.asr` (user data). **Never `adb uninstall com.asr`** — wipes the production database. If debugging a release build issue, use `installDebug` and restore user data from Settings → Export/Import, or build a debug variant without `applicationIdSuffix`.

Version code auto-derived from `git rev-list --count HEAD`.

### Release
```bash
# Bump version in gradle.properties (app.versionName) first
./scripts/gradlew assembleRelease

export GH_TOKEN=$(cat .secret)
gh release create v<version> --title "v<version>" --notes "## Highlights" \
  && gh release upload v<version> androidApp/build/outputs/apk/release/androidApp-release.apk
```
Upload only release APK (`androidApp-release.apk`), never debug.

### Design Decisions

#### 1. Koin Compiler Plugin with annotations
`startKoin<AppModule>` (reified generic) with `@Module @ComponentScan` annotations. ViewModels auto-discovered via `@KoinViewModel` in `shared:ui`, collected by `UIModules`. Repository implementations use `@Single(binds = [Interface::class])`. No manual DSL.

#### 2. kotlin.time.Clock.System for current date/time
Uses `kotlin.time.Clock.System` (Kotlin stdlib) — not `kotlinx.datetime.Clock.System` (doesn't exist). `TimeUtils.kt` in `shared:core` defines `LocalDate.now()`, `LocalDateTime.now()`, `LocalTime.now()` extension functions and `currentDateFlow()` (polls every 60s). Callers import `com.asr.core.now` and call `.now()`.

**Quirk:** Never cache `today` as a frozen `val` in ViewModels — if the process survives overnight, daily habits appear checked instead of reset. Use `currentDateFlow()` + `flatMapLatest` to keep both `today` and `recordsForDate(today)` reactive (see `TodayViewModel.kt:40`, `HabitsViewModel.kt:42`).

Caveat: `AlarmSchedulerImpl.kt` in `:androidApp` uses `java.time.LocalTime` for alarm arithmetic — unavoidable platform constraint.

#### 3. NixOS compatibility
`scripts/gradlew` is self-contained on NixOS — it auto-detects the OS, resolves
`JAVA_HOME`, `NIX_LD`, `NIX_LD_LIBRARY_PATH` via `nix eval`, and sets
`ANDROID_HOME` / `GRADLE_USER_HOME` to project-local dirs automatically.
Results are cached in `.gradle-home/.nix-env-cache` for sub-second subsequent runs.

AAPT2 handling:
- **AAPT2 override:** `-Pandroid.aapt2FromMavenOverride=$AAPT2_PATH` (project property, not `-D` — AGP silently ignores `-D`). Redirects AGP to the SDK's pre-patched `aapt2`, skipping Maven's binary entirely.
- **Post-build patching:** `patch-aapt2.sh` runs after every build as safety net for newly downloaded Maven binaries.

**Important for opencode agents:** Do NOT use `nix develop --command` or `nix develop --print-env` — these produce no output in opencode's terminal. Instead, set env vars directly or just run `./scripts/gradlew` (it handles everything). The correct pattern is:
```bash
GRADLE_USER_HOME="$PWD/.gradle-home" ANDROID_HOME="$PWD/.android-sdk" \
JAVA_HOME=/nix/store/.../openjdk NIX_LD=... NIX_LD_LIBRARY_PATH=... \
./scripts/gradlew installDebug
```
Or simply `./scripts/gradlew installDebug` (auto-bootstraps).

If the transform cache gets corrupted, delete only `.gradle-home/caches/*/transforms/` then rebuild. Don't nuke the whole cache.

If the transform cache gets corrupted, delete only `.gradle-home/caches/*/transforms/` then rebuild. Don't nuke the whole cache.

#### 4. Custom XML vector drawables via Compose Resources
Drawables in `shared/ui/src/commonMain/composeResources/drawable/`, loaded with `vectorResource(Res.drawable.*)`. Generated resource package: `asr.shared.ui.generated.resources`. Sidesteps `material-icons-core` version gap.

#### 5. `androidResources.enable = true` for AGP 9+
`shared/ui/build.gradle.kts:30` sets `androidResources.enable = true` inside its `android {}` block. Without this, Compose Multiplatform resources in `composeResources/` are silently excluded from APK assets at runtime (`MissingResourceException`).

#### 6. Platform Parity: interfaces in common, platform DI bindings
Platform differences use interface-based polymorphism — not `expect`/`actual`. Interfaces in `shared:core/commonMain`; each platform provides implementations bound via `@Single(binds = [...]))`. No runtime platform checks (`isDesktop`, `BuildConfig`, etc.) in shared code.

| Service | Interface (shared:core) | Android | Desktop |
|---------|------------------------|---------|---------|
| Persistence | `TaskStorage`, `HabitStorage`, `TagStorage`, `SettingsStorage` | Room DB (SQLite) | JSON file (`~/.asr/data.json`) |
| File picker | `ExportRepo`, `RestoreRepo` | FileKit system dialogs | `~/.asr/exports/` (no picker) |
| Sound | `SoundPlayer` | MediaPlayer (`done.opus`) | No-op |
| Alarms/Notif. | `AlarmScheduler` | AlarmManager + NotificationManager | No-op |

Keep gaps minimal: implement both platform bindings in the same PR. No-op/stub on desktop is acceptable for Android-hardware features (notifications, alarms), but persistence, export, and settings must work identically.

#### 7. Raw RemoteViews + trampoline activity for widget (not Glance)
Android widget uses `RemoteViewsService` + `ListView` + transparent trampoline `Activity`, not Jetpack Glance. Chosen after Samsung One UI testing:

- Glance: unreliable refresh on Samsung (skips null-RemoteViews update signal)
- Broadcast PendingIntent template: Samsung strips fillInIntent extras
- Activity PendingIntent template + trampoline: **Works** — Samsung merges fillInIntent data correctly

**How it works:**
- `ListView` with `setPendingIntentTemplate(activity)` → Samsung merges fillInIntent
- FillInIntent carries `TOGGLE_TASK` / `INCREMENT_HABIT` action + ID + appWidgetId
- `WidgetActionActivity` (singleInstance, separate task, noHistory) does DB write → `notifyAppWidgetViewDataChanged()` → finishes

**Flicker fix:** Hot-path calls only `notifyAppWidgetViewDataChanged()` — never `updateAppWidget()`. Rebuilding RemoteViews triggers Samsung's animation flicker. Full `updateAppWidget` only in `onUpdate()` for initial placement.

**DB singleton:** Both app (Koin DI) and widget share `getDatabase()` (`WidgetDatabase.kt`). Two Room instances cause stale reads.

**Files:**
- `TodayWidgetProvider.kt` — AppWidgetProvider, builds ListView RemoteViews
- `TodayWidgetService.kt` / `TodayViewsFactory.kt` — RemoteViewsService + adapter
- `WidgetActionActivity.kt` — trampoline activity
- `WidgetDatabase.kt` — shared Room singleton

#### 8. Punishment Dialog for backsliding
A dialog on Today triggers when **more than half of yesterday's scheduled items** (habits + tasks) were left undone or skipped. UI-layer only:

- **Trigger:** `undoneYesterday > totalYesterday / 2` in `TodayViewModel.kt:105-120`
- **Dialog:** `AlertDialog` in `TodayPage.kt:256-281` with "I understand" (persists acknowledgment date via `SettingsRepo`) and "Remain ignorant" (just dismisses)
- **Guard:** Once per day max (`_punishmentDismissed` `MutableStateFlow`, reset on day change)
- **Persistence:** `PrefsSettingsStorage` (Android) and `JsonSettingsStorage` (desktop)
- No settings toggle, no sound, no animation

#### 9. No-op stubs in shared/ui must not carry `@Single`
No-op implementations in `shared:ui` (like `DefaultSoundPlayer`, `DefaultWidgetUpdater`) must NOT be annotated with `@Single(binds = [...])` — `UIModules` component-scans `com.asr.ui` and auto-registers them, creating duplicate bindings that override the real platform implementations in `:androidApp` / `:desktopApp`. Instead, they're plain classes instantiated explicitly by platform `@Single` functions (e.g., `DesktopAppModule.provideSoundPlayer()`). Platform-specific implementations sit in their respective modules with `@Single(binds = [...])`.

#### 10. Habits are done once per day (no tap-counting)
A habit is either DONE or NOT_DONE for a given day. `HabitRecord.count` is always 0 (NOT_DONE/SKIPPED) or 1 (DONE). Period progress is the number of DONE days in the period, not the sum of intra-day taps. `habitRecordWithNewState` in `shared/core` uses a pure toggle: DONE ↔ NOT_DONE, no increment logic. Widget `ACTION_INCREMENT_HABIT` is a misnomer — it toggles the day state. The DAO's `upsertRecordForDate` uses `@Transaction` + DELETE-then-INSERT for atomicity (see `Daos.kt`) to prevent duplicate records.

### Code Style
- UDF: sealed actions → ViewModel state
- Immutable state data classes, exposed via `StateFlow`
- DI: Koin annotations (`@Module`, `@ComponentScan`, `@Single`, `@KoinViewModel`)
- Kotlin official code style (`kotlin.code.style=official`)

### Data Model
- **Task:** one-time, hierarchical (`parentId`), states: done/not done
- **Habit:** recurring (daily/weekly/monthly × N), states: not_done/done/skipped
- **HabitRecord:** tracks daily habit completion
- **Tag:** labels via junction tables (task_tags, habit_tags)

### Testing
- Unit tests: 158 in `shared/core/src/commonTest/` (9 files)
- JVM UI tests: 15 in `shared/ui/src/jvmTest/` (3 files)
- Android tests: 2 in `androidApp/src/test/`
- Focus areas: import/export validation, date edge cases, input validation

### Known Issues & TODOs
- **CMP-10319**: TimePicker selector color wrong on desktop (Skia/OpenGL BlendMode bug). [Filed 2026-06-09](https://youtrack.jetbrains.com/issue/CMP-10319). Using analog `TimePicker` anyway — spatial awareness preferred over input. Desktop-only visual, no functional impact.
- **Desktop parity gaps**: Desktop export/restore uses `~/.asr/exports/` directory with no file picker; alarms and sound are no-ops. Acceptable (desktop is secondary), but new features should implement both platform bindings in the same PR — see Design Decision #6.
- **Unused Glance dependencies**: `glance-appwidget` and `glance-material3` declared in `:androidApp` but unused (raw RemoteViews approach used instead). Left from prior Samsung testing. Remove when dependencies are next cleaned up.
