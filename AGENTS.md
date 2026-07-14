# AGENTS.md

## ASR — Todo & Habit App

### Quick Start (NixOS)
```bash
direnv allow                 # Enter Nix dev shell (first time: downloads Android SDK ~1GB)
./scripts/gradlew assembleDebug  # Build APK (auto-patches AAPT2 via nix-ld)
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
- **File Dialogs:** FileKit
- **Widgets:** Glance

### Architecture: UDF/MVI-lite
```
UI → onAction(Sealed Action) → ViewModel → Repository (interface)
UI ← StateFlow<State> ← ViewModel ← Repository (Flow) ← Room
```

### Module Structure
```
:shared/core   — Domain models, repository interfaces, common interfaces
:shared/ui     — ViewModels, Compose UI, navigation
:androidApp    — Room DB, platform actuals, DI, widgets, notifications
```

### Release
```bash
gh release create v<version> --title "v<version>" --notes "<notes>"
gh release upload v<version> androidApp/build/outputs/apk/release/androidApp-release.apk
```
Only upload **release** APK (`androidApp-release.apk`), never debug.

### Commands
| Command | Description |
|---------|-------------|
| `./gradlew assembleDebug -w` | Build debug APK (warn level, hides UP-TO-DATE) |
| `./gradlew installDebug -w` | Build + install on connected Android device |
| `./gradlew test -w` | Run unit tests |
| `./gradlew assembleRelease -w` | Build release APK |
| `./gradlew installRelease -w` | Build + sign + install release on connected device |
| `./scripts/gradlew ... -w` | Script that auto-patches AAPT2 via nix-ld, available inside dev shell |
| `nix run .#gradlew -- ... -w` | Run via FHS environment (no patching needed) |
| `./gradlew assembleDebug -q` | Quiet mode (errors only) |

### Design Decisions

#### 1. Koin Compiler Plugin with annotations
Following Grit's pattern: `startKoin<AppModule>` (reified generic) with `@Module @ComponentScan` annotations. ViewModels auto-discovered via `@KoinViewModel` in `shared:ui` and collected by `UIModules` module. Repository implementations use `@Single(binds = [Interface::class])`. No manual `module { single { ... } }` DSL needed.

#### 2. kotlin.time.Clock.System for current date/time
Uses `kotlin.time.Clock.System` (Kotlin stdlib, available in KMP common) — not `kotlinx.datetime.Clock.System` (which doesn't exist). A single `TimeUtils.kt` in `shared:core` defines `LocalDate.now()`, `LocalDateTime.now()`, `LocalTime.now()` extension functions. All callers import `com.asr.core.now` and call `.now()` directly. No java.time needed.

#### 3. `nix-ld` for NixOS compatibility
The Android Gradle Plugin downloads prebuilt `aapt2` binaries from Maven that fail on NixOS due to missing dynamic linker paths. Fix: the dev shell sets `NIX_LD` + `NIX_LD_LIBRARY_PATH` and uses `nix-ld` as the ELF interpreter via `scripts/gradlew`. `nix-ld` redirects library resolution through `NIX_LD_LIBRARY_PATH`, so no re-patching is needed if library paths change. Set `ANDROID_HOME` to `./.android-sdk` so SDK downloads stay local.

The same patching is applied to `adb` in the dev shell shellHook so `installDebug` works on NixOS.

#### 4. Custom XML vector drawables via Compose Resources
Custom XML vector drawables in `shared/ui/src/commonMain/composeResources/drawable/`, loaded with `vectorResource(Res.drawable.*)`. Generated resource package: `asr.shared.ui.generated.resources`. This sidesteps the `material-icons-core` version gap (1.7.3 vs Compose 1.11.1) entirely — same approach as Grit's 51 custom drawables.

#### 5. `androidResources.enable = true` for AGP 9+
The `shared:ui` KMP module must set `androidResources.enable = true` inside its `android {}` block (`shared/ui/build.gradle.kts:30`). Without this, Compose Multiplatform resource files (drawables, strings, etc.) in `composeResources/` are silently excluded from Android APK assets at runtime, causing `MissingResourceException`. Required for AGP 9.x+ with KMP shared modules.

### Code Style
- UDF (Unidirectional Data Flow): sealed actions → ViewModel state
- Immutable state data classes, exposed via `StateFlow`
- DI: Koin manual modules (Kotlin DSL)
- Kotlin official code style (`kotlin.code.style=official`)

### Data Model
- **Task:** one-time, hierarchical (parentId), states: done/not done
- **Habit:** recurring (daily/weekly/monthly × N times), states: not_done/done/skipped
- **HabitRecord:** tracks daily habit completion
- **Tag:** labels via junction tables (task_tags, habit_tags)

### Testing
- Unit tests in `shared/core/src/commonTest/` (31 tests)
- Android tests in `androidApp/src/test/`
- Focus areas: import/export validation, date edge cases, input validation

### Known Issues & TODOs
- **CMP-10319**: TimePicker analog dial shows wrong selector color on desktop (Skia/OpenGL BlendMode bug). [Filed 2026-06-09](https://youtrack.jetbrains.com/issue/CMP-10319). Options to fix: `GraphicsApi.SOFTWARE_FAST` (try first), switch to `TimeInput`, upgrade CMP, or wait upstream.
