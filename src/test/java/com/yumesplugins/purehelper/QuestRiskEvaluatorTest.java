package com.yumesplugins.purehelper;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class QuestRiskEvaluatorTest
{
	@Test
	public void testSkillRequirementAboveCapLocksQuest()
	{
		QuestRule rule = new QuestRule();
		rule.name = "Skill Gate Quest";
		rule.prerequisites = new QuestPrerequisites();
		rule.prerequisites.quests = Collections.emptyList();
		rule.prerequisites.skills = Collections.singletonList(requirement("DEFENCE", 50));
		rule.fixedRewards = Collections.emptyList();
		rule.choiceRewards = Collections.emptyList();

		Map<String, QuestRule> rules = new HashMap<>();
		rules.put(NameNormalizer.normalize(rule.name), rule);
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(null, rules);

		Set<AvoidedSkill> avoided = EnumSet.of(AvoidedSkill.DEFENCE);
		Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
		caps.put(AvoidedSkill.DEFENCE, 45);

		QuestEvaluation eval = evaluator.evaluateQuest(
			rule,
			avoided,
			caps,
			QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE,
			SafeguardStrictness.BALANCED,
			new HashMap<>(),
			new HashSet<>());

		Assert.assertTrue(eval.locked);
		Assert.assertEquals(EvaluationReasonCode.SKILL_REQUIREMENT_EXCEEDS_CAP, eval.reasonCode);
	}

	@Test
	public void testBlockedPrerequisitePropagatesReasonCode()
	{
		QuestRule prerequisite = new QuestRule();
		prerequisite.name = "Hard Defence Quest";
		prerequisite.prerequisites = new QuestPrerequisites();
		prerequisite.prerequisites.quests = Collections.emptyList();
		prerequisite.prerequisites.skills = Collections.singletonList(requirement("DEFENCE", 50));
		prerequisite.fixedRewards = Collections.emptyList();
		prerequisite.choiceRewards = Collections.emptyList();

		QuestRule parent = new QuestRule();
		parent.name = "Parent Quest";
		parent.prerequisites = new QuestPrerequisites();
		parent.prerequisites.quests = Collections.singletonList(prerequisite.name);
		parent.prerequisites.skills = Collections.emptyList();
		parent.fixedRewards = Collections.emptyList();
		parent.choiceRewards = Collections.emptyList();

		Map<String, QuestRule> rules = new HashMap<>();
		rules.put(NameNormalizer.normalize(prerequisite.name), prerequisite);
		rules.put(NameNormalizer.normalize(parent.name), parent);
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(null, rules);

		Set<AvoidedSkill> avoided = EnumSet.of(AvoidedSkill.DEFENCE);
		Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
		caps.put(AvoidedSkill.DEFENCE, 45);

		QuestEvaluation eval = evaluator.evaluateQuest(
			parent,
			avoided,
			caps,
			QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE,
			SafeguardStrictness.BALANCED,
			new HashMap<>(),
			new HashSet<>());

		Assert.assertTrue(eval.locked);
		Assert.assertEquals(EvaluationReasonCode.PREREQUISITE_BLOCKED, eval.reasonCode);
	}

	@Test
	public void testStrictModeBlocksUnmodeledChoiceRewards()
	{
		QuestRule rule = new QuestRule();
		rule.name = "Unmodeled Choice Quest";
		rule.prerequisites = new QuestPrerequisites();
		rule.prerequisites.quests = Collections.emptyList();
		rule.prerequisites.skills = Collections.emptyList();
		rule.fixedRewards = Collections.emptyList();
		rule.choiceRewards = Collections.emptyList();
		rule.flags = new QuestFlags();
		rule.flags.hasUnmodeledChoiceRewards = true;

		Map<String, QuestRule> rules = new HashMap<>();
		rules.put(NameNormalizer.normalize(rule.name), rule);
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(null, rules);

		QuestEvaluation strictEval = evaluator.evaluateQuest(
			rule,
			EnumSet.of(AvoidedSkill.DEFENCE),
			new EnumMap<>(AvoidedSkill.class),
			QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE,
			SafeguardStrictness.STRICT,
			new HashMap<>(),
			new HashSet<>());

		Assert.assertTrue(strictEval.risky);
		Assert.assertEquals(EvaluationReasonCode.UNMODELED_CHOICE_REWARD, strictEval.reasonCode);
	}

	@Test
	public void testLenientModeDoesNotBlockUnmodeledChoiceRewards()
	{
		QuestRule rule = new QuestRule();
		rule.name = "Unmodeled Choice Quest";
		rule.prerequisites = new QuestPrerequisites();
		rule.prerequisites.quests = Collections.emptyList();
		rule.prerequisites.skills = Collections.emptyList();
		rule.fixedRewards = Collections.emptyList();
		rule.choiceRewards = Collections.emptyList();
		rule.flags = new QuestFlags();
		rule.flags.hasUnmodeledChoiceRewards = true;

		Map<String, QuestRule> rules = new HashMap<>();
		rules.put(NameNormalizer.normalize(rule.name), rule);
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(null, rules);

		QuestEvaluation lenientEval = evaluator.evaluateQuest(
			rule,
			EnumSet.of(AvoidedSkill.DEFENCE),
			new EnumMap<>(AvoidedSkill.class),
			QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE,
			SafeguardStrictness.LENIENT,
			new HashMap<>(),
			new HashSet<>());

		Assert.assertFalse(lenientEval.risky);
		Assert.assertFalse(lenientEval.locked);
		Assert.assertEquals(EvaluationReasonCode.NONE, lenientEval.reasonCode);
	}

	@Test
	public void testDiaryTierBlockedByRequiredQuest()
	{
		QuestRule requiredQuest = new QuestRule();
		requiredQuest.name = "Required Defence Quest";
		requiredQuest.prerequisites = new QuestPrerequisites();
		requiredQuest.prerequisites.quests = Collections.emptyList();
		requiredQuest.prerequisites.skills = Collections.singletonList(requirement("DEFENCE", 50));
		requiredQuest.fixedRewards = Collections.emptyList();
		requiredQuest.choiceRewards = Collections.emptyList();

		Map<String, QuestRule> rules = new HashMap<>();
		rules.put(NameNormalizer.normalize(requiredQuest.name), requiredQuest);
		QuestRiskEvaluator evaluator = new QuestRiskEvaluator(null, rules);

		DiaryTier tier = new DiaryTier();
		tier.tier = "HARD";
		tier.requiredQuests = Collections.singletonList(requiredQuest.name);
		tier.fixedRewards = Collections.emptyList();
		tier.choiceRewards = Collections.emptyList();

		Set<AvoidedSkill> avoided = EnumSet.of(AvoidedSkill.DEFENCE);
		Map<AvoidedSkill, Integer> caps = new EnumMap<>(AvoidedSkill.class);
		caps.put(AvoidedSkill.DEFENCE, 45);

		boolean risky = evaluator.evaluateDiaryTierRisk(
			tier,
			avoided,
			caps,
			QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE,
			SafeguardStrictness.BALANCED,
			new HashMap<>());
		String blockedQuest = evaluator.firstBlockedRequiredQuest(
			tier,
			avoided,
			caps,
			QuestChoicePolicy.SAFE_UNLESS_UNAVOIDABLE,
			SafeguardStrictness.BALANCED,
			new HashMap<>());

		Assert.assertTrue(risky);
		Assert.assertEquals(requiredQuest.name, blockedQuest);
	}

	private static QuestSkillRequirement requirement(String skill, int level)
	{
		QuestSkillRequirement requirement = new QuestSkillRequirement();
		requirement.skill = skill;
		requirement.level = level;
		return requirement;
	}
}
