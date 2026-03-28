package com.yumesplugins.purehelper;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

class PureHelperWarningInfoBox extends InfoBox
{
	private boolean flashOn = true;
	private String text = "XP";

	PureHelperWarningInfoBox(Plugin plugin, Color accent)
	{
		super(createWarningIcon(accent), plugin);
	}

	void tickFlash()
	{
		flashOn = !flashOn;
	}

	void setText(String text)
	{
		this.text = text == null || text.isBlank() ? "XP" : text;
	}

	@Override
	public String getText()
	{
		return text;
	}

	@Override
	public Color getTextColor()
	{
		return flashOn ? PureHelperUiConstants.ACCENT_SOFT : Color.WHITE;
	}

	private static BufferedImage createWarningIcon(Color accent)
	{
		Color baseAccent = accent == null ? PureHelperUiConstants.DEFAULT_ACCENT : accent;
		BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(new Color(24, 20, 40));
			g.fillRoundRect(2, 2, 20, 20, 5, 5);
			g.setColor(baseAccent);
			g.fillRoundRect(4, 4, 16, 16, 4, 4);
			g.setColor(Color.WHITE);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
			g.drawString("!", 10, 17);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}
}
