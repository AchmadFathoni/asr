# Business Logic Audit

## Bug 1 (Critical): ToggleTask double-toggles parent

Parent-auto-complete logic exists in **three places**, but should exist in only one.

### Locations

| Location | File | Lines |
|----------|------|-------|
| A | `shared/core/.../task/SharedTaskRepo.kt` | 27–33 |
| B | `shared/ui/.../viewmodel/TasksViewModel.kt` | 114–122 |
| C | `shared/ui/.../viewmodel/TodayViewModel.kt` | 119–128 |

### Trace

Given: Parent (undone) → Child A (done), Child B (undone). User taps Child B.

1. ViewModel calls `toggleTask(3)` → **A** runs:
   - Marks Child B done
   - Checks siblings: all done → marks Parent done
   - State: **everything done** ✓
2. ViewModel calls `toggleTask(parentId)` → **B** runs:
   - Parent is done → flips to **undone**
   - `uncompleteDescendants`: Child A + Child B flipped back to **undone**
3. **Final state: everything undone** — user's action was reversed.

### Why it's safe to remove B/C

Block A already implements the full rule:
- Marking done: if all siblings done → parent done
- Marking undone: if parent done → parent undone

Blocks B and C are redundant dead code that **re-applies** the same check after A already ran. Because `toggleTask` is a **flip** (not a set), the second application undoes the first.

**Fix**: Delete lines 119–121 in `TasksViewModel.kt` and lines 124–127 in `TodayViewModel.kt`.

---

## Bug 2 (Critical): Export schema misses day-constraint fields

`HabitSchema` in `ExportSchema.kt:27–34` lacks `daysOfWeek`, `daysOfMonth`, and `yearlyDates`.

Both the to-schema mapper (`ExportImpl.kt`) and from-schema mapper (`RestoreImpl.kt`) omit these fields. On export-restore round-trip, weekly/monthly/yearly habits become daily habits — **data loss**.

**Fix**: Add the three `Set<Int>` fields to `HabitSchema` and wire them in both mappers.

---

## Bug 3 (Critical): Restore compares export schema version against Room DB version

`RestoreImpl.kt`:
```kotlin
if (schema.schemaVersion != AppDatabase.SCHEMA_VERSION)
```

- `ExportSchema.schemaVersion` = `1` (export format version, hardcoded default)
- `AppDatabase.SCHEMA_VERSION` = `5` (Room migration counter)

These are unrelated. Every restore attempt fails with `OldSchema`. **Restore is non-functional.**

**Fix**: Replace `AppDatabase.SCHEMA_VERSION` with a dedicated export format constant (e.g. `ExportSchema.CURRENT_VERSION`).

---

## Bug 4 (Medium): Tag-item mappings not exported

`ExportSchema.kt` has no `taskTags` or `habitTags` fields. The export reads only the four entity tables but skips the junction tables. Tag-task and tag-habit associations are lost on export-restore.

**Fix**: Add `taskTags: Map<Long, List<Long>>` and `habitTags: Map<Long, List<Long>>` to `ExportSchema` and wire export/import in both platform implementations.

---

## Bug 5 (Medium): `deleteDoneTasks` orphans undone children

`SharedTaskRepo.deleteDoneTasks()` → `DELETE FROM tasks WHERE isDone = 1`. If a DONE parent has undone subtasks, those children keep a `parentId` pointing to a deleted row.

**Fix**: Cascade delete children recursively, or reject deletion of parents with undone children.

---

## Bug 6 (Low): `frequencyCount + daysOfWeek` ambiguity

For weekly habits with both `frequencyCount` and `daysOfWeek` set, the streak logic groups by week and sums counts. Three taps on Monday satisfies `frequencyCount = 3` even when `daysOfWeek = {1,3,5}` implied separate days. Design decision: whether `frequencyCount` is per-day or per-period.

---

## Bug 7 (Low): Undone children under done parent

The model doesn't enforce `parent isDone ⇒ all children isDone`. `upsertTask` allows creating undone children under a done parent, creating inconsistent states.

---

## Summary

| # | Impact | File(s) | Fix |
|---|--------|---------|-----|
| 1 | Critical | `TasksViewModel.kt:119–121`, `TodayViewModel.kt:124–127` | Remove redundant parent-toggle blocks |
| 2 | Critical | `ExportSchema.kt`, `ExportImpl.kt`, `RestoreImpl.kt` | Add `daysOfWeek`/`daysOfMonth`/`yearlyDates` to schema + mappers |
| 3 | Critical | `RestoreImpl.kt` | Use export format constant, not `AppDatabase.SCHEMA_VERSION` |
| 4 | Medium | `ExportSchema.kt`, `ExportImpl.kt`, `RestoreImpl.kt` | Add `taskTags`/`habitTags` maps to schema |
| 5 | Medium | `SharedTaskRepo.kt`, `TaskStorage` | Cascade or guard `deleteDoneTasks` for hierarchy |
| 6 | Low | `Habit.kt` (computeStreak) | Design decision — document intent |
| 7 | Low | `SharedTaskRepo.kt` | Validate or document parent/child invariant |
