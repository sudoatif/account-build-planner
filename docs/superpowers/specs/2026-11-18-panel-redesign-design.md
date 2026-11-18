# Build Planner panel redesign

## Why

The sidebar is one long stack of identical dark rows with headers. Nothing stands out, and the
last change (folding sections) left it looking the same. The user wants a real redesign, not a tweak.

## Goals

- See the next job at a glance, with exactly what's missing for it.
- See the whole route and where you are in it.
- See every skill the build needs, met or not, in one compact view.

## Non-goals

- No new builds, no change to how the route is planned (filler, QP, training placement stay as they are).
- No remembering the chosen build across restarts (would need a config class; add when there's >1 build).
- No in-game overlay.

## Anti-goals

- The old layout with tabs bolted on top.
- Anything that reads game state every frame.

## Constraints

- Swing in the RuneLite sidebar: about 205 px of usable width (`PluginPanel.PANEL_WIDTH` is 225 px, minus its border).
- RuneLite's look: `ColorScheme` colours, RuneScape fonts, the client's own components.
- The RuneScape font has no ✓ ✗ glyphs, so ticks/crosses are drawn as tiny images.
- Java 11. Game reads stay on the client thread, UI updates on the EDT (as today).

## Approaches considered

1. **Tabs: Next / Route / Skills**, picked by the user. A header that is always visible, then three tabs. It covers
   quick checks, planning and skills without one endless scroll. The cost is one click to reach the route.
2. **Focus card**: one huge next-job card, the next 3 steps, and a link to the full route. Best for quick checks,
   but the route and skills get pushed into a second screen.
3. **Route timeline**: the whole route as a vertical timeline with requirement tags. Good for planning,
   but it's still one long scroll and there's no skills overview.

All three are Swing in the sidebar; there is no other way to build a RuneLite panel. The tabs use
RuneLite's `MaterialTabGroup` (the same tabs as the Hiscore and Loot Tracker panels).

## Design

### Header (always visible)

- Build picker: dropdown showing the build's item icon (Barrows gloves = `ItemID.HUNDRED_GAUNTLETS_LEVEL_10`, via `ItemManager`), with the Start button beside it.
- Summary line: `38 of 74 quests · 112/175 QP`. Only quests the plan actually uses count; skipped filler doesn't.
- Thin progress bar of quests done, with the % on the right.

### Next tab

- **Next job card** (orange left accent): quest icon, name, `+N QP`, a filler tag if it's filler, and the note.
  Below that, a checklist of *every* requirement of that quest (quests to finish first, skill levels, quest points),
  each with a green tick or a red cross and "(now 22)" on the unmet ones. Then a **Quest guide** button that opens the wiki.
- **Can do now instead**: the doable alternative quest, shown only when the next job is blocked.
- **Then**: the next 3 rows of the route, numbered (quests and "Train X to N" steps), and a `Full route ›`
  link that switches to the Route tab.
- When the build is finished, a large green "Barrows gloves unlocked!" card replaces all of this.

### Route tab

- The route grouped by stage. The stages are today's comment groups in `Build.java`, which become data,
  with short names that fit the sidebar: Free-to-play, Early members, Quick quest points,
  Low-level requirements, Mid-level requirements, High-level requirements.
- Each stage header shows its name and `done/total`. Fully finished stages start folded; click to open.
- Rows: done = dimmed with a green tick; the current job = orange accent; still to do = normal;
  training steps = orange, placed right before the quest that needs them.
- Skipped filler is hidden, with one muted line at the end: "N filler quests not needed, you'll have the QP".
- Click any row to open its wiki page (quest guide or training guide), as today.

### Skills tab

- Summary: `9 of 16 met`.
- 2-column grid. Each cell has the skill icon, `current/target`, and a thin bar (orange until met, green when met).
  Tooltip: xp left. Click: wiki training guide.
- Order: skills still to train first, then met ones, each in RuneLite's skill order.
- Target per skill = the highest level the plan needs (as now).

### Behaviour

- **Start button stays** (user's call), next to the build picker. Same behaviour as today: Start plans for the
  selected build and turns into Refresh; after that it re-plans by itself on level-ups and finished quests.
  Before Start, or when logged out, the panel shows a message in a `PluginErrorPanel` instead of the tabs.
- The open tab and folded stages stay as they are when the plan refreshes.

### Data / logic changes

- `Step` gets its stage; `Build` gets its item icon id.
- `Planner.Plan` adds what the new tabs need: the whole route with a status per row (done / current / to do /
  training), the skipped-filler count, and the next job's requirement checklist (each item: text + met).
  `blocker()` becomes a filter over that checklist so the two can't disagree.
- `PlannerTest` covers row statuses, the checklist, and the skipped-filler count.

## My calls (change any of these)

- Hiding skipped filler behind one line rather than showing it greyed out.
- Skill grid order: to train first, then met.
- Finished stages folded by default.

## Left to the builder

Exact spacing, font sizes, icon drawing, and how the panel is split into classes.
