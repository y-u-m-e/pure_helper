package com.yumesplugins.purehelper;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FlashNotification;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup("purehelper")
public interface PureHelperConfig extends Config
{
	@ConfigSection(name = "Protected skills", description = "Highest level each skill may reach.", position = 1)
	String caps = "caps";

	@ConfigSection(name = "Guards", description = "What Pure Helper protects.", position = 2)
	String guards = "guards";

	@ConfigSection(name = "Alerts", description = "How you are told.", position = 3)
	String alerts = "alerts";

	@ConfigSection(name = "Look", description = "Appearance.", position = 4)
	String look = "look";

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_attackCap", name = "Attack cap", description = "Highest Attack level allowed.", section = caps, position = 1)
	default int attackCap()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_strengthCap", name = "Strength cap", description = "Highest Strength level allowed.", section = caps, position = 2)
	default int strengthCap()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_defenceCap", name = "Defence cap", description = "Highest Defence level allowed.", section = caps, position = 3)
	default int defenceCap()
	{
		return 1;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_hitpointsCap", name = "Hitpoints cap", description = "Highest Hitpoints level allowed.", section = caps, position = 4)
	default int hitpointsCap()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_rangedCap", name = "Ranged cap", description = "Highest Ranged level allowed.", section = caps, position = 5)
	default int rangedCap()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_magicCap", name = "Magic cap", description = "Highest Magic level allowed.", section = caps, position = 6)
	default int magicCap()
	{
		return 99;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(keyName = "v2_prayerCap", name = "Prayer cap", description = "Highest Prayer level allowed.", section = caps, position = 7)
	default int prayerCap()
	{
		return 1;
	}

	@ConfigItem(keyName = "v2_combatGuard", name = "Combat guard", description = "Hide combat styles that would train a protected skill.", section = guards, position = 1)
	default boolean combatGuard()
	{
		return true;
	}

	@ConfigItem(keyName = "v2_questGuard", name = "Quest guard", description = "Flag quests that would level a protected skill.", section = guards, position = 2)
	default boolean questGuard()
	{
		return true;
	}

	@ConfigItem(keyName = "v2_lampGuard", name = "Lamp guard", description = "Block XP lamps from raising a protected skill.", section = guards, position = 3)
	default boolean lampGuard()
	{
		return true;
	}

	@ConfigItem(keyName = "v2_dangerBanner", name = "Danger banner", description = "Show an on-screen banner when a guard fires.", section = alerts, position = 1)
	default boolean dangerBanner()
	{
		return true;
	}

	@ConfigItem(keyName = "v2_styleNotification", name = "Style notification", description = "Notify when a combat style would train a protected skill.", section = alerts, position = 2)
	default Notification styleNotification()
	{
		return Notification.ON.withFlash(FlashNotification.FLASH_TWO_SECONDS);
	}

	@ConfigItem(keyName = "v2_blockNotification", name = "Block notification", description = "Notify when an action is blocked.", section = alerts, position = 3)
	default Notification blockNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(keyName = "v2_accentColor", name = "Accent colour", description = "Used only for danger.", section = look, position = 1)
	default Color accentColor()
	{
		return new Color(0xE5484D);
	}
}
