package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class PanelStateTest
{
	private static final String LEFT_59 = String.format("%,d", net.runelite.api.Experience.getXpForLevel(60) - net.runelite.api.Experience.getXpForLevel(59)) + " XP left";
	private static final Instant NOW = Instant.parse("2026-10-11T12:00:00Z");

	/** Attack capped at 60, sitting at level 59. Strength unprotected at level 40. */
	private static Build nearCap()
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		caps.put(CombatSkill.ATTACK, 60);
		Map<CombatSkill, Integer> xp = new EnumMap<>(CombatSkill.class);
		xp.put(CombatSkill.ATTACK, net.runelite.api.Experience.getXpForLevel(59));
		xp.put(CombatSkill.STRENGTH, 37_224);
		return Build.of(caps, xp);
	}

	private static PanelState state(Build build, List<RiskLog.Entry> log, int slips)
	{
		return PanelState.of(build, log, Collections.emptyList(), slips, NOW);
	}

	private static RiskLog.Entry entry(Instant time, String what, boolean blocked, boolean overridden)
	{
		return new RiskLog.Entry(time, "Combat", what, blocked, overridden);
	}

	private static RewardRules.Breach breach(String name, RewardRules.Kind kind, long overshoot)
	{
		RewardRules.Entry e = new RewardRules.Entry(name, kind, Collections.singletonMap(CombatSkill.DEFENCE, 500));
		return new RewardRules.Breach(e, CombatSkill.DEFENCE, 500, overshoot);
	}

	@Test
	public void heroShowsIntactWhenSafe()
	{
		PanelState s = state(Build.EMPTY, Collections.emptyList(), 0);
		assertEquals("BUILD INTACT", s.getHeroTitle());
		assertEquals("0 slips this session", s.getHeroSub());
		assertFalse(s.isHeroDanger());
	}

	@Test
	public void singleSlipIsSingular()
	{
		assertEquals("1 slip this session", state(Build.EMPTY, Collections.emptyList(), 1).getHeroSub());
	}

	@Test
	public void heroShowsNearestCap()
	{
		PanelState s = state(nearCap(), Collections.emptyList(), 0);
		assertEquals("ATTACK 59/60", s.getHeroTitle());
		assertEquals(LEFT_59, s.getHeroSub());
		assertTrue(s.isHeroDanger());
	}

	@Test
	public void heroShowsLevel58OfCap59()
	{
		// Level 58 of cap 60 is 49k XP out, beyond the 10% window; cap 59 puts level 58 inside it.
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		caps.put(CombatSkill.ATTACK, 59);
		Map<CombatSkill, Integer> xp = new EnumMap<>(CombatSkill.class);
		xp.put(CombatSkill.ATTACK, net.runelite.api.Experience.getXpForLevel(58));
		PanelState s = state(Build.of(caps, xp), Collections.emptyList(), 0);
		assertEquals("ATTACK 58/59", s.getHeroTitle());
		int left = net.runelite.api.Experience.getXpForLevel(59) - net.runelite.api.Experience.getXpForLevel(58);
		assertEquals(String.format("%,d", left) + " XP left", s.getHeroSub());
	}

	@Test
	public void freshOneDefTileIsLocked()
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		caps.put(CombatSkill.DEFENCE, 1);
		PanelState.Tile t = state(Build.of(caps, new EnumMap<>(CombatSkill.class)), Collections.emptyList(), 0).getTiles().get(2);
		assertEquals(CombatSkill.DEFENCE, t.getSkill());
		assertTrue(t.isLocked());
		assertEquals(1.0, t.getProgress(), 0.0);
	}

	@Test
	public void recentEntryTakesHero()
	{
		List<RiskLog.Entry> log = Collections.singletonList(entry(NOW.minusSeconds(2), "Defensive selected", false, false));
		PanelState s = state(nearCap(), log, 1);
		assertEquals("DEFENSIVE SELECTED", s.getHeroTitle());
		assertEquals("Combat · just now", s.getHeroSub());
		assertTrue(s.isHeroDanger());

		List<RiskLog.Entry> old = Collections.singletonList(entry(NOW.minusSeconds(10), "Defensive selected", false, false));
		assertEquals("ATTACK 59/60", state(nearCap(), old, 1).getHeroTitle());
	}

	@Test
	public void emptyLogLine()
	{
		assertEquals(Collections.singletonList("Nothing caught yet."), state(Build.EMPTY, Collections.emptyList(), 0).logLines());
	}

	@Test
	public void logLinesShowTimeGuardWhatAndOverride()
	{
		Instant t = NOW.minusSeconds(600);
		LocalDateTime local = LocalDateTime.ofInstant(t, ZoneId.systemDefault());
		String hhmm = String.format("%02d:%02d", local.getHour(), local.getMinute());
		List<RiskLog.Entry> log = Arrays.asList(entry(t, "Defensive selected", false, true), entry(t, "Prayer blocked", true, false));
		assertEquals(Arrays.asList(hhmm + "  Combat  Defensive selected ⇧", hhmm + "  Combat  Prayer blocked"),
			state(Build.EMPTY, log, 1).logLines());
	}

	@Test
	public void tileTooltips()
	{
		List<PanelState.Tile> tiles = state(nearCap(), Collections.emptyList(), 0).getTiles();
		assertEquals(7, tiles.size());
		assertEquals(CombatSkill.ATTACK, tiles.get(0).getSkill());
		assertEquals("Level 59 / cap 60 — " + LEFT_59, tiles.get(0).tooltip());
		assertEquals("Level 40 — not protected", tiles.get(1).tooltip());
		assertEquals(0.0, tiles.get(1).getProgress(), 0.0);
		assertTrue(tiles.get(0).getProgress() > 0.9 && tiles.get(0).getProgress() < 1.0);
		assertTrue(tiles.get(0).isNear());
		assertFalse(tiles.get(0).isLocked());
	}

	@Test
	public void offLimitsHeaderCountsByKindAndLinesSortBySeverity()
	{
		List<RewardRules.Breach> off = Arrays.asList(
			breach("Small Quest", RewardRules.Kind.QUEST, 10),
			breach("Big Quest", RewardRules.Kind.QUEST, 900),
			breach("Ardougne Hard", RewardRules.Kind.DIARY, 400));
		PanelState s = PanelState.of(Build.EMPTY, Collections.emptyList(), off, 0, NOW);
		assertEquals("2 quests · 1 diary tier", s.getOffLimitsHeader());
		assertEquals(Arrays.asList("Big Quest — +500 Defence XP", "Ardougne Hard — +500 Defence XP", "Small Quest — +500 Defence XP"),
			s.offLimitsLines());
	}

	@Test
	public void equalInputsGiveEqualStates()
	{
		assertEquals(state(nearCap(), Collections.emptyList(), 0), state(nearCap(), Collections.emptyList(), 0));
	}
}
