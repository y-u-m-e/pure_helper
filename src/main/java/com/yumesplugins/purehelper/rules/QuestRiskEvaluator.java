package com.yumesplugins.purehelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

final class QuestRiskEvaluator
{
	private final Client client;
	private final Map<String, QuestRule> questRulesByName;

	QuestRiskEvaluator(Client client, Map<String, QuestRule> questRulesByName)
	{
		this.client = client;
		this.questRulesByName = questRulesByName;
	}

	QuestEvaluation evaluateQuest(
		QuestRule rule,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps,
		QuestChoicePolicy choicePolicy,
		SafeguardStrictness strictness,
		Map<String, QuestEvaluation> memo,
		Set<String> recursionGuard)
	{
		if (rule == null)
		{
			return QuestEvaluation.safe();
		}

		String key = NameNormalizer.normalize(rule.name);
		QuestEvaluation memoized = memo.get(key);
		if (memoized != null)
		{
			return memoized;
		}
		if (!recursionGuard.add(key))
		{
			return QuestEvaluation.safe();
		}

		QuestEvaluation skillRequirementEval = evaluateSkillRequirementRisk(rule, avoidedSkills, skillCaps);

		boolean risky = false;
		String riskReason = "";
		EvaluationReasonCode reasonCode = EvaluationReasonCode.NONE;
		if (rule.fixedRewards != null)
		{
			risky = matchesAnyAvoidedReward(rule.fixedRewards, avoidedSkills, skillCaps);
			if (risky)
			{
				riskReason = "Quest gives avoided-skill XP";
				reasonCode = EvaluationReasonCode.FIXED_REWARD_AVOIDED_SKILL_XP;
			}
		}

		if (!risky && choicePolicy == QuestChoicePolicy.ANY_CHOICE_MATCH_IS_RISKY && rule.choiceRewards != null)
		{
			risky = rule.choiceRewards.stream()
				.filter(Objects::nonNull)
				.flatMap(choice -> choice.options == null ? java.util.stream.Stream.empty() : choice.options.stream().map(option -> new ChoiceOptionView(choice, option)))
				.anyMatch(optionView -> isChoiceOptionRisky(optionView.choice, optionView.option, avoidedSkills, skillCaps));
			if (risky)
			{
				riskReason = "Quest choice can award avoided-skill XP";
				reasonCode = EvaluationReasonCode.CHOICE_REWARD_AVOIDED_SKILL_XP;
			}
			else if (rule.flags != null && rule.flags.hasUnmodeledChoiceRewards)
			{
				risky = true;
				riskReason = "Quest has unmodeled choice rewards";
				reasonCode = EvaluationReasonCode.UNMODELED_CHOICE_REWARD;
			}
		}
		else if (!risky
			&& strictness == SafeguardStrictness.STRICT
			&& rule.flags != null
			&& rule.flags.hasUnmodeledChoiceRewards)
		{
			risky = true;
			riskReason = "Quest has unmodeled choice rewards (strict)";
			reasonCode = EvaluationReasonCode.UNMODELED_CHOICE_REWARD;
		}

		QuestEvaluation prerequisiteEval = evaluatePrerequisiteRisk(rule, avoidedSkills, skillCaps, choicePolicy, strictness, memo, recursionGuard);
		QuestEvaluation lockedEval = mergeLockedEvaluations(skillRequirementEval, prerequisiteEval);
		recursionGuard.remove(key);

		QuestEvaluation result;
		if (risky && lockedEval.locked)
		{
			result = new QuestEvaluation(true, true, riskReason + "; " + lockedEval.reason, reasonCode);
		}
		else if (risky)
		{
			result = new QuestEvaluation(true, false, riskReason, reasonCode);
		}
		else if (lockedEval.locked)
		{
			result = lockedEval;
		}
		else
		{
			result = QuestEvaluation.safe();
		}

		memo.put(key, result);
		return result;
	}

	boolean hasChoiceXpCaution(QuestRule rule)
	{
		if (rule == null)
		{
			return false;
		}

		boolean hasAnyChoiceRewards = rule.choiceRewards != null && !rule.choiceRewards.isEmpty();
		boolean hasAnyChoiceOptions = rule.choiceRewards != null && rule.choiceRewards.stream()
			.filter(Objects::nonNull)
			.flatMap(choice -> choice.options == null ? java.util.stream.Stream.empty() : choice.options.stream())
			.anyMatch(Objects::nonNull);
		return hasAnyChoiceRewards || hasAnyChoiceOptions
			|| (rule.flags != null && rule.flags.hasUnmodeledChoiceRewards);
	}

