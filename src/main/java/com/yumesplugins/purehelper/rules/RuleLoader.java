package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class RuleLoader
{
	private RuleLoader()
	{
	}

	static Map<String, QuestRule> loadQuestRulesByName(Gson gson, Class<?> resourceClass, String resourcePath)
	{
		Map<String, QuestRule> loaded = new HashMap<>();
		try (InputStream input = openResource(resourceClass, resourcePath))
		{
			if (input == null)
			{
				return loaded;
			}

			try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
			{
				QuestRulesRoot root = gson.fromJson(reader, QuestRulesRoot.class);
				if (root == null || root.quests == null)
				{
					return loaded;
				}
				for (QuestRule rule : root.quests)
				{
					if (rule == null || rule.name == null)
					{
						continue;
					}
					loaded.put(NameNormalizer.normalize(rule.name), rule);
				}
			}
		}
		catch (Exception ignored)
		{
			log.warn("Failed loading quest rules from {}", resourcePath, ignored);
			return new HashMap<>();
		}
		return loaded;
	}

	static List<DiaryRule> loadDiaryRulesList(Gson gson, Class<?> resourceClass, String resourcePath, String fallbackResourceName)
	{
		List<DiaryRule> loaded = new ArrayList<>();
		try (InputStream input = openResource(resourceClass, resourcePath, fallbackResourceName))
		{
			if (input == null)
			{
				return loaded;
			}

			try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
			{
				DiaryRulesRoot root = gson.fromJson(reader, DiaryRulesRoot.class);
				if (root == null || root.diaries == null)
				{
					return loaded;
				}

				for (DiaryRule diary : root.diaries)
				{
					if (diary != null && diary.name != null)
					{
						loaded.add(diary);
					}
				}
			}
		}
		catch (Exception ignored)
		{
			log.warn("Failed loading diary rules from {}", resourcePath, ignored);
			return new ArrayList<>();
		}
		return loaded;
	}

	static List<String> validateQuestRuleGraph(Map<String, QuestRule> rulesByName)
	{
		List<String> issues = new ArrayList<>();
		if (rulesByName == null || rulesByName.isEmpty())
		{
			issues.add("No quest rules loaded.");
			return issues;
		}

		for (QuestRule rule : rulesByName.values())
		{
			if (rule == null || rule.name == null || rule.name.isBlank())
			{
				continue;
			}

			if (rule.prerequisites == null || rule.prerequisites.quests == null)
			{
				continue;
			}

			for (String prerequisiteName : rule.prerequisites.quests)
			{
				if (prerequisiteName == null || prerequisiteName.isBlank())
				{
					continue;
				}

				String normalizedPrerequisite = NameNormalizer.normalize(prerequisiteName);
				if (!rulesByName.containsKey(normalizedPrerequisite))
				{
					issues.add("Unknown prerequisite '" + prerequisiteName + "' referenced by '" + rule.name + "'");
				}
			}
		}

		return issues;
	}

	private static InputStream openResource(Class<?> resourceClass, String resourcePath)
	{
		return openResource(resourceClass, resourcePath, null);
	}

	private static InputStream openResource(Class<?> resourceClass, String resourcePath, String fallbackResourceName)
	{
		InputStream panelPathInput = resourceClass.getResourceAsStream(resourcePath);
		if (panelPathInput != null)
		{
			return panelPathInput;
		}
		if (fallbackResourceName == null || fallbackResourceName.isBlank())
		{
			log.debug("Resource not found: {}", resourcePath);
			return null;
		}
		InputStream fallback = resourceClass.getClassLoader().getResourceAsStream(fallbackResourceName);
		if (fallback == null)
		{
			log.debug("Fallback resource not found: {}", fallbackResourceName);
		}
		return fallback;
	}
}
