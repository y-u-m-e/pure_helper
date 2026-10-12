package com.yumesplugins.purehelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StyleTableTest
{
	@Test
	public void everyStyleTrainsHitpoints()
	{
		for (StyleTable.Style style : StyleTable.Style.values())
		{
			assertEquals(style.name(), style != StyleTable.Style.OTHER, style.trains().contains(CombatSkill.HITPOINTS));
		}
	}

	@Test
	public void longrangeTrainsDefence()
	{
		assertEquals(EnumSet.of(CombatSkill.RANGED, CombatSkill.DEFENCE, CombatSkill.HITPOINTS), StyleTable.Style.LONGRANGE.trains());
	}

	@Test
	public void slotFiveDefensiveIsDefensiveCasting()
	{
		StyleTable.Style[] slots = StyleTable.slots(1, Arrays.asList("Accurate", "Aggressive", "Other", "Defensive", "Casting", "Defensive"));
		assertEquals(StyleTable.Style.DEFENSIVE, slots[3]);
		assertEquals(StyleTable.Style.DEFENSIVE_CASTING, slots[5]);
		assertNull(slots[2]);
	}

	@Test
	public void unknownWeaponHasNoStyles()
	{
		assertEquals(0, StyleTable.slots(99, Collections.emptyList()).length);
	}

	@Test
	public void staffFallback()
	{
		StyleTable.Style[] slots = StyleTable.slots(22, Collections.emptyList());
		assertEquals(6, slots.length);
		assertNull(slots[2]);
		assertEquals(StyleTable.Style.DEFENSIVE_CASTING, slots[5]);
	}

	@Test
	public void activeSlotAddsDefensiveAutocast()
	{
		assertEquals(5, StyleTable.activeSlot(4, 1));
		assertEquals(1, StyleTable.activeSlot(1, 1));
	}

	@Test
	public void unknownNameIsOther()
	{
		assertEquals(StyleTable.Style.OTHER, StyleTable.fromStructName("Nonsense"));
		assertEquals(StyleTable.Style.DEFENSIVE_CASTING, StyleTable.fromStructName(" defensive casting "));
	}

	@Test
	public void slotComponents()
	{
		assertEquals(6, StyleTable.SLOT_COMPONENTS.length);
		for (int i = 0; i < 5; i++)
		{
			assertEquals(1, StyleTable.SLOT_COMPONENTS[i].length);
		}
		assertEquals(4, StyleTable.SLOT_COMPONENTS[5].length);
		assertArrayEquals(new int[]{InterfaceID.CombatInterface.AUTOCAST_BUTTONS}, StyleTable.SLOT_COMPONENTS[4]);
		assertTrue(StyleTable.SLOT_COMPONENTS[5][0] == InterfaceID.CombatInterface.AUTOCAST_DEFENSIVE);
	}
}
