package com.yumesplugins.purehelper;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class DangerOverlay extends Overlay
{
	private final Alerts alerts;
	private final PureHelperConfig config;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	DangerOverlay(Alerts alerts, PureHelperConfig config)
	{
		this.alerts = alerts;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		String text = alerts.bannerText();
		if (text == null)
		{
			return null;
		}
		g.setFont(g.getFont().deriveFont(Font.BOLD));
		panel.getChildren().clear();
		panel.setBackgroundColor(config.accentColor());
		panel.getChildren().add(TitleComponent.builder().text(text).color(java.awt.Color.WHITE).build());
		return panel.render(g);
	}
}
