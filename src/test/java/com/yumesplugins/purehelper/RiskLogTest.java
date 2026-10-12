package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class RiskLogTest
{
	private static RiskLog.Entry entry(String what, boolean blocked, boolean overridden)
	{
		return new RiskLog.Entry(Instant.now(), "combat", what, blocked, overridden);
	}

	@Test
	public void keepsNewestFifty()
	{
		RiskLog log = new RiskLog();
		for (int i = 1; i <= 60; i++)
		{
			log.add(entry("e" + i, false, false));
		}
		assertEquals(50, log.snapshot().size());
		assertEquals("e60", log.snapshot().get(0).what);
	}

	@Test
	public void sessionSlipsCountsOverrides()
	{
		RiskLog log = new RiskLog();
		log.add(entry("blocked", true, false));
		assertEquals(0, log.sessionSlips());
		log.add(entry("overridden", false, true));
		assertEquals(1, log.sessionSlips());
	}

	@Test
	public void sessionSlipsOutliveTheFiftyEntryCap()
	{
		RiskLog log = new RiskLog();
		for (int i = 1; i <= 60; i++)
		{
			log.add(entry("e" + i, false, false));
		}
		assertEquals(60, log.sessionSlips());
	}
}
