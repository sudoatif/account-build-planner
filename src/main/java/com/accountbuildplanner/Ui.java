package com.accountbuildplanner;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.ThinProgressBar;
import net.runelite.client.util.LinkBrowser;

/** Swing pieces shared by the panel and its tabs. */
final class Ui
{
	// HTML wrap widths (Swing CSS px): full panel, a row with an icon, inside the accented card
	static final int FULL = 170;
	static final int ROW = 140;
	static final int CARD = 132;
	static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	static final BufferedImage QUEST_ICON = questIcon();
	static final BufferedImage TICK = mark(true);
	static final BufferedImage CROSS = mark(false);
	static final BufferedImage OPEN = chevron(true);  // stage unfolded
	static final BufferedImage SHUT = chevron(false); // stage folded
	private static final String WIKI_SEARCH = "https://oldschool.runescape.wiki/w/Special:Search?search=";

	private Ui()
	{
	}

	/** Dark list row with an icon that opens the wiki on click, like RuneLite's own panels. */
	static JPanel row(Image icon, JComponent body, String wikiSearch, String tooltip)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(5, 6, 5, 6));

		JLabel iconLabel = new JLabel(new ImageIcon(icon));
		iconLabel.setVerticalAlignment(SwingConstants.TOP);
		row.add(iconLabel, BorderLayout.WEST);
		body.setOpaque(false);
		row.add(body, BorderLayout.CENTER);

		row.setToolTipText(tooltip);
		onClick(row, () -> wiki(wikiSearch));
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
		return row;
	}

	/** A quest row; done quests get a tick and drop the QP suffix. {@code prefix} is HTML. */
	static JPanel questRow(Step s, String prefix, boolean done)
	{
		String qp = done || s.qp == 0 ? "" : "<font color='#7f7f7f'>  +" + s.qp + " QP</font>";
		JLabel text = new JLabel(html(prefix + esc(s.label()) + qp, ROW));
		text.setForeground(done || s.filler ? MUTED : Color.WHITE);
		return row(done ? TICK : QUEST_ICON, text, s.quest.getName(), s.filler
			? "Filler quest: only here for quest points. Click for the wiki guide."
			: "Click for the wiki quest guide");
	}

	static JPanel trainRow(SkillIconManager icons, Skill skill, int level, String prefix)
	{
		JLabel text = new JLabel(html(prefix + esc("Train " + skill.getName() + " to " + level), ROW));
		text.setForeground(ColorScheme.BRAND_ORANGE);
		return row(icons.getSkillImage(skill, true), text, skill.getName() + " training", "Click for the wiki training guide");
	}

	static JPanel itemRow(Planner.Item i, SkillIconManager icons, String prefix)
	{
		return i.step == null
			? trainRow(icons, i.skill, i.level, prefix)
			: questRow(i.step, prefix, i.status == Planner.Status.DONE);
	}

	/** 3 px coloured bar on the left, keeping the component's own padding. */
	static <T extends JComponent> T accent(T c, Color color)
	{
		c.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, color), c.getBorder()));
		return c;
	}

	static JLabel header(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(Color.WHITE);
		l.setBorder(new EmptyBorder(8, 0, 0, 0));
		return l;
	}

	static JLabel muted(String text, int width)
	{
		JLabel l = new JLabel(html(esc(text), width));
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		return l;
	}

	/** Small muted text that lights up on hover and runs {@code action} on click. */
	static JLabel link(String text, Runnable action)
	{
		JLabel l = muted(text, FULL);
		onClick(l, action);
		l.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				l.setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				l.setForeground(MUTED);
			}
		});
		return l;
	}

	static void onClick(JComponent c, Runnable action)
	{
		c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				action.run();
			}
		});
	}

	static ThinProgressBar bar(int value, int max, Color color)
	{
		ThinProgressBar bar = new ThinProgressBar();
		bar.setMaximumValue(Math.max(1, max));
		bar.setValue(Math.min(value, max));
		bar.setForeground(color);
		bar.setPreferredSize(new Dimension(0, 4));
		return bar;
	}

	static void wiki(String search)
	{
		LinkBrowser.browse(WIKI_SEARCH + URLEncoder.encode(search, StandardCharsets.UTF_8));
	}

	static String esc(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
	}

	/** Wrap already-escaped HTML to a fixed width so long quest names flow onto new lines. */
	static String html(String body, int width)
	{
		return "<html><body style='width:" + width + "px'>" + body + "</body></html>";
	}

	/** Small scroll/checklist icon, drawn so there's no image asset to ship. */
	private static BufferedImage questIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0xC8AA6E));
		g.fillRoundRect(2, 1, 12, 14, 4, 4);
		g.setColor(new Color(0x3A2E1C));
		for (int y = 4; y <= 11; y += 3)
		{
			g.fillRect(5, y, 6, 1);
		}
		g.dispose();
		return img;
	}

	/** Small grey arrow: pointing down when open, right when folded. */
	private static BufferedImage chevron(boolean open)
	{
		BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(MUTED);
		if (open)
		{
			g.drawPolyline(new int[]{2, 5, 8}, new int[]{3, 7, 3}, 3);
		}
		else
		{
			g.drawPolyline(new int[]{3, 7, 3}, new int[]{2, 5, 8}, 3);
		}
		g.dispose();
		return img;
	}

	/** Green tick or red cross, 16x16 so it lines up with quest and skill icons. */
	private static BufferedImage mark(boolean tick)
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if (tick)
		{
			g.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			g.drawPolyline(new int[]{3, 7, 13}, new int[]{8, 12, 4}, 3);
		}
		else
		{
			g.setColor(ColorScheme.PROGRESS_ERROR_COLOR);
			g.drawLine(4, 4, 12, 12);
			g.drawLine(12, 4, 4, 12);
		}
		g.dispose();
		return img;
	}
}
