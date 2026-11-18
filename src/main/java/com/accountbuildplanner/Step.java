package com.accountbuildplanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * One quest in a build route, plus what it needs before you can do it.
 * Filler steps exist only to reach the build's quest point target and are skipped once it's covered.
 */
final class Step
{
	final Quest quest;
	final int qp;
	final boolean filler;
	final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
	final List<Quest> after = new ArrayList<>();
	int qpReq;
	boolean startOnly;
	String note;
	String stage; // route section, set by Build

	private Step(Quest quest, int qp, boolean filler)
	{
		this.quest = quest;
		this.qp = qp;
		this.filler = filler;
	}

	static Step q(Quest quest, int qp)
	{
		return new Step(quest, qp, false);
	}

	static Step f(Quest quest, int qp)
	{
		return new Step(quest, qp, true);
	}

	Step skill(Skill skill, int level)
	{
		skills.put(skill, level);
		return this;
	}

	Step after(Quest... quests)
	{
		after.addAll(Arrays.asList(quests));
		return this;
	}

	Step qpReq(int qp)
	{
		qpReq = qp;
		return this;
	}

	/** Done as soon as the quest is started (e.g. Legends' Quest for Recipe for Disaster). */
	Step startOnly()
	{
		startOnly = true;
		return this;
	}

	Step note(String note)
	{
		this.note = note;
		return this;
	}

	String label()
	{
		return (startOnly ? "Start " : "") + quest.getName() + (note == null ? "" : " (" + note + ")");
	}
}
