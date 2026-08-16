package com.yumesplugins.purehelper;

import java.awt.Color;
import net.runelite.client.ui.ColorScheme;

final class PureHelperUiConstants
{
	static final Color ROW_EVEN = ColorScheme.DARK_GRAY_COLOR;
	static final Color ROW_ODD = ColorScheme.DARKER_GRAY_COLOR;
	static final Color ROW_HOVER = new Color(60, 60, 60);
	static final Color DEFAULT_ACCENT = new Color(156, 126, 245);

	static Color ERROR_TEXT = new Color(206, 186, 255);

	static Color ACCENT_PRIMARY = DEFAULT_ACCENT;
	static Color ACCENT_PRIMARY_DIM = new Color(108, 87, 176);
	static Color ACCENT_SOFT = new Color(196, 178, 255);

	static Color BLOCKED_ACCENT = new Color(136, 108, 222);
	static Color BLOCKED_TEXT = new Color(216, 204, 255);
	static Color CAUTION_ACCENT = new Color(122, 98, 204);
	static Color CAUTION_TEXT = new Color(198, 184, 252);
	static Color SAFE_ACCENT = new Color(98, 98, 128);
	static Color SAFE_TEXT = ColorScheme.LIGHT_GRAY_COLOR;

	static String CHAT_PREFIX_HEX = "9c7ef5";

	private PureHelperUiConstants()
	{
	}

	static void applyAccent(Color accent)
	{
		Color base = accent == null ? DEFAULT_ACCENT : accent;
		ACCENT_PRIMARY = base;
		ACCENT_PRIMARY_DIM = darken(base, 0.28f);
		ACCENT_SOFT = lighten(base, 0.22f);

		BLOCKED_ACCENT = darken(base, 0.12f);
		BLOCKED_TEXT = lighten(base, 0.30f);
		CAUTION_ACCENT = darken(base, 0.22f);
		CAUTION_TEXT = lighten(base, 0.20f);
		SAFE_ACCENT = desaturate(base, 0.55f, 0.55f);
		SAFE_TEXT = ColorScheme.LIGHT_GRAY_COLOR;
		ERROR_TEXT = lighten(base, 0.24f);

		CHAT_PREFIX_HEX = toHex(base);
	}

	private static Color lighten(Color color, float amount)
	{
		int r = (int) (color.getRed() + (255 - color.getRed()) * clamp01(amount));
		int g = (int) (color.getGreen() + (255 - color.getGreen()) * clamp01(amount));
		int b = (int) (color.getBlue() + (255 - color.getBlue()) * clamp01(amount));
		return new Color(clamp255(r), clamp255(g), clamp255(b));
	}

	private static Color darken(Color color, float amount)
	{
		float scale = 1f - clamp01(amount);
		int r = (int) (color.getRed() * scale);
		int g = (int) (color.getGreen() * scale);
		int b = (int) (color.getBlue() * scale);
		return new Color(clamp255(r), clamp255(g), clamp255(b));
	}

	private static Color desaturate(Color color, float saturationScale, float brightnessScale)
	{
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		float s = clamp01(hsb[1] * saturationScale);
		float v = clamp01(hsb[2] * brightnessScale);
		return Color.getHSBColor(hsb[0], s, v);
	}

	private static String toHex(Color color)
	{
		return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static float clamp01(float value)
	{
		return Math.max(0f, Math.min(1f, value));
	}

	private static int clamp255(int value)
	{
		return Math.max(0, Math.min(255, value));
	}
}
