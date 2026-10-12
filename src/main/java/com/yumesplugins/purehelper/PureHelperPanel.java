package com.yumesplugins.purehelper;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** The single read-only page: hero, skill tiles, off-limits, session log. EDT only. */
class PureHelperPanel extends PluginPanel
{
	private static final int GAP = 8;
	private static final int PAD = 10;
	private static final int TILE_HEIGHT = 52;
	private static final Font SMALL = FontManager.getRunescapeSmallFont();
	private static final Font BODY = FontManager.getRunescapeFont();
	private static final Font HERO = FontManager.getRunescapeBoldFont();
	/** The RuneScape fonts have no ⇧; the logical Dialog font falls back to a system font that does. */
	private static final Font SYMBOL = new Font(Font.DIALOG, Font.BOLD, 12);

	private final PureHelperConfig config;
	private final Hero hero = new Hero();
	private final SkillTile[] tiles = new SkillTile[CombatSkill.values().length];
	private final JLabel offLimitsHeader = label(BODY, ColorScheme.TEXT_COLOR);
	private final Chevron chevron = new Chevron();
	private final JPanel offLimitsList = column();
	private final JPanel logList = column();
	private boolean offLimitsOpen;
	private List<PanelState.OffLimit> offLimits = Collections.emptyList();

	PureHelperPanel(PureHelperConfig config, SkillIconManager skillIconManager)
	{
		this.config = config;
		setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		JPanel page = column();
		add(page, BorderLayout.NORTH);

		page.add(hero);
		page.add(Box.createVerticalStrut(2 * GAP));

		JPanel grid = new JPanel(new GridLayout(2, 4, GAP, GAP));
		grid.setOpaque(false);
		for (CombatSkill s : CombatSkill.values())
		{
			tiles[s.ordinal()] = new SkillTile(skillIconManager.getSkillImage(s.skill()));
			grid.add(tiles[s.ordinal()]);
		}
		page.add(grid);

		page.add(Box.createVerticalStrut(2 * GAP));
		page.add(new SectionLabel("Off-limits"));
		page.add(Box.createVerticalStrut(GAP));
		page.add(offLimitsToggle());
		page.add(offLimitsList);

		page.add(Box.createVerticalStrut(2 * GAP));
		page.add(new SectionLabel("This session"));
		page.add(Box.createVerticalStrut(GAP));
		page.add(logList);
	}

	void render(PanelState state)
	{
		Color accent = config.accentColor();
		hero.set(state.getHeroTitle(), state.getHeroSub(), state.isHeroDanger() ? accent : null);

		for (PanelState.Tile t : state.getTiles())
		{
			tiles[t.getSkill().ordinal()].set(t, accent);
		}

		offLimitsHeader.setText(state.getOffLimitsHeader());
		offLimits = state.getOffLimits();
		fillOffLimits();

		logList.removeAll();
		if (state.getLog().isEmpty())
		{
			logList.add(label(BODY, ColorScheme.LIGHT_GRAY_COLOR, PanelState.EMPTY_LOG));
		}
		for (PanelState.LogRow r : state.getLog())
		{
			logList.add(logRow(r, accent));
		}

		revalidate();
		repaint();
	}

	private JPanel offLimitsToggle()
	{
		JPanel row = new JPanel(new BorderLayout(GAP, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(6, GAP, 6, GAP));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("Quests and diary tiers that would push a protected skill past its cap");
		row.add(offLimitsHeader, BorderLayout.CENTER);
		row.add(chevron, BorderLayout.EAST);
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				offLimitsOpen = !offLimitsOpen;
				fillOffLimits();
				chevron.repaint();
				revalidate();
				repaint();
			}

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

	/** Rows exist only while open: DynamicGridLayout would still reserve space for hidden ones. */
	private void fillOffLimits()
	{
		offLimitsList.removeAll();
		if (offLimitsOpen)
		{
			for (PanelState.OffLimit o : offLimits)
			{
				offLimitsList.add(offLimitRow(o));
			}
		}
	}

	private static JComponent offLimitRow(PanelState.OffLimit o)
	{
		JPanel row = new JPanel(new GridLayout(2, 1));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(6, GAP, 0, GAP));
		JLabel name = label(BODY, ColorScheme.TEXT_COLOR, o.getName());
		name.setToolTipText(o.text());
		row.add(name);
		row.add(label(SMALL, ColorScheme.LIGHT_GRAY_COLOR, o.getReason()));
		return row;
	}

	private static JComponent logRow(PanelState.LogRow r, Color accent)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		// Every entry is a block or a slip, so every row carries the danger mark.
		row.setBorder(new MatteBorder(0, 2, 0, 0, accent));
		row.setToolTipText(r.text());

		JLabel time = label(SMALL, ColorScheme.LIGHT_GRAY_COLOR, r.getTime());
		time.setBorder(new EmptyBorder(0, 6, 0, 0));
		row.add(time, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(2, 1));
		text.setOpaque(false);
		text.add(label(SMALL, ColorScheme.LIGHT_GRAY_COLOR, r.getGuard()));
		text.add(label(BODY, ColorScheme.TEXT_COLOR, r.getWhat()));
		row.add(text, BorderLayout.CENTER);

		if (r.isOverridden())
		{
			JLabel shift = label(SYMBOL, accent, "⇧");
			shift.setToolTipText("Shift held: guard overridden");
			row.add(shift, BorderLayout.EAST);
		}

