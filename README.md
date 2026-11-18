# Account Build Planner

Pick an account build in the sidebar, press **Start**, and the plugin reads your quests, skill levels and quest points to tell you the next job: the next quest in the route, or the skill you need to train first. It re-plans by itself when you level up or finish a quest.

## Builds

- **Mithril gloves** / **Adamant gloves**: free 5 / 6 Recipe for Disaster subquests (Goblin generals, Mountain Dwarf, Pirate Pete, Lumbridge Guide, Skrach Uglogwee, and Evil Dave for Adamant): the ones that need the least training.
- **Barrows gloves**: the whole of Recipe for Disaster. Required quests come in prerequisite order. Filler quests are added only until you'll have the 175 quest points the Culinaromancer needs, and are skipped if you already have the points.
- **Song of the Elves**: everything Song of the Elves needs, up to the eight level-70 skills, for Prifddinas.
- **Quest cape**: every quest RuneLite can track, in the OSRS Wiki's [Optimal quest guide](https://oldschool.runescape.wiki/w/Optimal_quest_guide) order.

## Running it locally

```
./gradlew run
```

To log in with a Jagex Account, see [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Adding a build

Add an entry to `Build.java`. Either write an ordered route of `q(...)` (required) and `f(...)` (filler) steps, or take what a goal needs from an existing route with `needed(route, quests...)`. `PlannerTest` checks that every build's prerequisites come earlier in its route and that a fresh account reaches every quest point gate.

## Quest data

`QuestData.java` (used by Song of the Elves and Quest cape) is generated from the OSRS Wiki. When new quests come out, update RuneLite and run:

```
node tools/quest-data.js
```

It needs Node 18+ and Java on the PATH, and fails loudly if the wiki's order would break a prerequisite.
