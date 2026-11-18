package com.accountbuildplanner;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
	name = "Account Build Planner",
	description = "Pick an account goal like Barrows gloves, Song of the Elves or the quest cape and get told the next quest or skill to train",
	tags = {"quest", "barrows", "gloves", "rfd", "sote", "prifddinas", "cape", "guide", "route"}
)
public class AccountBuildPlannerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SkillIconManager skillIconManager;

	@Inject
	private ItemManager itemManager;

	private BuildPlannerPanel panel;
	private NavigationButton navButton;
	private final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
	private Build active;
	private boolean dirty;

	@Override
	protected void startUp()
	{
		panel = new BuildPlannerPanel(skillIconManager, itemManager, this::start);
		navButton = NavigationButton.builder()
			.tooltip("Account Build Planner")
			.icon(Ui.QUEST_ICON)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		active = null;
		levels.clear();
	}

	private void start(Build build)
	{
		active = build;
		clientThread.invokeLater(this::refresh);
	}

	// After Start, re-plan whenever you level up or finish a quest (quest points change).
	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		if (!Objects.equals(levels.put(e.getSkill(), e.getLevel()), e.getLevel()))
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarpId() == VarPlayerID.QP)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (dirty)
		{
			dirty = false;
			refresh();
		}
	}

	private void refresh()
	{
		Build build = active;
		if (build == null)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			SwingUtilities.invokeLater(() -> panel.showMessage("Log in, then press Start."));
			return;
		}

		Planner.Account acc = new Planner.Account();
		for (Step s : build.route)
		{
			acc.quests.put(s.quest, s.quest.getState(client));
			for (Quest q : s.after)
			{
				acc.quests.computeIfAbsent(q, k -> k.getState(client));
			}
			for (Skill k : s.skills.keySet())
			{
				acc.levels.computeIfAbsent(k, client::getRealSkillLevel);
				acc.xp.computeIfAbsent(k, client::getSkillExperience);
			}
		}
		acc.qp = client.getVarpValue(VarPlayerID.QP);

		Planner.Plan plan = Planner.plan(build, acc);
		SwingUtilities.invokeLater(() -> panel.showPlan(plan));
	}
}
