package com.yumesplugins.purehelper;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("purehelper")
public interface PureHelperConfig extends Config
{
	// --- Sections ---

	@ConfigSection(
		name = "Build Preset",
		description = "Choose an account preset. This selector applies protected skills and level caps.",
		position = 0
	)
	String profileSection = "profileSection";

	@ConfigSection(
		name = "Quest and Diary",
		description = "Control quest and achievement diary safeguards for your build.",
		position = 1
	)
	String questSection = "questSection";

	@ConfigSection(
		name = "Combat Protection",
		description = "Warnings, overlays, and protections for dangerous attack styles and equipment.",
		position = 2
	)
	String combatSection = "combatSection";

	@ConfigSection(
		name = "Notifications",
		description = "Chatbox messages, screen flash, and notification behavior.",
		position = 3
	)
	String notifySection = "notifySection";

	@ConfigSection(
		name = "Panel",
		description = "Sidebar panel visibility and layout. Protected skill edits live in Sidebar -> Skills.",
		position = 4
	)
	String panelSection = "panelSection";

	// --- Build Profile ---

	@ConfigItem(
		keyName = "buildProfile",
		name = " ",
		description = "Select a preset to auto-populate protected skills and level caps. Custom lets you configure manually.",
		section = profileSection,
		position = 1
	)
	default BuildProfile buildProfile()
	{
		return BuildProfile.CUSTOM;
	}

	// --- Quest Filter ---

