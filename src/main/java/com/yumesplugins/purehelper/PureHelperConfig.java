package com.yumesplugins.purehelper;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("purehelper")
public interface PureHelperConfig extends Config
{
	// ============================================================
	// Sections
	// ============================================================

	@ConfigSection(
		name = "Protected skills",
		description = "Pick a build preset or choose individual skills to protect from XP.",
		position = 1
	)
	String protectedSkillsSection = "protectedSkillsSection";

	@ConfigSection(
		name = "Skill level caps",
		description = "Optional max level per protected skill. 0 = block all XP in that skill.",
		position = 2,
		closedByDefault = true
	)
	String skillCapsSection = "skillCapsSection";

	@ConfigSection(
		name = "Quest & diary",
		description = "Safeguards for quests and achievement diary steps.",
		position = 3
	)
	String questDiarySection = "questDiarySection";

	@ConfigSection(
		name = "Combat protection",
		description = "Warnings and guards for risky combat styles.",
		position = 4
	)
	String combatSection = "combatSection";

	@ConfigSection(
		name = "Notifications",
		description = "How and when you are warned.",
		position = 5,
		closedByDefault = true
	)
	String notifySection = "notifySection";

	@ConfigSection(
		name = "Advanced",
		description = "Tuning and panel options.",
		position = 6,
		closedByDefault = true
	)
	String advancedSection = "advancedSection";

	// ============================================================
	// Protected skills
	// ============================================================

	@ConfigItem(
		keyName = "buildProfile",
		name = "Build preset",
		description = "Apply a preset to auto-fill protected skills and level caps. Editing a skill below switches this to Custom.",
		section = protectedSkillsSection,
		position = 1
	)
	default BuildProfile buildProfile()
	{
		return BuildProfile.CUSTOM;
	}

	@ConfigItem(
		keyName = "protectAttack",
		name = "Protect Attack",
		description = "Warn/block actions that would grant Attack XP.",
		section = protectedSkillsSection,
		position = 2
	)
	default boolean protectAttack()
	{
		return false;
	}

	@ConfigItem(
		keyName = "protectStrength",
		name = "Protect Strength",
		description = "Warn/block actions that would grant Strength XP.",
		section = protectedSkillsSection,
		position = 3
	)
	default boolean protectStrength()
	{
		return false;
	}

