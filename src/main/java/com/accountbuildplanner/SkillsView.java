package com.accountbuildplanner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/** Skills tab: every skill the build needs, like the in-game skills tab. Orange = to train, green = met. */
class SkillsView extends JPanel
{
	private final SkillIconManager skillIcons;

	SkillsView(SkillIconManager skillIcons)
	{
		super(new DynamicGridLayout(0, 1, 0, 6));
		this.skillIcons = skillIcons;
		setOpaque(false);
	}

	void show(Planner.Plan p)
	{
		removeAll();
		long met = p.skills.stream().filter(Planner.SkillGoal::met).count();
		JLabel summary = Ui.muted(p.skills.isEmpty() ? "No skill requirements" : met + " of " + p.skills.size() + " met", Ui.FULL);
		summary.setBorder(new EmptyBorder(8, 0, 0, 0));
		add(summary);

		if (!p.skills.isEmpty())
		{
			JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
			grid.setOpaque(false);
			p.skills.forEach(g -> grid.add(cell(g)));
			add(grid);
		}
		revalidate();
		repaint();
	}

	private JPanel cell(Planner.SkillGoal g)
	{
		JLabel levels = new JLabel(g.level + "/" + g.target);
		levels.setFont(FontManager.getRunescapeSmallFont());
		levels.setForeground(g.met() ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);

		JPanel body = new JPanel(new BorderLayout(0, 4));
		body.add(levels, BorderLayout.NORTH);
		body.add(Ui.bar(g.xp, g.targetXp, g.met() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.BRAND_ORANGE), BorderLayout.SOUTH);

		String name = g.skill.getName();
		String tip = g.met()
			? String.format("%s: done, the build needs level %d. Click for the wiki training guide.", name, g.target)
			: String.format("%s: %,d xp to level %d. Click for the wiki training guide.", name, Math.max(0, g.targetXp - g.xp), g.target);
		return Ui.row(skillIcons.getSkillImage(g.skill, true), body, name + " training", tip);
	}
}
