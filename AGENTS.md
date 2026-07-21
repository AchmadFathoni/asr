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

Release notes should be short user-facing highlights. Let AI generate them from commits since last tag if desired.

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
Uses `kotlin.time.Clock.System` (Kotlin stdlib, available in KMP common) — not `kotlinx.datetime.Clock.System` (which doesn't exist). A single `TimeUtils.kt` in `shared:core` defines `LocalDate.now()`, `LocalDateTime.now()`, `LocalTime.now()` extension functions and `currentDateFlow()` (polls every 60s). All callers import `com.asr.core.now` and call `.now()` directly. No java.time needed in shared code.

**Quirk:** Never cache `today` as a frozen `val` in ViewModels — if the process survives overnight, the stale date causes daily habits to appear checked instead of reset. Use `currentDateFlow()` and `flatMapLatest` to keep both `today` and `recordsForDate(today)` reactive (see `TodayViewModel.kt:40`, `HabitsViewModel.kt:42`).

Caveat: `androidApp` `AlarmSchedulerImpl.kt` uses `java.time.LocalTime` for alarm time arithmetic — a platform-constrained exception.

#### 3. `nix-ld` for NixOS compatibility
The Android Gradle Plugin downloads prebuilt `aapt2` binaries from Maven that fail on NixOS due to missing dynamic linker paths. Two fixes in `scripts/gradlew`:

- **AAPT2 override:** Sets `-Pandroid.aapt2FromMavenOverride=$AAPT2_PATH` (project property, *not* `-D` system property — AGP reads this as a project property, and `-D` silently ignores it on AGP 9.x). This redirects AGP to the SDK's pre-patched `aapt2` so the Maven one is never used at all.
- **Post-build patching:** `patch-aapt2.sh` runs after every build to fix any newly downloaded Maven `aapt2` binaries, as a safety net.

The dev shell sets `NIX_LD` + `NIX_LD_LIBRARY_PATH` and uses `nix-ld` as the ELF interpreter via `scripts/gradlew`. `nix-ld` redirects library resolution through `NIX_LD_LIBRARY_PATH`, so no re-patching is needed if library paths change. Set `ANDROID_HOME` to `./.android-sdk` so SDK downloads stay local.

The same patching is applied to `adb` in the dev shell shellHook so `installDebug` works on NixOS.

**Don't delete `.gradle-home/caches/` to fix AAPT2 issues.** Instead run `./scripts/gradlew assembleDebug` — the `-Pandroid.aapt2FromMavenOverride` flag makes AGP skip Maven's `aapt2` entirely. If the transform cache gets corrupted, delete only `.gradle-home/caches/*/transforms/` then rebuild.

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

#### 7. Raw RemoteViews + trampoline activity for widget (not Glance)
Android widget uses raw `RemoteViewsService` + `ListView` + transparent trampoline `Activity`, **not** Jetpack Glance. Chosen after extensive Samsung One UI testing (see codebase history):

| Approach tested | Result on Samsung |
|---|---|
| Glance `Column` + `clickable(actionRunCallback)` | Clicks fire, widget refresh unreliable (Samsung skips Glance's null-RemoteViews update signal) |
| Glance `LazyColumn` + `clickable` | Same refresh issue; per-item PendingIntents don't dispatch on Samsung |
| Raw `RemoteViews` + `setPendingIntentTemplate` (broadcast) | Broadcast fires, fillInIntent extras **stripped** by Samsung's `HoneyAppWidgetHostView` |
| Raw `RemoteViews` + `setPendingIntentTemplate` (activity) + trampoline | **Works** — fillInIntent data survives, trampoline does DB write + `notifyAppWidgetViewDataChanged()` |

**Samsung quirk:** Samsung's `HoneyAppWidgetHostView` strips `fillInIntent` extras and URI data for **broadcast** `PendingIntent` templates — the extras arrive as `null` even though the broadcast fires. Activity `PendingIntent` templates correctly merge fillInIntent data (action, extras, data URI). This is why Glance's internal `InvisibleActionTrampolineActivity` uses an activity template: Samsung dispatches the activity with merged data, then the trampoline re-dispatches as a local broadcast. Google Keep uses the same pattern.

**How it works:**
- `ListView` with `setPendingIntentTemplate(activity)` → Samsung properly merges fillInIntent for activity intents
- FillInIntent per item carries action (`TOGGLE_TASK` / `INCREMENT_HABIT`) + ID + appWidgetId
- Transparent `WidgetActionActivity` (singleInstance, separate task, noHistory) does DB write via shared `getDatabase()` singleton → calls `notifyAppWidgetViewDataChanged()` for instant refresh → finishes

**Widget flicker fix:** Hot-path refreshes (trampoline / `AndroidWidgetUpdater`) call only `notifyAppWidgetViewDataChanged()` — never `updateAppWidget()`. Building new `RemoteViews` + `updateAppWidget` triggers Samsung's animation and causes visible flicker. `notifyAppWidgetViewDataChanged` just re-queries the adapter (`onDataSetChanged → getViewAt`) with zero visual disruption. Full `updateAppWidget` is only used in `onUpdate()` for initial widget placement/resize.

**DB singleton:** Both app (Koin DI) and widget share the same `getDatabase()` instance (`WidgetDatabase.kt`). Two separate Room instances would cause stale-read — the app's invalidation tracker must see widget writes.

**Files:**
- `TodayWidgetProvider.kt` — `AppWidgetProvider`, builds ListView RemoteViews, sets activity template
- `TodayWidgetService.kt` / `TodayViewsFactory.kt` — `RemoteViewsService` + `RemoteViewsFactory` adapter
- `WidgetActionActivity.kt` — transparent trampoline activity
- `WidgetDatabase.kt` — shared Room singleton

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

#### 8. Punishment Dialog for backsliding (ponytail: daily reckoning)
A punishment dialog on the Today tab triggers when **more than half of yesterday's scheduled items** (habits + tasks) were left undone or skipped. Implemented entirely in the UI layer to keep core logic clean:

- **Trigger:** `undoneYesterday > totalYesterday / 2` computed in `TodayViewModel.kt:105-120` using the existing `shouldShowToday(yesterday)` for habits and `dueDate == yesterday` for tasks.
- **Dialog:** Standard `AlertDialog` in `TodayPage.kt:256-281` with a guilt-inducing message and two buttons:
  - **"I understand"** — acknowledges by persisting `yesterday.toString()` via `SettingsRepo.setPunishmentAcknowledgedDate()`. After app restart, the dialog won't re-show for the same yesterday.
  - **"Remain ignorant"** — just dismisses (no persistence). After app restart, the dialog shows again for the same yesterday if the condition is still met.
- **Frequency guard:** Once per day max (tracked via `_punishmentDismissed` `MutableStateFlow` reset on day change). "Relax on enough" built in via the >50% threshold — missing 1 of 5 items is fine, missing 3 of 5 triggers it.
- **Settings persistence:** Both platforms implemented — `PrefsSettingsStorage` (Android SharedPreferences) and `JsonSettingsStorage` (desktop `~/.asr/data.json`).
- **No settings toggle** (YAGNI). No sound/animation. Just a text dialog.

### Known Issues & TODOs
- **CMP-10319**: TimePicker analog dial shows wrong selector color on desktop (Skia/OpenGL BlendMode bug). [Filed 2026-06-09](https://youtrack.jetbrains.com/issue/CMP-10319). Using analog `TimePicker` anyway — spatial awareness of clock dial is preferred over number input. Bug is desktop-only visual (wrong selector color), no functional impact. If the bug becomes intolerable, revisit with `GraphicsApi.SOFTWARE_FAST` or `TimeInput`.
- **Desktop parity gaps**: Desktop export/restore uses `~/.asr/exports/` directory with no file picker; alarms and sound are no-ops. These are acceptable for now (desktop is secondary), but new features should implement both platform bindings in the same PR — see Design Decision #6.
