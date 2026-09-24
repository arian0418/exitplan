# ExitPlan improvement plan

## Intended experience

ExitPlan remains a small, offline Java 17 Swing application. A navy header and
teal accents introduce a spacious light workspace. The leave-by time, total
duration, and live countdown lead the screen. A settings panel and activity
editor sit beside a complete, read-only itinerary. At smaller sizes the workspace
scrolls without hiding controls.

The user can start from Dinner, Dinner + movie, or Errands examples, edit a
deadline and durations, add/remove/reorder stops, calculate, save a plan, and
copy/export its itinerary. Editing any input clears calculated output immediately;
the user always knows when another calculation is needed. The last stop has no
onward travel; moving/removing stops preserves this rule and explains the change.

## Design and implementation tasks

1. Expand plain-Java calculator tests around nulls, invalid bounds, overnight and
   past plans, and contiguous, complete timeline segments; observe failures.
2. Implement shared input validation and immutable calculated steps. Keep local
   date/time arithmetic explicit and handle unsupported dates without overflow.
3. Add a small immutable plan input object and JDK Properties persistence with
   version checks, a file-size bound, bounded stop counts, complete validation,
   and temporary-file replacement. Add round-trip and malformed-file tests.
4. Add readable UTF-8 itinerary formatting and export; use the same calculated
   steps as the screen. Test chronological dates and all allowances.
5. Build the Swing workspace, reusable colors/spacing, validated table editor,
   presets, keyboard actions, live status, and save/load/copy/export actions.
   Verify dirty state, editing, ordering, presets, and minimum-size layout with
   real Swing components on a virtual display.
6. Update Windows/Linux setup and demo instructions, architecture and limitations,
   test workflow, and run every test from a clean output directory.
7. Inspect the rendered app, fix material issues, record results, and commit
   locally. No remote publication or additional dependencies.

## Validation rules

- At least one and at most 100 named activities; names contain 1–80 visible
  characters and no control characters.
- Activity duration: 1–1440 minutes. Travel: 0–1440 minutes per leg.
- Parking/walking: 0–240 minutes each way. Buffer: 0–1440 minutes.
- Final onward travel must be zero; return travel has its own setting.
- Supported calendar years: 1900–9999, with a representable departure in that
  range. Past departure is a visible warning, not a calculation failure.
- Saved files have a schema version and are limited to 256 KiB. Loaded data uses
  the same validation as direct calculations.

## Evidence to collect

Keep the original overnight fixture (225 minutes, 2026-09-30 20:45). Add meaningful
core, persistence, and GUI checks. Run `javac -Xlint:all` and the complete test
suite. Verify startup, successful edit/recalculate, invalid values, last-stop
behavior, round-trip save/load, export/copy, and both normal and minimum window
sizes. Report exact commands, counts, and known limitations.
