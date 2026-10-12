package com.yumesplugins.purehelper;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Outlines protected skills on the XP lamp / book interface. */
public class LampOverlay extends Overlay
{
	private final Client client;
	private final PureHelperConfig config;
	private final Supplier<Build> build;

	LampOverlay(Client client, PureHelperConfig config, Supplier<Build> build)
	{
		this.client = client;
		this.config = config;
		this.build = build;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!config.lampGuard())
		{
			return null;
		}
		Build b = build.get();
		g.setColor(config.accentColor());
		g.setStroke(new BasicStroke(2));
		for (Map.Entry<Integer, CombatSkill> e : LampGuard.SKILL_COMPONENTS.entrySet())
		{
			Widget w = client.getWidget(e.getKey());
			if (w != null && !w.isHidden() && b.protects(e.getValue()))
			{
				Rectangle r = w.getBounds();
				g.drawRect(r.x, r.y, r.width, r.height);
			}
		}
		return null;
	}
}
