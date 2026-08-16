package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.ParamID;
import net.runelite.api.ScriptID;
import net.runelite.api.Skill;
import net.runelite.api.StructComposition;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.Notifier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Pure Helper",
	enabledByDefault = true,
	tags = {"panel", "pure", "clan", "quest", "combat"}
)
public class PureHelperPlugin extends Plugin
{
	private static final String CONFIG_GROUP = "purehelper";
	private static final String QUEST_RULES_PATH = "/quest-filter-rules.json";
	private static final int QUEST_TOOLTIP_MAX_CHARS = 180;
	private static final int QUEST_TOOLTIP_WRAP_AT = 46;
	private static final AvoidedSkill[] PROTECTABLE_SKILLS = {
		AvoidedSkill.ATTACK, AvoidedSkill.STRENGTH, AvoidedSkill.DEFENCE,
		AvoidedSkill.HITPOINTS, AvoidedSkill.RANGED, AvoidedSkill.MAGIC, AvoidedSkill.PRAYER
	};

	private static BufferedImage createSidebarIcon(Color accent)
	{
		Color baseAccent = accent == null ? PureHelperUiConstants.DEFAULT_ACCENT : accent;
		Color accentDim = PureHelperUiConstants.ACCENT_PRIMARY_DIM;
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Modern split-panel badge.
			g.setPaint(new GradientPaint(0, 0, new Color(14, 15, 21), 0, 15, new Color(7, 8, 12)));
			g.fillRoundRect(0, 0, 15, 15, 4, 4);

			// Diagonal accent split gives motion without noisy details.
			int[] sx = {0, 15, 15, 7, 0};
			int[] sy = {10, 4, 15, 15, 15};
			g.setColor(new Color(baseAccent.getRed(), baseAccent.getGreen(), baseAccent.getBlue(), 165));
			g.fillPolygon(sx, sy, 5);

			// Counter-shape notch.
			int[] nx = {8, 15, 15, 10};
			int[] ny = {0, 0, 4, 5};
			g.setColor(new Color(accentDim.getRed(), accentDim.getGreen(), accentDim.getBlue(), 175));
			g.fillPolygon(nx, ny, 4);

			// Crisp double border.
			g.setColor(new Color(247, 246, 255, 235));
			g.setStroke(new BasicStroke(1.2f));
			g.drawRoundRect(0, 0, 15, 15, 4, 4);
			g.setColor(new Color(28, 30, 40, 210));
			g.drawRoundRect(1, 1, 13, 13, 3, 3);

			// Strong foreground P with clean highlight.
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
			g.setColor(new Color(0, 0, 0, 210));
			g.drawString("P", 3, 13);
			g.setColor(new Color(255, 255, 255, 245));
			g.drawString("P", 2, 12);
			g.setColor(new Color(baseAccent.getRed(), baseAccent.getGreen(), baseAccent.getBlue(), 220));
			g.drawString("P", 2, 13);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PureHelperConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PureHelperDangerOverlay dangerOverlay;

	@Inject
	private PureHelperPanel pureHelperPanel;

	@Inject
	private Gson gson;

	@Inject
	private Notifier notifier;

	private NavigationButton navigationButton;
	private BufferedImage sidebarIcon;
	private PureHelperWarningInfoBox warningInfoBox;
	private PureAttackStyle currentAttackStyle = PureAttackStyle.OTHER;
	private int equippedWeaponTypeVarbit = -1;
	private boolean combatInitialized;
	private String lastChatWarning = "";
	private int lastChatWarningTick;
	private boolean applyingProfile;
	private Set<AvoidedSkill> cachedAvoidedSkills = Collections.emptySet();
	// weapon type, component, hidden
	private final Table<Integer, Integer, Boolean> widgetsToHide = HashBasedTable.create();
	private Map<String, QuestRule> questRulesByName = Collections.emptyMap();
	private int lastQuestSafeguardRefreshTick = -8;

	@Override
	protected void startUp() throws Exception
	{
		PureHelperUiConstants.applyAccent(config.accentColor());
		sidebarIcon = createSidebarIcon(config.accentColor());
		migrateLegacySkillSelection();
		loadQuestRules();
		updatePanelVisibility();
		overlayManager.add(dangerOverlay);

		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				updateCombatStateAndUi(true);
			}
		});
		log.debug("Pure Helper started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		removeWarningInfoBox();
		dangerOverlay.setActive(false);
		overlayManager.remove(dangerOverlay);
		clientThread.invoke(() ->
		{
			updateWidgetsToHide(false, Collections.emptySet());
			processWidgets();
		});
		removeNavigationButton();
		combatInitialized = false;
		equippedWeaponTypeVarbit = -1;
		currentAttackStyle = PureAttackStyle.OTHER;
		cachedAvoidedSkills = Collections.emptySet();
		lastChatWarning = "";
		lastChatWarningTick = 0;
		lastQuestSafeguardRefreshTick = -8;
		log.debug("Pure Helper stopped");
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() == VarPlayerID.COM_MODE
			|| event.getVarbitId() == VarbitID.COMBAT_WEAPON_CATEGORY
			|| event.getVarbitId() == VarbitID.AUTOCAST_DEFMODE)
		{
			updateCombatStateAndUi(false);
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.COMBAT_INTERFACE_SETUP)
		{
			processWidgets();
		}
		else if (event.getScriptId() == ScriptID.QUESTLIST_INIT)
		{
			clientThread.invokeLater(() -> refreshQuestSafeguardsIfVisible(true));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("accentColor".equals(event.getKey()))
		{
			PureHelperUiConstants.applyAccent(config.accentColor());
			sidebarIcon = createSidebarIcon(config.accentColor());
			removeNavigationButton();
			updatePanelVisibility();
			removeWarningInfoBox();
		}

		if ("buildProfile".equals(event.getKey()))
		{
			applyBuildProfile(config.buildProfile());
		}

		if (!applyingProfile && isSkillSelectionKey(event.getKey()) && config.buildProfile() != BuildProfile.CUSTOM)
		{
			configManager.setConfiguration(CONFIG_GROUP, "buildProfile", BuildProfile.CUSTOM);
		}

		if (applyingProfile)
		{
			return;
		}

		clientThread.invoke(() ->
		{
			updateCombatStateAndUi(true);
			updatePanelVisibility();
			refreshQuestSafeguardsIfVisible(true);
		});
		pureHelperPanel.refreshFromConfig();
	}

	private void applyBuildProfile(BuildProfile profile)
	{
		if (profile == BuildProfile.CUSTOM)
		{
			return;
		}

		applyingProfile = true;
		try
		{
			Set<AvoidedSkill> skills = profile.avoidedSkills();
			Map<AvoidedSkill, Integer> caps = profile.skillCaps();
			writeSkillSelection(skills, caps);
			log.debug("Applied build profile: {} -> skills={}, caps={}", profile, skills, caps);
		}
		finally
		{
			applyingProfile = false;
		}

		clientThread.invoke(() ->
		{
			updateCombatStateAndUi(true);
			updatePanelVisibility();
			refreshQuestSafeguardsIfVisible(true);
		});
		pureHelperPanel.refreshFromConfig();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (warningInfoBox != null)
		{
			warningInfoBox.tickFlash();
		}
		dangerOverlay.tickFlash();
		refreshQuestSafeguardsIfVisible(false);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.QUESTLIST)
		{
			refreshQuestSafeguardsIfVisible(true);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.WORN)
		{
			return;
		}
		if (!config.equipmentWarnings())
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			Set<AvoidedSkill> avoidedSkills = getAvoidedSkills();
			if (avoidedSkills.isEmpty())
			{
				return;
			}

			int weaponType = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
			PureAttackStyle[] styles = getWeaponTypeStyles(weaponType);
			if (styles.length == 0)
			{
				return;
			}

			boolean allRisky = true;
			for (PureAttackStyle style : styles)
			{
				if (style != null && !isRiskyStyle(style, avoidedSkills))
				{
					allRisky = false;
					break;
				}
			}

			if (allRisky)
			{
				sendChatWarning(RiskSeverity.DANGEROUS,
					"All attack styles on this weapon train protected skills (" + joinAvoidedSkills(avoidedSkills) + ").");
			}
		});
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.hideAttackOnRiskyStyle())
		{
			return;
		}

		if (!isRiskyStyle(currentAttackStyle, cachedAvoidedSkills))
		{
			return;
		}

		MenuEntry triggerEntry = event.getMenuEntry();
		if (isPlayerAttackEntry(triggerEntry))
		{
			return;
		}

		String option = Text.removeTags(triggerEntry.getOption());
		if (!"Attack".equals(option))
		{
			return;
		}

		MenuEntry[] entries = client.getMenu().getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}

		int removeIndex = -1;
		for (int i = entries.length - 1; i >= 0; i--)
		{
			if ("Attack".equals(Text.removeTags(entries[i].getOption())) && !isPlayerAttackEntry(entries[i]))
			{
				removeIndex = i;
				break;
			}
		}

		if (removeIndex < 0)
		{
			return;
		}

		MenuEntry[] filtered = new MenuEntry[entries.length - 1];
		System.arraycopy(entries, 0, filtered, 0, removeIndex);
		System.arraycopy(entries, removeIndex + 1, filtered, removeIndex, entries.length - removeIndex - 1);
		client.getMenu().setMenuEntries(filtered);
	}

	private static boolean isPlayerAttackEntry(MenuEntry entry)
	{
		if (entry == null || entry.getType() == null)
		{
			return false;
		}
		MenuAction action = entry.getType();
		return action.name().startsWith("PLAYER_");
	}

	private void sendChatWarning(RiskSeverity severity, String message)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		int tick = client.getTickCount();
		if (message.equals(lastChatWarning) && tick - lastChatWarningTick < 10)
		{
			return;
		}
		lastChatWarning = message;
		lastChatWarningTick = tick;

		if (config.chatboxWarnings() && severity.isAtLeast(config.chatboxWarningThreshold()))
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"<col=" + PureHelperUiConstants.CHAT_PREFIX_HEX + ">[Pure Helper]</col> " + severity.getLabel() + ": " + message, null);
		}

		if (config.toastWarnings() && severity.isAtLeast(RiskSeverity.DANGEROUS))
		{
			notifier.notify("Pure Helper: " + severity.getLabel() + " - " + message);
		}
	}

	private void updatePanelVisibility()
	{
		if (config.showSidebarPanel())
		{
			ensureNavigationButton();
			return;
		}
		removeNavigationButton();
	}

	private void ensureNavigationButton()
	{
		if (navigationButton != null)
		{
			return;
		}

		navigationButton = NavigationButton.builder()
			.tooltip("Pure Helper")
			.icon(sidebarIcon == null ? createSidebarIcon(config.accentColor()) : sidebarIcon)
			.priority(7)
			.panel(pureHelperPanel)
			.build();
		clientToolbar.addNavigation(navigationButton);
	}

	private void removeNavigationButton()
	{
		if (navigationButton == null)
		{
			return;
		}
		clientToolbar.removeNavigation(navigationButton);
		navigationButton = null;
	}

	private void updateCombatStateAndUi(boolean processWidgetsNow)
	{
		Set<AvoidedSkill> avoidedSkills = getAvoidedSkills();
		cachedAvoidedSkills = avoidedSkills;
		int attackStyleIndex = client.getVarpValue(VarPlayerID.COM_MODE);
		int weaponType = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
		int castingMode = client.getVarbitValue(VarbitID.AUTOCAST_DEFMODE);
		boolean weaponSwitch = weaponType != equippedWeaponTypeVarbit;
		equippedWeaponTypeVarbit = weaponType;

		PureAttackStyle previous = currentAttackStyle;
		currentAttackStyle = getCurrentAttackStyle(weaponType, attackStyleIndex, castingMode);
		boolean risky = isRiskyStyle(currentAttackStyle, avoidedSkills);

		if (combatInitialized && previous != currentAttackStyle && risky)
		{
			if (config.logRiskySelections())
			{
				pureHelperPanel.addRiskLogEntry(buildRiskLogLine(currentAttackStyle, avoidedSkills));
			}
			String skills = Arrays.stream(currentAttackStyle.getSkills())
				.map(Skill::getName)
				.collect(Collectors.joining(", "));
			sendChatWarning(RiskSeverity.DANGEROUS,
				currentAttackStyle.getName() + " style trains " + skills + ".");
		}
		combatInitialized = true;

		updateWarningInfoBox(risky, avoidedSkills);
		dangerOverlay.setActive(risky);
		updateWidgetsToHide(config.hideUnsafeAttackStyles(), avoidedSkills);
		if (processWidgetsNow || weaponSwitch)
		{
			processWidgets();
		}
	}

	private void updateWarningInfoBox(boolean risky, Set<AvoidedSkill> avoidedSkills)
	{
		if (!config.enableCombatWarnings() || !risky || avoidedSkills.isEmpty())
		{
			removeWarningInfoBox();
			return;
		}

		if (warningInfoBox == null)
		{
			warningInfoBox = new PureHelperWarningInfoBox(this, config.accentColor());
			infoBoxManager.addInfoBox(warningInfoBox);
		}

		warningInfoBox.setText("XP");
		warningInfoBox.setTooltip("Unsafe style: " + currentAttackStyle.getName()
			+ " trains avoided " + joinAvoidedSkills(avoidedSkills) + " XP");
	}

	private void removeWarningInfoBox()
	{
		if (warningInfoBox != null)
		{
			infoBoxManager.removeInfoBox(warningInfoBox);
			warningInfoBox = null;
		}
	}

	private String buildRiskLogLine(PureAttackStyle style, Set<AvoidedSkill> avoidedSkills)
	{
		String skills = Arrays.stream(style.getSkills())
			.map(Skill::getName)
			.collect(Collectors.joining(", "));
		return "Selected " + style.getName() + " (trains: " + skills + ") while avoiding " + joinAvoidedSkills(avoidedSkills);
	}

	private PureAttackStyle getCurrentAttackStyle(int equippedWeaponType, int attackStyleIndex, int castingMode)
	{
		PureAttackStyle[] styles = getWeaponTypeStyles(equippedWeaponType);
		if (attackStyleIndex >= styles.length)
		{
			return PureAttackStyle.OTHER;
		}

		if (attackStyleIndex == 4)
		{
			attackStyleIndex += castingMode;
		}

		if (attackStyleIndex >= styles.length)
		{
			return PureAttackStyle.OTHER;
		}

		PureAttackStyle style = styles[attackStyleIndex];
		return style == null ? PureAttackStyle.OTHER : style;
	}

	private PureAttackStyle[] getWeaponTypeStyles(int weaponType)
	{
		int weaponStyleEnum = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(weaponType);
		if (weaponStyleEnum == -1)
		{
			if (weaponType == 22)
			{
				return new PureAttackStyle[]{
					PureAttackStyle.ACCURATE, PureAttackStyle.AGGRESSIVE, null,
					PureAttackStyle.DEFENSIVE, PureAttackStyle.CASTING, PureAttackStyle.DEFENSIVE_CASTING
				};
			}
			if (weaponType == 30)
			{
				return new PureAttackStyle[]{
					PureAttackStyle.ACCURATE, PureAttackStyle.AGGRESSIVE, PureAttackStyle.AGGRESSIVE, PureAttackStyle.DEFENSIVE
				};
			}
			return new PureAttackStyle[0];
		}

		int[] weaponStyleStructs = client.getEnum(weaponStyleEnum).getIntVals();
		PureAttackStyle[] styles = new PureAttackStyle[weaponStyleStructs.length];
		int i = 0;
		for (int styleId : weaponStyleStructs)
		{
			StructComposition styleStruct = client.getStructComposition(styleId);
			String styleName = styleStruct.getStringValue(ParamID.ATTACK_STYLE_NAME);

			PureAttackStyle style = PureAttackStyle.fromStructName(styleName);
			if (style == PureAttackStyle.OTHER)
			{
				++i;
				continue;
			}

			if (i == 5 && style == PureAttackStyle.DEFENSIVE)
			{
				style = PureAttackStyle.DEFENSIVE_CASTING;
			}

			styles[i++] = style;
		}
		return styles;
	}

	private boolean isRiskyStyle(PureAttackStyle style, Set<AvoidedSkill> avoidedSkills)
	{
		if (style == null || avoidedSkills.isEmpty())
		{
			return false;
		}

		for (AvoidedSkill avoidedSkill : avoidedSkills)
		{
			if (avoidedSkill == AvoidedSkill.NONE || avoidedSkill == AvoidedSkill.PRAYER)
			{
				continue;
			}

			Skill target = toSkill(avoidedSkill);
			if (target != null)
			{
				for (Skill skill : style.getSkills())
				{
					if (skill == target)
					{
						return true;
					}
				}
				continue;
			}

			// Hitpoints is trained by most combat styles.
			if (avoidedSkill != AvoidedSkill.HITPOINTS)
			{
				continue;
			}

			for (Skill skill : style.getSkills())
			{
				if (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE
					|| skill == Skill.RANGED || skill == Skill.MAGIC)
				{
					return true;
				}
			}
		}

		return false;
	}

	private Set<AvoidedSkill> getAvoidedSkills()
	{
		return ConfigParsers.protectedSkills(config);
	}

	private Map<AvoidedSkill, Integer> getSkillCaps()
	{
		return ConfigParsers.skillCaps(config);
	}

	private void writeSkillSelection(Set<AvoidedSkill> skills, Map<AvoidedSkill, Integer> caps)
	{
		for (AvoidedSkill skill : PROTECTABLE_SKILLS)
		{
			boolean protect = skills.contains(skill);
			configManager.setConfiguration(CONFIG_GROUP, protectKey(skill), protect);
			Integer cap = caps == null ? null : caps.get(skill);
			configManager.setConfiguration(CONFIG_GROUP, capKey(skill), protect && cap != null ? cap : 0);
		}
	}

	private void migrateLegacySkillSelection()
	{
		if (config.skillsMigratedToNative())
		{
			return;
		}

		Set<AvoidedSkill> legacySkills = ConfigParsers.parseAvoidedSkillsCsv(config.avoidedSkillsCsv());
		Map<AvoidedSkill, Integer> legacyCaps = ConfigParsers.parseSkillCapsCsv(config.protectedSkillCapsCsv());
		if (!legacySkills.isEmpty())
		{
			applyingProfile = true;
			try
			{
				writeSkillSelection(legacySkills, legacyCaps);
			}
			finally
			{
				applyingProfile = false;
			}
		}
		configManager.setConfiguration(CONFIG_GROUP, "skillsMigratedToNative", true);
	}

	private static boolean isSkillSelectionKey(String key)
	{
		if (key == null)
		{
			return false;
		}
		return key.startsWith("protect") || key.endsWith("Cap");
	}

	private static String protectKey(AvoidedSkill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return "protectAttack";
			case STRENGTH:
				return "protectStrength";
			case DEFENCE:
				return "protectDefence";
			case HITPOINTS:
				return "protectHitpoints";
			case RANGED:
				return "protectRanged";
			case MAGIC:
				return "protectMagic";
			case PRAYER:
				return "protectPrayer";
			default:
				return null;
		}
	}

	private static String capKey(AvoidedSkill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return "attackCap";
			case STRENGTH:
				return "strengthCap";
			case DEFENCE:
				return "defenceCap";
			case HITPOINTS:
				return "hitpointsCap";
			case RANGED:
				return "rangedCap";
			case MAGIC:
				return "magicCap";
			case PRAYER:
				return "prayerCap";
			default:
				return null;
		}
	}

	private String joinAvoidedSkills(Set<AvoidedSkill> avoidedSkills)
	{
		return avoidedSkills.stream()
			.map(AvoidedSkill::getLabel)
			.sorted()
			.collect(Collectors.joining(", "));
	}

	private void loadQuestRules()
	{
		questRulesByName = RuleLoader.loadQuestRulesByName(gson, PureHelperPlugin.class, QUEST_RULES_PATH);
		List<String> integrityIssues = RuleLoader.validateQuestRuleGraph(questRulesByName);
		if (!integrityIssues.isEmpty())
		{
			log.warn("Quest rule integrity issues detected ({}): {}", integrityIssues.size(), integrityIssues);
		}
		log.debug("Loaded {} quest filter rules", questRulesByName.size());
	}

	private void applyQuestListSafeguards()
	{
		Widget list = client.getWidget(InterfaceID.Questlist.LIST);
		if (list == null)
		{
			return;
		}

		Set<AvoidedSkill> avoidedSkills = getAvoidedSkills();
		Map<AvoidedSkill, Integer> skillCaps = getSkillCaps();
		boolean enabled = config.enableQuestSafeguards() && !avoidedSkills.isEmpty();
		Map<String, QuestEvaluation> evaluationMemo = new HashMap<>();
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(client, questRulesByName);

		List<Widget> textWidgets = new ArrayList<>();
		collectTextWidgets(list, textWidgets);
		for (Widget textWidget : textWidgets)
		{
			applyQuestTextState(textWidget, enabled, avoidedSkills, skillCaps, evaluationMemo, evaluator);
		}
	}

	private void refreshQuestSafeguardsIfVisible(boolean force)
	{
		if (!config.enableQuestSafeguards())
		{
			return;
		}

		Widget list = client.getWidget(InterfaceID.Questlist.LIST);
		if (list == null || list.isHidden())
		{
			return;
		}

		int tick = client.getTickCount();
		int refreshIntervalTicks = Math.max(1, config.questSafeguardRefreshTicks());
		if (!force && tick - lastQuestSafeguardRefreshTick < refreshIntervalTicks)
		{
			return;
		}

		applyQuestListSafeguards();
		lastQuestSafeguardRefreshTick = tick;
	}

	private void applyQuestTextState(
		Widget widget,
		boolean enabled,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps,
		Map<String, QuestEvaluation> evaluationMemo,
		QuestRiskEvaluator evaluator)
	{
		String questName = extractQuestName(widget.getText());
		if (questName.isBlank())
		{
			return;
		}

		QuestRule rule = questRulesByName.get(NameNormalizer.normalize(questName));
		if (rule == null)
		{
			return;
		}

		QuestEvaluation eval = enabled
			? evaluator.evaluateQuest(
				rule,
				avoidedSkills,
				skillCaps,
				effectiveChoicePolicy(),
				config.safeguardStrictness(),
				evaluationMemo,
				new HashSet<>())
			: QuestEvaluation.safe();
		boolean shouldHide = enabled && (eval.risky || eval.locked) && !config.showLockedQuestReason();

		widget.setHidden(shouldHide);
		if (shouldHide)
		{
			return;
		}

		if (!enabled || (!eval.risky && !eval.locked))
		{
			widget.setText(questName);
			widget.setName("");
			return;
		}

		// Keep blocked quest text muted gray for readability; strike-through indicates blocked state.
		widget.setText("<col=b8b8b8><str>" + questName + "</str></col>");
		if (config.showLockedQuestReason() && eval.reason != null && !eval.reason.isBlank())
		{
			widget.setName(formatQuestTooltip(eval.reason));
			widget.setHasListener(true);
			widget.setAction(0, "Status");
		}
		else
		{
			widget.setName("");
		}
	}

	private static void collectTextWidgets(Widget root, List<Widget> out)
	{
		if (root == null)
		{
			return;
		}

		if (root.getType() == WidgetType.TEXT)
		{
			out.add(root);
		}

		collectChildren(root.getDynamicChildren(), out);
		collectChildren(root.getStaticChildren(), out);
		collectChildren(root.getNestedChildren(), out);
		collectChildren(root.getChildren(), out);
	}

	private static void collectChildren(Widget[] children, List<Widget> out)
	{
		if (children == null)
		{
			return;
		}

		for (Widget child : children)
		{
			collectTextWidgets(child, out);
		}
	}

	private static String extractQuestName(String text)
	{
		if (text == null || text.isBlank())
		{
			return "";
		}

		return text.replaceAll("<[^>]+>", "").trim();
	}

	private String formatQuestTooltip(String reason)
	{
		String compact = reason.replaceAll("\\s+", " ").trim();
		if (compact.isEmpty())
		{
			return "";
		}

		if (compact.length() > QUEST_TOOLTIP_MAX_CHARS)
		{
			compact = compact.substring(0, QUEST_TOOLTIP_MAX_CHARS - 3).trim() + "...";
		}

		String[] words = compact.split(" ");
		StringBuilder wrapped = new StringBuilder(compact.length() + 32);
		int lineLen = 0;
		for (String word : words)
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (lineLen > 0 && lineLen + 1 + word.length() > QUEST_TOOLTIP_WRAP_AT)
			{
				wrapped.append("<br>");
				lineLen = 0;
			}
			else if (lineLen > 0)
			{
				wrapped.append(' ');
				lineLen++;
			}

			wrapped.append(word);
			lineLen += word.length();
		}

		return "Why blocked:<br>" + wrapped;
	}

	private Skill toSkill(AvoidedSkill avoidedSkill)
	{
		switch (avoidedSkill)
		{
			case ATTACK:
				return Skill.ATTACK;
			case STRENGTH:
				return Skill.STRENGTH;
			case DEFENCE:
				return Skill.DEFENCE;
			case RANGED:
				return Skill.RANGED;
			case MAGIC:
				return Skill.MAGIC;
			default:
				return null;
		}
	}

	private void updateWidgetsToHide(boolean enabled, Set<AvoidedSkill> avoidedSkills)
	{
		PureAttackStyle[] styles = getWeaponTypeStyles(equippedWeaponTypeVarbit);
		for (int i = 0; i < styles.length; i++)
		{
			PureAttackStyle style = styles[i];
			if (style == null)
			{
				continue;
			}

			boolean hide = enabled && isRiskyStyle(style, avoidedSkills);
			switch (i)
			{
				case 0:
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface._0, hide);
					break;
				case 1:
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface._1, hide);
					break;
				case 2:
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface._2, hide);
					break;
				case 3:
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface._3, hide);
					break;
				case 4:
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface.AUTOCAST_BUTTONS, hide);
					break;
				case 5:
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface.AUTOCAST_DEFENSIVE, hide);
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface.DEFENSIVE_CONTAINER_GRAPHIC0, hide);
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface.DEFENSIVE_CONTAINER_GRAPHIC1, hide);
					widgetsToHide.put(equippedWeaponTypeVarbit, InterfaceID.CombatInterface.DEFENSIVE_CONTAINER_TEXT2, hide);
					break;
				default:
					break;
			}
		}
	}

	private void processWidgets()
	{
		for (int componentId : widgetsToHide.row(equippedWeaponTypeVarbit).keySet())
		{
			Boolean hidden = widgetsToHide.get(equippedWeaponTypeVarbit, componentId);
			hideWidget(client.getWidget(componentId), hidden != null && hidden);
		}
	}

	private static void hideWidget(Widget widget, boolean hidden)
	{
		if (widget != null)
		{
			widget.setHidden(hidden);
		}
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

	@Provides
	PureHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PureHelperConfig.class);
	}

}
