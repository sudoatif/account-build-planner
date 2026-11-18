# More builds: Mithril gloves, Adamant gloves, Song of the Elves, Quest cape

## Why

The user wants more account goals than Barrows gloves in the build picker.

## Builds (dropdown order, easiest first)

| Build | Route | Size |
|---|---|---|
| Mithril gloves | Barrows route trimmed to what 5 freed RFD subquests need | 18 quests, highest level Cooking 41 |
| Adamant gloves | Same, 6 subquests | 22 quests, adds Crafting 30, Thieving 25 |
| Barrows gloves | Unchanged (hand-made route with filler quests) | |
| Song of the Elves | SOTE and everything it needs | 15 quests, 70 in 8 skills |
| Quest cape | Every quest RuneLite can track | 202 steps (195 quests + 7 miniquests they need), 344 QP |

- Glove tiers from the wiki's Culinaromancer's Chest page: Mithril = 5 subquests freed, Adamant = 6.
- **Which subquests (my call):** Mithril = Goblin generals, Mountain Dwarf, Pirate Pete, Lumbridge Guide,
  Skrach Uglogwee (no Crafting/Thieving training at all); Adamant adds Evil Dave. Sir Amik and Awowogei are by far
  the hardest, so they're never picked.
- Mithril/Adamant are trimmed copies of the Barrows route, so they keep its notes ("needs a partner") and stages.
- Song of the Elves and Quest cape come from generated wiki data, in the order of the wiki's **Optimal quest guide**.
  Their Route-tab stages are quest point bands ("0-50 QP", "50-100 QP", ...), which stay in route order.
- Icons: Mithril gloves, Adamant gloves, Barrows gloves, a crystal item for SOTE, Quest point cape.

## Data

- `QuestData.java`, generated: every quest in Optimal quest guide order with its QP, prerequisite quests, and levels.
  Requirements come from the wiki's `Module:Questreq/data`. That module hasn't been updated since April 2025, so the
  19 newer quests it lacks are read from their own wiki pages instead.
- The generator (`tools/quest-data.js`, Node) stays in the repo, so the data can be regenerated when Jagex releases
  quests.
- Requirements RuneLite can't check (Barbarian Firemaking, Architectural Alliance, Kudos 100, Combat 85,
  "started X") become the step's note.
- Ironman-only requirements are left out. Boostable levels count as needed, as in the Barrows route.
- Not in RuneLite yet, so left out: The Birds and the Wasps (released 11 Nov 2026) and A Shell of a Friend. The last
  Quest cape step's note says to do them too.

## Planner change

- The QP part of the progress line ("112/175 QP") only shows for builds that use filler quests to hit a QP target (Barrows).
  Otherwise someone with QP from other quests would see e.g. "150/20 QP". Other builds show the quest count only.

## Fixes to the Barrows route found while cross-checking with the wiki

- Druidic Ritual is a prerequisite of Shades of Mort'ton, The Dig Site, Watchtower and Heroes' Quest (it's already earlier in
  the route; it just wasn't listed).
- Watchtower needs Magic 15, not 14.

## Tests

For every build: prerequisites come earlier in the route, a fresh account's plan meets every QP gate in order, and every
step has a stage. Mithril/Adamant contain no filler and exactly 5/6 freed subquests. Song of the Elves needs the
eight 70s. A test pins the QP line rule.

## Not in scope

- Quest Helper "Start" button (parked: Quest Helper has no way for other plugins to start a quest).
- Bronze through Black, Rune and Dragon gloves (each is one line once this lands).
