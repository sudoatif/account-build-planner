package com.accountbuildplanner;

import static com.accountbuildplanner.Step.f;
import static com.accountbuildplanner.Step.q;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import static net.runelite.api.Quest.*;
import static net.runelite.api.Skill.*;

/**
 * Account builds, easiest first. Each route is an ordered guide: quest prerequisites always come earlier,
 * skill requirements sit on the quest that needs them. Data from the OSRS Wiki (Nov 2026).
 */
enum Build
{
	// Culinaromancer's Chest: Mithril gloves after freeing 5 RFD subquests, Adamant after 6. Picked by the least direct
	// training xp, counting each quest's xp rewards (OSRS Wiki Optimal quest guide data), out of every possible pick.
	MITHRIL_GLOVES("Mithril gloves", ItemID.HUNDRED_GAUNTLETS_LEVEL_6, needed(barrowsGloves(),
		RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE, RECIPE_FOR_DISASTER__MOUNTAIN_DWARF, RECIPE_FOR_DISASTER__PIRATE_PETE,
		RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE, RECIPE_FOR_DISASTER__EVIL_DAVE)),
	ADAMANT_GLOVES("Adamant gloves", ItemID.HUNDRED_GAUNTLETS_LEVEL_7, needed(barrowsGloves(),
		RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE, RECIPE_FOR_DISASTER__MOUNTAIN_DWARF, RECIPE_FOR_DISASTER__PIRATE_PETE,
		RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE, RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE, RECIPE_FOR_DISASTER__EVIL_DAVE)),
	BARROWS_GLOVES("Barrows gloves", ItemID.HUNDRED_GAUNTLETS_LEVEL_10, barrowsGloves()),
	SOTE("Song of the Elves", ItemID.PRIF_TELEPORT_CRYSTAL, byQp(needed(QuestData.optimalOrder(), Quest.SONG_OF_THE_ELVES))),
	QUEST_CAPE("Quest cape", ItemID.SKILLCAPE_QP, byQp(QuestData.optimalOrder()));

	final String displayName;
	final int icon; // item shown next to the build name
	final List<Step> route;

	Build(String displayName, int icon, List<Step> route)
	{
		this.displayName = displayName;
		this.icon = icon;
		this.route = route;
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	/**
	 * Recipe for Disaster in full. Required quests give 79 QP and RFD's subquests 9 more,
	 * so a fresh account needs ~87 QP of filler (f) quests to start the Culinaromancer at 175.
	 */
	private static List<Step> barrowsGloves()
	{
		return Stream.of(
			stage("Free-to-play", // and no-requirement quests
			q(COOKS_ASSISTANT, 1),
			f(SHEEP_SHEARER, 1),
			f(ROMEO__JULIET, 5),
			f(RUNE_MYSTERIES, 1),
			f(IMP_CATCHER, 1),
			f(WITCHS_POTION, 1),
			f(DORICS_QUEST, 1),
			f(ERNEST_THE_CHICKEN, 4),
			f(VAMPYRE_SLAYER, 3),
			f(PRINCE_ALI_RESCUE, 3),
			f(PIRATES_TREASURE, 2),
			f(X_MARKS_THE_SPOT, 1),
			f(MISTHALIN_MYSTERY, 1),
			f(THE_CORSAIR_CURSE, 2),
			q(GOBLIN_DIPLOMACY, 5),
			q(THE_RESTLESS_GHOST, 1),
			q(DEMON_SLAYER, 3),
			q(SHIELD_OF_ARRAV, 1).note("needs a partner"),
			f(BLACK_KNIGHTS_FORTRESS, 3).qpReq(12),
			f(THE_KNIGHTS_SWORD, 1).skill(MINING, 10),
			f(BELOW_ICE_MOUNTAIN, 1).qpReq(16)),

			stage("Early members", // and the first RFD subquests
			q(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, 1).after(COOKS_ASSISTANT).skill(COOKING, 10),
			// first subquest: its Cooking xp counts towards the others' Cooking levels
			q(RECIPE_FOR_DISASTER__PIRATE_PETE, 1).after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST).skill(COOKING, 31),
			q(RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE, 1).after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, GOBLIN_DIPLOMACY),
			q(DRUIDIC_RITUAL, 4),
			q(PRIEST_IN_PERIL, 1),
			q(NATURE_SPIRIT, 2).after(PRIEST_IN_PERIL, THE_RESTLESS_GHOST),
			q(PLAGUE_CITY, 1),
			q(BIOHAZARD, 3).after(PLAGUE_CITY),
			q(WITCHS_HOUSE, 4),
			q(MURDER_MYSTERY, 3),
			q(GERTRUDES_CAT, 1),
			q(FISHING_CONTEST, 1).skill(FISHING, 10),
			q(RECIPE_FOR_DISASTER__MOUNTAIN_DWARF, 1).after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, FISHING_CONTEST),
			q(DEATH_PLATEAU, 1),
			q(WATERFALL_QUEST, 1),
			q(TREE_GNOME_VILLAGE, 2),
			q(MERLINS_CRYSTAL, 6)),