	boolean evaluateDiaryTierRisk(
		DiaryTier tier,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps,
		QuestChoicePolicy choicePolicy,
		SafeguardStrictness strictness,
		Map<String, QuestEvaluation> questMemo)
	{
		if (tier == null)
		{
			return false;
		}

		if (tier.requiredQuests != null)
		{
			for (String requiredQuestName : tier.requiredQuests)
			{
				if (requiredQuestName == null || requiredQuestName.isBlank())
				{
					continue;
				}
				QuestRule requiredQuestRule = questRulesByName.get(NameNormalizer.normalize(requiredQuestName));
				if (requiredQuestRule == null)
				{
					continue;
				}
				QuestEvaluation requiredQuestEval = evaluateQuest(
					requiredQuestRule,
					avoidedSkills,
					skillCaps,
					choicePolicy,
					strictness,
					questMemo == null ? new HashMap<>() : questMemo,
					new HashSet<>());
				if (requiredQuestEval.isBlocked())
				{
					return true;
				}
			}
		}

		if (tier.fixedRewards != null && tier.fixedRewards.stream()
			.filter(Objects::nonNull)
			.anyMatch(reward -> rewardExceedsCap(reward.skill, reward.xp, avoidedSkills, skillCaps)))
		{
			return true;
		}

		if (choicePolicy != QuestChoicePolicy.ANY_CHOICE_MATCH_IS_RISKY || tier.choiceRewards == null)
		{
			return (choicePolicy == QuestChoicePolicy.ANY_CHOICE_MATCH_IS_RISKY || strictness == SafeguardStrictness.STRICT)
				&& tier.flags != null
				&& tier.flags.hasUnmodeledChoiceRewards;
		}

		return tier.choiceRewards.stream()
			.filter(Objects::nonNull)
			.flatMap(choice -> choice.options == null ? java.util.stream.Stream.empty() : choice.options.stream().map(option -> new ChoiceOptionView(choice, option)))
			.anyMatch(view -> isChoiceOptionRiskyWithMinLevel(view.choice, view.option, avoidedSkills, skillCaps));
	}

	String firstBlockedRequiredQuest(
		DiaryTier tier,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps,
		QuestChoicePolicy choicePolicy,
		SafeguardStrictness strictness,
		Map<String, QuestEvaluation> questMemo)
	{
		if (tier == null || tier.requiredQuests == null)
		{
			return null;
		}

		for (String requiredQuestName : tier.requiredQuests)
		{
			if (requiredQuestName == null || requiredQuestName.isBlank())
			{
				continue;
			}

			QuestRule requiredQuestRule = questRulesByName.get(NameNormalizer.normalize(requiredQuestName));
			if (requiredQuestRule == null)
			{
				continue;
			}

			QuestEvaluation requiredQuestEval = evaluateQuest(
				requiredQuestRule,
				avoidedSkills,
				skillCaps,
				choicePolicy,
				strictness,
				questMemo == null ? new HashMap<>() : questMemo,
				new HashSet<>());
			if (requiredQuestEval.isBlocked())
			{
				return requiredQuestName;
			}
		}

		return null;
	}

	boolean hasDiaryChoiceXpCaution(DiaryTier tier)
	{
		if (tier == null)
		{
			return false;
		}
		return (tier.choiceRewards != null && !tier.choiceRewards.isEmpty())
			|| (tier.flags != null && tier.flags.hasUnmodeledChoiceRewards);
	}

	private QuestEvaluation evaluatePrerequisiteRisk(
		QuestRule rule,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps,
		QuestChoicePolicy choicePolicy,
		SafeguardStrictness strictness,
		Map<String, QuestEvaluation> memo,
		Set<String> recursionGuard)
	{
		if (rule.prerequisites == null || rule.prerequisites.quests == null)
		{
			return QuestEvaluation.safe();
		}

		for (String prerequisiteQuestName : rule.prerequisites.quests)
		{
			if (prerequisiteQuestName == null || prerequisiteQuestName.isBlank())
			{
				continue;
			}
			QuestRule prerequisiteRule = questRulesByName.get(NameNormalizer.normalize(prerequisiteQuestName));
			if (prerequisiteRule == null)
			{
				continue;
			}
			QuestEvaluation prerequisiteEvaluation = evaluateQuest(
				prerequisiteRule,
				avoidedSkills,
				skillCaps,
				choicePolicy,
				strictness,
				memo,
				recursionGuard);
			if (prerequisiteEvaluation.risky || prerequisiteEvaluation.locked)
			{
				String reason = "Blocked by prerequisite: " + prerequisiteQuestName;
				if (prerequisiteEvaluation.reason != null && !prerequisiteEvaluation.reason.isBlank())
				{
					reason += " (" + prerequisiteEvaluation.reason + ")";
				}
				return new QuestEvaluation(false, true, reason, EvaluationReasonCode.PREREQUISITE_BLOCKED);
			}
		}
		return QuestEvaluation.safe();
	}

