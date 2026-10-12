package com.yumesplugins.purehelper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/** Shows why a tinted quest breaches the build when hovered. */
public class QuestTooltipOverlay extends Overlay
{
	private final Client client;
	private final TooltipManager tooltipManager;
	private final QuestGuard guard;

	QuestTooltipOverlay(Client client, TooltipManager tooltipManager, QuestGuard guard)
	{
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.guard = guard;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		String reason = guard.reasonAt(client.getMouseCanvasPosition());
		if (reason != null)
		{
			tooltipManager.add(new Tooltip(reason));
		}
		return null;
	}
}
