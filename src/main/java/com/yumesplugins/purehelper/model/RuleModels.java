package com.yumesplugins.purehelper;

import java.util.List;

class QuestRulesRoot
{
	List<QuestRule> quests;
}

class QuestRule
{
	String name;
	QuestPrerequisites prerequisites;
	List<QuestSkillReward> fixedRewards;
	List<QuestChoiceReward> choiceRewards;
	QuestFlags flags;
}

class QuestPrerequisites
{
	List<String> quests;
	List<QuestSkillRequirement> skills;
}

class QuestSkillRequirement
{
	String skill;
	int level;
}

class QuestSkillReward
{
	String skill;
	int xp;
}

class QuestChoiceReward
{
	Integer xp;
	Integer count;
	Integer minSkillLevel;
	List<QuestSkillReward> options;
}

class QuestFlags
{
	boolean hasUnmodeledChoiceRewards;
}

class ChoiceOptionView
{
	final QuestChoiceReward choice;
	final QuestSkillReward option;

	ChoiceOptionView(QuestChoiceReward choice, QuestSkillReward option)
	{
		this.choice = choice;
		this.option = option;
	}
}

class DiaryRulesRoot
{
	List<DiaryRule> diaries;
}

class DiaryRule
{
	String name;
	List<DiaryTier> tiers;
}

class DiaryTier
{
	String tier;
	List<QuestSkillReward> fixedRewards;
	List<QuestChoiceReward> choiceRewards;
	List<String> requiredQuests;
	DiaryFlags flags;
}

class DiaryFlags
{
	boolean hasUnmodeledChoiceRewards;
}

class QuestEvaluation
{
	final boolean risky;
	final boolean locked;
	final String reason;
	final EvaluationReasonCode reasonCode;
	final RiskSeverity severity;

	QuestEvaluation(boolean risky, boolean locked, String reason)
	{
		this(risky, locked, reason, EvaluationReasonCode.NONE);
	}

	QuestEvaluation(boolean risky, boolean locked, String reason, EvaluationReasonCode reasonCode)
	{
		this.risky = risky;
		this.locked = locked;
		this.reason = reason;
		this.reasonCode = reasonCode == null ? EvaluationReasonCode.NONE : reasonCode;
		this.severity = resolveSeverity(risky, locked);
	}

	boolean isBlocked()
	{
		return risky || locked;
	}

	static QuestEvaluation safe()
	{
		return new QuestEvaluation(false, false, "", EvaluationReasonCode.NONE);
	}

	private static RiskSeverity resolveSeverity(boolean risky, boolean locked)
	{
		if (locked)
		{
			return RiskSeverity.BLOCKED;
		}
		if (risky)
		{
			return RiskSeverity.DANGEROUS;
		}
		return RiskSeverity.SAFE;
	}
}
