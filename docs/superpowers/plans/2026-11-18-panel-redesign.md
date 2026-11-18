# Build Planner Panel Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-scroll sidebar with a header (build picker with item icon + Start, progress summary) and three tabs: Next, Route, Skills.

**Architecture:** `Planner` stays pure logic and gains what the tabs need: route rows with a status, a requirement checklist for the next job, and a skipped-filler count. The panel becomes a thin shell (header, Start, `MaterialTabGroup`), with one `JPanel` class per tab and shared Swing helpers in `Ui`. Each view re-renders from the latest `Plan`, so the selected tab and fold state survive refreshes.

**Tech Stack:** Java 11, Swing, RuneLite client 1.13.6 (`MaterialTabGroup`, `ThinProgressBar`, `PluginErrorPanel`, `ItemManager`, `SkillIconManager`, `ColorScheme`, `FontManager`, `LinkBrowser`), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-18-panel-redesign-design.md`

## Global Constraints

- Java 11 (`options.release.set(11)`); no new dependencies.
- Sidebar content is about 205 px wide. Every multi-word label goes through `Ui.html(text, width)` so it wraps.
- RuneScape fonts have no ✓ ✗ › ▾ glyphs, so never put them in text. Ticks and crosses are drawn images (`Ui.TICK`, `Ui.CROSS`).
- Colours only from `ColorScheme`: `BRAND_ORANGE` = accent / still to train, `PROGRESS_COMPLETE_COLOR` = met / done, `PROGRESS_ERROR_COLOR` = cross, `LIGHT_GRAY_COLOR` = muted text, `DARKER_GRAY_COLOR` / `DARKER_GRAY_HOVER_COLOR` = rows.
- Wiki links via `LinkBrowser`, as today (`https://oldschool.runescape.wiki/w/Special:Search?search=`).
- The plugin keeps reading the game on the client thread and pushing to Swing with `SwingUtilities.invokeLater` (unchanged).
- The Start button stays: Start plans the selected build and becomes Refresh; it re-plans by itself on level-ups and quest points (unchanged).
- No git commits: the user hasn't asked for any. The restore point is `D:\Projects\osrs-build-planner-backups\osrs-build-planner-2026-11-18-before-redesign.zip`.

## Review Focus

