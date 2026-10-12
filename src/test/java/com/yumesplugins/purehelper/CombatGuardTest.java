package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.yumesplugins.purehelper.StyleTable.Style;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.config.Notification;
import org.junit.Before;
import org.junit.Test;

public class CombatGuardTest
{
	private static final Style[] SCIM = {Style.ACCURATE, Style.AGGRESSIVE, Style.AGGRESSIVE, Style.DEFENSIVE};
	private static final Style[] WHIP = {Style.ACCURATE, Style.DEFENSIVE, Style.CONTROLLED, Style.OTHER};

	private static Build build(int defCap, int defXp)
	{
		Map<CombatSkill, Integer> caps = new EnumMap<>(CombatSkill.class);
		Map<CombatSkill, Integer> xp = new EnumMap<>(CombatSkill.class);
		caps.put(CombatSkill.DEFENCE, defCap);
		xp.put(CombatSkill.DEFENCE, defXp);
		return Build.of(caps, xp);
	}

	private static class FakeAlerts extends Alerts
	{
		int notifies;
		int chats;

		FakeAlerts(RiskLog log)
		{
			super(null, null, new PureHelperConfig() { }, log);
		}

		@Override
		public void notify(Notification n, String text)
		{
			notifies++;
		}

		@Override
		public void chat(String text)
		{
			chats++;
		}
	}

	private Build current;
	private RiskLog log;
	private FakeAlerts alerts;
	private final List<String> calls = new ArrayList<>();
	private CombatGuard guard;

	@Before
	public void setUp()
	{
		current = build(1, 0);
		log = new RiskLog();
		alerts = new FakeAlerts(log);
		guard = new CombatGuard(null, new PureHelperConfig() { }, alerts, () -> current,
			(id, hidden) -> calls.add(id + ":" + hidden));
	}

	@Test
	public void defensiveIsDangerousOnA1Def()
	{
		assertEquals(Set.of(3), CombatGuard.dangerousSlots(SCIM, build(1, 83)));
	}

	@Test
	public void controlledIsDangerousWhenAnyTrainedSkillCapped()
	{
		assertEquals(Set.of(1, 2), CombatGuard.dangerousSlots(WHIP, build(1, 83)));
	}

	@Test
	public void nothingDangerousOnEmptyBuild()
	{
		assertTrue(CombatGuard.dangerousSlots(SCIM, Build.EMPTY).isEmpty());
	}

	@Test
	public void shiftRevealsEverything()
	{
		assertTrue(CombatGuard.componentsToHide(Set.of(3), true, true).isEmpty());
		assertTrue(CombatGuard.componentsToHide(Set.of(3), false, false).isEmpty());
		assertEquals(1, CombatGuard.componentsToHide(Set.of(3), false, true).size());
	}

	@Test
	public void raisingCapUnhides()
	{
		assertEquals(Set.of(3), CombatGuard.dangerousSlots(SCIM, build(1, 83)));
		assertTrue(CombatGuard.dangerousSlots(SCIM, build(99, 83)).isEmpty());
	}

	@Test
	public void resetUnhidesAll()
	{
		current = build(1, 83);
		guard.apply(SCIM, 0, false);
		assertEquals(1, calls.size());
		assertTrue(calls.get(0).endsWith(":true"));
		calls.clear();
		guard.reset();
		assertEquals(1, calls.size());
		assertTrue(calls.get(0).endsWith(":false"));
	}

	@Test
	public void enterStayLeaveDangerous()
	{
		current = build(1, 83);
		guard.apply(SCIM, 3, false);
		assertEquals("DEFENSIVE STYLE — trains Defence (cap 1)", alerts.bannerText());
		assertEquals(1, log.snapshot().size());
		assertEquals(1, alerts.notifies);
		assertEquals(1, alerts.chats);

		guard.apply(SCIM, 3, false);
		assertEquals(1, log.snapshot().size());
		assertEquals(1, alerts.notifies);

		guard.apply(SCIM, 0, false);
		assertNull(alerts.bannerText());
	}
}
