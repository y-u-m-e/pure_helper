package com.yumesplugins.purehelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;

/** Cancels the XP lamp / book Confirm click when the chosen skill is protected, unless Shift is held. */
public class LampGuard
{
	static final Map<Integer, CombatSkill> SKILL_COMPONENTS;

	static
	{
		Map<Integer, CombatSkill> m = new HashMap<>();
		m.put(InterfaceID.Xpreward.ATTACK, CombatSkill.ATTACK);
		m.put(InterfaceID.Xpreward.STRENGTH, CombatSkill.STRENGTH);
		m.put(InterfaceID.Xpreward.RANGED, CombatSkill.RANGED);
		m.put(InterfaceID.Xpreward.MAGIC, CombatSkill.MAGIC);
		m.put(InterfaceID.Xpreward.DEFENCE, CombatSkill.DEFENCE);
		m.put(InterfaceID.Xpreward.HITPOINTS, CombatSkill.HITPOINTS);
		m.put(InterfaceID.Xpreward.PRAYER, CombatSkill.PRAYER);
		SKILL_COMPONENTS = Collections.unmodifiableMap(m);
	}

	private final Client client;
	private final PureHelperConfig config;
	private final Alerts alerts;
	private final Supplier<Build> build;

	private CombatSkill selected;

	LampGuard(Client client, PureHelperConfig config, Alerts alerts, Supplier<Build> build)
	{
		this.client = client;
		this.config = config;
		this.alerts = alerts;
		this.build = build;
	}

	// Lamp amounts aren't shown on the interface, so any protected skill is blocked.
	static boolean blocksConfirm(CombatSkill selected, Build build, boolean shiftHeld)
	{
		return selected != null && build.protects(selected) && !shiftHeld;
	}

	/** Client thread. */
	void onMenuOptionClicked(MenuOptionClicked event)
	{
		int id = event.getWidgetId();
		if (SKILL_COMPONENTS.containsKey(id))
		{
			selected = SKILL_COMPONENTS.get(id);
			return;
		}
		if (id != InterfaceID.Xpreward.CONFIRM || selected == null)
		{
			return;
		}
		boolean shift = client.isKeyPressed(KeyCode.KC_SHIFT);
		String name = selected.skill().getName();
		if (config.lampGuard() && blocksConfirm(selected, build.get(), shift))
		{
			event.consume();
			String text = "BLOCKED — " + name + " is protected. Hold Shift to confirm.";
			alerts.flash(text);
			alerts.chat(text);
			alerts.notify(config.blockNotification(), text);
			alerts.record("Lamp", name + " blocked", true, false);
		}
		else if (config.lampGuard() && shift && build.get().protects(selected))
		{
			alerts.record("Lamp", name + " chosen", false, true);
		}
	}

	void clearSelection()
	{
		selected = null;
	}
}
