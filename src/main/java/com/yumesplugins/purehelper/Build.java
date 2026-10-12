package com.yumesplugins.purehelper;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.runelite.api.Experience;

/** Immutable per-skill level caps and current XP. A cap of 99 means the skill is unprotected. */
public final class Build
{
	private static final int MAX_LEVEL = 99;

	/** Not logged in: nothing protected, nothing capped. */
	public static final Build EMPTY = of(new EnumMap<>(CombatSkill.class), new EnumMap<>(CombatSkill.class));

	private final Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
	private final Map<CombatSkill, Integer> xp = new EnumMap<>(CombatSkill.class);

	private Build(Map<CombatSkill, Integer> caps, Map<CombatSkill, Integer> xp)
	{
		for (CombatSkill s : CombatSkill.values())
		{
			this.caps.put(s, caps.getOrDefault(s, MAX_LEVEL));
			this.xp.put(s, xp.getOrDefault(s, 0));
		}
	}

	public static Build of(Map<CombatSkill, Integer> caps, Map<CombatSkill, Integer> xp)
	{
		return new Build(caps, xp);
	}

	public int cap(CombatSkill s)
	{
		return caps.get(s);
	}

	/** Highest XP the skill may hold: one short of the next level. */
	public int ceiling(CombatSkill s)
	{
		return protects(s) ? Experience.getXpForLevel(cap(s) + 1) - 1 : Integer.MAX_VALUE;
	}

	public int xp(CombatSkill s)
	{
		return xp.get(s);
	}

	public boolean protects(CombatSkill s)
	{
		return cap(s) < MAX_LEVEL;
	}

	/** The skill has reached its cap level, so any further training is unwanted. */
	public boolean atCap(CombatSkill s)
	{
		return protects(s) && level(s) >= cap(s);
	}

	public boolean allows(CombatSkill s, int xpGain)
	{
		return (long) xp.get(s) + xpGain <= ceiling(s);
	}

	/** XP until the cap level is reached. */
	public int xpLeft(CombatSkill s)
	{
		return protects(s) ? Math.max(0, Experience.getXpForLevel(cap(s)) - xp.get(s)) : Integer.MAX_VALUE;
	}

	public int level(CombatSkill s)
	{
		return Experience.getLevelForXp(xp.get(s));
	}

	/** Protected, below the cap level, and within 10% of the cap level's XP. */
	public boolean nearCap(CombatSkill s)
	{
		return protects(s) && !atCap(s) && xpLeft(s) <= Experience.getXpForLevel(cap(s)) / 10;
	}

	/** The protected, not-yet-capped skill closest to its cap level, if near it. */
	public Optional<CombatSkill> nearestCap()
	{
		return java.util.Arrays.stream(CombatSkill.values())
			.filter(this::nearCap)
			.min(Comparator.comparingInt(this::xpLeft));
	}
}