	private QuestEvaluation evaluateSkillRequirementRisk(
		QuestRule rule,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps)
	{
		if (rule.prerequisites == null || rule.prerequisites.skills == null)
		{
			return QuestEvaluation.safe();
		}

		for (QuestSkillRequirement requiredSkill : rule.prerequisites.skills)
		{
			if (requiredSkill == null || requiredSkill.skill == null || requiredSkill.skill.isBlank())
			{
				continue;
			}

			AvoidedSkill avoidedSkill = AvoidedSkill.fromRewardSkill(requiredSkill.skill);
			if (avoidedSkill == null || !avoidedSkills.contains(avoidedSkill))
			{
				continue;
			}

			int requiredLevel = Math.max(1, requiredSkill.level);
			Integer cap = skillCaps.get(avoidedSkill);
			if (cap != null && requiredLevel > cap)
			{
				return new QuestEvaluation(
					false,
					true,
					"Requires " + avoidedSkill.getLabel() + " " + requiredLevel + " (cap " + cap + ")",
					EvaluationReasonCode.SKILL_REQUIREMENT_EXCEEDS_CAP);
			}

			// No cap means "do not train this skill"; if requirement is above current level, quest is not feasible.
			if (cap == null)
			{
				Skill runeliteSkill = avoidedSkill.getRuneliteSkill();
				int currentLevel = runeliteSkill != null && client != null
					? client.getRealSkillLevel(runeliteSkill)
					: 1;
				if (requiredLevel > currentLevel)
				{
					return new QuestEvaluation(
						false,
						true,
						"Requires " + avoidedSkill.getLabel() + " " + requiredLevel + " while this skill is protected",
						EvaluationReasonCode.SKILL_REQUIREMENT_PROTECTED);
				}
			}
		}

		return QuestEvaluation.safe();
	}

	private static QuestEvaluation mergeLockedEvaluations(QuestEvaluation first, QuestEvaluation second)
	{
		boolean firstLocked = first != null && first.locked;
		boolean secondLocked = second != null && second.locked;
		if (!firstLocked && !secondLocked)
		{
			return QuestEvaluation.safe();
		}
		if (firstLocked && !secondLocked)
		{
			return first;
		}
		if (!firstLocked)
		{
			return second;
		}

		String firstReason = first.reason == null ? "" : first.reason.trim();
		String secondReason = second.reason == null ? "" : second.reason.trim();
		if (firstReason.isEmpty())
		{
			return second;
		}
		if (secondReason.isEmpty())
		{
			return first;
		}
		if (firstReason.equals(secondReason))
		{
			return first;
		}
		return new QuestEvaluation(false, true, firstReason + "; " + secondReason);
	}

	private boolean matchesAnyAvoidedReward(
		java.util.List<QuestSkillReward> rewards,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps)
	{
		return rewards.stream()
			.filter(Objects::nonNull)
			.anyMatch(reward -> rewardExceedsCap(reward.skill, reward.xp, avoidedSkills, skillCaps));
	}

	private boolean isChoiceOptionRisky(
		QuestChoiceReward choice,
		QuestSkillReward option,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps)
	{
		if (option == null)
		{
			return false;
		}

		int optionXp = option.xp > 0
			? option.xp
			: choice != null && choice.xp != null && choice.xp > 0 ? choice.xp : -1;
		int count = choice != null && choice.count != null && choice.count > 0 ? choice.count : 1;
		int totalXp = optionXp > 0 ? optionXp * count : optionXp;
		return rewardExceedsCap(option.skill, totalXp, avoidedSkills, skillCaps);
	}

	private boolean isChoiceOptionRiskyWithMinLevel(
		QuestChoiceReward choice,
		QuestSkillReward option,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps)
	{
		AvoidedSkill avoidedSkill = option == null ? null : AvoidedSkill.fromRewardSkill(option.skill);
		if (option == null || avoidedSkill == null || !avoidedSkills.contains(avoidedSkill))
		{
			return false;
		}

		Skill skill = avoidedSkill.getRuneliteSkill();
		int currentLevel = skill == null || client == null ? 1 : client.getRealSkillLevel(skill);
		if (choice.minSkillLevel != null && currentLevel < choice.minSkillLevel)
		{
			return false;
		}
		return isChoiceOptionRisky(choice, option, avoidedSkills, skillCaps);
	}

	private boolean rewardExceedsCap(
		String rewardSkill,
		int rewardXp,
		Set<AvoidedSkill> avoidedSkills,
		Map<AvoidedSkill, Integer> skillCaps)
	{
		AvoidedSkill avoidedSkill = AvoidedSkill.fromRewardSkill(rewardSkill);
		if (avoidedSkill == null || !avoidedSkills.contains(avoidedSkill))
		{
			return false;
		}

		Integer cap = skillCaps.get(avoidedSkill);
		if (cap == null)
		{
			return true;
		}

		Skill skill = avoidedSkill.getRuneliteSkill();
		if (skill == null || client == null)
		{
			return true;
		}
		if (rewardXp <= 0)
		{
			return true;
		}

		int currentXp = client.getSkillExperience(skill);
		int projectedXp = currentXp + rewardXp;
		int projectedLevel = Experience.getLevelForXp(projectedXp);
		return projectedLevel > cap;
	}
}
