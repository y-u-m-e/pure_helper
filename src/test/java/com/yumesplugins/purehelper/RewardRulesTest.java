package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import org.junit.Test;

public class RewardRulesTest
{
	/** Quest names in the bundled JSON that are not in net.runelite.api.Quest, with why. */
	private static final Set<String> KNOWN_UNMATCHED = new HashSet<>(Arrays.asList(
		"Desert Treasure" // wiki title; RuneLite names it "Desert Treasure I"
	));

	private static Reader res(String name)
	{
		return new InputStreamReader(RewardRulesTest.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8);
	}

	private static RewardRules load()
	{
		return RewardRules.load(new Gson(), res("quest-filter-rules.json"), res("achievement-diary-rules.json"));
	}

	private static Build defenceCap1()
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		caps.put(CombatSkill.DEFENCE, 1);
		return Build.of(caps, new EnumMap<>(CombatSkill.class));
	}

	@Test
	public void loadsAllBundledEntries()
	{
		RewardRules rules = load();
		assertEquals(157, rules.quests().size());
		assertEquals(48, rules.diaryTiers().size());
		assertTrue(rules.diaryTiers().stream().anyMatch(e -> e.getName().equals("Ardougne Hard")));
	}

	@Test
	public void aSoulsBaneIsADefenceBreachOnA1Def()
	{
		RewardRules rules = load();
		List<RewardRules.Breach> breaches = RewardRules.breaches(rules.quests(), defenceCap1(), Collections.emptySet());
		RewardRules.Breach b = breaches.stream().filter(x -> x.getEntry().getName().equals("A Soul's Bane")).findFirst().orElse(null);
		assertNotNull(b);
		assertEquals(CombatSkill.DEFENCE, b.getWorst());
		assertEquals("+500 Defence XP", b.reason());
	}

	@Test
	public void choiceRewardsAreIgnored()
	{
		RewardRules rules = load();
		assertFalse(RewardRules.breaches(rules.quests(), defenceCap1(), Collections.emptySet())
			.stream().anyMatch(x -> x.getEntry().getName().equals("A Tail of Two Cats")));
	}

	@Test
	public void excludedNamesAreSkipped()
	{
		RewardRules rules = load();
		assertFalse(RewardRules.breaches(rules.quests(), defenceCap1(), Collections.singleton("A Soul's Bane"))
			.stream().anyMatch(x -> x.getEntry().getName().equals("A Soul's Bane")));
	}

	@Test
	public void everyQuestNameIsKnownOrAllowlisted()
	{
		Set<String> known = new HashSet<>();
		for (Quest q : Quest.values())
		{
			known.add(q.getName());
		}
		Set<String> unmatched = new HashSet<>();
		for (RewardRules.Entry e : load().quests())
		{
			if (!known.contains(e.getName()) && !KNOWN_UNMATCHED.contains(e.getName()))
			{
				unmatched.add(e.getName());
			}
		}
		assertTrue("unmatched: " + unmatched, unmatched.isEmpty());
	}
}
