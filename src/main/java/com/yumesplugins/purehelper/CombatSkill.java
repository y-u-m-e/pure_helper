package com.yumesplugins.purehelper;

import net.runelite.api.Skill;

/** The seven skills a build can cap. */
public enum CombatSkill
{
	ATTACK(Skill.ATTACK),
	STRENGTH(Skill.STRENGTH),
	DEFENCE(Skill.DEFENCE),
	HITPOINTS(Skill.HITPOINTS),
	RANGED(Skill.RANGED),
	MAGIC(Skill.MAGIC),
	PRAYER(Skill.PRAYER);

	private final Skill skill;

	CombatSkill(Skill skill)
	{
		this.skill = skill;
	}

	public Skill skill()
	{
		return skill;
	}
}
