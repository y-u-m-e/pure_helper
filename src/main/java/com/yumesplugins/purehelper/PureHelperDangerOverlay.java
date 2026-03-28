package com.yumesplugins.purehelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Flashing border around the viewport when a dangerous attack style is active.
 * Color, opacity, and border width are configurable.
 */
class PureHelperDangerOverlay extends Overlay
{
	private final Client client;
	private final PureHelperConfig config;
	private boolean active;
	private boolean flashPhase;

	@Inject
	PureHelperDangerOverlay(Client client, PureHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	void setActive(boolean active)
	{
		this.active = active;
	}

	void tickFlash()
	{
		flashPhase = !flashPhase;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!active || !config.screenFlashOnDanger())
		{
			return null;
		}

		int w = client.getCanvasWidth();
		int h = client.getCanvasHeight();
		if (w <= 0 || h <= 0)
		{
			return null;
		}

		Color base = config.screenFlashColor();
		int opacityPct = Math.max(0, Math.min(100, config.screenFlashOpacity()));
		int peakAlpha = (int) (255 * opacityPct / 100.0);
		int dimAlpha = Math.max(0, peakAlpha / 3);
		int alpha = flashPhase ? peakAlpha : dimAlpha;

		int borderWidth = Math.max(1, Math.min(20, config.screenFlashWidth()));

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
		graphics.setStroke(new BasicStroke(borderWidth));
		int half = borderWidth / 2;
		graphics.drawRect(half, half, w - borderWidth, h - borderWidth);

		return new Dimension(w, h);
	}
}
