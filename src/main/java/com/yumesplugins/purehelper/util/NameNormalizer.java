package com.yumesplugins.purehelper;

import java.util.Locale;

final class NameNormalizer
{
	private NameNormalizer()
	{
	}

	static String normalize(String name)
	{
		if (name == null)
		{
			return "";
		}

		return name
			.toLowerCase(Locale.ENGLISH)
			.replace("&", "and")
			.replaceAll("[^a-z0-9 ]", "")
			.replaceAll("\\s+", " ")
			.trim();
	}
}
