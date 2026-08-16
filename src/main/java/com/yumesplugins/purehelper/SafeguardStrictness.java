package com.yumesplugins.purehelper;

public enum SafeguardStrictness
{
	LENIENT("Lenient"),
	BALANCED("Balanced"),
	STRICT("Strict");

	private final String label;

	SafeguardStrictness(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
