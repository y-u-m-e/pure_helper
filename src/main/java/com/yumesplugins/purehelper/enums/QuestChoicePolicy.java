package com.yumesplugins.purehelper;

public enum QuestChoicePolicy
{
	SAFE_UNLESS_UNAVOIDABLE("Safe unless unavoidable"),
	ANY_CHOICE_MATCH_IS_RISKY("Any matching choice is risky");

	private final String label;

	QuestChoicePolicy(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
}
