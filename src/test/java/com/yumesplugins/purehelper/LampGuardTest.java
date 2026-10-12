package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;

public class LampGuardTest
{
	private static Build capped(CombatSkill skill, int cap)
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		Map<CombatSkill, Integer> xps = new EnumMap<>(CombatSkill.class);
		caps.put(skill, cap);
		xps.put(skill, 0);
		return Build.of(caps, xps);
	}

	@Test
	public void blocksProtectedSkill()
	{
		assertTrue(LampGuard.blocksConfirm(CombatSkill.DEFENCE, capped(CombatSkill.DEFENCE, 1), false));
	}

	@Test
	public void allowsUnprotectedSkill()
	{
		assertFalse(LampGuard.blocksConfirm(CombatSkill.ATTACK, capped(CombatSkill.DEFENCE, 1), false));
	}

	@Test
	public void shiftOverrides()
	{
		assertFalse(LampGuard.blocksConfirm(CombatSkill.DEFENCE, capped(CombatSkill.DEFENCE, 1), true));
	}

	@Test
	public void noSelectionDoesNotBlock()
	{
		assertFalse(LampGuard.blocksConfirm(null, capped(CombatSkill.DEFENCE, 1), false));
	}

	@Test
	public void emptyBuildNeverBlocks()
	{
		assertFalse(LampGuard.blocksConfirm(CombatSkill.DEFENCE, Build.EMPTY, false));
	}

	@Test
	public void componentsMapToExactlySevenCombatSkills()
	{
		assertEquals(7, LampGuard.SKILL_COMPONENTS.size());
		assertEquals(EnumSet.allOf(CombatSkill.class), EnumSet.copyOf(LampGuard.SKILL_COMPONENTS.values()));
		assertEquals(CombatSkill.PRAYER, LampGuard.SKILL_COMPONENTS.get(InterfaceID.Xpreward.PRAYER));
	}
}
