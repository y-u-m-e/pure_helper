package com.yumesplugins.purehelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "Pure Helper")
public class PureHelperPlugin extends Plugin
{
	@Inject
	private PureHelperConfig config;

	@Override
	protected void startUp()
	{
	}

	@Override
	protected void shutDown()
	{
	}

	@Provides
	PureHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PureHelperConfig.class);
	}
}