	@ConfigItem(
		keyName = "protectDefence",
		name = "Protect Defence",
		description = "Warn/block actions that would grant Defence XP.",
		section = protectedSkillsSection,
		position = 4
	)
	default boolean protectDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "protectHitpoints",
		name = "Protect Hitpoints",
		description = "Warn/block actions that would grant Hitpoints XP.",
		section = protectedSkillsSection,
		position = 5
	)
	default boolean protectHitpoints()
	{
		return false;
	}

	@ConfigItem(
		keyName = "protectRanged",
		name = "Protect Ranged",
		description = "Warn/block actions that would grant Ranged XP.",
		section = protectedSkillsSection,
		position = 6
	)
	default boolean protectRanged()
	{
		return false;
	}

	@ConfigItem(
		keyName = "protectMagic",
		name = "Protect Magic",
		description = "Warn/block actions that would grant Magic XP.",
		section = protectedSkillsSection,
		position = 7
	)
	default boolean protectMagic()
	{
		return false;
	}

	@ConfigItem(
		keyName = "protectPrayer",
		name = "Protect Prayer",
		description = "Warn/block actions that would grant Prayer XP.",
		section = protectedSkillsSection,
		position = 8
	)
	default boolean protectPrayer()
	{
		return false;
	}

	// ============================================================
	// Skill level caps (collapsed by default)
	// ============================================================

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "attackCap",
		name = "Attack max level",
		description = "Highest Attack level allowed. 0 = block all Attack XP.",
		section = skillCapsSection,
		position = 1
	)
	default int attackCap()
	{
		return 0;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "strengthCap",
		name = "Strength max level",
		description = "Highest Strength level allowed. 0 = block all Strength XP.",
		section = skillCapsSection,
		position = 2
	)
	default int strengthCap()
	{
		return 0;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "defenceCap",
		name = "Defence max level",
		description = "Highest Defence level allowed. 0 = block all Defence XP.",
		section = skillCapsSection,
		position = 3
	)
	default int defenceCap()
	{
		return 0;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "hitpointsCap",
		name = "Hitpoints max level",
		description = "Highest Hitpoints level allowed. 0 = block all Hitpoints XP.",
		section = skillCapsSection,
		position = 4
	)
	default int hitpointsCap()
	{
		return 0;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "rangedCap",
		name = "Ranged max level",
		description = "Highest Ranged level allowed. 0 = block all Ranged XP.",
		section = skillCapsSection,
		position = 5
	)
	default int rangedCap()
	{
		return 0;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "magicCap",
		name = "Magic max level",
		description = "Highest Magic level allowed. 0 = block all Magic XP.",
		section = skillCapsSection,
		position = 6
	)
	default int magicCap()
	{
		return 0;
	}

	@Range(min = 0, max = 99)
	@ConfigItem(
		keyName = "prayerCap",
		name = "Prayer max level",
		description = "Highest Prayer level allowed. 0 = block all Prayer XP.",
		section = skillCapsSection,
		position = 7
	)
	default int prayerCap()
	{
		return 0;
	}

	// ============================================================
	// Quest & diary
	// ============================================================

	@ConfigItem(
		keyName = "enableQuestSafeguards",
		name = "Quest safeguards",
		description = "Block quests and diary steps that would train a protected skill.",
		section = questDiarySection,
		position = 1
	)
	default boolean enableQuestSafeguards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "questChoicePolicy",
		name = "Reward choices",
		description = "How optional quest reward choices are handled.",
		section = questDiarySection,
		position = 2
	)
	default QuestChoicePolicy questChoicePolicy()
	{
		return QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE;
	}

	@ConfigItem(
		keyName = "safeguardStrictness",
		name = "Strictness",
		description = "How aggressively borderline quests are flagged.",
		section = questDiarySection,
		position = 3
	)
	default SafeguardStrictness safeguardStrictness()
	{
		return SafeguardStrictness.BALANCED;
	}

	@ConfigItem(
		keyName = "showLockedQuestReason",
		name = "Show blocked reason",
		description = "Show why a quest was blocked in its tooltip. When off, blocked quests are hidden from the list entirely.",
		section = questDiarySection,
		position = 4
	)
	default boolean showLockedQuestReason()
	{
		return true;
	}

	// ============================================================
	// Combat protection
	// ============================================================

	@ConfigItem(
		keyName = "enableCombatWarnings",
		name = "Combat warnings",
		description = "Warn when a selected style trains a protected skill.",
		section = combatSection,
		position = 1
	)
	default boolean enableCombatWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideUnsafeAttackStyles",
		name = "Hide unsafe styles",
		description = "Hide attack-style buttons that would train a protected skill.",
		section = combatSection,
		position = 2
	)
	default boolean hideUnsafeAttackStyles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "equipmentWarnings",
		name = "Equipment warnings",
		description = "Warn when equipping gear where every attack style trains a protected skill.",
		section = combatSection,
		position = 3
	)
	default boolean equipmentWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideAttackOnRiskyStyle",
		name = "Hide attack on risky",
		description = "Remove the Attack menu option on targets while the active style trains a protected skill.",
		section = combatSection,
		position = 4
	)
	default boolean hideAttackOnRiskyStyle()
	{
		return false;
	}

	@ConfigItem(
		keyName = "logRiskySelections",
		name = "Log risky changes",
		description = "Record risky style changes in the Risk Log panel.",
		section = combatSection,
		position = 5
	)
	default boolean logRiskySelections()
	{
		return true;
	}

	// ============================================================
	// Notifications (collapsed by default)
	// ============================================================

	@ConfigItem(
		keyName = "chatboxWarnings",
		name = "Chatbox warnings",
		description = "Print warnings to the game chatbox.",
		section = notifySection,
		position = 1
	)
	default boolean chatboxWarnings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatboxWarningThreshold",
		name = "Chatbox severity",
		description = "Minimum severity that prints to chat.",
		section = notifySection,
		position = 2
	)
	default RiskSeverity chatboxWarningThreshold()
	{
		return RiskSeverity.WARNING;
	}

	@ConfigItem(
		keyName = "toastWarnings",
		name = "Desktop toasts",
		description = "Send OS notifications for danger events.",
		section = notifySection,
		position = 3
	)
	default boolean toastWarnings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "screenFlashOnDanger",
		name = "Screen flash on danger",
		description = "Flash a colored border around the game viewport on a danger event.",
		section = notifySection,
		position = 4
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
		position = 5
	)
	default Color screenFlashColor()
	{
		return accentColor();
	}

	@Range(
		min = 0,
		max = 100
	)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "screenFlashOpacity",
		name = "Flash opacity",
		description = "Peak opacity of the flash border. Higher = more visible.",
		section = notifySection,
		position = 6
	)
	default int screenFlashOpacity()
	{
		return 47;
	}

	@Range(
		min = 1,
		max = 20
	)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "screenFlashWidth",
		name = "Flash border width",
		description = "Thickness of the flash border.",
		section = notifySection,
		position = 7
	)
	default int screenFlashWidth()
	{
		return 4;
	}

	// ============================================================
	// Advanced (collapsed by default)
	// ============================================================

	@Range(
		min = 1,
		max = 30
	)
	@ConfigItem(
		keyName = "questSafeguardRefreshTicks",
		name = "Quest refresh interval (ticks)",
		description = "How often in-game quest list safeguards refresh while the quest interface is open.",
		section = advancedSection,
		position = 1
	)
	default int questSafeguardRefreshTicks()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "showSidebarPanel",
		name = "Show sidebar panel",
		description = "Show the Pure Helper side panel button.",
		section = advancedSection,
		position = 2
	)
	default boolean showSidebarPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "accentColor",
		name = "Panel accent",
		description = "Primary accent color used for panel highlights, warning UI, and icon accents.",
		section = advancedSection,
		position = 3
	)
	default Color accentColor()
	{
		return PureHelperUiConstants.DEFAULT_ACCENT;
	}

	// ============================================================
	// Hidden / sidebar-backed state (not shown in native config)
	// ============================================================

	@ConfigItem(
		keyName = "skillsMigratedToNative",
		name = "Skills migrated",
		description = "Internal: marks that legacy CSV skill selection has been migrated to native toggles.",
		hidden = true
	)
	default boolean skillsMigratedToNative()
	{
		return false;
	}

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