			stage("Quick quest points",
			f(SHEEP_HERDER, 4),
			f(MONKS_FRIEND, 1),
			f(CLOCK_TOWER, 1),
			f(HAZEEL_CULT, 1),
			f(FIGHT_ARENA, 2),
			f(OBSERVATORY_QUEST, 2),
			f(DWARF_CANNON, 1),
			f(RAG_AND_BONE_MAN_I, 1),
			f(MAKING_HISTORY, 3).after(PRIEST_IN_PERIL, THE_RESTLESS_GHOST),
			f(CHILDREN_OF_THE_SUN, 1),
			f(TWILIGHTS_PROMISE, 1).after(CHILDREN_OF_THE_SUN),
			f(MEAT_AND_GREET, 1).after(CHILDREN_OF_THE_SUN),
			f(CLIENT_OF_KOUREND, 1).after(X_MARKS_THE_SPOT),
			f(THE_FORSAKEN_TOWER, 1).after(CLIENT_OF_KOUREND),
			f(TALE_OF_THE_RIGHTEOUS, 1).after(CLIENT_OF_KOUREND).skill(STRENGTH, 16).skill(MINING, 10),
			f(THE_DEPTHS_OF_DESPAIR, 1).after(CLIENT_OF_KOUREND).skill(AGILITY, 18),
			f(THE_QUEEN_OF_THIEVES, 1).after(CLIENT_OF_KOUREND).skill(THIEVING, 20),
			f(TRIBAL_TOTEM, 1).skill(THIEVING, 21),
			f(ICTHLARINS_LITTLE_HELPER, 2).after(GERTRUDES_CAT),
			f(RECRUITMENT_DRIVE, 1).after(BLACK_KNIGHTS_FORTRESS, DRUIDIC_RITUAL),
			f(THE_FREMENNIK_TRIALS, 3),
			f(HOLY_GRAIL, 2).after(MERLINS_CRYSTAL).skill(ATTACK, 20),
			f(MOUNTAIN_DAUGHTER, 2).skill(AGILITY, 20),
			f(THE_LOST_TRIBE, 1).after(GOBLIN_DIPLOMACY, RUNE_MYSTERIES).skill(AGILITY, 13).skill(THIEVING, 13).skill(MINING, 17),
			f(ELEMENTAL_WORKSHOP_I, 1).skill(MINING, 20).skill(SMITHING, 20).skill(CRAFTING, 20),
			f(THE_RIBBITING_TALE_OF_A_LILY_PAD_LABOUR_DISPUTE, 1).after(CHILDREN_OF_THE_SUN).skill(WOODCUTTING, 15)),