		JPanel spaced = new JPanel(new BorderLayout());
		spaced.setOpaque(false);
		spaced.setBorder(new EmptyBorder(0, 0, GAP, 0));
		spaced.add(row);
		return spaced;
	}

	/** Full-width rows at their preferred heights (RuneLite's own layout; unlike BoxLayout it caches no sizes). */
	private static JPanel column()
	{
		JPanel p = new JPanel(new DynamicGridLayout(0, 1));
		p.setOpaque(false);
		return p;
	}

	private static JLabel label(Font font, Color color)
	{
		return label(font, color, "");
	}

	private static JLabel label(Font font, Color color, String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(font);
		l.setForeground(color);
		return l;
	}

	private static Graphics2D smooth(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return g2;
	}

	/** The most pressing live fact: big title, one-line sub, a status stripe. */
	private static final class Hero extends JComponent
	{
		private static final int STRIPE = 3;
		private String title = "";
		private String sub = "";
		private Color danger;

		Hero()
		{
			setPreferredSize(new Dimension(0, 60));
		}

		void set(String title, String sub, Color danger)
		{
			this.title = title;
			this.sub = sub;
			this.danger = danger;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = smooth(g);
			g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setColor(danger != null ? danger : ColorScheme.PROGRESS_COMPLETE_COLOR);
			g2.fillRect(0, 0, STRIPE, getHeight());

			int x = STRIPE + PAD;
			int room = getWidth() - x - PAD;
			// Largest size up to +8pt that still fits on one line.
			Font font = HERO;
			for (float size = HERO.getSize2D() + 8; size >= HERO.getSize2D(); size--)
			{
				font = HERO.deriveFont(size);
				if (g2.getFontMetrics(font).stringWidth(title) <= room)
				{
					break;
				}
			}
			FontMetrics fm = g2.getFontMetrics(font);
			g2.setFont(font);
			g2.setColor(danger != null ? danger : Color.WHITE);
			g2.drawString(title, x, PAD + fm.getAscent() - 2);

			g2.setFont(SMALL);
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g2.drawString(sub, x, getHeight() - PAD);
			g2.dispose();
		}
	}

	/** Small grey uppercase label followed by a 1 px rule to the right edge. */
	private static final class SectionLabel extends JComponent
	{
		private final String text;

		SectionLabel(String text)
		{
			this.text = text.toUpperCase(Locale.ROOT);
			setPreferredSize(new Dimension(0, 12));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setFont(SMALL);
			FontMetrics fm = g2.getFontMetrics();
			int baseline = (getHeight() + fm.getAscent()) / 2 - 1;
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g2.drawString(text, 0, baseline);
			g2.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
			g2.fillRect(fm.stringWidth(text) + 6, getHeight() / 2, getWidth(), 1);
			g2.dispose();
		}
	}

	/** Disclosure triangle, drawn because the RuneScape fonts have no ▸ / ▾. */
	private final class Chevron extends JComponent
	{
		Chevron()
		{
			setPreferredSize(new Dimension(8, 8));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = smooth(g);
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			int cx = getWidth() / 2;
			int cy = getHeight() / 2;
			if (offLimitsOpen)
			{
				g2.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 2, cy - 2, cy + 3}, 3);
			}
			else
			{
				g2.fillPolygon(new int[]{cx - 2, cx - 2, cx + 3}, new int[]{cy - 4, cy + 4, cy}, 3);
			}
			g2.dispose();
		}
	}

	/** Skill icon, level/cap, 2 px progress bar; dimmed with a padlock at cap. */
	private static final class SkillTile extends JComponent
	{
		private final BufferedImage icon;
		private PanelState.Tile tile;
		private Color accent;

		SkillTile(BufferedImage icon)
		{
			this.icon = icon;
			setPreferredSize(new Dimension(0, TILE_HEIGHT));
		}

		void set(PanelState.Tile tile, Color accent)
		{
			this.tile = tile;
			this.accent = accent;
			setToolTipText(tile.tooltip());
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			int w = getWidth();
			int h = getHeight();
			Graphics2D g2 = smooth(g);
			g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
			g2.fillRect(0, 0, w, h);
			if (tile == null)
			{
				g2.dispose();
				return;
			}

			Graphics2D body = (Graphics2D) g2.create();
			if (tile.isLocked())
			{
				body.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			}
			if (icon != null)
			{
				body.drawImage(icon, (w - icon.getWidth()) / 2, 6, null);
			}

			body.setFont(SMALL);
			FontMetrics fm = body.getFontMetrics();
			String level = String.valueOf(tile.getLevel());
			String cap = tile.isGuarded() ? "/" + tile.getCap() : "";
			int x = (w - fm.stringWidth(level + cap)) / 2;
			int y = h - 8;
			body.setColor(tile.isGuarded() ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			body.drawString(level, x, y);
			body.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			body.drawString(cap, x + fm.stringWidth(level), y);

			if (tile.isGuarded())
			{
				body.setColor(ColorScheme.DARK_GRAY_COLOR);
				body.fillRect(0, h - 2, w, 2);
				body.setColor(tile.isNear() ? accent : ColorScheme.MEDIUM_GRAY_COLOR);
				body.fillRect(0, h - 2, (int) Math.round(w * tile.getProgress()), 2);
			}
			body.dispose();

			if (tile.isLocked())
			{
				paintPadlock(g2, w - 12, 3);
			}
			g2.dispose();
		}

		private static void paintPadlock(Graphics2D g2, int x, int y)
		{
			g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
			g2.fillOval(x - 2, y - 1, 13, 14);
			g2.setColor(ColorScheme.TEXT_COLOR);
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawArc(x + 2, y + 1, 5, 7, 0, 180);
			g2.fillRoundRect(x + 1, y + 5, 7, 6, 2, 2);
		}
	}
}
