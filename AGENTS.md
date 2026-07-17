# AGENTS.md

## ASR — Todo & Habit App

### Quick Start (NixOS)
```bash
direnv allow                 # Enter Nix dev shell (first time: downloads Android SDK ~1GB)
./scripts/gradlew assembleDebug  # Build APK (auto-patches AAPT2 via nix-ld)
./scripts/gradlew :desktopApp:run  # Run desktop app for development
```

### Technology Stack
- **Language:** Kotlin 2.4.0
- **UI:** Compose Multiplatform 1.11.1 + Material 3
- **Build:** Gradle 9.6.0 with Kotlin DSL
- **DI:** Koin 4.2.2
- **Database:** Room 3
- **Navigation:** State-based (flat structure with 3 tabs + settings)
- **Serialization:** kotlinx-serialization-json
- **Date/Time:** kotlinx-datetime + kotlin.time.Clock.System (see Design Decision #2)
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

### Release
```bash
# 1. Bump version in gradle.properties (app.versionName)
# 2. Build release APK
./scripts/gradlew assembleRelease

# 3. Create release + upload APK
export GH_TOKEN=$(cat .secret)
gh release create v<version> --title "v<version>" \
  --notes "## Highlights

- Feature summary here"
gh release upload v<version> androidApp/build/outputs/apk/release/androidApp-release.apk
```
Only upload **release** APK (`androidApp-release.apk`), never debug. Version code is auto-derived from `git rev-list --count HEAD`.

### Commands
| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew installDebug` | Build + install on connected Android device |
| `./gradlew test` | Run unit tests |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew installRelease` | Build + sign + install release on connected device |
| `./gradlew :desktopApp:run` | Run desktop app (via Compose Multiplatform) |
| `./scripts/gradlew` | Script that auto-patches AAPT2 via nix-ld, available inside dev shell |
| `nix run .#gradlew` | Run via FHS environment (no patching needed) |

### Design Decisions

#### 1. Koin Compiler Plugin with annotations
Following Grit's pattern: `startKoin<AppModule>` (reified generic) with `@Module @ComponentScan` annotations. ViewModels auto-discovered via `@KoinViewModel` in `shared:ui` and collected by `UIModules` module. Repository implementations use `@Single(binds = [Interface::class])`. No manual `module { single { ... } }` DSL needed.

#### 2. kotlin.time.Clock.System for current date/time
Uses `kotlin.time.Clock.System` (Kotlin stdlib, available in KMP common) — not `kotlinx.datetime.Clock.System` (which doesn't exist). A single `TimeUtils.kt` in `shared:core` defines `LocalDate.now()`, `LocalDateTime.now()`, `LocalTime.now()` extension functions. All callers import `com.asr.core.now` and call `.now()` directly. No java.time needed in shared code.

Caveat: `androidApp` `AlarmSchedulerImpl.kt` uses `java.time.LocalTime` for alarm time arithmetic — a platform-constrained exception.

#### 3. `nix-ld` for NixOS compatibility
The Android Gradle Plugin downloads prebuilt `aapt2` binaries from Maven that fail on NixOS due to missing dynamic linker paths. Fix: the dev shell sets `NIX_LD` + `NIX_LD_LIBRARY_PATH` and uses `nix-ld` as the ELF interpreter via `scripts/gradlew`. `nix-ld` redirects library resolution through `NIX_LD_LIBRARY_PATH`, so no re-patching is needed if library paths change. Set `ANDROID_HOME` to `./.android-sdk` so SDK downloads stay local.

The same patching is applied to `adb` in the dev shell shellHook so `installDebug` works on NixOS.

#### 4. Custom XML vector drawables via Compose Resources
Custom XML vector drawables in `shared/ui/src/commonMain/composeResources/drawable/`, loaded with `vectorResource(Res.drawable.*)`. Generated resource package: `asr.shared.ui.generated.resources`. This sidesteps the `material-icons-core` version gap (1.7.3 vs Compose 1.11.1) entirely — same approach as Grit's 51 custom drawables.

#### 5. `androidResources.enable = true` for AGP 9+
The `shared:ui` KMP module must set `androidResources.enable = true` inside its `android {}` block (`shared/ui/build.gradle.kts:30`). Without this, Compose Multiplatform resource files (drawables, strings, etc.) in `composeResources/` are silently excluded from Android APK assets at runtime, causing `MissingResourceException`. Required for AGP 9.x+ with KMP shared modules.

#### 6. Platform Parity: interfaces in common, platform DI bindings
Platform differences are handled via interface-based polymorphism — not `expect`/`actual`. Common interfaces live in `shared:core/commonMain`; each platform module provides its own implementations bound via Koin `@Single(binds = [...]))`. No runtime platform checks (`isDesktop`, `BuildConfig`, etc.) exist in shared code — the two code paths never meet at runtime.

| Service | Interface (shared:core) | Android (`:androidApp`) | Desktop (`:desktopApp`) |
|---------|------------------------|------------------------|------------------------|
| Persistence | `TaskStorage`, `HabitStorage`, `TagStorage`, `SettingsStorage` | Room DB (SQLite) | JSON file (`~/.asr/data.json`) |
| File picker | `ExportRepo`, `RestoreRepo` | FileKit system dialogs | `~/.asr/exports/` (no picker) |
| Sound | `SoundPlayer` | MediaPlayer (`done.opus`) | No-op |
| Alarms/Notif. | `AlarmScheduler` | AlarmManager + NotificationManager | No-op |

**Keep gaps minimal.** When adding a shared feature, implement both platform bindings in the same PR. A no-op/stub on desktop is acceptable for Android-hardware features (notifications, alarms), but persistence, export, and settings must work identically. If a UI component behaves differently on desktop (e.g. `TimePicker` — see CMP-10319), isolate the workaround in a single composable and mark with a `// platform:` comment referencing this doc.

**Avoid `expect`/`actual` unless the interface pattern cannot work.** The current interface+DI approach keeps all platform variance at the module boundary, keeping shared code pure KMP.

### Code Style
- UDF (Unidirectional Data Flow): sealed actions → ViewModel state
- Immutable state data classes, exposed via `StateFlow`
- DI: Koin annotations (@Module, @ComponentScan, @Single, @KoinViewModel)
- Kotlin official code style (`kotlin.code.style=official`)

### Data Model
- **Task:** one-time, hierarchical (parentId), states: done/not done
- **Habit:** recurring (daily/weekly/monthly × N times), states: not_done/done/skipped
- **HabitRecord:** tracks daily habit completion
- **Tag:** labels via junction tables (task_tags, habit_tags)

### Testing
- Unit tests in `shared/core/src/commonTest/` (31 tests)
- JVM UI tests in `shared/ui/src/jvmTest/` (3 tests)
- Android tests in `androidApp/src/test/`
- Focus areas: import/export validation, date edge cases, input validation

### Known Issues & TODOs
- **CMP-10319**: TimePicker analog dial shows wrong selector color on desktop (Skia/OpenGL BlendMode bug). [Filed 2026-06-09](https://youtrack.jetbrains.com/issue/CMP-10319). Options to fix: `GraphicsApi.SOFTWARE_FAST` (try first), switch to `TimeInput`, upgrade CMP, or wait upstream.
- **Desktop parity gaps**: Desktop export/restore uses `~/.asr/exports/` directory with no file picker; alarms and sound are no-ops. These are acceptable for now (desktop is secondary), but new features should implement both platform bindings in the same PR — see Design Decision #6.
