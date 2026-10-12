package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class QuestGuardTest
{
	private static Reader res(String name)
	{
		return new InputStreamReader(QuestGuardTest.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8);
	}

	private static RewardRules rules()
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
	public void tintsOnlyBreachingQuests()
	{
		List<RewardRules.Breach> b = RewardRules.breaches(rules().quests(), defenceCap1(), Collections.emptySet());
		Map<String, RewardRules.Breach> tints = QuestGuard.tints(b);
		assertEquals("+500 Defence XP", tints.get("A Soul's Bane").reason());
		assertFalse(tints.containsKey("A Tail of Two Cats"));
	}

	@Test
	public void finishedQuestsExcludedFromOffLimits()
	{
		List<RewardRules.Breach> b = RewardRules.breaches(rules().quests(), defenceCap1(), Collections.singleton("A Soul's Bane"));
		assertFalse(QuestGuard.tints(b).containsKey("A Soul's Bane"));
	}

	@Test
	public void emptyBuildHasNoBreaches()
	{
		assertTrue(RewardRules.breaches(rules().quests(), Build.EMPTY, Collections.emptySet()).isEmpty());
	}
}
