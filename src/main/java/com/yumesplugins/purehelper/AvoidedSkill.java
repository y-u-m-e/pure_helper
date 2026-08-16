package com.yumesplugins.purehelper;

import java.util.Locale;
import net.runelite.api.Skill;

/**
 * Skill XP the player is trying not to gain (quest rewards + combat style checks will use this).
 */
public enum AvoidedSkill
{
	NONE("None"),
	ATTACK("Attack"),
	STRENGTH("Strength"),
	DEFENCE("Defence"),
	HITPOINTS("Hitpoints"),
	RANGED("Ranged"),
	MAGIC("Magic"),
	PRAYER("Prayer");

	private final String label;

	AvoidedSkill(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public Skill getRuneliteSkill()
	{
		switch (this)
		{
			case ATTACK:
				return Skill.ATTACK;
			case STRENGTH:
				return Skill.STRENGTH;
			case DEFENCE:
				return Skill.DEFENCE;
			case HITPOINTS:
				return Skill.HITPOINTS;
			case RANGED:
				return Skill.RANGED;
			case MAGIC:
				return Skill.MAGIC;
			case PRAYER:
				return Skill.PRAYER;
			default:
				return null;
		}
	}

	public static AvoidedSkill fromConfigToken(String token)
	{
		if (token == null || token.isBlank())
		{
			return null;
		}
		try
		{
			AvoidedSkill skill = AvoidedSkill.valueOf(token.trim().toUpperCase(Locale.ENGLISH));
			return skill == NONE ? null : skill;
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	public static AvoidedSkill fromRewardSkill(String rewardSkill)
	{
		return fromConfigToken(rewardSkill);
	}
}
