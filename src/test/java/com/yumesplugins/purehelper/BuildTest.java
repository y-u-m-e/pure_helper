package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import org.junit.Test;

public class BuildTest
{
	private static Build build(CombatSkill skill, int cap, int xp)
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		Map<CombatSkill, Integer> xps = new EnumMap<>(CombatSkill.class);
		caps.put(skill, cap);
		xps.put(skill, xp);
		return Build.of(caps, xps);
	}

	@Test
	public void ceilingIsLastXpBeforeNextLevel()
	{
		assertEquals(82, build(CombatSkill.ATTACK, 1, 0).ceiling(CombatSkill.ATTACK));
		assertEquals(173, build(CombatSkill.ATTACK, 2, 0).ceiling(CombatSkill.ATTACK));
		assertEquals(13034430, build(CombatSkill.ATTACK, 98, 0).ceiling(CombatSkill.ATTACK));
		assertEquals(Experience.getXpForLevel(99) - 1, 13034430);
		assertEquals(Integer.MAX_VALUE, build(CombatSkill.ATTACK, 99, 0).ceiling(CombatSkill.ATTACK));
	}

	@Test
	public void allowsUpToCeilingOnly()
	{
		Build b = build(CombatSkill.DEFENCE, 1, 0);
		assertTrue(b.allows(CombatSkill.DEFENCE, 82));
		assertFalse(b.allows(CombatSkill.DEFENCE, 83));
	}

	@Test
	public void atCapWhenXpAtCeiling()
	{
		assertFalse(build(CombatSkill.ATTACK, 60, Experience.getXpForLevel(50)).atCap(CombatSkill.ATTACK));
		Build b = build(CombatSkill.ATTACK, 60, 0);
		int ceiling = b.ceiling(CombatSkill.ATTACK);
		assertTrue(build(CombatSkill.ATTACK, 60, ceiling).atCap(CombatSkill.ATTACK));
	}

	@Test
	public void emptyBuildAllowsEverything()
	{
		for (CombatSkill s : CombatSkill.values())
		{
			assertFalse(Build.EMPTY.atCap(s));
			assertTrue(Build.EMPTY.allows(s, 1_000_000));
		}
	}

	@Test
	public void nearestCapPicksSmallestXpLeft()
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		Map<CombatSkill, Integer> xps = new EnumMap<>(CombatSkill.class);
		caps.put(CombatSkill.ATTACK, 60);
		caps.put(CombatSkill.STRENGTH, 70);
		xps.put(CombatSkill.ATTACK, (int) ((Experience.getXpForLevel(61) - 1) * 0.95));
		xps.put(CombatSkill.STRENGTH, (int) ((Experience.getXpForLevel(71) - 1) * 0.99));
		assertEquals(CombatSkill.STRENGTH, Build.of(caps, xps).nearestCap().get());
	}

	@Test
	public void nearestCapIgnoresFarUnprotectedAndCapped()
	{
		assertFalse(build(CombatSkill.ATTACK, 60, 0).nearestCap().isPresent());
		assertFalse(Build.EMPTY.nearestCap().isPresent());
		Build capped = build(CombatSkill.ATTACK, 60, Experience.getXpForLevel(61) - 1);
		assertFalse(capped.nearestCap().isPresent());
	}
}
