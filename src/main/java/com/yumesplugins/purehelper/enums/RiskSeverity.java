package com.yumesplugins.purehelper;

import java.awt.Color;

/**
 * Four-tier risk classification for all integrity checks.
 */
public enum RiskSeverity
{
	SAFE("Safe"),
	WARNING("Warning"),
	DANGEROUS("Dangerous"),
	BLOCKED("Blocked");

	private final String label;

	RiskSeverity(String label)
	{
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}

	public Color getColor()
	{
		switch (this)
		{
			case SAFE:
				return PureHelperUiConstants.SAFE_ACCENT;
			case WARNING:
				return PureHelperUiConstants.CAUTION_ACCENT;
			case DANGEROUS:
				return PureHelperUiConstants.ACCENT_PRIMARY;
			case BLOCKED:
				return PureHelperUiConstants.BLOCKED_ACCENT;
			default:
				return Color.WHITE;
		}
	}

	public boolean isAtLeast(RiskSeverity threshold)
	{
		return ordinal() >= threshold.ordinal();
	}
}
