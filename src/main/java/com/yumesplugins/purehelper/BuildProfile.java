package com.yumesplugins.purehelper;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Preset account build profiles that auto-populate protected skills and level caps.
 * Covers the most common restricted PvP and skiller builds.
 */
public enum BuildProfile
{
	ONE_DEF_PURE("1 Def Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 1);
			return caps;
		}
	},

	THIRTEEN_DEF_PURE("13 Def Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 13);
			return caps;
		}
	},

	TWENTY_DEF_PURE("20 Def Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 20);
			return caps;
		}
	},

	RUNE_PURE("40 Def Rune Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 40);
			return caps;
		}
	},

	VOID_PURE("42 Def Void Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 42);
			return caps;
		}
	},

	ZERKER("45 Def Zerker")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 45);
			return caps;
		}
	},

	BARROWS_PURE("70 Def Barrows Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 70);
			return caps;
		}
	},

	PIETY_PURE("70 Def/Pray Piety Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE, AvoidedSkill.PRAYER);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 70);
			caps.put(AvoidedSkill.PRAYER, 70);
			return caps;
		}
	},

	MED_LEVEL("75 Def Med")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 75);
			return caps;
		}
	},

	OBBY_MAULER("Obby Mauler 1 Att/Def")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.ATTACK, AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 1);
			caps.put(AvoidedSkill.DEFENCE, 1);
			return caps;
		}
	},

	G_MAULER("G-Maul Pure 50 Att")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.ATTACK, AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 50);
			caps.put(AvoidedSkill.DEFENCE, 1);
			return caps;
		}
	},

	SIXTY_ATK_PURE("60 Att Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.ATTACK, AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 60);
			caps.put(AvoidedSkill.DEFENCE, 1);
			return caps;
		}
	},

	SEVENTY_FIVE_ATK_PURE("75 Att Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.ATTACK, AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 75);
			caps.put(AvoidedSkill.DEFENCE, 1);
			return caps;
		}
	},

	RANGE_TANK("Range Tank 1 Att/Str")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.ATTACK, AvoidedSkill.STRENGTH);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 1);
			caps.put(AvoidedSkill.STRENGTH, 1);
			return caps;
		}
	},

	TEN_HP_SKILLER("10 HP Skiller")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(
				AvoidedSkill.ATTACK, AvoidedSkill.STRENGTH, AvoidedSkill.DEFENCE,
				AvoidedSkill.HITPOINTS, AvoidedSkill.RANGED, AvoidedSkill.MAGIC,
				AvoidedSkill.PRAYER);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 1);
			caps.put(AvoidedSkill.STRENGTH, 1);
			caps.put(AvoidedSkill.DEFENCE, 1);
			caps.put(AvoidedSkill.HITPOINTS, 10);
			caps.put(AvoidedSkill.RANGED, 1);
			caps.put(AvoidedSkill.MAGIC, 1);
			caps.put(AvoidedSkill.PRAYER, 1);
			return caps;
		}
	},

	ONE_PRAYER_PURE("1 Def/Pray Pure")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.DEFENCE, AvoidedSkill.PRAYER);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.DEFENCE, 1);
			caps.put(AvoidedSkill.PRAYER, 1);
			return caps;
		}
	},

	SOTETSEG_PURE("Sotetseg Pure 75 Att/45 Def")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return EnumSet.of(AvoidedSkill.ATTACK, AvoidedSkill.DEFENCE);
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
			caps.put(AvoidedSkill.ATTACK, 75);
			caps.put(AvoidedSkill.DEFENCE, 45);
			return caps;
		}
	},

	CUSTOM("Custom Build")
	{
		@Override
		public Set<AvoidedSkill> avoidedSkills()
		{
			return Collections.emptySet();
		}

		@Override
		public Map<AvoidedSkill, Integer> skillCaps()
		{
			return Collections.emptyMap();
		}
	};

	private final String label;

	BuildProfile(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public abstract Set<AvoidedSkill> avoidedSkills();

	public abstract Map<AvoidedSkill, Integer> skillCaps();

	@Override
	public String toString()
	{
		return label;
	}
}
