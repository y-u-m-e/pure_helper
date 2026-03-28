package com.yumesplugins.purehelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.ImageUtil;

@Slf4j
@Singleton
public class PureHelperPanel extends PluginPanel
{
	private static final String CONFIG_GROUP = "purehelper";
	private static final String QUEST_RULES_PATH = "/quest-filter-rules.json";
	private static final String DIARY_RULES_PATH = "/achievement-diary-rules.json";
	private static final String SKILL_CAPS_KEY = "protectedSkillCapsCsv";

	private final ConfigManager configManager;
	private final PureHelperConfig config;
	private final Client client;
	private final SpriteManager spriteManager;
	private final PureHelperStateManager stateManager;

	private final JPanel tabDisplay = new JPanel(new BorderLayout());
	private final MaterialTabGroup tabGroup = new MaterialTabGroup(tabDisplay);
	private final JPanel questSettingsTab = createTabPanel();
	private final JPanel combatSettingsTab = createTabPanel();
	private final JPanel skillSettingsTab = createTabPanel();
	private final JPanel activityTab = createTabPanel();
	private final JPanel questSettingsPreviewContainer = new JPanel(new GridBagLayout());
	private final JPanel questBrowserContainer = new JPanel(new GridBagLayout());
	private final JPanel questSummaryStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
	private final ToggleChoiceRow questToggle = new ToggleChoiceRow();
	private final ToggleChoiceRow questChoicePolicyToggle = new ToggleChoiceRow();
	private final ToggleChoiceRow showLockedQuestReasonToggle = new ToggleChoiceRow();
	private final ToggleChoiceRow combatToggle = new ToggleChoiceRow();
	private final ToggleChoiceRow hideUnsafeStylesToggle = new ToggleChoiceRow();
	private final ToggleChoiceRow logRiskyToggle = new ToggleChoiceRow();
	private final ToggleChoiceRow skillLockToggle = new ToggleChoiceRow();
	private final JComboBox<BuildProfile> buildProfileSelector = new JComboBox<>(BuildProfile.values());
	private final JPanel avoidedSkillGrid = new JPanel(new GridLayout(0, 2, 6, 6));
	private final JPanel skillCapGrid = new JPanel(new GridLayout(0, 2, 6, 6));
	private final JPanel riskLogContainer = new JPanel(new GridBagLayout());
	private final Map<AvoidedSkill, SelectTile> skillTiles = new EnumMap<>(AvoidedSkill.class);
	private final Map<AvoidedSkill, JTextField> skillCapInputs = new EnumMap<>(AvoidedSkill.class);
	private final Map<String, Boolean> collapsedDiarySections = new HashMap<>();
	private final Deque<String> riskLogEntries = new ArrayDeque<>();
	private final Set<AvoidedSkill> selectedAvoidedSkills = EnumSet.noneOf(AvoidedSkill.class);
	private final JLabel avoidedSkillTitleLabel = new JLabel();
	private Map<String, QuestRule> questRulesByName = new HashMap<>();
	private List<QuestRule> allQuestRules = new java.util.ArrayList<>();
	private List<DiaryRule> allDiaryRules = new java.util.ArrayList<>();

	private static final int MAX_RISK_LOG_ENTRIES = 8;
	private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	private static final Color ROW_EVEN = PureHelperUiConstants.ROW_EVEN;
	private static final Color ROW_ODD = PureHelperUiConstants.ROW_ODD;
	private static final Color ROW_HOVER = PureHelperUiConstants.ROW_HOVER;

	private boolean syncingUi;
	private boolean compactRows;
	private boolean collapseNotDoableQuestList = true;
	private boolean collapseNotDoableDiaryList = true;
	private int rowIndex;

	@Inject
	private PureHelperPanel(
		ConfigManager configManager,
		PureHelperConfig config,
		Client client,
		SpriteManager spriteManager,
		PureHelperStateManager stateManager)
	{
		this.configManager = configManager;
		this.config = config;
		this.client = client;
		this.spriteManager = spriteManager;
		this.stateManager = stateManager;
		PureHelperUiConstants.applyAccent(config.accentColor());
		this.compactRows = stateManager.isCompactRows();
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

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.setBorder(new EmptyBorder(0, 6, 6, 6));
		tabGroup.setLayout(new GridLayout(1, 4, 2, 0));
		tabGroup.setBorder(new EmptyBorder(5, 0, 10, 0));
		tabGroup.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabGroup.setPreferredSize(new Dimension(0, 46));
		northPanel.add(tabGroup, BorderLayout.SOUTH);
		tabDisplay.setBackground(ColorScheme.DARK_GRAY_COLOR);
		centerPanel.add(tabDisplay, BorderLayout.CENTER);

		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);

