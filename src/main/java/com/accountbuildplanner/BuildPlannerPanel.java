package com.accountbuildplanner;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.ThinProgressBar;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;

/** Sidebar shell: build picker + Start, progress summary, and the Next / Route / Skills tabs. */
class BuildPlannerPanel extends PluginPanel
{
	private final JComboBox<Build> builds = new JComboBox<>(Build.values());
	private final JButton start = new JButton("Start");
	private final JPanel body = new JPanel(new BorderLayout()); // holds either the message or the plan
	private final PluginErrorPanel message = new PluginErrorPanel();
	private final JPanel planBox = new JPanel(new BorderLayout(0, 6));
	private final JLabel summary = small();
	private final JLabel percent = small();
	private final ThinProgressBar progress = Ui.bar(0, 1, ColorScheme.BRAND_ORANGE);
	private final JPanel display = new JPanel();
	private final MaterialTabGroup tabs = new MaterialTabGroup(display);
	private final NextView next;
	private final RouteView route;
	private final SkillsView skills;
	private final MaterialTab nextTab;
	private boolean planned;

	BuildPlannerPanel(SkillIconManager skillIcons, ItemManager items, Consumer<Build> onStart)
	{
		Map<Build, ImageIcon> icons = new EnumMap<>(Build.class);
		for (Build b : Build.values())
		{
			AsyncBufferedImage img = items.getImage(b.icon);
			icons.put(b, new ImageIcon(img));
			img.onLoaded(builds::repaint);
		}
		builds.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
			{
				super.getListCellRendererComponent(list, value, index, selected, focus);
				setIcon(icons.get((Build) value));
				return this;
			}
		});

		start.setToolTipText("Read your quests and levels and plan the next steps");
		start.setFocusPainted(false);
		start.addActionListener(e ->
		{
			start.setText("Refresh");
			onStart.accept((Build) builds.getSelectedItem());
		});

		JPanel picker = new JPanel(new BorderLayout(6, 0));
		picker.setOpaque(false);
		picker.add(builds, BorderLayout.CENTER);
		picker.add(start, BorderLayout.EAST);

		JPanel summaryRow = new JPanel(new BorderLayout());
		summaryRow.setOpaque(false);
		summaryRow.add(summary, BorderLayout.WEST);
		summaryRow.add(percent, BorderLayout.EAST);
		progress.setPreferredSize(new Dimension(0, 6));

		JPanel status = new JPanel(new BorderLayout(0, 4));
		status.setOpaque(false);
		status.add(summaryRow, BorderLayout.NORTH);
		status.add(progress, BorderLayout.SOUTH);

		route = new RouteView(skillIcons);
		MaterialTab routeTab = new MaterialTab("Route", tabs, route);
		next = new NextView(skillIcons, () -> tabs.select(routeTab));
		nextTab = new MaterialTab("Next", tabs, next);
		skills = new SkillsView(skillIcons);
		tabs.addTab(nextTab);
		tabs.addTab(routeTab);
		tabs.addTab(new MaterialTab("Skills", tabs, skills));
		tabs.setBorder(new EmptyBorder(4, 0, 2, 0));

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(status, BorderLayout.NORTH);
		top.add(tabs, BorderLayout.SOUTH);

		display.setOpaque(false);
		planBox.setOpaque(false);
		planBox.add(top, BorderLayout.NORTH);
		planBox.add(display, BorderLayout.CENTER);
		body.setOpaque(false);

		add(picker);
		add(body);
		showMessage("Pick a build and press Start.");
	}

	void showMessage(String text)
	{
		message.setContent("Account Build Planner", text);
		swap(message);
	}

	void showPlan(Planner.Plan p)
	{
		int total = p.done + p.todo.size();
		summary.setText(p.done + " of " + total + " quests" + (p.qpTarget > 0 ? ", " + p.qp + "/" + p.qpTarget + " QP" : ""));
		percent.setText((total == 0 ? 100 : p.done * 100 / total) + "%");
		progress.setMaximumValue(Math.max(1, total));
		progress.setValue(p.done);

		next.show(p);
		route.show(p);
		skills.show(p);
		if (!planned)
		{
			planned = true;
			tabs.select(nextTab); // only the first time, so refreshes keep whatever tab you're on
		}
		swap(planBox);
	}

	private void swap(JComponent c)
	{
		body.removeAll();
		body.add(c, BorderLayout.CENTER);
		body.revalidate();
		body.repaint();
	}

	private static JLabel small()
	{
		JLabel l = new JLabel();
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Ui.MUTED);
		return l;
	}
}