			stage("Low-level requirements",
			q(JUNGLE_POTION, 1).after(DRUIDIC_RITUAL).skill(HERBLORE, 3),
			q(THE_GOLEM, 1).skill(CRAFTING, 20).skill(THIEVING, 25),
			q(SHADOW_OF_THE_STORM, 1).after(THE_GOLEM, DEMON_SLAYER).skill(CRAFTING, 30),
			q(RECIPE_FOR_DISASTER__EVIL_DAVE, 1).after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, GERTRUDES_CAT, SHADOW_OF_THE_STORM).skill(COOKING, 25),
			q(BIG_CHOMPY_BIRD_HUNTING, 2).skill(FLETCHING, 5).skill(COOKING, 30).skill(RANGED, 30),
			f(CREATURE_OF_FENKENSTRAIN, 2).after(PRIEST_IN_PERIL, THE_RESTLESS_GHOST).skill(CRAFTING, 20).skill(THIEVING, 25),
			f(SHADES_OF_MORTTON, 3).after(PRIEST_IN_PERIL, DRUIDIC_RITUAL).skill(CRAFTING, 20).skill(HERBLORE, 15).skill(FIREMAKING, 5),
			q(THE_DIG_SITE, 2).after(DRUIDIC_RITUAL).skill(AGILITY, 10).skill(HERBLORE, 10).skill(THIEVING, 25),
			q(THE_TOURIST_TRAP, 2).skill(FLETCHING, 10).skill(SMITHING, 20),
			q(TROLL_STRONGHOLD, 1).after(DEATH_PLATEAU).skill(AGILITY, 15),
			f(DEATH_TO_THE_DORGESHUUN, 1).after(THE_LOST_TRIBE).skill(AGILITY, 23).skill(THIEVING, 23),
			f(GHOSTS_AHOY, 2).after(PRIEST_IN_PERIL, THE_RESTLESS_GHOST).skill(AGILITY, 25).skill(COOKING, 20),
			f(IN_SEARCH_OF_THE_MYREQUE, 2).after(NATURE_SPIRIT).skill(AGILITY, 25),
			q(THE_GRAND_TREE, 5).skill(AGILITY, 25).note("lvl 172 black demon, safespottable"),
			q(MONKEY_MADNESS_I, 3).after(THE_GRAND_TREE, TREE_GNOME_VILLAGE).note("lvl 195 jungle demon"),
			q(UNDERGROUND_PASS, 5).after(BIOHAZARD).skill(RANGED, 25),
			f(TROLL_ROMANCE, 2).after(TROLL_STRONGHOLD).skill(AGILITY, 28),
			f(SEA_SLUG, 1).skill(FIREMAKING, 30),
			f(ZOGRE_FLESH_EATERS, 1).after(BIG_CHOMPY_BIRD_HUNTING, JUNGLE_POTION).skill(SMITHING, 4).skill(HERBLORE, 8).skill(RANGED, 30),
			f(ETHICALLY_ACQUIRED_ANTIQUITIES, 1).after(CHILDREN_OF_THE_SUN, SHIELD_OF_ARRAV).skill(THIEVING, 25)),

			stage("Mid-level requirements",
			q(LOST_CITY, 3).skill(CRAFTING, 31).skill(WOODCUTTING, 36),
			f(FAIRYTALE_I__GROWING_PAINS, 2).after(LOST_CITY, NATURE_SPIRIT),
			q(DRAGON_SLAYER_I, 2).qpReq(32).note("bring an anti-dragon shield"),
			q(SHILO_VILLAGE, 2).after(JUNGLE_POTION).skill(CRAFTING, 20).skill(AGILITY, 32),
			f(DEATH_ON_THE_ISLE, 2).after(CHILDREN_OF_THE_SUN).skill(THIEVING, 34).skill(AGILITY, 32),
			f(THE_GIANT_DWARF, 2).skill(CRAFTING, 12).skill(FIREMAKING, 16).skill(MAGIC, 33).skill(THIEVING, 14),
			f(SPIRITS_OF_THE_ELID, 2).skill(MAGIC, 33).skill(RANGED, 37).skill(MINING, 37).skill(THIEVING, 37),
			q(ALFRED_GRIMHANDS_BARCRAWL, 0),
			q(HORROR_FROM_THE_DEEP, 2).after(ALFRED_GRIMHANDS_BARCRAWL).skill(AGILITY, 35),
			q(RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE, 1)
				.after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, BIG_CHOMPY_BIRD_HUNTING, BIOHAZARD, DEMON_SLAYER, MURDER_MYSTERY, NATURE_SPIRIT, WITCHS_HOUSE)
				.skill(COOKING, 40),
			q(RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE, 1).after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, BIG_CHOMPY_BIRD_HUNTING).skill(COOKING, 41).skill(FIREMAKING, 20),
			f(WATCHTOWER, 4).after(DRUIDIC_RITUAL).skill(MAGIC, 15).skill(THIEVING, 15).skill(AGILITY, 25).skill(HERBLORE, 14).skill(MINING, 40)),

			stage("High-level requirements",
			q(TEMPLE_OF_IKOV, 1).skill(THIEVING, 42).skill(RANGED, 40),
			// before Family Crest: its 20,000 Magic xp counts towards Family Crest's Magic 59
			q(DESERT_TREASURE_I, 3)
				.after(THE_DIG_SITE, TEMPLE_OF_IKOV, THE_TOURIST_TRAP, TROLL_STRONGHOLD, PRIEST_IN_PERIL, WATERFALL_QUEST)
				.skill(THIEVING, 53).skill(MAGIC, 50).skill(FIREMAKING, 50).skill(SLAYER, 10),
			q(FAMILY_CREST, 1).skill(MINING, 40).skill(SMITHING, 40).skill(MAGIC, 59).skill(CRAFTING, 40).note("lvl 170 demon, safespottable"),
			q(HEROES_QUEST, 1).after(SHIELD_OF_ARRAV, LOST_CITY, MERLINS_CRYSTAL, DRAGON_SLAYER_I, DRUIDIC_RITUAL).qpReq(55)
				.skill(COOKING, 53).skill(FISHING, 53).skill(HERBLORE, 25).skill(MINING, 50).note("needs a partner"),
			q(LEGENDS_QUEST, 0).startOnly().after(FAMILY_CREST, HEROES_QUEST, SHILO_VILLAGE, UNDERGROUND_PASS, WATERFALL_QUEST).qpReq(107)
				.note("only until you can enter Kharazi Jungle"),
			q(RECIPE_FOR_DISASTER__SIR_AMIK_VARZE, 1)
				.after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, FAMILY_CREST, HEROES_QUEST, SHILO_VILLAGE, UNDERGROUND_PASS, WATERFALL_QUEST)
				.qpReq(107).note("needs Kharazi Jungle access, lvl 227 black dragon"),
			q(RECIPE_FOR_DISASTER__KING_AWOWOGEI, 1).after(RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST, MONKEY_MADNESS_I)
				.skill(COOKING, 70).skill(AGILITY, 48),
			q(RECIPE_FOR_DISASTER__CULINAROMANCER, 1)
				.after(RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE, RECIPE_FOR_DISASTER__MOUNTAIN_DWARF, RECIPE_FOR_DISASTER__PIRATE_PETE,
					RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE, RECIPE_FOR_DISASTER__EVIL_DAVE, RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE,
					RECIPE_FOR_DISASTER__SIR_AMIK_VARZE, RECIPE_FOR_DISASTER__KING_AWOWOGEI, DESERT_TREASURE_I, HORROR_FROM_THE_DEEP)
				.qpReq(175).note("no Prayer allowed in the fights"))
		).flatMap(List::stream).collect(Collectors.toList());
	}

	/** The steps of {@code route} that {@code targets} need, themselves or as prerequisites, in route order. */
	private static List<Step> needed(List<Step> route, Quest... targets)
	{
		Map<Quest, Step> byQuest = route.stream().collect(Collectors.toMap(s -> s.quest, s -> s));
		Set<Quest> need = EnumSet.noneOf(Quest.class);
		Deque<Quest> open = new ArrayDeque<>(Arrays.asList(targets));
		while (!open.isEmpty())
		{
			Quest q = open.pop();
			if (need.add(q))
			{
				open.addAll(byQuest.get(q).after);
			}
		}
		return route.stream().filter(s -> need.contains(s.quest)).collect(Collectors.toList());
	}

	/** Stage a long generated route by the quest points you'll have when you reach each step: "0-50 QP", "50-100 QP", ... */
	private static List<Step> byQp(List<Step> route)
	{
		int qp = 0;
		for (Step s : route)
		{
			int band = qp / 50 * 50;
			s.stage = band + "-" + (band + 50) + " QP";
			qp += s.qp;
		}
		return route;
	}

	/** Tag steps with the route section the panel groups them under. */
	private static List<Step> stage(String name, Step... steps)
	{
		for (Step s : steps)
		{
			s.stage = name;
		}
		return List.of(steps);
	}
}
