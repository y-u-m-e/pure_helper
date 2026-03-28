package com.yumesplugins.purehelper;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class ConfigParsers
{
	private ConfigParsers()
	{
	}

	static Set<AvoidedSkill> parseAvoidedSkillsCsv(String csv)
	{
		Set<AvoidedSkill> parsed = EnumSet.noneOf(AvoidedSkill.class);
		if (csv == null || csv.isBlank())
		{
			return parsed;
		}

		for (String token : csv.split(","))
		{
			AvoidedSkill skill = AvoidedSkill.fromConfigToken(token);
			if (skill != null)
			{
				parsed.add(skill);
			}
		}
		return parsed;
	}

	static Map<AvoidedSkill, Integer> parseSkillCapsCsv(String csv)
	{
		Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
		if (csv == null || csv.isBlank())
		{
			return caps;
		}

		for (String token : csv.split(","))
		{
			String trimmed = token == null ? "" : token.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			String[] parts = trimmed.split(":");
			if (parts.length != 2)
			{
				continue;
			}
			AvoidedSkill skill = AvoidedSkill.fromConfigToken(parts[0]);
			if (skill == null)
			{
				continue;
			}
			try
			{
				int cap = Integer.parseInt(parts[1].trim());
				if (cap >= 1 && cap <= 99)
				{
					caps.put(skill, cap);
				}
			}
			catch (NumberFormatException ignored)
			{
				// Keep parsing to handle malformed legacy values.
			}
		}

		return caps;
	}

	static String toAvoidedSkillsCsv(Set<AvoidedSkill> skills)
	{
		return skills.stream()
			.filter(skill -> skill != AvoidedSkill.NONE)
			.map(Enum::name)
			.sorted()
			.collect(Collectors.joining(","));
	}

	static String toSkillCapsCsv(Map<AvoidedSkill, Integer> caps)
	{
		return caps.entrySet().stream()
			.filter(entry -> entry.getKey() != AvoidedSkill.NONE)
			.filter(entry -> entry.getValue() != null && entry.getValue() >= 1 && entry.getValue() <= 99)
			.sorted((a, b) -> a.getKey().name().compareTo(b.getKey().name()))
			.map(entry -> entry.getKey().name() + ":" + entry.getValue())
			.collect(Collectors.joining(","));
	}
}
