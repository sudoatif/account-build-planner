package com.accountbuildplanner;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/** Route tab: the whole route by stage, done quests ticked, finished stages folded. */
class RouteView extends JPanel
{
	private final SkillIconManager skillIcons;
	private final Map<String, Boolean> open = new HashMap<>(); // stages the user folded or unfolded
	private Planner.Plan shown;

	RouteView(SkillIconManager skillIcons)
	{
		super(new DynamicGridLayout(0, 1, 0, 4));
		this.skillIcons = skillIcons;
		setOpaque(false);
	}

	void show(Planner.Plan p)
	{
		shown = p;
		removeAll();
		Map<String, List<Planner.Item>> stages = p.route.stream()
			.collect(Collectors.groupingBy(i -> i.stage, LinkedHashMap::new, Collectors.toList()));
		stages.forEach((stage, rows) ->
		{
			long quests = rows.stream().filter(i -> i.step != null).count();
			long done = rows.stream().filter(i -> i.status == Planner.Status.DONE).count();
			boolean isOpen = open.getOrDefault(stage, done < quests);
			add(stageHeader(stage, done, quests, isOpen));
			if (isOpen)
			{
				for (Planner.Item i : rows)
				{
					JPanel row = Ui.itemRow(i, skillIcons, "");
					add(i.status == Planner.Status.NEXT ? Ui.accent(row, ColorScheme.BRAND_ORANGE) : row);
				}
			}
		});
		if (p.skipped > 0)
		{
			JLabel skipped = Ui.muted(p.skipped + " filler quests not needed, you'll have the QP", Ui.FULL);
			skipped.setBorder(new EmptyBorder(6, 0, 0, 0));
			add(skipped);
		}
		revalidate();
		repaint();
	}

	private JPanel stageHeader(String stage, long done, long quests, boolean isOpen)
	{
		JLabel name = Ui.header(stage);
		name.setIcon(new ImageIcon(isOpen ? Ui.OPEN : Ui.SHUT));
		name.setIconTextGap(4);

		JLabel count = new JLabel(done + "/" + quests);
		count.setFont(FontManager.getRunescapeSmallFont());
		count.setForeground(done == quests ? ColorScheme.PROGRESS_COMPLETE_COLOR : Ui.MUTED);
		count.setBorder(new EmptyBorder(8, 0, 0, 0));

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.add(name, BorderLayout.CENTER);
		header.add(count, BorderLayout.EAST);
		header.setToolTipText(isOpen ? "Click to fold this stage" : "Click to show this stage");
		Ui.onClick(header, () ->
		{
			open.put(stage, !isOpen);
			show(shown);
		});
		return header;
	}
}