1. Long names ("The Ribbiting Tale of a Lily Pad Labour Dispute", RFD subquests with notes) wrap inside the sidebar and never widen it or add a horizontal scrollbar.
2. A refresh (level-up) while on the Route or Skills tab, with stages unfolded, keeps the same tab and fold state.
3. A next quest with no requirements (Cook's Assistant on a fresh account) shows no empty checklist box. Pinned by Task 2's `freshAccount...` assertion `plan.checklist.isEmpty()`; Task 3 hides the section when it's empty.
4. A finished build shows the unlocked card on Next, every Route row ticked with every stage folded, and every skill green. Pinned by Task 2's `unlockedBuildStillListsMetSkills` additions.
5. Start pressed while logged out shows the message and no stale tabs; Refresh after logging in brings the tabs back. (Manual, Task 6.)

---

### Task 1: Stages and build icon as data

**Files:**
- Modify: `src/main/java/com/buildplanner/Step.java`
- Modify: `src/main/java/com/buildplanner/Build.java`
- Test: `src/test/java/com/buildplanner/PlannerTest.java`

**Interfaces:**
- Produces: `Step.stage` (`String`, package-private, set by `Build`); `Build.icon` (`final int`, an item id); `Build(String displayName, int icon, List<Step> route)`.

- [ ] **Step 1: Write the failing test**

```java
@Test
public void everyStepHasAStage()
{
	GLOVES.route.forEach(s -> assertNotNull(s.quest.getName(), s.stage));
	assertEquals(List.of("Free-to-play", "Early members", "Quick quest points",
			"Low-level requirements", "Mid-level requirements", "High-level requirements"),
		GLOVES.route.stream().map(s -> s.stage).distinct().collect(Collectors.toList()));
	assertEquals(ItemID.HUNDRED_GAUNTLETS_LEVEL_10, GLOVES.icon);
}
```

- [ ] **Step 2: Run it to make sure it fails**

Run: `.\gradlew.bat test --tests com.buildplanner.PlannerTest`
Expected: compile FAIL (`stage` / `icon` not found).

- [ ] **Step 3: Implement**

In `Step`, add `String stage;`. In `Build`, add `final int icon` and a helper `private static List<Step> stage(String name, Step... steps)` that sets each step's `stage` and returns them. `barrowsGloves()` concatenates six `stage(...)` calls in place of the six comment groups, with the steps kept in the same order (`Stream.of(...).flatMap(List::stream).collect(toList())`). `BARROWS_GLOVES("Barrows gloves", ItemID.HUNDRED_GAUNTLETS_LEVEL_10, barrowsGloves())`, using `net.runelite.api.gameval.ItemID`.

- [ ] **Step 4: Run the tests and make sure they pass**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL, all `PlannerTest` tests pass, including `prerequisitesComeEarlierInRoute` (so the order didn't change).

---

### Task 2: Planner gives route rows, checklist, skipped count

**Files:**
- Modify: `src/main/java/com/buildplanner/Planner.java`
- Test: `src/test/java/com/buildplanner/PlannerTest.java`

**Interfaces:**
- Consumes: `Step.stage` (Task 1).
- Produces:
  - `enum Planner.Status { DONE, NEXT, TODO, TRAIN }`
  - `Planner.Item(Step step, Skill skill, int level, Status status, String stage)`: adds `status` and `stage` fields; `text()` unchanged. TRAIN rows take the stage of the quest that needs them.
  - `Planner.Req(String text, boolean met, Skill skill)`: `skill` is null unless it's a skill requirement.
  - `Plan.route` (`List<Item>`), `Plan.skipped` (`int`), `Plan.checklist` (`List<Req>`). **`Plan.upcoming` is removed**, because "Then" = rows after the NEXT row of `route`.
  - `static List<Req> checklist(Step s, Account acc)`; `blocker(s, acc)` keeps its signature and output, built as the unmet `text`s of `checklist` joined by `"\n"` (null if none).

- [ ] **Step 1: Write the failing tests**

Update and add to `PlannerTest`:

```java
// freshAccountPlanMeetsEveryQuestPointGate: replace the plan.upcoming lines with
List<String> rows = plan.route.stream().map(Planner.Item::text).collect(Collectors.toList());
int train = rows.indexOf("Train Mining to 10");
assertEquals(Quest.THE_KNIGHTS_SWORD.getName(), rows.get(train + 1));
assertEquals(Planner.Status.NEXT, plan.route.get(0).status);
assertEquals(Quest.COOKS_ASSISTANT, plan.route.get(0).step.quest);
assertTrue(plan.checklist.isEmpty()); // no requirements: no checklist box

@Test
public void routeMarksDoneNextAndTraining()
{
	Planner.Account acc = new Planner.Account();
	acc.quests.put(Quest.COOKS_ASSISTANT, QuestState.FINISHED);
	Planner.Plan plan = Planner.plan(GLOVES, acc);

	assertEquals(Planner.Status.DONE, plan.route.get(0).status);
	List<Planner.Item> next = plan.route.stream().filter(i -> i.status == Planner.Status.NEXT).collect(Collectors.toList());
	assertEquals(1, next.size());
	assertEquals(plan.nextStep, next.get(0).step);
	for (int i = 0; i < plan.route.size(); i++)
	{
		Planner.Item row = plan.route.get(i);
		assertNotNull(row.text(), row.stage);
		if (row.status == Planner.Status.TRAIN)
		{
			// same stage as the quest it's for
			Planner.Item quest = plan.route.subList(i, plan.route.size()).stream().filter(r -> r.step != null).findFirst().get();
			assertEquals(quest.stage, row.stage);
		}
	}
}

// skillBlockSaysWhatToTrain: add
assertEquals(List.of(
		"Finished " + Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST.getName(),
		"Finished " + Quest.MONKEY_MADNESS_I.getName(),
		"Cooking 70",
		"Train Agility to 48 (now 40)"),
	plan.checklist.stream().map(r -> r.text).collect(Collectors.toList()));
assertEquals(List.of(true, true, true, false), plan.checklist.stream().map(r -> r.met).collect(Collectors.toList()));
assertEquals(Skill.AGILITY, plan.checklist.get(3).skill);

// fillersSkippedWhenQuestPointsAlreadyCovered: add
Planner.Plan plan = Planner.plan(GLOVES, acc);
assertEquals(GLOVES.route.stream().filter(s -> s.filler).count(), plan.skipped);
assertTrue(plan.route.stream().noneMatch(i -> i.step != null && i.step.filler));

// unlockedBuildStillListsMetSkills: add
assertTrue(plan.route.stream().allMatch(i -> i.status == Planner.Status.DONE));
assertTrue(plan.checklist.isEmpty());
```

QP requirement texts follow the same pattern (met: `"175 quest points"`, unmet: `"Get 175 quest points (now 174)"`), and come last in the checklist.

- [ ] **Step 2: Run it to make sure it fails**

Run: `.\gradlew.bat test`
Expected: compile FAIL (`route`, `Status`, `checklist`, `skipped` not found).

- [ ] **Step 3: Implement**

The first pass over the route stays as it is (it decides `todo`) and also counts `skipped` for filler it drops. Then a second pass in route order: done step → DONE row; todo step → a TRAIN row for each skill level it raises (the existing `goal` logic, now *including* the first todo step), then its quest row (NEXT for `todo.get(0)`, else TODO); skipped filler → no row. Checklist order: `after` quests, then `skills` (EnumMap order), then QP if `qpReq > 0`. `p.checklist = checklist(first, acc)` when there is a next step. `alt` and `block` keep working through `blocker`.

- [ ] **Step 4: Run the tests and make sure they pass**

Run: `.\gradlew.bat test`
Expected: BUILD SUCCESSFUL. `BuildPlannerPanel` will no longer compile against `upcoming`, so in this task change its "Coming up" loop to iterate `route` rows after the NEXT row. It gets replaced in Task 3.

---

### Task 3: Ui helpers, new panel shell, Next tab

**Files:**
- Create: `src/main/java/com/buildplanner/Ui.java`
- Create: `src/main/java/com/buildplanner/NextView.java`
- Modify (rewrite): `src/main/java/com/buildplanner/BuildPlannerPanel.java`
- Modify: `src/main/java/com/buildplanner/BuildPlannerPlugin.java`

**Interfaces:**
- Consumes: `Plan.route`, `Plan.checklist`, `Plan.skipped`, `Status`, `Req` (Task 2); `Build.icon` (Task 1).
- Produces:
  - `final class Ui` (static only): `QUEST_ICON` (the 16×16 scroll the plugin draws today, moved here), `TICK`, `CROSS` (10×10 drawn images in `PROGRESS_COMPLETE_COLOR` / `PROGRESS_ERROR_COLOR`), `JPanel row(Image icon, JComponent body, String wikiSearch, String tooltip)` (today's hover/click row), `JLabel header(String)`, `JLabel muted(String text, int width)`, `ThinProgressBar bar(int value, int max, Color color)`, `String html(String escapedBody, int width)`, `String esc(String)`, `void wiki(String search)`, plus the width constants `FULL = 170`, `ROW = 140`, `CARD = 132` (moved from the panel).
  - `class NextView extends JPanel`: `NextView(SkillIconManager skillIcons, Runnable showRoute)`, `void show(Planner.Plan p)`.
  - `BuildPlannerPanel(SkillIconManager skillIcons, ItemManager items, Consumer<Build> onStart)`; keeps `void showMessage(String)` and `void showPlan(Planner.Plan)` (the plugin's calls don't change).

- [ ] **Step 1: Build `Ui`** by moving the helpers out of the current panel unchanged. Add `TICK`, `CROSS`, `muted`, `bar`, `wiki`.

- [ ] **Step 2: Build `NextView`** (top to bottom, re-rendered on each `show`):
  - Unlocked (`p.nextStep == null`): one card holding `p.next` in bold, `PROGRESS_COMPLETE_COLOR`, with a 3 px green left border.
  - Otherwise, the **Next job** card (3 px `BRAND_ORANGE` left border, as today): quest icon, `s.label()` bold white, muted `"+N QP"` (+ `" · Filler"` for filler). Then, only if `p.checklist` is non-empty, one line per `Req`: `TICK`/`CROSS` + text (met muted, unmet white); unmet skill lines open `"<Skill> training"` on the wiki. Then a `JButton("Quest guide")` → `Ui.wiki(s.quest.getName())`.
  - **Can do now instead** header + quest row, only if `p.alt != null`.
  - **Then** header + the first 3 `route` rows after the NEXT row, prefixed `2`, `3`, `4`; then a clickable muted `"See full route"` label that runs `showRoute`.

- [ ] **Step 3: Rewrite `BuildPlannerPanel`**:
  - Top row: `JComboBox<Build>` (renderer shows `items.getImage(build.icon)` + name; call `onLoaded(builds::repaint)` on the image) in CENTER, Start `JButton` in EAST. Start keeps today's behaviour (text becomes "Refresh").
  - Summary (hidden until a plan is shown): muted `"<done> of <done+todo> quests · <qp>/<qpTarget> QP"` (drop the QP part when `qpTarget == 0`) with `"<pct>%"` on the right, then `Ui.bar(done, done + todo, BRAND_ORANGE)`.
  - `MaterialTabGroup tabs = new MaterialTabGroup(display)` with a "Next" `MaterialTab` (content = the `NextView`). Select Next on first plan only; after that, never change the selection.
  - A `PluginErrorPanel` shows `showMessage` text (title "Build Planner") with the summary and tabs hidden. `showPlan` hides it and shows them.

- [ ] **Step 4: Plugin wiring**: inject `ItemManager`, pass it to the panel, use `Ui.QUEST_ICON` for the nav button, and delete `icon()`.

- [ ] **Step 5: Run the tests and make sure they pass**

Run: `.\gradlew.bat build`
Expected: BUILD SUCCESSFUL (compiles, all tests pass).

---

### Task 4: Route tab

**Files:**
- Create: `src/main/java/com/buildplanner/RouteView.java`
- Modify: `src/main/java/com/buildplanner/BuildPlannerPanel.java` (add the tab)

**Interfaces:**
- Consumes: `Ui` (Task 3), `Plan.route`, `Plan.skipped`, `Item.status`, `Item.stage`.
- Produces: `class RouteView extends JPanel`: `RouteView(SkillIconManager skillIcons)`, `void show(Planner.Plan p)`.

- [ ] **Step 1: Build `RouteView`**:
  - Group `p.route` by `stage` in order of first appearance.
  - Stage header row, clickable: name on the left (bold), `"<done>/<quests>"` muted on the right (count quest rows only).
  - Fold state: `Map<String, Boolean> open`, default `!allQuestRowsDone`. A click flips it and re-renders from the last plan.
  - Rows (all `Ui.row`, all open the wiki):
    - DONE: `TICK` icon, muted text.
    - NEXT: quest icon, white text, 3 px `BRAND_ORANGE` left border.
    - TODO: quest icon, white text (muted for filler), with `"+N QP"` in grey.
    - TRAIN: skill icon, `BRAND_ORANGE` `"Train X to N"`, linking to the `"<Skill> training"` page.
  - If `p.skipped > 0`, a muted footer: `"<n> filler quests not needed, you'll have the QP"`.
- [ ] **Step 2: Add a "Route" `MaterialTab`** after Next, and make NextView's `showRoute` select it.
- [ ] **Step 3: Verify:** run `.\gradlew.bat build` and expect BUILD SUCCESSFUL.

---

### Task 5: Skills tab

**Files:**
- Create: `src/main/java/com/buildplanner/SkillsView.java`
- Modify: `src/main/java/com/buildplanner/BuildPlannerPanel.java` (add the tab)

**Interfaces:**
- Consumes: `Ui` (Task 3), `Plan.skills`, `SkillGoal.met()`.
- Produces: `class SkillsView extends JPanel`: `SkillsView(SkillIconManager skillIcons)`, `void show(Planner.Plan p)`.

- [ ] **Step 1: Build `SkillsView`**:
  - Muted summary `"<met> of <total> met"`.
  - `GridLayout(0, 2, 4, 4)` grid in `p.skills` order (Planner already puts to-train first, then met, each in `Skill` enum order).
  - Cell: small skill icon + `"<level>/<target>"` (`PROGRESS_COMPLETE_COLOR` when met, white otherwise) above `Ui.bar(xp, targetXp, met ? PROGRESS_COMPLETE_COLOR : BRAND_ORANGE)`.
  - Tooltip: met → `"Done: the build needs level N"`; else `"%,d xp to level N"`. A click opens `"<Skill> training"`.
  - If `p.skills` is empty, show a muted `"No skill requirements"`.
- [ ] **Step 2: Add a "Skills" `MaterialTab`** after Route.
- [ ] **Step 3: Verify:** run `.\gradlew.bat build` and expect BUILD SUCCESSFUL. Then delete the leftover `section`/`expanded`/`shown` code from the old panel if any survived (it should be gone after the Task 3 rewrite).

---

### Task 6: In-game check (user)

- [ ] **Step 1:** Launch with `Start-Process .\gradlew.bat run` (its own console window, so the low-memory cleanup can't kill it).
- [ ] **Step 2:** The user checks the golden path (pick the build, Start, flip all three tabs, open a wiki link) and the 5 Review Focus items. Done only when they confirm it works in-game.
