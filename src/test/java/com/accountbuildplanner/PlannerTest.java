package com.accountbuildplanner;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PlannerTest
{
	private static final Build GLOVES = Build.BARROWS_GLOVES;

	@Test
	public void everyBuildIsAValidRoute()
	{
		for (Build b : Build.values())
		{
			Set<Quest> seen = EnumSet.noneOf(Quest.class);
			for (Step s : b.route)
			{
				// also proves generated routes only wait on quests RuneLite can track: the rest became notes
				for (Quest q : s.after)
				{
					assertTrue(b + ": " + s.quest.getName() + " needs " + q.getName() + " earlier", seen.contains(q));
				}
				assertTrue(b + ": duplicate " + s.quest.getName(), seen.add(s.quest));
				assertNotNull(b + ": " + s.quest.getName(), s.stage);
			}

			int qp = 0;
			for (Step s : Planner.plan(b, new Planner.Account()).todo)
			{
				assertTrue(b + ": " + s.quest.getName() + " needs " + s.qpReq + " QP, plan only has " + qp, qp >= s.qpReq);
				qp += s.qp;
			}
		}
	}

	@Test
	public void glovesBuildsFreeTheRightSubquests()
	{
		Set<Quest> mithril = EnumSet.of(Quest.RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE, Quest.RECIPE_FOR_DISASTER__MOUNTAIN_DWARF,
			Quest.RECIPE_FOR_DISASTER__PIRATE_PETE, Quest.RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE, Quest.RECIPE_FOR_DISASTER__EVIL_DAVE);
		assertEquals(mithril, freed(Build.MITHRIL_GLOVES));
		Set<Quest> adamant = EnumSet.copyOf(mithril);
		adamant.add(Quest.RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE);
		assertEquals(adamant, freed(Build.ADAMANT_GLOVES));

		for (Build b : List.of(Build.MITHRIL_GLOVES, Build.ADAMANT_GLOVES))
		{
			assertTrue(b + " has filler, the planner could skip a needed quest", b.route.stream().noneMatch(s -> s.filler));
		}
		assertEquals(ItemID.HUNDRED_GAUNTLETS_LEVEL_6, Build.MITHRIL_GLOVES.icon);
		assertEquals(ItemID.HUNDRED_GAUNTLETS_LEVEL_7, Build.ADAMANT_GLOVES.icon);
	}

	private static Set<Quest> freed(Build b)
	{
		return b.route.stream()
			.map(s -> s.quest)
			.filter(q -> q.name().startsWith("RECIPE_FOR_DISASTER__") && q != Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST)
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(Quest.class)));
	}

	@Test
	public void songOfTheElvesNeedsTheSeventies()
	{
		Planner.Plan plan = Planner.plan(Build.SOTE, new Planner.Account());
		for (Skill k : List.of(Skill.AGILITY, Skill.CONSTRUCTION, Skill.FARMING, Skill.HERBLORE, Skill.HUNTER, Skill.MINING, Skill.SMITHING, Skill.WOODCUTTING))
		{
			assertTrue(k.getName(), plan.skills.stream().anyMatch(g -> g.skill == k && g.target == 70));
		}
		assertEquals(15, Build.SOTE.route.size());
		assertEquals(Quest.SONG_OF_THE_ELVES, Build.SOTE.route.get(Build.SOTE.route.size() - 1).quest);
	}

	@Test
	public void questCapeIsStagedByQuestPoints()
	{
		List<Step> cape = Build.QUEST_CAPE.route;
		assertEquals(202, cape.size());
		assertEquals("0-50 QP", cape.get(0).stage);
		assertEquals("300-350 QP", cape.get(cape.size() - 1).stage);
		assertEquals(ItemID.SKILLCAPE_QP, Build.QUEST_CAPE.icon);
	}

	@Test
	public void xpRewardsComeBeforeTheQuestsThatNeedThem()
	{
		// Measured with the wiki's quest XP rewards: these two moves cut direct training for every gloves build
		List<Quest> order = GLOVES.route.stream().map(s -> s.quest).collect(Collectors.toList());
		// Desert Treasure I's 20,000 Magic xp counts towards Family Crest's Magic 59
		assertTrue(order.indexOf(Quest.DESERT_TREASURE_I) < order.indexOf(Quest.FAMILY_CREST));
		// Pirate Pete's Cooking xp lands before the other subquests' Cooking levels
		assertEquals(order.indexOf(Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST) + 1, order.indexOf(Quest.RECIPE_FOR_DISASTER__PIRATE_PETE));
	}

	@Test
	public void qpTargetOnlyForFillerBuilds()
	{
		assertEquals(175, Planner.plan(GLOVES, new Planner.Account()).qpTarget);
		assertEquals(0, Planner.plan(Build.MITHRIL_GLOVES, new Planner.Account()).qpTarget);
		assertEquals(0, Planner.plan(Build.QUEST_CAPE, new Planner.Account()).qpTarget);
	}

	@Test
	public void barrowsRouteMatchesTheWiki()
	{
		Map<Quest, Step> by = GLOVES.route.stream().collect(Collectors.toMap(s -> s.quest, s -> s));
		assertEquals(Integer.valueOf(15), by.get(Quest.WATCHTOWER).skills.get(Skill.MAGIC));
		for (Quest q : List.of(Quest.SHADES_OF_MORTTON, Quest.THE_DIG_SITE, Quest.WATCHTOWER, Quest.HEROES_QUEST))
		{
			assertTrue(q.getName(), by.get(q).after.contains(Quest.DRUIDIC_RITUAL));
		}
	}

	@Test
	public void everyStepHasAStage()
	{
		GLOVES.route.forEach(s -> assertNotNull(s.quest.getName(), s.stage));
		assertEquals(List.of("Free-to-play", "Early members", "Quick quest points",
				"Low-level requirements", "Mid-level requirements", "High-level requirements"),
			GLOVES.route.stream().map(s -> s.stage).distinct().collect(Collectors.toList()));
		assertEquals(ItemID.HUNDRED_GAUNTLETS_LEVEL_10, GLOVES.icon);
	}

	@Test
	public void freshAccountPlanMeetsEveryQuestPointGate()
	{
		Planner.Plan plan = Planner.plan(GLOVES, new Planner.Account());
		assertEquals(Quest.COOKS_ASSISTANT.getName(), plan.next);

		// training is listed right before the first quest that needs it
		List<String> rows = plan.route.stream().map(Planner.Item::text).collect(Collectors.toList());
		int train = rows.indexOf("Train Mining to 10");
		assertEquals(Quest.THE_KNIGHTS_SWORD.getName(), rows.get(train + 1));
		assertEquals(Planner.Status.NEXT, plan.route.get(0).status);
		assertEquals(Quest.COOKS_ASSISTANT, plan.route.get(0).step.quest);
		assertTrue(plan.checklist.isEmpty()); // no requirements: no checklist box
		assertTrue(plan.skills.stream().anyMatch(g -> g.skill == Skill.COOKING && g.level == 1 && g.target == 70));
	}

	@Test
	public void fillersSkippedWhenQuestPointsAlreadyCovered()
	{
		Planner.Account acc = new Planner.Account();
		acc.qp = 150; // earned from quests outside this route
		Planner.Plan plan = Planner.plan(GLOVES, acc);
		assertTrue(plan.todo.stream().noneMatch(s -> s.filler));
		assertEquals(GLOVES.route.stream().filter(s -> s.filler).count(), plan.skipped);
		assertTrue(plan.route.stream().noneMatch(i -> i.step != null && i.step.filler));
	}

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

	@Test
	public void skillBlockSaysWhatToTrain()
	{
		Planner.Account acc = new Planner.Account();
		GLOVES.route.forEach(s -> acc.quests.put(s.quest, QuestState.FINISHED));
		acc.quests.put(Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI, QuestState.NOT_STARTED);
		acc.quests.put(Quest.RECIPE_FOR_DISASTER__CULINAROMANCER, QuestState.NOT_STARTED);
		acc.levels.put(Skill.COOKING, 70);
		acc.levels.put(Skill.AGILITY, 40);
		acc.qp = 174;

		Planner.Plan plan = Planner.plan(GLOVES, acc);
		assertEquals("Train Agility to 48 (now 40)\nfor " + Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI.getName(), plan.next);
		assertNull(plan.alt); // the Culinaromancer needs Awowogei first
		assertEquals(2, plan.todo.size());
		// to train first, then skills already met; skills below a done quest's level (boosted) are left out
		assertEquals(2, plan.skills.size());
		assertEquals(48, plan.skills.get(0).target);
		Planner.SkillGoal cooking = plan.skills.get(1);
		assertEquals(Skill.COOKING, cooking.skill);
		assertTrue(cooking.met());

		assertEquals(List.of(
				"Finished " + Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST.getName(),
				"Finished " + Quest.MONKEY_MADNESS_I.getName(),
				"Cooking 70",
				"Train Agility to 48 (now 40)"),
			plan.checklist.stream().map(r -> r.text).collect(Collectors.toList()));
		assertEquals(List.of(true, true, true, false), plan.checklist.stream().map(r -> r.met).collect(Collectors.toList()));
		assertEquals(Skill.AGILITY, plan.checklist.get(3).skill);
	}

	@Test
	public void questPointRequirementComesLastInChecklist()
	{
		Planner.Account acc = new Planner.Account();
		GLOVES.route.forEach(s -> acc.quests.put(s.quest, QuestState.FINISHED));
		acc.quests.put(Quest.RECIPE_FOR_DISASTER__CULINAROMANCER, QuestState.NOT_STARTED);
		acc.qp = 174;

		List<Planner.Req> checklist = Planner.plan(GLOVES, acc).checklist;
		Planner.Req qp = checklist.get(checklist.size() - 1);
		assertEquals("Get 175 quest points (now 174)", qp.text);
		assertFalse(qp.met);
		acc.qp = 175;
		assertEquals("175 quest points", Planner.checklist(Planner.plan(GLOVES, acc).nextStep, acc).get(checklist.size() - 1).text);
	}

	@Test
	public void unlockedBuildStillListsMetSkills()
	{
		Planner.Account acc = new Planner.Account();
		GLOVES.route.forEach(s -> acc.quests.put(s.quest, QuestState.FINISHED));
		acc.levels.put(Skill.COOKING, 75);

		Planner.Plan plan = Planner.plan(GLOVES, acc);
		assertNull(plan.nextStep);
		assertEquals(1, plan.skills.size());
		assertEquals(70, plan.skills.get(0).target);
		assertTrue(plan.skills.get(0).met());
		assertTrue(plan.route.stream().allMatch(i -> i.status == Planner.Status.DONE));
		assertTrue(plan.checklist.isEmpty());
	}
}
