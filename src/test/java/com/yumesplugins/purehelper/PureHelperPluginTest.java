package com.yumesplugins.purehelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PureHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PureHelperPlugin.class);
		RuneLite.main(args);
	}
}
