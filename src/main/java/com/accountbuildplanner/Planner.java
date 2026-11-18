package com.accountbuildplanner;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.Experience;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/** Pure logic: account snapshot + build route -> what to do next. No client access, so it's unit-testable. */
final class Planner
{
	/** Snapshot of the account, read on the client thread. */
	static final class Account
	{
		final Map<Quest, QuestState> quests = new EnumMap<>(Quest.class);
		final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
		final Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
		int qp;

		boolean finished(Quest q)
		{
			return quests.getOrDefault(q, QuestState.NOT_STARTED) == QuestState.FINISHED;
		}

		boolean done(Step s)
		{
			return s.startOnly
				? quests.getOrDefault(s.quest, QuestState.NOT_STARTED) != QuestState.NOT_STARTED
				: finished(s.quest);
		}

		int level(Skill s)
		{
			return levels.getOrDefault(s, 1);
		}
	}

	enum Status
	{
		DONE, NEXT, TODO, TRAIN
	}

	/** A route row: a quest, or training placed right before the first quest that needs it. */
	static final class Item
	{
		final Step step;
		final Skill skill;
		final int level;
		final Status status;
		final String stage; // training rows take the stage of the quest they're for

		Item(Step step, Skill skill, int level, Status status, String stage)
		{
			this.step = step;
			this.skill = skill;
			this.level = level;
			this.status = status;
			this.stage = stage;
		}

		String text()
		{
			return step != null ? step.label() : "Train " + skill.getName() + " to " + level;
		}
	}

	static final class SkillGoal
	{
		final Skill skill;
		final int level;
		final int target;
		final int xp;
		final int targetXp;

		SkillGoal(Skill skill, int level, int target, int xp)
		{
			this.skill = skill;
			this.level = level;
			this.target = target;
			this.xp = xp;
			this.targetXp = Experience.getXpForLevel(target);
		}

		boolean met()
		{
			return level >= target;
		}
	}

	/** One requirement of a step, worded for a checklist. */
	static final class Req
	{
		final String text;
		final boolean met;
		final Skill skill; // null unless it's a level requirement

		Req(String text, boolean met, Skill skill)
		{
			this.text = text;
			this.met = met;
			this.skill = skill;
		}
	}

	static final class Plan
	{
		String next;
		Step nextStep;   // null once the build is done
		Step alt;        // something doable now when nextStep is blocked
		int done;
		int qp;
		int qpTarget;
		int skipped;     // filler quests not needed for the QP target
		final List<Step> todo = new ArrayList<>();
		final List<Item> route = new ArrayList<>(); // every planned row, done ones too, in route order
		final List<SkillGoal> skills = new ArrayList<>();
		List<Req> checklist = List.of(); // nextStep's requirements
	}

	static Plan plan(Build build, Account acc)
	{
		Plan p = new Plan();
		List<Step> route = build.route;
		p.qp = acc.qp;
		// Only builds that use filler quests aim for a QP total. For the rest QP is a side effect, and showing
		// "150/20 QP" to someone with QP from other quests would be noise.
		p.qpTarget = route.stream().anyMatch(s -> s.filler) ? route.stream().mapToInt(s -> s.qpReq).max().orElse(0) : 0;

		// QP the fillers must cover = target - what you have - what the remaining required quests give.
		// Steps that themselves need the target (the final boss) don't count, you need the QP before them.
		int fillerQp = p.qpTarget - acc.qp - route.stream()
			.filter(s -> !s.filler && s.qpReq < p.qpTarget && !acc.done(s))
			.mapToInt(s -> s.qp)
			.sum();

		// Highest level each done or planned step asks for (skipped filler doesn't count)
		Map<Skill, Integer> reqs = new EnumMap<>(Skill.class);
		// Levels the plan has already asked you to train to, so "Train X to N" sits before the first quest needing it
		Map<Skill, Integer> goal = new EnumMap<>(acc.levels);
		for (Step s : route)
		{
			if (acc.done(s))
			{
				p.done++;
				p.route.add(new Item(s, null, 0, Status.DONE, s.stage));
				s.skills.forEach((k, v) -> reqs.merge(k, v, Math::max));
				continue;
			}
			if (s.filler)
			{
				if (fillerQp <= 0)
				{
					p.skipped++;
					continue;
				}
				fillerQp -= s.qp;
			}
			s.skills.forEach((k, v) ->
			{
				reqs.merge(k, v, Math::max);
				if (v > goal.getOrDefault(k, acc.level(k)))
				{
					goal.put(k, v);
					p.route.add(new Item(null, k, v, Status.TRAIN, s.stage));
				}
			});
			p.route.add(new Item(s, null, 0, p.todo.isEmpty() ? Status.NEXT : Status.TODO, s.stage));
			p.todo.add(s);
		}

		goal.forEach((skill, lvl) ->
		{
			if (lvl > acc.level(skill))
			{
				p.skills.add(new SkillGoal(skill, acc.level(skill), lvl, acc.xp.getOrDefault(skill, 0)));
			}
		});
		// Then skills already at the build's highest requirement
		reqs.forEach((skill, lvl) ->
		{
			if (acc.level(skill) >= lvl)
			{
				p.skills.add(new SkillGoal(skill, acc.level(skill), lvl, acc.xp.getOrDefault(skill, 0)));
			}
		});

		if (p.todo.isEmpty())
		{
			p.next = build.displayName + " unlocked!";
			return p;
		}

		Step first = p.todo.get(0);
		p.nextStep = first;
		p.checklist = checklist(first, acc);
		String block = blocker(first, acc);
		if (block == null)
		{
			p.next = first.label();
			return p;
		}

		p.next = block + "\nfor " + first.label();
		// Planned steps first, then any skipped filler.
		p.alt = Stream.concat(p.todo.stream(), route.stream())
			.filter(s -> s != first && !acc.done(s) && blocker(s, acc) == null)
			.findFirst()
			.orElse(null);
		return p;
	}

	/** Every requirement of a step: quests first, then levels, then quest points. Unmet ones say what to do. */
	static List<Req> checklist(Step s, Account acc)
	{
		List<Req> list = new ArrayList<>();
		for (Quest q : s.after)
		{
			boolean met = acc.finished(q);
			list.add(new Req((met ? "Finished " : "Finish ") + q.getName(), met, null));
		}
		s.skills.forEach((skill, req) ->
		{
			int lvl = acc.level(skill);
			boolean met = lvl >= req;
			list.add(new Req(met ? skill.getName() + " " + req
				: "Train " + skill.getName() + " to " + req + " (now " + lvl + ")", met, skill));
		});
		if (s.qpReq > 0)
		{
			boolean met = acc.qp >= s.qpReq;
			list.add(new Req(met ? s.qpReq + " quest points"
				: "Get " + s.qpReq + " quest points (now " + acc.qp + ")", met, null));
		}
		return list;
	}

	/** What stops you doing this step right now, or null if you can do it. */
	static String blocker(Step s, Account acc)
	{
		String need = checklist(s, acc).stream().filter(r -> !r.met).map(r -> r.text).collect(Collectors.joining("\n"));
		return need.isEmpty() ? null : need;
	}
}
