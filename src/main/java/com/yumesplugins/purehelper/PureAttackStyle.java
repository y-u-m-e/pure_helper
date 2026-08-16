package com.yumesplugins.purehelper;

import net.runelite.api.Skill;

enum PureAttackStyle
{
	ACCURATE("Accurate", Skill.ATTACK),
	AGGRESSIVE("Aggressive", Skill.STRENGTH),
	DEFENSIVE("Defensive", Skill.DEFENCE),
	CONTROLLED("Controlled", Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
	RANGING("Ranging", Skill.RANGED),
	LONGRANGE("Longrange", Skill.RANGED, Skill.DEFENCE),
	CASTING("Casting", Skill.MAGIC),
	DEFENSIVE_CASTING("Defensive Casting", Skill.MAGIC, Skill.DEFENCE),
	OTHER("Other");

	private final String name;
	private final Skill[] skills;

	PureAttackStyle(String name, Skill... skills)
	{
		this.name = name;
		this.skills = skills;
	}

	String getName()
	{
		return name;
	}

	Skill[] getSkills()
	{
		return skills;
	}

	static PureAttackStyle fromStructName(String name)
	{
		if (name == null || name.isBlank())
		{
			return OTHER;
		}

		String normalized = name.trim().replace(' ', '_').toUpperCase();
		try
		{
			return PureAttackStyle.valueOf(normalized);
		}
		catch (IllegalArgumentException ex)
		{
			return OTHER;
		}
	}
}
