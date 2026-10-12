package com.yumesplugins.purehelper;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.gameval.InterfaceID.CombatInterface;

/** Pure lookup from weapon category to the attack style behind each combat tab slot. */
public final class StyleTable
{
	public enum Style
	{
		ACCURATE(CombatSkill.ATTACK),
		AGGRESSIVE(CombatSkill.STRENGTH),
		DEFENSIVE(CombatSkill.DEFENCE),
		CONTROLLED(CombatSkill.ATTACK, CombatSkill.STRENGTH, CombatSkill.DEFENCE),
		RANGING(CombatSkill.RANGED),
		LONGRANGE(CombatSkill.RANGED, CombatSkill.DEFENCE),
		CASTING(CombatSkill.MAGIC),
		DEFENSIVE_CASTING(CombatSkill.MAGIC, CombatSkill.DEFENCE),
		OTHER();

		private final Set<CombatSkill> trains;

		Style(CombatSkill... skills)
		{
			Set<CombatSkill> set = EnumSet.noneOf(CombatSkill.class);
			if (skills.length > 0)
			{
				set.add(CombatSkill.HITPOINTS);
				Collections.addAll(set, skills);
			}
			trains = Collections.unmodifiableSet(set);
		}

		public Set<CombatSkill> trains()
		{
			return trains;
		}
	}

	/** Combat tab slot to the widget components that make up that button. */
	public static final int[][] SLOT_COMPONENTS = {
		{CombatInterface._0},
		{CombatInterface._1},
		{CombatInterface._2},
		{CombatInterface._3},
		{CombatInterface.AUTOCAST_BUTTONS},
		{
			CombatInterface.AUTOCAST_DEFENSIVE,
			CombatInterface.DEFENSIVE_CONTAINER_GRAPHIC0,
			CombatInterface.DEFENSIVE_CONTAINER_GRAPHIC1,
			CombatInterface.DEFENSIVE_CONTAINER_TEXT2
		}
	};

	private StyleTable()
	{
	}

	public static Style fromStructName(String name)
	{
		try
		{
			return Style.valueOf(name.trim().replace(' ', '_').toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException | NullPointerException e)
		{
			return Style.OTHER;
		}
	}

	/** Index is the combat tab slot; null where the slot has no trainable style. */
	public static Style[] slots(int weaponCategory, List<String> structNames)
	{
		if (structNames.isEmpty())
		{
			if (weaponCategory == 22)
			{
				return new Style[]{Style.ACCURATE, Style.AGGRESSIVE, null, Style.DEFENSIVE, Style.CASTING, Style.DEFENSIVE_CASTING};
			}
			if (weaponCategory == 30)
			{
				return new Style[]{Style.ACCURATE, Style.AGGRESSIVE, Style.AGGRESSIVE, Style.DEFENSIVE};
			}
			return new Style[0];
		}

		Style[] slots = new Style[structNames.size()];
		for (int i = 0; i < slots.length; i++)
		{
			Style style = fromStructName(structNames.get(i));
			if (style == Style.OTHER)
			{
				continue;
			}
			slots[i] = i == 5 && style == Style.DEFENSIVE ? Style.DEFENSIVE_CASTING : style;
		}
		return slots;
	}

	public static int activeSlot(int comMode, int autocastDefMode)
	{
		return comMode == 4 ? 4 + autocastDefMode : comMode;
	}
}