	@ConfigItem(
		keyName = "enableQuestSafeguards",
		name = "Enable quest safeguards",
		description = "Strike through or hide quests that would grant XP in protected skills (or have prerequisites that do).",
		section = questSection,
		position = 0
	)
	default boolean enableQuestSafeguards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "questChoicePolicy",
		name = "Choice reward policy",
		description = "Safe default: only block when unavoidable. Strict: also block if any optional choice reward can give protected XP.",
		section = questSection,
		position = 1
	)
	default QuestChoicePolicy questChoicePolicy()
	{
		return QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE;
	}

	@ConfigItem(
		keyName = "safeguardStrictness",
		name = "Safeguard strictness",
		description = "Lenient ignores unknown reward modeling; Strict blocks unknown or unmodeled paths.",
		section = questSection,
		position = 2
	)
	default SafeguardStrictness safeguardStrictness()
	{
		return SafeguardStrictness.BALANCED;
	}

	@ConfigItem(
		keyName = "showLockedQuestReason",
		name = "Show blocked quest reason",
		description = "Show blocked quests with a hover reason. When off, blocked quests are hidden from the list entirely.",
		section = questSection,
		position = 3
	)
	default boolean showLockedQuestReason()
	{
		return true;
	}

	@Range(
		min = 1,
		max = 30
	)
	@ConfigItem(
		keyName = "questSafeguardRefreshTicks",
		name = "Quest refresh interval (ticks)",
		description = "How often in-game quest list safeguards refresh while the quest interface is open.",
		section = questSection,
		position = 4
	)
	default int questSafeguardRefreshTicks()
	{
		return 8;
	}

	// --- Combat Protection ---

	@ConfigItem(
		keyName = "enableCombatWarnings",
		name = "Combat style warnings",
		description = "Show a flashing infobox when the active attack style would train a protected skill.",
		section = combatSection,
		position = 0
	)
	default boolean enableCombatWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideUnsafeAttackStyles",
		name = "Hide unsafe attack styles",
		description = "Hide combat style buttons that would train protected skills so they cannot be clicked.",
		section = combatSection,
		position = 1
	)
	default boolean hideUnsafeAttackStyles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "equipmentWarnings",
		name = "Equipment risk warnings",
		description = "Warn when equipping a weapon where every attack style trains a protected skill.",
		section = combatSection,
		position = 2
	)
	default boolean equipmentWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideAttackOnRiskyStyle",
		name = "Hide Attack option on risky style",
		description = "Remove the right-click 'Attack' option on NPCs and players while the active combat style trains a protected skill.",
		section = combatSection,
		position = 3
	)
	default boolean hideAttackOnRiskyStyle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "logRiskySelections",
		name = "Log risky style changes",
		description = "Record risky combat style switches in the sidebar panel Risk Log tab.",
		section = combatSection,
		position = 4
	)
	default boolean logRiskySelections()
	{
		return true;
	}

	// --- Notifications ---

	@ConfigItem(
		keyName = "chatboxWarnings",
		name = "Show chatbox warnings",
		description = "Post integrity warnings as game chat messages (e.g. risky style, dangerous equipment).",
		section = notifySection,
		position = 0
	)
	default boolean chatboxWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatboxWarningThreshold",
		name = "Chatbox severity threshold",
		description = "Only post chatbox messages for warnings at or above this severity level.",
		section = notifySection,
		position = 1
	)
	default RiskSeverity chatboxWarningThreshold()
	{
		return RiskSeverity.WARNING;
	}

	@ConfigItem(
		keyName = "toastWarnings",
		name = "Show desktop toasts",
		description = "Show RuneLite notifier popups for dangerous warnings.",
		section = notifySection,
		position = 2
	)
	default boolean toastWarnings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "screenFlashOnDanger",
		name = "Screen flash on danger",
		description = "Flash a colored border around the game viewport while a dangerous attack style is active.",
		section = notifySection,
		position = 3
	)
	default boolean screenFlashOnDanger()
	{
		return true;
	}

	@ConfigItem(
		keyName = "screenFlashColor",
		name = "Flash color",
		description = "The color of the danger screen flash border.",
		section = notifySection,
		position = 4
	)
	default Color screenFlashColor()
	{
		return accentColor();
	}

	@ConfigItem(
		keyName = "screenFlashOpacity",
		name = "Flash opacity (%)",
		description = "Peak opacity of the flash border (0-100). Higher = more visible.",
		section = notifySection,
		position = 5
	)
	default int screenFlashOpacity()
	{
		return 47;
	}

	@ConfigItem(
		keyName = "screenFlashWidth",
		name = "Flash border width",
		description = "Thickness of the flash border in pixels (1-20).",
		section = notifySection,
		position = 6
	)
	default int screenFlashWidth()
	{
		return 4;
	}

	// --- Hidden state-backed values ---

	@ConfigItem(
		keyName = "avoidedSkillsCsv",
		name = "Protected skills",
		description = "Comma-separated list of protected skills. Use the sidebar panel to edit visually.",
		hidden = true
	)
	default String avoidedSkillsCsv()
	{
		return AvoidedSkill.DEFENCE.name();
	}

	@ConfigItem(
		keyName = "protectedSkillCapsCsv",
		name = "Skill level caps",
		description = "Per-skill max level (e.g. DEFENCE:1). Blank = block all XP in that skill. Use the sidebar panel to edit.",
		hidden = true
	)
	default String protectedSkillCapsCsv()
	{
		return "";
	}

	// --- Panel ---

	@ConfigItem(
		keyName = "showSidebarPanel",
		name = "Show sidebar panel",
		description = "Show or hide the Pure Helper sidebar button.",
		section = panelSection,
		position = 0
	)
	default boolean showSidebarPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "accentColor",
		name = "Accent color",
		description = "Primary accent color used for panel highlights, warning UI, and icon accents.",
		section = panelSection,
		position = 1
	)
	default Color accentColor()
	{
		return PureHelperUiConstants.DEFAULT_ACCENT;
	}

	// --- Hidden / Legacy ---

	@ConfigItem(
		keyName = "loginNotice",
		name = "Login notice",
		description = "Legacy field; leave empty.",
		hidden = true
	)
	default String loginNotice()
	{
		return "";
	}

	@ConfigItem(
		keyName = "avoidedSkill",
		name = "Avoided skill XP",
		description = "Legacy single-skill value for migration.",
		hidden = true
	)
	default AvoidedSkill avoidedSkill()
	{
		return AvoidedSkill.DEFENCE;
	}
}
