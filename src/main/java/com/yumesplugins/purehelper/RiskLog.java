package com.yumesplugins.purehelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class RiskLog
{
	private static final int MAX = 50;

	public static final class Entry
	{
		public final Instant time;
		public final String guard;
		public final String what;
		public final boolean blocked;
		public final boolean overridden;

		public Entry(Instant time, String guard, String what, boolean blocked, boolean overridden)
		{
			this.time = time;
			this.guard = guard;
			this.what = what;
			this.blocked = blocked;
			this.overridden = overridden;
		}
	}

	private final LinkedList<Entry> entries = new LinkedList<>();

	public synchronized void add(Entry e)
	{
		entries.addFirst(e);
		if (entries.size() > MAX)
		{
			entries.removeLast();
		}
	}

	/** Newest first, at most 50. */
	public synchronized List<Entry> snapshot()
	{
		return Collections.unmodifiableList(new ArrayList<>(entries));
	}

	/** Dangerous states that actually happened: anything not blocked. */
	public synchronized int slips()
	{
		int n = 0;
		for (Entry e : entries)
		{
			if (!e.blocked)
			{
				n++;
			}
		}
		return n;
	}
}
