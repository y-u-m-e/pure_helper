package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;
import net.runelite.client.util.ImageUtil;

/**
 * Read-only sidebar dashboard. All configuration lives in the native plugin settings panel;
 * this surface only reports the current build, what quests/diaries are not doable, and a risk log.
 */
@Slf4j
@Singleton
public class PureHelperPanel extends PluginPanel
{
	private static final String QUEST_RULES_PATH = "/quest-filter-rules.json";
	private static final String DIARY_RULES_PATH = "/achievement-diary-rules.json";
	private static final int MAX_RISK_LOG_ENTRIES = 8;
	private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private static final Color ROW_EVEN = PureHelperUiConstants.ROW_EVEN;
	private static final Color ROW_ODD = PureHelperUiConstants.ROW_ODD;
	private static final Color ROW_HOVER = PureHelperUiConstants.ROW_HOVER;

	private final PureHelperConfig config;
	private final Client client;
	private final SpriteManager spriteManager;
	private final PureHelperStateManager stateManager;
	private final Gson gson;

	private final JPanel skillsContainer = new JPanel(new GridBagLayout());
	private final JPanel questContainer = new JPanel(new GridBagLayout());
	private final JPanel riskLogContainer = new JPanel(new GridBagLayout());
	private final Deque<String> riskLogEntries = new ArrayDeque<>();

	private Map<String, QuestRule> questRulesByName = new HashMap<>();
	private List<QuestRule> allQuestRules = new ArrayList<>();
	private List<DiaryRule> allDiaryRules = new ArrayList<>();

	private boolean collapseNotDoableQuestList;
	private boolean collapseNotDoableDiaryList;
	private int rowIndex;

