package com.accountbuildplanner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/** Next tab: the job to do now, what it needs, and the few steps after it. */
class NextView extends JPanel
{
	private final SkillIconManager skillIcons;
	private final Runnable showRoute;

	NextView(SkillIconManager skillIcons, Runnable showRoute)
	{
		super(new DynamicGridLayout(0, 1, 0, 4));
		this.skillIcons = skillIcons;
		this.showRoute = showRoute;
		setOpaque(false);
	}

	void show(Planner.Plan p)
	{
		removeAll();
		if (p.nextStep == null)
		{
			add(unlocked(p.next));
		}
		else
		{
			add(Ui.header("Next job"));
			add(card(p));
			if (p.alt != null)
			{
				add(Ui.header("Can do now instead"));
				add(Ui.questRow(p.alt, "", false));
			}
			List<Planner.Item> then = p.route.stream()
				.dropWhile(i -> i.status != Planner.Status.NEXT)
				.skip(1)
				.limit(3)
				.collect(Collectors.toList());
			if (!then.isEmpty())
			{
				add(Ui.header("Then"));
				for (int i = 0; i < then.size(); i++)
				{
					add(Ui.itemRow(then.get(i), skillIcons, "<font color='#7f7f7f'>" + (i + 2) + "</font>&nbsp;&nbsp;"));
				}
				JLabel more = Ui.link("See full route", showRoute);
				more.setBorder(new EmptyBorder(2, 0, 0, 0));
				add(more);
			}
		}
		revalidate();
		repaint();
	}

	private JPanel card(Planner.Plan p)
	{
		Step s = p.nextStep;
		JPanel body = new JPanel(new DynamicGridLayout(0, 1, 0, 3));
		body.setOpaque(false);

		JLabel title = new JLabel(Ui.html(Ui.esc((s.startOnly ? "Start " : "") + s.quest.getName()), Ui.CARD));
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		body.add(title);
		body.add(Ui.muted((s.qp > 0 ? "+" + s.qp + " QP" : "No quest points") + (s.filler ? ", filler quest" : ""), Ui.CARD));
		if (s.note != null)
		{
			body.add(Ui.muted(s.note, Ui.CARD));
		}

		if (!p.checklist.isEmpty())
		{
			JPanel list = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
			list.setOpaque(false);
			list.setBorder(new EmptyBorder(6, 0, 4, 0));
			p.checklist.forEach(r -> list.add(requirement(r)));
			body.add(list);
		}

		JButton guide = new JButton("Quest guide");
		guide.setFocusPainted(false);
		guide.addActionListener(e -> Ui.wiki(s.quest.getName()));
		body.add(guide);

		JLabel icon = new JLabel(new ImageIcon(Ui.QUEST_ICON));
		icon.setVerticalAlignment(SwingConstants.TOP);

		JPanel card = new JPanel(new BorderLayout(8, 0));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));
		card.add(icon, BorderLayout.WEST);
		card.add(body, BorderLayout.CENTER);
		return Ui.accent(card, ColorScheme.BRAND_ORANGE);
	}

	private static JPanel requirement(Planner.Req r)
	{
		JLabel mark = new JLabel(new ImageIcon(r.met ? Ui.TICK : Ui.CROSS));
		mark.setVerticalAlignment(SwingConstants.TOP);
		JLabel text = new JLabel(Ui.html(Ui.esc(r.text), Ui.CARD - 22));
		text.setFont(FontManager.getRunescapeSmallFont());
		text.setForeground(r.met ? Ui.MUTED : Color.WHITE);

		JPanel line = new JPanel(new BorderLayout(4, 0));
		line.setOpaque(false);
		line.add(mark, BorderLayout.WEST);
		line.add(text, BorderLayout.CENTER);
		if (!r.met && r.skill != null)
		{
			line.setToolTipText("Click for the wiki training guide");
			Ui.onClick(line, () -> Ui.wiki(r.skill.getName() + " training"));
		}
		return line;
	}

	private static JPanel unlocked(String text)
	{
		JLabel title = new JLabel(Ui.html(Ui.esc(text), Ui.CARD));
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);

		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(12, 10, 12, 10));
		card.add(title, BorderLayout.CENTER);
		return Ui.accent(card, ColorScheme.PROGRESS_COMPLETE_COLOR);
	}
}
