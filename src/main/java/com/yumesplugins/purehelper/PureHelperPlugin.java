package com.yumesplugins.purehelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(name = "Pure Helper")
public class PureHelperPlugin extends Plugin
{
	@Inject
	private PureHelperConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DangerOverlay dangerOverlay;

	@Override
	protected void startUp()
	{
		overlayManager.add(dangerOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(dangerOverlay);
	}

	@Provides
	PureHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PureHelperConfig.class);
	}
}