	@Inject
	private PureHelperPanel(
		PureHelperConfig config,
		Client client,
		SpriteManager spriteManager,
		PureHelperStateManager stateManager,
		Gson gson)
	{
		this.config = config;
		this.client = client;
		this.spriteManager = spriteManager;
		this.stateManager = stateManager;
		this.gson = gson;
		PureHelperUiConstants.applyAccent(config.accentColor());
		this.collapseNotDoableQuestList = stateManager.isCollapseNotDoableQuestList();
		this.collapseNotDoableDiaryList = stateManager.isCollapseNotDoableDiaryList();

		getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		getScrollPane().setBorder(null);
		getScrollPane().setViewportBorder(null);
		applyRuneLiteScrollBarUi();

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel northPanel = new JPanel(new BorderLayout());
		northPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(6, 6, 6, 6)));
		northPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Pure Helper");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		northPanel.add(title, BorderLayout.WEST);

		JLabel subtitle = new JLabel("Status");
		subtitle.setFont(FontManager.getRunescapeSmallFont());
		subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		northPanel.add(subtitle, BorderLayout.EAST);

		JPanel body = new JPanel(new GridBagLayout());
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setBorder(new EmptyBorder(0, 6, 6, 6));

		skillsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		questContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		riskLogContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		GridBagConstraints c = baseConstraints();
		addTitle(body, "Protected build", c);
		body.add(skillsContainer, c);
		c.gridy++;
		addTitle(body, "Quest & diary", c);
		body.add(questContainer, c);
		c.gridy++;
		addTitle(body, "Risk log", c);
		body.add(riskLogContainer, c);
		c.gridy++;

		// Bottom filler keeps content anchored to the top.
		GridBagConstraints filler = baseConstraints();
		filler.gridy = c.gridy;
		filler.weighty = 1;
		filler.fill = GridBagConstraints.BOTH;
		body.add(Box.createGlue(), filler);

		add(northPanel, BorderLayout.NORTH);
		add(body, BorderLayout.CENTER);

		loadQuestRules();
		loadDiaryRules();
		refreshAll();
	}

	@Override
	public void onActivate()
	{
		applyRuneLiteScrollBarUi();
		refreshAll();
	}

	public void refreshFromConfig()
	{
		SwingUtilities.invokeLater(() ->
		{
			PureHelperUiConstants.applyAccent(config.accentColor());
			applyRuneLiteScrollBarUi();
			refreshAll();
		});
	}

	public void addRiskLogEntry(String message)
	{
		if (message == null || message.isBlank())
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			String line = LOG_TIME.format(LocalTime.now()) + "  " + message;
			riskLogEntries.addFirst(line);
			while (riskLogEntries.size() > MAX_RISK_LOG_ENTRIES)
			{
				riskLogEntries.removeLast();
			}
			refreshRiskLog();
		});
	}

	private void refreshAll()
	{
		refreshSkillsSummary();
		refreshQuestSummary();
		refreshRiskLog();
	}

	// ============================================================
	// Protected build summary
	// ============================================================

	private void refreshSkillsSummary()
	{
		skillsContainer.removeAll();
		GridBagConstraints c = baseConstraints();

		JLabel preset = new JLabel("Preset: " + config.buildProfile().getLabel());
		preset.setForeground(Color.WHITE);
		preset.setFont(FontManager.getRunescapeBoldFont());
		skillsContainer.add(preset, c);
		c.gridy++;

		Set<AvoidedSkill> protectedSkills = ConfigParsers.protectedSkills(config);
		Map<AvoidedSkill, Integer> caps = ConfigParsers.skillCaps(config);

		if (protectedSkills.isEmpty())
		{
			JLabel none = new JLabel("No skills protected. Edit in plugin settings.");
			none.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			none.setFont(FontManager.getRunescapeSmallFont());
			skillsContainer.add(none, c);
			c.gridy++;
		}
		else
		{
			for (AvoidedSkill skill : protectedSkills)
			{
				Integer cap = caps.get(skill);
				String detail = cap == null ? "all XP blocked" : "max level " + cap;
				JPanel row = buildAccentRow(
					SpriteID.RS2_TAB_STATS,
					skill.getLabel() + " \u2014 " + detail,
					PureHelperUiConstants.BLOCKED_ACCENT,
					PureHelperUiConstants.BLOCKED_TEXT,
					"Protected skill. Edit which skills are protected and their caps in plugin settings.");
				skillsContainer.add(row, c);
				c.gridy++;
			}
		}

		skillsContainer.revalidate();
		skillsContainer.repaint();
	}

	// ============================================================
	// Quest & diary not-doable summary
	// ============================================================

	private void refreshQuestSummary()
	{
		rowIndex = 0;
		questContainer.removeAll();
		GridBagConstraints c = baseConstraints();

		if (allQuestRules.isEmpty())
		{
			loadQuestRules();
		}
		if (allDiaryRules.isEmpty())
		{
			loadDiaryRules();
		}

		if (!config.enableQuestSafeguards())
		{
			JLabel off = new JLabel("Quest safeguards are off.");
			off.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			off.setFont(FontManager.getRunescapeSmallFont());
			questContainer.add(off, c);
			c.gridy++;
			questContainer.revalidate();
			questContainer.repaint();
			return;
		}

		Set<AvoidedSkill> avoidedSkills = ConfigParsers.protectedSkills(config);
		Map<AvoidedSkill, Integer> skillCaps = ConfigParsers.skillCaps(config);
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(client, questRulesByName);
		Map<String, QuestEvaluation> memo = new HashMap<>();

		List<QuestRule> blockedRules = new ArrayList<>();
		for (QuestRule rule : allQuestRules)
		{
			if (rule == null || rule.name == null || rule.name.isBlank())
			{
				continue;
			}
			QuestEvaluation eval = evaluator.evaluateQuest(
				rule, avoidedSkills, skillCaps, effectiveChoicePolicy(),
				config.safeguardStrictness(), memo, new HashSet<>());
			if (eval.isBlocked())
			{
				blockedRules.add(rule);
			}
		}
		blockedRules.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

		Map<String, List<String>> riskyDiaryTiers = new LinkedHashMap<>();
		for (DiaryRule diary : allDiaryRules)
		{
			if (diary == null || diary.name == null || diary.name.isBlank())
			{
				continue;
			}
			for (DiaryTier tier : diary.tiers == null ? java.util.Collections.<DiaryTier>emptyList() : diary.tiers)
			{
				if (tier == null || tier.tier == null || tier.tier.isBlank())
				{
					continue;
				}
				if (evaluator.evaluateDiaryTierRisk(tier, avoidedSkills, skillCaps,
					effectiveChoicePolicy(), config.safeguardStrictness(), memo))
				{
					riskyDiaryTiers.computeIfAbsent(diary.name, key -> new ArrayList<>()).add(tier.tier);
				}
			}
		}

		// Not doable quests
		JPanel questsHeader = buildCollapsibleHeader(
			SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
			"Not doable quests",
			blockedRules.size(),
			collapseNotDoableQuestList,
			() ->
			{
				collapseNotDoableQuestList = !collapseNotDoableQuestList;
				stateManager.setCollapseNotDoableQuestList(collapseNotDoableQuestList);
				refreshQuestSummary();
			},
			PureHelperUiConstants.BLOCKED_ACCENT);
		questContainer.add(questsHeader, c);
		c.gridy++;

		if (!collapseNotDoableQuestList)
		{
			if (blockedRules.isEmpty())
			{
				questContainer.add(noneRow(), c);
				c.gridy++;
			}
			else
			{
				for (QuestRule rule : blockedRules)
				{
					QuestEvaluation eval = evaluator.evaluateQuest(
						rule, avoidedSkills, skillCaps, effectiveChoicePolicy(),
						config.safeguardStrictness(), memo, new HashSet<>());
					JPanel row = buildAccentRow(
						SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
						rule.name,
						PureHelperUiConstants.BLOCKED_ACCENT,
						PureHelperUiConstants.BLOCKED_TEXT,
						eval.reason == null || eval.reason.isBlank() ? "No protected-skill XP risk found." : eval.reason);
					questContainer.add(row, c);
					c.gridy++;
				}
			}
		}

		questContainer.add(Box.createRigidArea(new Dimension(0, 6)), c);
		c.gridy++;

		// Not doable achievement diaries
		JPanel diariesHeader = buildCollapsibleHeader(
			SpriteID.QUESTS_PAGE_ICON_GREEN_ACHIEVEMENT_DIARIES,
			"Not doable achievement diaries",
			riskyDiaryTiers.size(),
			collapseNotDoableDiaryList,
			() ->
			{
				collapseNotDoableDiaryList = !collapseNotDoableDiaryList;
				stateManager.setCollapseNotDoableDiaryList(collapseNotDoableDiaryList);
				refreshQuestSummary();
			},
			PureHelperUiConstants.BLOCKED_ACCENT);
		questContainer.add(diariesHeader, c);
		c.gridy++;

		if (!collapseNotDoableDiaryList)
		{
			if (riskyDiaryTiers.isEmpty())
			{
				questContainer.add(noneRow(), c);
				c.gridy++;
			}
			else
			{
				for (Map.Entry<String, List<String>> entry : riskyDiaryTiers.entrySet())
				{
					List<String> tiers = new ArrayList<>(entry.getValue());
					tiers.sort((a, b) -> Integer.compare(tierOrder(a), tierOrder(b)));

					JPanel diaryRow = buildAccentRow(
						SpriteID.QUESTS_PAGE_ICON_GREEN_ACHIEVEMENT_DIARIES,
						entry.getKey(),
						PureHelperUiConstants.BLOCKED_ACCENT,
						PureHelperUiConstants.BLOCKED_TEXT,
						"This diary has at least one not-doable tier for current protected skills/caps.");
					questContainer.add(diaryRow, c);
					c.gridy++;

					for (String tierName : tiers)
					{
						JPanel tierRow = buildAccentRow(
							SpriteID.RS2_TAB_STATS,
							formatTierLabel(tierName),
							PureHelperUiConstants.BLOCKED_ACCENT,
							PureHelperUiConstants.BLOCKED_TEXT,
							"Not doable tier.");
						questContainer.add(tierRow, c);
						c.gridy++;
					}
				}
			}
		}

		questContainer.revalidate();
		questContainer.repaint();
	}

	// ============================================================
	// Risk log
	// ============================================================

	private void refreshRiskLog()
	{
		rowIndex = 0;
		riskLogContainer.removeAll();
		GridBagConstraints c = baseConstraints();

		if (riskLogEntries.isEmpty())
		{
			JLabel empty = new JLabel("No risky selections yet.");
			empty.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			empty.setFont(FontManager.getRunescapeSmallFont());
			riskLogContainer.add(empty, c);
		}
		else
		{
			for (String line : riskLogEntries)
			{
				JPanel row = buildAccentRow(
					SpriteID.QUESTS_PAGE_ICON_RED_MINIGAMES,
					line,
					PureHelperUiConstants.ACCENT_PRIMARY_DIM,
					ColorScheme.LIGHT_GRAY_COLOR,
					"Recent risky selection.");
				riskLogContainer.add(row, c);
				c.gridy++;
			}
		}

		riskLogContainer.revalidate();
		riskLogContainer.repaint();
	}

	// ============================================================
	// Rule loading
	// ============================================================

	private void loadQuestRules()
	{
		questRulesByName = RuleLoader.loadQuestRulesByName(gson, PureHelperPanel.class, QUEST_RULES_PATH);
		allQuestRules = new ArrayList<>(questRulesByName.values());
		log.debug("Panel loaded {} quest rules", questRulesByName.size());
	}

	private void loadDiaryRules()
	{
		allDiaryRules = RuleLoader.loadDiaryRulesList(
			gson, PureHelperPanel.class, DIARY_RULES_PATH, "achievement-diary-rules.json");
		log.debug("Panel loaded {} diary rules", allDiaryRules.size());
	}

	private QuestChoicePolicy effectiveChoicePolicy()
	{
		switch (config.safeguardStrictness())
		{
			case STRICT:
				return QuestChoicePolicy.ANY_CHOICE_MATCH_IS_RISKY;
			case LENIENT:
				return QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE;
			default:
				return config.questChoicePolicy();
		}
	}

	// ============================================================
	// Rendering helpers
	// ============================================================

	private GridBagConstraints baseConstraints()
	{
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new java.awt.Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		return c;
	}

	private void addTitle(JPanel panel, String title, GridBagConstraints c)
	{
		panel.add(Box.createRigidArea(new Dimension(0, 6)), c);
		c.gridy++;

		JPanel titleRow = new JPanel(new BorderLayout(6, 0));
		titleRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		titleRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));

		JLabel iconLabel = spriteLabel(sectionSpriteId(title), 16);
		titleRow.add(iconLabel, BorderLayout.WEST);

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		titleRow.add(titleLabel, BorderLayout.CENTER);

		panel.add(titleRow, c);
		c.gridy++;

		panel.add(Box.createRigidArea(new Dimension(0, 4)), c);
		c.gridy++;
	}

	private JLabel noneRow()
	{
		JLabel none = new JLabel("None");
		none.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		none.setFont(FontManager.getRunescapeSmallFont());
		return none;
	}

	private JPanel buildAccentRow(int spriteId, String text, Color barColor, Color textColor, String tooltip)
	{
		Color bg = (rowIndex++ % 2 == 0) ? ROW_EVEN : ROW_ODD;
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(bg);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, barColor),
			new EmptyBorder(3, 6, 3, 0)));

		JLabel iconLabel = spriteLabel(spriteId, 14);
		row.add(iconLabel, BorderLayout.WEST);

		JLabel label = new JLabel(text);
		label.setForeground(textColor);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setToolTipText(tooltip);
		row.setToolTipText(tooltip);
		row.add(label, BorderLayout.CENTER);

		addRowHover(row, bg);
		return row;
	}

	private void addRowHover(JPanel row, Color baseBg)
	{
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ROW_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(baseBg);
			}
		});
	}

	private JPanel buildCollapsibleHeader(int spriteId, String title, int count, boolean collapsed, Runnable onToggle, Color accentColor)
	{
		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accentColor),
			new EmptyBorder(3, 6, 3, 6)));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel iconLabel = spriteLabel(spriteId, 14);
		header.add(iconLabel, BorderLayout.WEST);

		String marker = collapsed ? "\u25B6" : "\u25BC";
		JLabel label = new JLabel(marker + " " + title + " (" + count + ")");
		label.setForeground(new Color(255, 186, 186));
		label.setFont(FontManager.getRunescapeSmallFont());
		header.add(label, BorderLayout.CENTER);
		header.setToolTipText("Click to expand/collapse.");
		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (onToggle != null)
				{
					onToggle.run();
				}
			}
		});
		return header;
	}

	private JLabel spriteLabel(int spriteId, int size)
	{
		JLabel label = new JLabel();
		label.setPreferredSize(new Dimension(size, size));
		label.setMinimumSize(new Dimension(size, size));
		label.setMaximumSize(new Dimension(size, size));
		spriteManager.getSpriteAsync(spriteId, 0, img ->
		{
			if (img != null)
			{
				BufferedImage scaled = ImageUtil.resizeImage(img, size, size);
				SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(scaled)));
			}
		});
		return label;
	}

	private int sectionSpriteId(String title)
	{
		String lower = title == null ? "" : title.toLowerCase(Locale.ENGLISH);
		if (lower.contains("quest") || lower.contains("diary"))
		{
			return SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS;
		}
		if (lower.contains("build") || lower.contains("skill"))
		{
			return SpriteID.RS2_TAB_STATS;
		}
		if (lower.contains("log"))
		{
			return SpriteID.QUESTS_PAGE_ICON_RED_MINIGAMES;
		}
		return SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS;
	}

	private int tierOrder(String tier)
	{
		if ("EASY".equalsIgnoreCase(tier))
		{
			return 0;
		}
		if ("MEDIUM".equalsIgnoreCase(tier))
		{
			return 1;
		}
		if ("HARD".equalsIgnoreCase(tier))
		{
			return 2;
		}
		if ("ELITE".equalsIgnoreCase(tier))
		{
			return 3;
		}
		return 99;
	}

	private String formatTierLabel(String tier)
	{
		if (tier == null || tier.isBlank())
		{
			return "";
		}
		String lower = tier.toLowerCase(Locale.ENGLISH);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	private void applyRuneLiteScrollBarUi()
	{
		JScrollPane scrollPane = getScrollPane();
		scrollPane.getVerticalScrollBar().setUI(new RuneLiteScrollBarUI());
		scrollPane.getHorizontalScrollBar().setUI(new RuneLiteScrollBarUI());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
	}
}
