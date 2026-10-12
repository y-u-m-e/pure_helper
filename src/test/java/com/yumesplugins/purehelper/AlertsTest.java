package com.yumesplugins.purehelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

public class AlertsTest
{
	private static class TestClock extends Clock
	{
		Instant now = Instant.EPOCH;

		@Override
		public ZoneOffset getZone()
		{
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone)
		{
			return this;
		}

		@Override
		public Instant instant()
		{
			return now;
		}
	}

	@Test
	public void bannerPriorityAndExpiry()
	{
		TestClock clock = new TestClock();
		Alerts alerts = new Alerts(null, null, new PureHelperConfig() { }, new RiskLog(), clock);
		assertNull(alerts.bannerText());

		alerts.persistent("lamp", "LAMP");
		assertEquals("LAMP", alerts.bannerText());

		alerts.flash("STYLE");
		assertEquals("STYLE", alerts.bannerText());

		clock.now = clock.now.plus(Duration.ofSeconds(4));
		assertEquals("LAMP", alerts.bannerText());

		alerts.clear("lamp");
		assertNull(alerts.bannerText());
	}
}
