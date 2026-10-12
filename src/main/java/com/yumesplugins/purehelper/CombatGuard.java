package com.yumesplugins.purehelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.KeyCode;
import net.runelite.api.ParamID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/** Hides combat tab styles that would train a capped skill and shouts when one is active anyway. */
public class CombatGuard
{
	private static final String KEY = "combat";

	private final Client client;
	private final PureHelperConfig config;
	private final Alerts alerts;
	private final Supplier<Build> build;
	private final WidgetHider hider;

	private final Set<Integer> hidden = new HashSet<>();
	private boolean dangerous;
	private boolean lastShift;

	CombatGuard(Client client, PureHelperConfig config, Alerts alerts, Supplier<Build> build, WidgetHider hider)
	{
		this.client = client;
		this.config = config;
		this.alerts = alerts;
		this.build = build;
		this.hider = hider;
	}

	static Set<Integer> dangerousSlots(StyleTable.Style[] slots, Build build)
	{
		Set<Integer> out = new HashSet<>();
		for (int i = 0; i < slots.length; i++)
		{
			if (slots[i] != null && firstCapped(slots[i], build) != null)
			{
				out.add(i);
			}
		}
		return out;
	}

	static Set<Integer> componentsToHide(Set<Integer> dangerous, boolean shiftHeld, boolean enabled)
	{
		Set<Integer> out = new HashSet<>();
		if (shiftHeld || !enabled)
		{
			return out;
		}
		for (int slot : dangerous)
		{
			if (slot < StyleTable.SLOT_COMPONENTS.length)
			{
				for (int id : StyleTable.SLOT_COMPONENTS[slot])
				{
					out.add(id);
				}
			}
		}
		return out;
	}

	private static CombatSkill firstCapped(StyleTable.Style style, Build build)
	{
		for (CombatSkill s : style.trains())
		{
			if (build.atCap(s))
			{
				return s;
			}
		}
		return null;
	}

	/** Client thread. */
	void refresh()
	{
		int category = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
		List<String> names = new ArrayList<>();
		int styleEnum = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(category);
		if (styleEnum != -1)
		{
			for (int id : client.getEnum(styleEnum).getIntVals())
			{
				names.add(client.getStructComposition(id).getStringValue(ParamID.ATTACK_STYLE_NAME));
			}
		}
		int active = StyleTable.activeSlot(client.getVarpValue(VarPlayerID.COM_MODE), client.getVarbitValue(VarbitID.AUTOCAST_DEFMODE));
		lastShift = client.isKeyPressed(KeyCode.KC_SHIFT);
		apply(StyleTable.slots(category, names), active, lastShift);
	}

	/** Client thread; refreshes only when Shift flipped since the last look. */
	void onClientTick()
	{
		if (client.isKeyPressed(KeyCode.KC_SHIFT) != lastShift)
		{
			refresh();
		}
	}

	void apply(StyleTable.Style[] slots, int active, boolean shift)
	{
		boolean enabled = config.combatGuard();
		Build b = build.get();
		Set<Integer> toHide = componentsToHide(dangerousSlots(slots, b), shift, enabled);
		for (int id : new ArrayList<>(hidden))
		{
			if (!toHide.contains(id))
			{
				hider.setHidden(id, false);
				hidden.remove(id);
			}
		}
		for (int id : toHide)
		{
			hider.setHidden(id, true); // the setup script re-shows buttons, so reapply every time
			hidden.add(id);
		}

		StyleTable.Style style = active >= 0 && active < slots.length ? slots[active] : null;
		CombatSkill capped = enabled && style != null ? firstCapped(style, b) : null;
		if (capped == null)
		{
			if (dangerous)
			{
				alerts.clear(KEY);
				dangerous = false;
			}
			return;
		}

		String text = style.name().replace('_', ' ') + " STYLE — trains " + capped.skill().getName() + " (cap " + b.cap(capped) + ")";
		alerts.persistent(KEY, text);
		if (!dangerous)
		{
			dangerous = true;
			alerts.notify(config.styleNotification(), text);
			alerts.chat(text);
			String pretty = style.name().charAt(0) + style.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
			alerts.record("Combat", pretty + " selected", false, shift);
		}
	}

	void reset()
	{
		for (int id : hidden)
		{
			hider.setHidden(id, false);
		}
		hidden.clear();
		alerts.clear(KEY);
		dangerous = false;
	}
}