		loadQuestRules();
		loadDiaryRules();
		buildContent();
	}

	@Override
	public void onActivate()
	{
		applyRuneLiteScrollBarUi();
		syncFromConfig();
	}

	public void refreshFromConfig()
	{
		SwingUtilities.invokeLater(() ->
		{
			applyRuneLiteScrollBarUi();
			syncFromConfig();
		});
	}

	private void buildContent()
	{
		buildQuestSettingsTab();
		buildCombatTab();
		buildSkillTab();
		buildActivityTab();

		tabGroup.removeAll();
		MaterialTab questSettings = new IconMaterialTab(tabGroup, questSettingsTab, "/tab-icons/safeguards.png", 30, "Safeguards");
		MaterialTab combatSettings = new IconMaterialTab(tabGroup, combatSettingsTab, "/tab-icons/combat.png", 30, "Combat");
		MaterialTab skillSettings = new IconMaterialTab(tabGroup, skillSettingsTab, "/tab-icons/skills.png", 30, "Skills");
		MaterialTab activity = new IconMaterialTab(tabGroup, activityTab, "/tab-icons/log.png", 30, "Risk Log");
		tabGroup.addTab(questSettings);
		tabGroup.addTab(combatSettings);
		tabGroup.addTab(skillSettings);
		tabGroup.addTab(activity);
		tabGroup.select(questSettings);
		tabGroup.revalidate();
		tabGroup.repaint();
	}

	private JPanel createTabPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		return panel;
	}

	private void buildQuestSettingsTab()
	{
		questSettingsTab.removeAll();
		GridBagConstraints c = baseConstraints();

		addTitle(questSettingsTab, "Quest Safeguards", c);
		addSettingLabel(questSettingsTab, "Quest filtering", "Enable or disable quest XP protection.", c);
		questToggle.setLabels("Enabled", "Disabled");
		questToggle.setOptionTooltips(
			"Filter quests that are unsafe for your protected skills.",
			"Do not apply quest filtering.");
		questToggle.setChangeListener(selected ->
		{
			if (!syncingUi)
			{
				configManager.setConfiguration(CONFIG_GROUP, "enableQuestSafeguards", selected);
			}
		});
		questSettingsTab.add(questToggle, c);
		c.gridy++;

		addSettingLabel(questSettingsTab, "Choice reward policy", "Control how optional reward choices are treated.", c);
		questChoicePolicyToggle.setLabels("Safe default", "Strict risky");
		questChoicePolicyToggle.setOptionTooltips(
			"Only block unavoidable reward paths.",
			"Block if any quest reward choice can give protected XP.");
		questChoicePolicyToggle.setChangeListener(selectedSafeDefault ->
		{
			if (!syncingUi)
			{
				configManager.setConfiguration(
					CONFIG_GROUP,
					"questChoicePolicy",
					selectedSafeDefault ? QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE : QuestChoicePolicy.ANY_CHOICE_MATCH_IS_RISKY);
			}
		});
		questSettingsTab.add(questChoicePolicyToggle, c);
		c.gridy++;

		addSettingLabel(questSettingsTab, "Blocked quest visibility", "Choose whether blocked quests stay visible.", c);
		showLockedQuestReasonToggle.setLabels("Show", "Hide");
		showLockedQuestReasonToggle.setOptionTooltips(
			"Keep blocked quests visible with a hover reason.",
			"Hide blocked quests from the quest list.");
		showLockedQuestReasonToggle.setChangeListener(showReason ->
		{
			if (!syncingUi)
			{
				configManager.setConfiguration(CONFIG_GROUP, "showLockedQuestReason", showReason);
			}
		});
		questSettingsTab.add(showLockedQuestReasonToggle, c);
		c.gridy++;

		addSettingLabel(questSettingsTab, "Quest + diary preview", "Quick summary using current protected skills.", c);
		questSettingsPreviewContainer.removeAll();
		questSettingsPreviewContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		questSettingsTab.add(questSettingsPreviewContainer, c);
		c.gridy++;

		// Populate safeguards preview immediately when this tab is built.
		refreshQuestBrowser();
	}

	private void buildCombatTab()
	{
		combatSettingsTab.removeAll();
		GridBagConstraints c = baseConstraints();

		addTitle(combatSettingsTab, "Combat Safety", c);
		addSettingLabel(combatSettingsTab, "Warning infobox", "Show warnings when a style trains protected XP.", c);
		combatToggle.setLabels("Enabled", "Disabled");
		combatToggle.setOptionTooltips(
			"Show warning infobox for unsafe styles.",
			"Disable warning infobox.");
		combatToggle.setChangeListener(selected ->
		{
			if (!syncingUi)
			{
				configManager.setConfiguration(CONFIG_GROUP, "enableCombatWarnings", selected);
			}
		});
		combatSettingsTab.add(combatToggle, c);
		c.gridy++;

		addSettingLabel(combatSettingsTab, "Hide unsafe styles", "Hide combat buttons that would train protected skills.", c);
		hideUnsafeStylesToggle.setLabels("Enabled", "Disabled");
		hideUnsafeStylesToggle.setOptionTooltips(
			"Hide unsafe style buttons from the combat tab.",
			"Leave all style buttons visible.");
		hideUnsafeStylesToggle.setChangeListener(selected ->
		{
			if (!syncingUi)
			{
				configManager.setConfiguration(CONFIG_GROUP, "hideUnsafeAttackStyles", selected);
			}
		});
		combatSettingsTab.add(hideUnsafeStylesToggle, c);
		c.gridy++;

		addSettingLabel(combatSettingsTab, "Risk logging", "Keep a timestamped panel log of risky style changes.", c);
		logRiskyToggle.setLabels("Enabled", "Disabled");
		logRiskyToggle.setOptionTooltips(
			"Record risky style changes in the panel log.",
			"Do not record risky style changes.");
		logRiskyToggle.setChangeListener(selected ->
		{
			if (!syncingUi)
			{
				configManager.setConfiguration(CONFIG_GROUP, "logRiskySelections", selected);
			}
		});
		combatSettingsTab.add(logRiskyToggle, c);
		c.gridy++;
	}

	private void buildSkillTab()
	{
		skillSettingsTab.removeAll();
		GridBagConstraints c = baseConstraints();

		addTitle(skillSettingsTab, "Protected Skills", c);

		addSettingLabel(skillSettingsTab, "Build preset", "Apply a preset here or from plugin settings; both stay in sync.", c);
		applyRuneLiteComboBoxStyle(buildProfileSelector);
		buildProfileSelector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		buildProfileSelector.setForeground(Color.WHITE);
		buildProfileSelector.setFont(FontManager.getRunescapeSmallFont());
		buildProfileSelector.setFocusable(false);
		buildProfileSelector.setToolTipText("Choose a build preset. Selecting a non-custom profile updates protected skills and caps.");
		buildProfileSelector.addActionListener(e ->
		{
			if (syncingUi)
			{
				return;
			}
			BuildProfile selected = (BuildProfile) buildProfileSelector.getSelectedItem();
			if (selected != null)
			{
				configManager.setConfiguration(CONFIG_GROUP, "buildProfile", selected.name());
			}
		});
		skillSettingsTab.add(buildProfileSelector, c);
		c.gridy++;
		avoidedSkillTitleLabel.setForeground(Color.WHITE);
		avoidedSkillTitleLabel.setFont(FontManager.getRunescapeBoldFont());
		avoidedSkillTitleLabel.setToolTipText("Skill selection is edited directly in this panel.");
		skillSettingsTab.add(avoidedSkillTitleLabel, c);
		c.gridy++;
		addSettingLabel(skillSettingsTab, "Skill editing lock", "Prevent accidental changes to protected skills and caps.", c);
		skillLockToggle.setLabels("Unlocked", "Locked");
		skillLockToggle.setOptionTooltips(
			"Allow editing of protected skills and caps.",
			"Disable editing until unlocked.");
		skillLockToggle.setChangeListener(unlocked ->
		{
			stateManager.setSkillSelectionLocked(!unlocked);
			updateSkillSelectionLockUi();
		});
		skillSettingsTab.add(skillLockToggle, c);
		c.gridy++;
		JLabel avoidedSkillSubtitleLabel = new JLabel("Select one or more XP types to avoid.");
		avoidedSkillSubtitleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		avoidedSkillSubtitleLabel.setFont(FontManager.getRunescapeSmallFont());
		avoidedSkillSubtitleLabel.setToolTipText("Tip: choose multiple skills if your account build avoids several XP types.");
		skillSettingsTab.add(avoidedSkillSubtitleLabel, c);
		c.gridy++;
		skillSettingsTab.add(Box.createRigidArea(new Dimension(0, 6)), c);
		c.gridy++;
		buildSkillTiles();
		skillSettingsTab.add(avoidedSkillGrid, c);
		c.gridy++;

		addSettingLabel(skillSettingsTab, "Max protected levels", "Optional caps per skill (blank = block any XP gain).", c);
		buildSkillCapInputs();
		skillSettingsTab.add(skillCapGrid, c);
		c.gridy++;

		addSettingLabel(skillSettingsTab, "Build preset file actions", "Import/export your current protected skills and level caps.", c);
		JPanel fileActions = new JPanel(new GridLayout(1, 3, 6, 0));
		fileActions.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JButton exportButton = buildPanelButton("Export");
		exportButton.addActionListener(e -> exportBuildPreset());
		JButton importButton = buildPanelButton("Import");
		importButton.addActionListener(e -> importBuildPreset());
		JButton resetButton = buildPanelButton("Reset local");
		resetButton.addActionListener(e -> resetLocalState());
		fileActions.add(exportButton);
		fileActions.add(importButton);
		fileActions.add(resetButton);
		skillSettingsTab.add(fileActions, c);
		c.gridy++;
	}

	private static <T> void applyRuneLiteComboBoxStyle(JComboBox<T> comboBox)
	{
		ListCellRenderer<? super T> defaultRenderer = comboBox.getRenderer();
		comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) ->
		{
			JLabel label = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setBorder(new EmptyBorder(3, 6, 3, 6));
			label.setFont(FontManager.getRunescapeSmallFont());
			if (isSelected)
			{
				label.setBackground(PureHelperUiConstants.ACCENT_PRIMARY);
				label.setForeground(Color.WHITE);
			}
			else
			{
				label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				label.setForeground(Color.WHITE);
			}
			return label;
		});
		comboBox.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
		comboBox.setUI(new BasicComboBoxUI()
		{
			@Override
			protected JButton createArrowButton()
			{
				JButton arrowButton = new JButton("\u25BE");
				arrowButton.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR));
				arrowButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				arrowButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				arrowButton.setFont(FontManager.getRunescapeSmallFont());
				arrowButton.setFocusable(false);
				arrowButton.setContentAreaFilled(true);
				return arrowButton;
			}

			@Override
			protected ComboPopup createPopup()
			{
				return new BasicComboPopup(comboBox)
				{
					@Override
					protected JScrollPane createScroller()
					{
						JScrollPane scrollPane = super.createScroller();
						scrollPane.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
						scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						scrollPane.getVerticalScrollBar().setUI(new RuneLiteScrollBarUI());
						scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
						return scrollPane;
					}
				};
			}
		});
	}

	private void buildActivityTab()
	{
		activityTab.removeAll();
		GridBagConstraints c = baseConstraints();
		addTitle(activityTab, "Risk Log", c);
		riskLogContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		activityTab.add(riskLogContainer, c);
		c.gridy++;
		refreshRiskLog();
	}

	private GridBagConstraints baseConstraints()
	{
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 6, 0);
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
		titleLabel.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 13));
		titleRow.add(titleLabel, BorderLayout.CENTER);

		panel.add(titleRow, c);
		c.gridy++;
	}

	private void addSettingLabel(JPanel panel, String label, String subtitle, GridBagConstraints c)
	{
		JLabel settingLabel = new JLabel(label);
		settingLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		settingLabel.setFont(FontManager.getRunescapeSmallFont());
		panel.add(settingLabel, c);
		c.gridy++;

		JLabel subtitleLabel = new JLabel(subtitle);
		subtitleLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		subtitleLabel.setFont(FontManager.getRunescapeSmallFont());
		panel.add(subtitleLabel, c);
		c.gridy++;

		panel.add(Box.createRigidArea(new Dimension(0, 2)), c);
		c.gridy++;
	}

	private void buildSkillTiles()
	{
		avoidedSkillGrid.removeAll();
		skillTiles.clear();
		avoidedSkillGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		for (AvoidedSkill skill : AvoidedSkill.values())
		{
			SelectTile tile = new SelectTile(skill.getLabel());
			tile.setIcon(loadSkillIcon(skill));
			tile.setClickListener(() ->
			{
				if (stateManager.isSkillSelectionLocked())
				{
					return;
				}
				toggleAvoidedSkill(skill);
				if (!syncingUi)
				{
					persistAvoidedSkills();
				}
			});
			tile.setHoverText("Click to " + (skill == AvoidedSkill.NONE ? "clear all protected skills." : "toggle " + skill.getLabel() + " protection."));
			skillTiles.put(skill, tile);
			avoidedSkillGrid.add(tile);
		}
		updateSkillSelectionLockUi();
	}

	private void buildSkillCapInputs()
	{
		skillCapGrid.removeAll();
		skillCapInputs.clear();
		skillCapGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);

		for (AvoidedSkill skill : AvoidedSkill.values())
		{
			if (skill == AvoidedSkill.NONE)
			{
				continue;
			}

			JLabel label = new JLabel(skill.getLabel() + " max");
			label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			label.setFont(FontManager.getRunescapeSmallFont());

			JTextField input = new JTextField();
			applyRuneLiteInputStyle(input);
			input.setToolTipText("Set max " + skill.getLabel() + " level (1-99). Leave blank to block all XP in this skill.");
			input.setColumns(3);
			input.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent e)
				{
					persistSkillCapsFromInputs();
				}

				@Override
				public void removeUpdate(DocumentEvent e)
				{
					persistSkillCapsFromInputs();
				}

				@Override
				public void changedUpdate(DocumentEvent e)
				{
					persistSkillCapsFromInputs();
				}
			});
			input.addFocusListener(new FocusAdapter()
			{
				@Override
				public void focusLost(FocusEvent e)
				{
					normalizeSkillCapInput(skill, input);
				}
			});
			skillCapInputs.put(skill, input);
			skillCapGrid.add(label);
			skillCapGrid.add(input);
		}
	}

	private void applyRuneLiteInputStyle(JTextField input)
	{
		input.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		input.setForeground(Color.WHITE);
		input.setCaretColor(Color.WHITE);
		input.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		input.setFont(FontManager.getRunescapeSmallFont());
	}

	private JButton buildPanelButton(String text)
	{
		JButton button = new JButton(text);
		button.setFocusPainted(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		button.setFont(FontManager.getRunescapeSmallFont());
		return button;
	}

	private void exportBuildPreset()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Export Pure Helper Build Preset");
		chooser.setSelectedFile(new java.io.File("pure-helper-build-preset.json"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		try
		{
			Path output = chooser.getSelectedFile().toPath();
			stateManager.exportBuildPreset(output, config.buildProfile().toString(), "Exported from Pure Helper panel");
			JOptionPane.showMessageDialog(this, "Build preset exported:\n" + output, "Pure Helper", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception ex)
		{
			log.warn("Failed exporting build preset", ex);
			JOptionPane.showMessageDialog(this, "Failed to export build preset:\n" + ex.getMessage(), "Pure Helper", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void importBuildPreset()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Import Pure Helper Build Preset");
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		try
		{
			Path input = chooser.getSelectedFile().toPath();
			boolean imported = stateManager.importBuildPreset(input);
			if (!imported)
			{
				JOptionPane.showMessageDialog(this, "Selected file is not a valid Pure Helper preset.", "Pure Helper", JOptionPane.WARNING_MESSAGE);
				return;
			}
			syncFromConfig();
			refreshQuestBrowser();
			JOptionPane.showMessageDialog(this, "Build preset imported:\n" + input, "Pure Helper", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception ex)
		{
			log.warn("Failed importing build preset", ex);
			JOptionPane.showMessageDialog(this, "Failed to import build preset:\n" + ex.getMessage(), "Pure Helper", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void resetLocalState()
	{
		int result = JOptionPane.showConfirmDialog(
			this,
			"Reset local Pure Helper state to defaults?\nThis clears local skill/cap selections and UI layout state.",
			"Reset local state",
			JOptionPane.YES_NO_OPTION);
		if (result != JOptionPane.YES_OPTION)
		{
			return;
		}

		stateManager.resetState(config.avoidedSkillsCsv(), config.protectedSkillCapsCsv());
		compactRows = stateManager.isCompactRows();
		collapseNotDoableQuestList = stateManager.isCollapseNotDoableQuestList();
		collapseNotDoableDiaryList = stateManager.isCollapseNotDoableDiaryList();
		collapsedDiarySections.clear();
		syncFromConfig();
		refreshQuestBrowser();
	}

	private ImageIcon loadSkillIcon(AvoidedSkill skill)
	{
		String iconName;
		switch (skill)
		{
			case ATTACK:
				iconName = "Attack_icon.png";
				break;
			case STRENGTH:
				iconName = "Strength_icon.png";
				break;
			case DEFENCE:
				iconName = "Defence_icon.png";
				break;
			case HITPOINTS:
				iconName = "Hitpoints_icon.png";
				break;
			case RANGED:
				iconName = "Ranged_icon.png";
				break;
			case MAGIC:
				iconName = "Magic_icon.png";
				break;
			case PRAYER:
				iconName = "Prayer_icon.png";
				break;
			default:
				return null;
		}

		try
		{
			java.net.URL url = PureHelperPanel.class.getResource("/" + iconName);
			if (url == null)
			{
				return null;
			}
			ImageIcon raw = new ImageIcon(url);
			Image scaled = raw.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
		}
		catch (Exception ignored)
		{
			return null;
		}
	}

	private void toggleAvoidedSkill(AvoidedSkill skill)
	{
		if (skill == AvoidedSkill.NONE)
		{
			selectedAvoidedSkills.clear();
			selectAvoidedSkills(selectedAvoidedSkills);
			return;
		}

		if (selectedAvoidedSkills.contains(skill))
		{
			selectedAvoidedSkills.remove(skill);
		}
		else
		{
			selectedAvoidedSkills.add(skill);
		}

		selectedAvoidedSkills.remove(AvoidedSkill.NONE);
		selectAvoidedSkills(selectedAvoidedSkills);
	}

	private void selectAvoidedSkills(Set<AvoidedSkill> skills)
	{
		Set<AvoidedSkill> incoming = EnumSet.noneOf(AvoidedSkill.class);
		incoming.addAll(skills);

		selectedAvoidedSkills.clear();
		selectedAvoidedSkills.addAll(incoming);

		boolean noneSelected = selectedAvoidedSkills.isEmpty();
		for (Map.Entry<AvoidedSkill, SelectTile> entry : skillTiles.entrySet())
		{
			boolean selected = entry.getKey() == AvoidedSkill.NONE ? noneSelected : selectedAvoidedSkills.contains(entry.getKey());
			entry.getValue().setSelected(selected);
		}
		updateSkillSelectionLockUi();
	}

	private void updateSkillSelectionLockUi()
	{
		boolean locked = stateManager.isSkillSelectionLocked();
		avoidedSkillTitleLabel.setText("Avoided skill XP (" + (locked ? "Locked" : "Unlocked") + ")");
		avoidedSkillTitleLabel.setToolTipText(locked
			? "Unlock in this tab to edit protected skills and caps."
			: "Skill selection is edited directly in this panel.");
		skillLockToggle.setSelected(!locked);
		for (Map.Entry<AvoidedSkill, SelectTile> entry : skillTiles.entrySet())
		{
			AvoidedSkill skill = entry.getKey();
			SelectTile tile = entry.getValue();
			tile.setText(skill.getLabel());
			tile.setEnabled(!locked);
			tile.setHoverText(locked
				? "Locked. Switch to Unlocked above to edit."
				: skill == AvoidedSkill.NONE
					? "Click to clear all protected skills."
					: "Click to toggle " + skill.getLabel() + " protection.");
		}

		for (JTextField input : skillCapInputs.values())
		{
			input.setEnabled(!locked);
		}
	}

	private void normalizeSkillCapInput(AvoidedSkill skill, JTextField input)
	{
		if (input == null)
		{
			return;
		}

		String raw = input.getText() == null ? "" : input.getText().trim();
		if (raw.isEmpty())
		{
			input.setText("");
			persistSkillCapsFromInputs();
			return;
		}

		try
		{
			int value = Integer.parseInt(raw);
			if (value < 1 || value > 99)
			{
				input.setText("");
			}
			else
			{
				input.setText(Integer.toString(value));
			}
		}
		catch (NumberFormatException ex)
		{
			input.setText("");
		}
		persistSkillCapsFromInputs();
	}

	private void persistSkillCapsFromInputs()
	{
		if (syncingUi)
		{
			return;
		}
		if (stateManager.isSkillSelectionLocked())
		{
			return;
		}

		Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
		for (Map.Entry<AvoidedSkill, JTextField> entry : skillCapInputs.entrySet())
		{
			String raw = entry.getValue().getText();
			if (raw == null || raw.trim().isEmpty())
			{
				continue;
			}
			try
			{
				int value = Integer.parseInt(raw.trim());
				if (value >= 1 && value <= 99)
				{
					caps.put(entry.getKey(), value);
				}
			}
			catch (NumberFormatException ignored)
			{
				// Ignore bad temporary user input while typing.
			}
		}

		configManager.setConfiguration(CONFIG_GROUP, SKILL_CAPS_KEY, ConfigParsers.toSkillCapsCsv(caps));
		stateManager.setSkillState(currentAvoidedSkillsCsv(), ConfigParsers.toSkillCapsCsv(caps));
	}

	private void loadQuestRules()
	{
		questRulesByName = RuleLoader.loadQuestRulesByName(PureHelperPanel.class, QUEST_RULES_PATH);
		allQuestRules = new java.util.ArrayList<>(questRulesByName.values());
		log.debug("Panel loaded {} quest rules", questRulesByName.size());
	}

	private void loadDiaryRules()
	{
		allDiaryRules = RuleLoader.loadDiaryRulesList(
			PureHelperPanel.class,
			DIARY_RULES_PATH,
			"achievement-diary-rules.json");
		log.debug("Panel loaded {} diary rules", allDiaryRules.size());
	}

	private void refreshQuestBrowser()
	{
		rowIndex = 0;
		questBrowserContainer.removeAll();
		questSettingsPreviewContainer.removeAll();
		questSummaryStrip.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		GridBagConstraints questTabPreviewConstraints = new GridBagConstraints();
		questTabPreviewConstraints.fill = GridBagConstraints.HORIZONTAL;
		questTabPreviewConstraints.weightx = 1;
		questTabPreviewConstraints.gridx = 0;
		questTabPreviewConstraints.gridy = 0;

		if (allQuestRules.isEmpty())
		{
			loadQuestRules();
		}
		if (allDiaryRules.isEmpty())
		{
			loadDiaryRules();
		}

		Set<AvoidedSkill> avoidedSkills = ConfigParsers.parseAvoidedSkillsCsv(loadedAvoidedSkillsCsv());
		Map<AvoidedSkill, Integer> skillCaps = ConfigParsers.parseSkillCapsCsv(loadedSkillCapsCsv());
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(client, questRulesByName);

		List<QuestRule> rules;
		try
		{
			rules = allQuestRules.stream()
				.filter(Objects::nonNull)
				.filter(rule -> rule.name != null && !rule.name.isBlank())
				.sorted((a, b) -> a.name.compareToIgnoreCase(b.name))
				.collect(Collectors.toList());
		}
		catch (Exception ex)
		{
			log.warn("Failed building quest list rows", ex);
			JLabel error = new JLabel("Unable to render quest list.");
			error.setForeground(PureHelperUiConstants.ERROR_TEXT);
			error.setFont(FontManager.getRunescapeSmallFont());
			questBrowserContainer.add(error, c);
			questBrowserContainer.revalidate();
			questBrowserContainer.repaint();
			return;
		}

		List<QuestRule> blockedRules = new java.util.ArrayList<>();
		List<QuestRule> allowedRules = new java.util.ArrayList<>();
		List<QuestRule> cautionRules = new java.util.ArrayList<>();
		Map<String, QuestEvaluation> memo = new HashMap<>();
		for (QuestRule rule : rules)
		{
			QuestEvaluation eval = evaluator.evaluateQuest(
				rule,
				avoidedSkills,
				skillCaps,
				effectiveChoicePolicy(),
				config.safeguardStrictness(),
				memo,
				new HashSet<>());
			if (evaluator.hasChoiceXpCaution(rule))
			{
				cautionRules.add(rule);
			}
			if (eval.isBlocked())
			{
				blockedRules.add(rule);
			}
			else
			{
				allowedRules.add(rule);
			}
		}

		List<String> diaryRiskyTierLabels = new java.util.ArrayList<>();
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
				String tierLabel = diary.name + " " + tier.tier;
				if (evaluator.evaluateDiaryTierRisk(
					tier,
					avoidedSkills,
					skillCaps,
					effectiveChoicePolicy(),
					config.safeguardStrictness(),
					memo))
				{
					diaryRiskyTierLabels.add(tierLabel);
					riskyDiaryTiers.computeIfAbsent(diary.name, key -> new java.util.ArrayList<>()).add(tier.tier);
				}
			}
		}

		questSummaryStrip.add(buildChip("Blocked " + blockedRules.size(), PureHelperUiConstants.BLOCKED_ACCENT));
		questSummaryStrip.add(buildChip("Caution " + cautionRules.size(), PureHelperUiConstants.CAUTION_ACCENT));
		questSummaryStrip.add(buildChip("Safe " + allowedRules.size(), PureHelperUiConstants.SAFE_ACCENT));
		questSummaryStrip.add(buildChip("Diaries " + riskyDiaryTiers.size(), PureHelperUiConstants.ACCENT_PRIMARY_DIM));

		addPreviewSeparator(questSettingsPreviewContainer, questTabPreviewConstraints);

		JLabel previewQuestCaution = new JLabel("Quest choice caution: " + cautionRules.size());
		previewQuestCaution.setForeground(PureHelperUiConstants.CAUTION_TEXT);
		previewQuestCaution.setFont(FontManager.getRunescapeSmallFont());
		questSettingsPreviewContainer.add(previewQuestCaution, questTabPreviewConstraints);
		questTabPreviewConstraints.gridy++;

		JLabel previewDiaryRisky = new JLabel("Diary not doable tiers: " + diaryRiskyTierLabels.size());
		previewDiaryRisky.setForeground(PureHelperUiConstants.BLOCKED_TEXT);
		previewDiaryRisky.setFont(FontManager.getRunescapeSmallFont());
		questSettingsPreviewContainer.add(previewDiaryRisky, questTabPreviewConstraints);
		questTabPreviewConstraints.gridy++;

		JLabel previewDiaryRiskyDiaries = new JLabel("Not doable diaries: " + riskyDiaryTiers.size());
		previewDiaryRiskyDiaries.setForeground(PureHelperUiConstants.BLOCKED_TEXT);
		previewDiaryRiskyDiaries.setFont(FontManager.getRunescapeSmallFont());
		questSettingsPreviewContainer.add(previewDiaryRiskyDiaries, questTabPreviewConstraints);
		questTabPreviewConstraints.gridy++;

		addPreviewSeparator(questSettingsPreviewContainer, questTabPreviewConstraints);

		JPanel previewNotDoableDiariesHeader = buildCollapsibleHeader(
			SpriteID.QUESTS_PAGE_ICON_GREEN_ACHIEVEMENT_DIARIES,
			"Not doable achievement diaries",
			riskyDiaryTiers.size(),
			collapseNotDoableDiaryList,
			() ->
			{
				collapseNotDoableDiaryList = !collapseNotDoableDiaryList;
				stateManager.setCollapseNotDoableDiaryList(collapseNotDoableDiaryList);
				refreshQuestBrowser();
			},
			PureHelperUiConstants.BLOCKED_ACCENT,
			FontManager.getRunescapeSmallFont());
		questSettingsPreviewContainer.add(previewNotDoableDiariesHeader, questTabPreviewConstraints);
		questTabPreviewConstraints.gridy++;

		if (!collapseNotDoableDiaryList && riskyDiaryTiers.isEmpty())
		{
			JLabel previewNoDiaries = new JLabel("None");
			previewNoDiaries.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			previewNoDiaries.setFont(FontManager.getRunescapeSmallFont());
			questSettingsPreviewContainer.add(previewNoDiaries, questTabPreviewConstraints);
			questTabPreviewConstraints.gridy++;
		}
		else if (!collapseNotDoableDiaryList)
		{
			for (Map.Entry<String, List<String>> entry : riskyDiaryTiers.entrySet())
			{
				String diaryName = entry.getKey();
				List<String> tiers = new java.util.ArrayList<>(entry.getValue());
				tiers.sort((a, b) -> Integer.compare(tierOrder(a), tierOrder(b)));

				JPanel previewDiaryRow = buildAccentRow(
					SpriteID.QUESTS_PAGE_ICON_GREEN_ACHIEVEMENT_DIARIES,
					diaryName,
					PureHelperUiConstants.BLOCKED_ACCENT,
					PureHelperUiConstants.BLOCKED_TEXT,
					"This diary has at least one not-doable tier for current protected skills/caps.");
				questSettingsPreviewContainer.add(previewDiaryRow, questTabPreviewConstraints);
				questTabPreviewConstraints.gridy++;

				for (String tierName : tiers)
				{
					JPanel previewTierRow = buildAccentRow(
						SpriteID.RS2_TAB_STATS,
						formatTierLabel(tierName),
						PureHelperUiConstants.BLOCKED_ACCENT,
						PureHelperUiConstants.BLOCKED_TEXT,
						"Not doable tier.");
					questSettingsPreviewContainer.add(previewTierRow, questTabPreviewConstraints);
					questTabPreviewConstraints.gridy++;
				}
			}
		}

		questSettingsPreviewContainer.add(Box.createRigidArea(new Dimension(0, 4)), questTabPreviewConstraints);
		questTabPreviewConstraints.gridy++;

		JLabel summary = new JLabel("Quest blocked: " + blockedRules.size() + " / " + rules.size());
		summary.setForeground(Color.WHITE);
		summary.setFont(FontManager.getRunescapeBoldFont());
		questBrowserContainer.add(summary, c);
		c.gridy++;

		JLabel cautionHeader = new JLabel("Quest choice XP caution (" + cautionRules.size() + ")");
		cautionHeader.setForeground(PureHelperUiConstants.CAUTION_TEXT);
		cautionHeader.setFont(FontManager.getRunescapeBoldFont());
		questBrowserContainer.add(cautionHeader, c);
		c.gridy++;

		if (cautionRules.isEmpty())
		{
			JLabel noneCaution = new JLabel("None");
			noneCaution.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			noneCaution.setFont(FontManager.getRunescapeSmallFont());
			questBrowserContainer.add(noneCaution, c);
			c.gridy++;
		}

		for (QuestRule rule : cautionRules)
		{
			JPanel row = buildAccentRow(
				SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
				rule.name,
				PureHelperUiConstants.CAUTION_ACCENT,
				PureHelperUiConstants.CAUTION_TEXT,
				"This quest has choice-based XP that may include protected skills.");
			questBrowserContainer.add(row, c);
			c.gridy++;
		}

		questBrowserContainer.add(Box.createRigidArea(new Dimension(0, spacing(4, 8))), c);
		c.gridy++;
		try
		{
			addDiarySections(questBrowserContainer, c, avoidedSkills, skillCaps, memo, evaluator);
		}
		catch (Exception ex)
		{
			log.warn("Failed rendering diary sections", ex);
			JLabel diaryError = new JLabel("Diary section failed to render.");
			diaryError.setForeground(PureHelperUiConstants.ERROR_TEXT);
			diaryError.setFont(FontManager.getRunescapeSmallFont());
			questBrowserContainer.add(diaryError, c);
			c.gridy++;
		}
		questBrowserContainer.add(Box.createRigidArea(new Dimension(0, spacing(6, 10))), c);
		c.gridy++;

		JPanel blockedHeader = buildCollapsibleHeader(
			SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
			"Not doable quests",
			blockedRules.size(),
			collapseNotDoableQuestList,
			() ->
			{
				collapseNotDoableQuestList = !collapseNotDoableQuestList;
				stateManager.setCollapseNotDoableQuestList(collapseNotDoableQuestList);
				refreshQuestBrowser();
			},
			PureHelperUiConstants.BLOCKED_ACCENT,
			FontManager.getRunescapeBoldFont());
		questBrowserContainer.add(blockedHeader, c);
		c.gridy++;

		if (!collapseNotDoableQuestList && blockedRules.isEmpty())
		{
			JLabel noneBlocked = new JLabel("None");
			noneBlocked.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			noneBlocked.setFont(FontManager.getRunescapeSmallFont());
			questBrowserContainer.add(noneBlocked, c);
			c.gridy++;
		}

		addPreviewSeparator(questSettingsPreviewContainer, questTabPreviewConstraints);

		JPanel previewBlockedHeader = buildCollapsibleHeader(
			SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
			"Not doable quests",
			blockedRules.size(),
			collapseNotDoableQuestList,
			() ->
			{
				collapseNotDoableQuestList = !collapseNotDoableQuestList;
				stateManager.setCollapseNotDoableQuestList(collapseNotDoableQuestList);
				refreshQuestBrowser();
			},
			PureHelperUiConstants.BLOCKED_ACCENT,
			FontManager.getRunescapeSmallFont());
		questSettingsPreviewContainer.add(previewBlockedHeader, questTabPreviewConstraints);
		questTabPreviewConstraints.gridy++;

		if (!collapseNotDoableQuestList && blockedRules.isEmpty())
		{
			JLabel previewNone = new JLabel("None");
			previewNone.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			previewNone.setFont(FontManager.getRunescapeSmallFont());
			questSettingsPreviewContainer.add(previewNone, questTabPreviewConstraints);
			questTabPreviewConstraints.gridy++;
		}

		for (QuestRule rule : collapseNotDoableQuestList ? java.util.Collections.<QuestRule>emptyList() : blockedRules)
		{
			QuestEvaluation eval = evaluator.evaluateQuest(
				rule,
				avoidedSkills,
				skillCaps,
				effectiveChoicePolicy(),
				config.safeguardStrictness(),
				memo,
				new HashSet<>());
			JPanel row = buildAccentRow(
				SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
				rule.name,
				PureHelperUiConstants.BLOCKED_ACCENT,
				PureHelperUiConstants.BLOCKED_TEXT,
				eval.reason == null || eval.reason.isBlank() ? "No protected-skill XP risk found." : eval.reason);
			questBrowserContainer.add(row, c);
			c.gridy++;

			JPanel previewRow = buildAccentRow(
				SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
				rule.name,
				PureHelperUiConstants.BLOCKED_ACCENT,
				PureHelperUiConstants.BLOCKED_TEXT,
				eval.reason == null || eval.reason.isBlank() ? "No protected-skill XP risk found." : eval.reason);
			questSettingsPreviewContainer.add(previewRow, questTabPreviewConstraints);
			questTabPreviewConstraints.gridy++;
		}

		questBrowserContainer.add(Box.createRigidArea(new Dimension(0, spacing(4, 8))), c);
		c.gridy++;

		JLabel allowedHeader = new JLabel("Allowed quests");
		allowedHeader.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		allowedHeader.setFont(FontManager.getRunescapeBoldFont());
		questBrowserContainer.add(allowedHeader, c);
		c.gridy++;

		for (QuestRule rule : allowedRules)
		{
			JPanel row = buildAccentRow(
				SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS,
				rule.name,
				PureHelperUiConstants.SAFE_ACCENT,
				PureHelperUiConstants.SAFE_TEXT,
				"No protected-skill XP risk found.");
			questBrowserContainer.add(row, c);
			c.gridy++;
		}

		questBrowserContainer.revalidate();
		questBrowserContainer.repaint();
		questSettingsPreviewContainer.revalidate();
		questSettingsPreviewContainer.repaint();
	}

	private void addDiarySections(
		JPanel container,
		GridBagConstraints c,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps,
		Map<String, QuestEvaluation> questMemo,
		QuestRiskEvaluator evaluator)
	{
		JLabel diarySummary = new JLabel("Achievement diaries (click a header to collapse/expand)");
		diarySummary.setForeground(Color.WHITE);
		diarySummary.setFont(FontManager.getRunescapeBoldFont());
		container.add(diarySummary, c);
		c.gridy++;

		List<DiaryRule> diaries = allDiaryRules.stream()
			.filter(Objects::nonNull)
			.filter(d -> d.name != null && !d.name.isBlank())
			.sorted((a, b) -> a.name.compareToIgnoreCase(b.name))
			.collect(Collectors.toList());

		if (diaries.isEmpty())
		{
			JLabel none = new JLabel("Diary data not available.");
			none.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			none.setFont(FontManager.getRunescapeSmallFont());
			container.add(none, c);
			c.gridy++;
			return;
		}

		Map<DiaryRule, List<DiaryTierEvaluation>> evaluationsByDiary = new LinkedHashMap<>();
		List<String> riskyDiaries = new java.util.ArrayList<>();
		Map<String, Integer> riskyTierCountByDiary = new LinkedHashMap<>();
		List<String> blockedByRequiredQuestRows = new java.util.ArrayList<>();
		int riskyTiers = 0;
		int cautionOnlyTiers = 0;
		int allowedTiers = 0;
		for (DiaryRule diary : diaries)
		{
			List<DiaryTierEvaluation> tierRows = new java.util.ArrayList<>();
			for (DiaryTier tier : diary.tiers == null ? java.util.Collections.<DiaryTier>emptyList() : diary.tiers)
			{
				if (tier == null || tier.tier == null)
				{
					continue;
				}
				boolean caution = evaluator.hasDiaryChoiceXpCaution(tier);
				boolean risky = evaluator.evaluateDiaryTierRisk(
					tier,
					avoidedSkills,
					skillCaps,
					effectiveChoicePolicy(),
					config.safeguardStrictness(),
					questMemo);
				String blockedRequiredQuest = evaluator.firstBlockedRequiredQuest(
					tier,
					avoidedSkills,
					skillCaps,
					effectiveChoicePolicy(),
					config.safeguardStrictness(),
					questMemo);
				if (blockedRequiredQuest != null)
				{
					blockedByRequiredQuestRows.add(diary.name + " " + tier.tier + " -> " + blockedRequiredQuest);
				}
				tierRows.add(new DiaryTierEvaluation(tier, risky, caution));
				if (risky)
				{
					riskyTiers++;
					riskyTierCountByDiary.merge(diary.name, 1, Integer::sum);
				}
				else if (caution)
				{
					cautionOnlyTiers++;
				}
				else
				{
					allowedTiers++;
				}
			}

			tierRows.sort((a, b) -> tierOrder(a.tier.tier) - tierOrder(b.tier.tier));
			evaluationsByDiary.put(diary, tierRows);
		}

		JLabel diaryTierSummary = new JLabel("Diary tier summary - not doable: " + riskyTiers
			+ ", choice caution: " + cautionOnlyTiers + ", allowed: " + allowedTiers);
		diaryTierSummary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		diaryTierSummary.setFont(FontManager.getRunescapeSmallFont());
		container.add(diaryTierSummary, c);
		c.gridy++;

		riskyDiaries.addAll(riskyTierCountByDiary.keySet());
		JPanel notDoableDiaryHeader = buildCollapsibleHeader(
			SpriteID.QUESTS_PAGE_ICON_GREEN_ACHIEVEMENT_DIARIES,
			"Not doable achievement diaries",
			riskyDiaries.size(),
			collapseNotDoableDiaryList,
			() ->
			{
				collapseNotDoableDiaryList = !collapseNotDoableDiaryList;
				refreshQuestBrowser();
			},
			PureHelperUiConstants.BLOCKED_ACCENT,
			FontManager.getRunescapeBoldFont());
		container.add(notDoableDiaryHeader, c);
		c.gridy++;

		if (!collapseNotDoableDiaryList && riskyDiaries.isEmpty())
		{
			JLabel noneNotDoable = new JLabel("None");
			noneNotDoable.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			noneNotDoable.setFont(FontManager.getRunescapeSmallFont());
			container.add(noneNotDoable, c);
			c.gridy++;
		}
		else if (!collapseNotDoableDiaryList)
		{
			for (String diaryName : riskyDiaries)
			{
				int riskyCount = riskyTierCountByDiary.getOrDefault(diaryName, 0);
				JLabel row = new JLabel("- " + diaryName + " (" + riskyCount + " risky tier" + (riskyCount == 1 ? "" : "s") + ")");
				row.setForeground(PureHelperUiConstants.BLOCKED_TEXT);
				row.setFont(FontManager.getRunescapeSmallFont());
				row.setToolTipText("At least one tier in this diary would exceed protected-skill limits.");
				container.add(row, c);
				c.gridy++;
			}
		}

		container.add(Box.createRigidArea(new Dimension(0, 6)), c);
		c.gridy++;

		JLabel blockedByQuestHeader = new JLabel("Blocked by required quests (" + blockedByRequiredQuestRows.size() + ")");
		blockedByQuestHeader.setForeground(PureHelperUiConstants.CAUTION_TEXT);
		blockedByQuestHeader.setFont(FontManager.getRunescapeBoldFont());
		container.add(blockedByQuestHeader, c);
		c.gridy++;

		if (blockedByRequiredQuestRows.isEmpty())
		{
			JLabel noneBlockedByQuest = new JLabel("None");
			noneBlockedByQuest.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			noneBlockedByQuest.setFont(FontManager.getRunescapeSmallFont());
			container.add(noneBlockedByQuest, c);
			c.gridy++;
		}
		else
		{
			for (String rowText : blockedByRequiredQuestRows)
			{
				JLabel row = new JLabel("- " + rowText);
				row.setForeground(PureHelperUiConstants.CAUTION_TEXT);
				row.setFont(FontManager.getRunescapeSmallFont());
				row.setToolTipText("Tier is blocked because at least one required quest is not doable for protected skills.");
				container.add(row, c);
				c.gridy++;
			}
		}

		container.add(Box.createRigidArea(new Dimension(0, 6)), c);
		c.gridy++;

		for (Map.Entry<DiaryRule, List<DiaryTierEvaluation>> entry : evaluationsByDiary.entrySet())
		{
			DiaryRule diary = entry.getKey();
			List<DiaryTierEvaluation> tierRows = entry.getValue();
			int riskyCount = (int) tierRows.stream().filter(row -> row.risky).count();
			int cautionCount = (int) tierRows.stream().filter(row -> row.caution).count();
			boolean collapsed = collapsedDiarySections.getOrDefault(diary.name, stateManager.isDiarySectionCollapsed(diary.name, true));

			JPanel header = buildDiaryHeaderRow(diary.name, riskyCount, cautionCount, collapsed);
			container.add(header, c);
			c.gridy++;

			if (collapsed)
			{
				continue;
			}

			for (DiaryTierEvaluation row : tierRows)
			{
				Color barColor = row.risky
					? PureHelperUiConstants.BLOCKED_ACCENT
					: row.caution ? PureHelperUiConstants.CAUTION_ACCENT : PureHelperUiConstants.SAFE_ACCENT;
				Color textColor = row.risky
					? PureHelperUiConstants.BLOCKED_TEXT
					: row.caution ? PureHelperUiConstants.CAUTION_TEXT : PureHelperUiConstants.SAFE_TEXT;

				String status = row.risky ? "Not doable" : row.caution ? "Choice caution" : "Allowed";
				String tooltip = row.risky
					? "Would exceed your protected-skill cap if claimed on a protected skill."
					: row.caution ? "This diary tier has a lamp/choice reward that can train protected skills."
					: "No protected-skill XP risk found.";
				JPanel rowPanel = buildStatusRow(
					row.tier.tier + " - " + status,
					barColor,
					textColor,
					tooltip);
				container.add(rowPanel, c);
				c.gridy++;
			}

			container.add(Box.createRigidArea(new Dimension(0, 6)), c);
			c.gridy++;
		}
	}

	private JPanel buildDiaryHeaderRow(String diaryName, int riskyCount, int cautionCount, boolean collapsed)
	{
		JPanel panel = new JPanel(new BorderLayout(4, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, PureHelperUiConstants.ACCENT_PRIMARY_DIM),
			new EmptyBorder(spacing(2, 4), 6, spacing(2, 4), 6)));
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel iconLabel = spriteLabel(SpriteID.QUESTS_PAGE_ICON_GREEN_ACHIEVEMENT_DIARIES, 14);
		panel.add(iconLabel, BorderLayout.WEST);

		String marker = collapsed ? "\u25B6" : "\u25BC";
		JLabel label = new JLabel(marker + " " + diaryName + "  (not doable: " + riskyCount + ", caution: " + cautionCount + ")");
		label.setForeground(Color.WHITE);
		label.setFont(FontManager.getRunescapeBoldFont());
		panel.add(label, BorderLayout.CENTER);
		panel.setToolTipText("Click to expand/collapse diary tiers.");
		panel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				boolean nextState = !collapsed;
				collapsedDiarySections.put(diaryName, nextState);
				stateManager.setDiarySectionCollapsed(diaryName, nextState);
				refreshQuestBrowser();
			}
		});
		return panel;
	}

	private JPanel buildStatusRow(String text, Color barColor, Color textColor, String tooltip)
	{
		Color bg = (rowIndex++ % 2 == 0) ? ROW_EVEN : ROW_ODD;
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(bg);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, barColor),
			new EmptyBorder(spacing(1, 2), 6, spacing(1, 2), 0)));

		JLabel iconLabel = spriteLabel(SpriteID.RS2_TAB_STATS, 14);
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

	private int sectionSpriteId(String title)
	{
		String lower = title == null ? "" : title.toLowerCase(Locale.ENGLISH);
		if (lower.contains("quest") || lower.contains("safeguard"))
		{
			return SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS;
		}
		if (lower.contains("combat"))
		{
			return SpriteID.RS2_TAB_COMBAT;
		}
		if (lower.contains("skill"))
		{
			return SpriteID.RS2_TAB_STATS;
		}
		if (lower.contains("log"))
		{
			return SpriteID.QUESTS_PAGE_ICON_RED_MINIGAMES;
		}
		return SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS;
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

	private JPanel buildChip(String text, Color borderColor)
	{
		JPanel chip = new JPanel(new BorderLayout());
		chip.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		chip.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(borderColor, 1, true),
			new EmptyBorder(2, 8, 2, 8)));

		JLabel label = new JLabel(text);
		label.setForeground(Color.WHITE);
		label.setFont(FontManager.getRunescapeSmallFont());
		chip.add(label, BorderLayout.CENTER);
		return chip;
	}

	private JPanel buildAccentRow(int spriteId, String text, Color barColor, Color textColor, String tooltip)
	{
		Color bg = (rowIndex++ % 2 == 0) ? ROW_EVEN : ROW_ODD;
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(bg);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, barColor),
			new EmptyBorder(compactRows ? 1 : 3, 6, compactRows ? 1 : 3, 0)));

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

	private JPanel buildCollapsibleHeader(
		int spriteId,
		String title,
		int count,
		boolean collapsed,
		Runnable onToggle,
		Color accentColor,
		java.awt.Font font)
	{
		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accentColor),
			new EmptyBorder(spacing(2, 4), 6, spacing(2, 4), 6)));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel iconLabel = spriteLabel(spriteId, 14);
		header.add(iconLabel, BorderLayout.WEST);

		String marker = collapsed ? "\u25B6" : "\u25BC";
		JLabel label = new JLabel(marker + " " + title + " (" + count + ")");
		boolean notDoableHeader = title != null && title.toLowerCase(Locale.ENGLISH).contains("not doable");
		label.setForeground(notDoableHeader ? new Color(255, 186, 186) : Color.WHITE);
		label.setFont(font == null ? FontManager.getRunescapeSmallFont() : font);
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

	private int spacing(int compact, int comfortable)
	{
		return compactRows ? compact : comfortable;
	}

	private void addPreviewSeparator(JPanel container, GridBagConstraints c)
	{
		container.add(Box.createRigidArea(new Dimension(0, 4)), c);
		c.gridy++;

		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		separator.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		separator.setBackground(ColorScheme.DARK_GRAY_COLOR);
		container.add(separator, c);
		c.gridy++;

		container.add(Box.createRigidArea(new Dimension(0, 4)), c);
		c.gridy++;
	}

	private void applyRuneLiteScrollBarUi()
	{
		JScrollPane scrollPane = getScrollPane();
		scrollPane.getVerticalScrollBar().setUI(new RuneLiteScrollBarUI());
		scrollPane.getHorizontalScrollBar().setUI(new RuneLiteScrollBarUI());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
	}

	private void syncFromConfig()
	{
		syncingUi = true;
		try
		{
			PureHelperUiConstants.applyAccent(config.accentColor());
			questToggle.setSelected(config.enableQuestSafeguards());
			questChoicePolicyToggle.setSelected(config.questChoicePolicy() == QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE);
			showLockedQuestReasonToggle.setSelected(config.showLockedQuestReason());
			combatToggle.setSelected(config.enableCombatWarnings());
			hideUnsafeStylesToggle.setSelected(config.hideUnsafeAttackStyles());
			logRiskyToggle.setSelected(config.logRiskySelections());
			buildProfileSelector.setSelectedItem(config.buildProfile());
			Set<AvoidedSkill> parsed = ConfigParsers.parseAvoidedSkillsCsv(loadedAvoidedSkillsCsv());
			Map<AvoidedSkill, Integer> caps = ConfigParsers.parseSkillCapsCsv(loadedSkillCapsCsv());
			selectAvoidedSkills(parsed);
			for (Map.Entry<AvoidedSkill, JTextField> entry : skillCapInputs.entrySet())
			{
				Integer cap = caps.get(entry.getKey());
				entry.getValue().setText(cap == null ? "" : Integer.toString(cap));
			}
			refreshQuestBrowser();
		}
		finally
		{
			syncingUi = false;
		}
	}

	private void persistAvoidedSkills()
	{
		String csv = currentAvoidedSkillsCsv();
		configManager.setConfiguration(CONFIG_GROUP, "avoidedSkillsCsv", csv);
		stateManager.setSkillState(csv, currentSkillCapsCsv());
	}

	private String currentAvoidedSkillsCsv()
	{
		return ConfigParsers.toAvoidedSkillsCsv(selectedAvoidedSkills);
	}

	private String currentSkillCapsCsv()
	{
		String csv = stateManager.getProtectedSkillCapsCsv();
		return csv.isBlank() ? config.protectedSkillCapsCsv() : csv;
	}

	private String loadedAvoidedSkillsCsv()
	{
		String csv = stateManager.getAvoidedSkillsCsv();
		return csv.isBlank() ? config.avoidedSkillsCsv() : csv;
	}

	private String loadedSkillCapsCsv()
	{
		String csv = stateManager.getProtectedSkillCapsCsv();
		return csv.isBlank() ? config.protectedSkillCapsCsv() : csv;
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

	private void refreshRiskLog()
	{
		rowIndex = 0;
		riskLogContainer.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;

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
				JPanel row = buildAccentRow(SpriteID.QUESTS_PAGE_ICON_RED_MINIGAMES, line, PureHelperUiConstants.ACCENT_PRIMARY_DIM, ColorScheme.LIGHT_GRAY_COLOR, "Recent risky selection.");
				riskLogContainer.add(row, c);
				c.gridy++;
			}
		}

		riskLogContainer.revalidate();
		riskLogContainer.repaint();
	}

	private static final class IconMaterialTab extends MaterialTab
	{
		private BufferedImage icon;

		private IconMaterialTab(MaterialTabGroup tabGroup, JPanel panel, String resourcePath, int size, String tooltip)
		{
			super("", tabGroup, panel);
			setToolTipText(tooltip);
			try (InputStream in = PureHelperPanel.class.getResourceAsStream(resourcePath))
			{
				if (in == null)
				{
					icon = createMissingIcon(size);
				}
				else
				{
					BufferedImage loaded = javax.imageio.ImageIO.read(in);
					icon = loaded == null ? createMissingIcon(size) : ImageUtil.resizeImage(loaded, size, size);
				}
			}
			catch (Exception ex)
			{
				icon = createMissingIcon(size);
			}
			SwingUtilities.invokeLater(this::repaint);
		}

		private static BufferedImage createMissingIcon(int size)
		{
			BufferedImage missing = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = missing.createGraphics();
			try
			{
				g.setColor(new Color(120, 120, 120, 200));
				g.fillRect(0, 0, size, size);
				g.setColor(new Color(220, 220, 220, 230));
				g.drawRect(0, 0, size - 1, size - 1);
				g.drawLine(0, 0, size - 1, size - 1);
				g.drawLine(size - 1, 0, 0, size - 1);
			}
			finally
			{
				g.dispose();
			}
			return missing;
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			if (icon == null)
			{
				return;
			}
			int x = (getWidth() - icon.getWidth()) / 2;
			int y = (getHeight() - icon.getHeight()) / 2;
			graphics.drawImage(icon, x, y, null);
		}
	}

	private static final class DiaryTierEvaluation
	{
		private final DiaryTier tier;
		private final boolean risky;
		private final boolean caution;

		private DiaryTierEvaluation(DiaryTier tier, boolean risky, boolean caution)
		{
			this.tier = tier;
			this.risky = risky;
			this.caution = caution;
		}
	}

	private static final class ToggleChoiceRow extends JPanel
	{
		private final SelectTile left = new SelectTile("");
		private final SelectTile right = new SelectTile("");
		private Consumer<Boolean> changeListener = b -> { };

		private ToggleChoiceRow()
		{
			setLayout(new GridLayout(1, 2, 6, 0));
			setBackground(ColorScheme.DARK_GRAY_COLOR);
			left.setClickListener(() ->
			{
				setSelected(true);
				changeListener.accept(true);
			});
			right.setClickListener(() ->
			{
				setSelected(false);
				changeListener.accept(false);
			});
			add(left);
			add(right);
		}

		private void setLabels(String leftLabel, String rightLabel)
		{
			left.setText(leftLabel);
			right.setText(rightLabel);
		}

		private void setChangeListener(Consumer<Boolean> changeListener)
		{
			this.changeListener = changeListener == null ? b -> { } : changeListener;
		}

		private void setOptionTooltips(String leftTooltip, String rightTooltip)
		{
			left.setHoverText(leftTooltip);
			right.setHoverText(rightTooltip);
		}

		private void setSelected(boolean selectedLeft)
		{
			left.setSelected(selectedLeft);
			right.setSelected(!selectedLeft);
		}
	}

	private static final class SelectTile extends JPanel
	{
		private final JLabel iconLabel = new JLabel();
		private final JLabel label = new JLabel();
		private Runnable clickListener = () -> { };
		private boolean selected;

		private SelectTile(String text)
		{
			setLayout(new BorderLayout());
			setBackground(ColorScheme.DARKER_GRAY_COLOR);
			setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
			setOpaque(true);
			JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
			content.setOpaque(false);
			iconLabel.setHorizontalAlignment(JLabel.CENTER);
			content.add(iconLabel);
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setFont(FontManager.getRunescapeSmallFont());
			label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			content.add(label);
			add(content, BorderLayout.CENTER);
			setText(text);

			MouseAdapter mouseAdapter = new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (!SelectTile.this.isEnabled())
					{
						return;
					}
					clickListener.run();
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					if (!selected)
					{
						setBorder(new LineBorder(PureHelperUiConstants.ACCENT_PRIMARY_DIM));
					}
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					if (!selected)
					{
						setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
					}
				}
			};
			addMouseListener(mouseAdapter);
			label.addMouseListener(mouseAdapter);
		}

		private void setText(String text)
		{
			label.setText(text);
		}

		private void setClickListener(Runnable clickListener)
		{
			this.clickListener = clickListener == null ? () -> { } : clickListener;
		}

		private void setIcon(ImageIcon icon)
		{
			iconLabel.setIcon(icon);
		}

		private void setHoverText(String text)
		{
			setToolTipText(text);
			label.setToolTipText(text);
		}

		private void setSelected(boolean selected)
		{
			this.selected = selected;
			setBorder(new LineBorder(selected ? PureHelperUiConstants.ACCENT_PRIMARY : ColorScheme.MEDIUM_GRAY_COLOR));
		}

		@Override
		public void setEnabled(boolean enabled)
		{
			super.setEnabled(enabled);
			label.setEnabled(enabled);
			if (!enabled)
			{
				label.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
				// Keep selected skills visibly highlighted even when editing is locked.
				setBorder(new LineBorder(selected ? PureHelperUiConstants.ACCENT_PRIMARY : ColorScheme.MEDIUM_GRAY_COLOR));
			}
			else
			{
				label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				setBorder(new LineBorder(selected ? PureHelperUiConstants.ACCENT_PRIMARY : ColorScheme.MEDIUM_GRAY_COLOR));
			}
		}
	}
}
