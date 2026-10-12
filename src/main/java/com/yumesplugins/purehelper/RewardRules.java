package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/** Fixed (unavoidable) combat XP rewards of quests and diary tiers, and which of them would break a build. */
public final class RewardRules
{
	public enum Kind
	{
		QUEST, DIARY
	}

	@Value
	public static class Entry
	{
		String name;
		Kind kind;
		Map<CombatSkill, Integer> fixedXp;
	}

	@Value
	public static class Breach
	{
		Entry entry;
		CombatSkill worst;
		int xp;
		/** XP beyond the ceiling in the worst skill. */
		long overshoot;

		public String reason()
		{
			return "+" + String.format(Locale.ROOT, "%,d", xp) + " " + worst.skill().getName() + " XP";
		}
	}

	private final List<Entry> quests;
	private final List<Entry> diaryTiers;

	private RewardRules(List<Entry> quests, List<Entry> diaryTiers)
	{
		this.quests = quests;
		this.diaryTiers = diaryTiers;
	}

	public static RewardRules load(Gson gson, Reader quests, Reader diaries)
	{
		List<Entry> q = new ArrayList<>();
		for (JsonElement e : gson.fromJson(quests, JsonObject.class).getAsJsonArray("quests"))
		{
			JsonObject o = e.getAsJsonObject();
			q.add(new Entry(o.get("name").getAsString(), Kind.QUEST, fixed(o)));
		}
		List<Entry> d = new ArrayList<>();
		for (JsonElement e : gson.fromJson(diaries, JsonObject.class).getAsJsonArray("diaries"))
		{
			JsonObject diary = e.getAsJsonObject();
			for (JsonElement t : diary.getAsJsonArray("tiers"))
			{
				JsonObject tier = t.getAsJsonObject();
				String name = tier.get("tier").getAsString();
				d.add(new Entry(diary.get("name").getAsString() + " " + name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT),
					Kind.DIARY, fixed(tier)));
			}
		}
		return new RewardRules(Collections.unmodifiableList(q), Collections.unmodifiableList(d));
	}

	private static Map<CombatSkill, Integer> fixed(JsonObject o)
	{
		Map<CombatSkill, Integer> xp = new EnumMap<>(CombatSkill.class);
		JsonArray rewards = o.getAsJsonArray("fixedRewards");
		if (rewards == null)
		{
			return xp;
		}
		for (JsonElement r : rewards)
		{
			JsonObject ro = r.getAsJsonObject();
			try
			{
				xp.merge(CombatSkill.valueOf(ro.get("skill").getAsString()), ro.get("xp").getAsInt(), Integer::sum);
			}
			catch (IllegalArgumentException notCombat)
			{
				// non-combat skill: not tracked
			}
		}
		return xp;
	}

	public List<Entry> quests()
	{
		return quests;
	}

	public List<Entry> diaryTiers()
	{
		return diaryTiers;
	}

	public static List<Breach> breaches(List<Entry> entries, Build build, Set<String> excludedNames)
	{
		List<Breach> out = new ArrayList<>();
		for (Entry e : entries)
		{
			if (excludedNames.contains(e.getName()))
			{
				continue;
			}
			Breach worst = null;
			for (Map.Entry<CombatSkill, Integer> r : e.getFixedXp().entrySet())
			{
				CombatSkill s = r.getKey();
				if (build.allows(s, r.getValue()))
				{
					continue;
				}
				long over = (long) build.xp(s) + r.getValue() - build.ceiling(s);
				if (worst == null || over > worst.getOvershoot())
				{
					worst = new Breach(e, s, r.getValue(), over);
				}
			}
			if (worst != null)
			{
				out.add(worst);
			}
		}
		out.sort(Comparator.comparingLong(Breach::getOvershoot).reversed().thenComparing(b -> b.getEntry().getName()));
		return out;
	}
}
