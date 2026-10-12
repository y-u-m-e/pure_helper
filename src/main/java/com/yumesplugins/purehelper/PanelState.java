package com.yumesplugins.purehelper;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Value;

/** Everything the panel shows, built on the client thread. Immutable and comparable, so unchanged states are not pushed. */
@Value
public class PanelState
{
	static final String EMPTY_LOG = "Nothing caught yet.";
	private static final Duration RECENT = Duration.ofSeconds(5);
	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

	String heroTitle;
	String heroSub;
	boolean heroDanger;
	List<Tile> tiles;
	String offLimitsHeader;
	List<OffLimit> offLimits;
	/** Newest first. */
	List<LogRow> log;

	@Value
	public static class Tile
	{
		CombatSkill skill;
		int level;
		int cap;
		boolean guarded;
		int xpLeft;
		/** XP over the ceiling, 0..1; 0 when unguarded. */
		double progress;
		boolean locked;
		/** Within 10% of the cap, or at it. */
		boolean near;

		public String tooltip()
		{
			return guarded
				? "Level " + level + " / cap " + cap + " — " + String.format(Locale.ROOT, "%,d", xpLeft) + " XP left"
				: "Level " + level + " — not protected";
		}
	}

	@Value
	public static class OffLimit
	{
		String name;
		String reason;

		public String text()
		{
			return name + " — " + reason;
		}
	}

	@Value
	public static class LogRow
	{
		String time;
		String guard;
		String what;
		boolean blocked;
		boolean overridden;

		public String text()
		{
			return time + "  " + guard + "  " + what + (overridden ? " ⇧" : "");
		}
	}

	public static PanelState of(Build build, List<RiskLog.Entry> log, List<RewardRules.Breach> offLimits, int sessionSlips, Instant now)
	{
		String title;
		String sub;
		boolean danger = true;
		Optional<CombatSkill> near = build.nearestCap();
		if (!log.isEmpty() && Duration.between(log.get(0).time, now).compareTo(RECENT) < 0)
		{
			title = log.get(0).what.toUpperCase(Locale.ROOT);
			sub = log.get(0).guard + " · just now";
		}
		else if (near.isPresent())
		{
			CombatSkill s = near.get();
			title = s.name() + " " + build.level(s) + "/" + build.cap(s);
			sub = String.format(Locale.ROOT, "%,d", build.xpLeft(s)) + " XP left";
		}
		else
		{
			title = "BUILD INTACT";
			sub = sessionSlips + (sessionSlips == 1 ? " slip" : " slips") + " this session";
			danger = false;
		}

		List<Tile> tiles = new ArrayList<>();
		for (CombatSkill s : CombatSkill.values())
		{
			boolean guarded = build.protects(s);
			double progress = guarded ? Math.min(1.0, Math.max(0.0, (double) build.xp(s) / build.ceiling(s))) : 0;
			tiles.add(new Tile(s, build.level(s), build.cap(s), guarded, build.xpLeft(s), progress, build.atCap(s), build.nearCap(s)));
		}

		long quests = offLimits.stream().filter(b -> b.getEntry().getKind() == RewardRules.Kind.QUEST).count();
		long tiers = offLimits.size() - quests;
		String header = quests + (quests == 1 ? " quest" : " quests") + " · " + tiers + (tiers == 1 ? " diary tier" : " diary tiers");
		List<OffLimit> lines = offLimits.stream()
			.sorted(Comparator.comparingLong(RewardRules.Breach::getOvershoot).reversed())
			.map(b -> new OffLimit(b.getEntry().getName(), b.reason()))
			.collect(Collectors.toList());

		ZoneId zone = ZoneId.systemDefault();
		List<LogRow> rows = log.stream()
			.map(e -> new LogRow(HH_MM.format(e.time.atZone(zone)), e.guard, e.what, e.blocked, e.overridden))
			.collect(Collectors.toList());

		return new PanelState(title, sub, danger, Collections.unmodifiableList(tiles), header,
			Collections.unmodifiableList(lines), Collections.unmodifiableList(rows));
	}

	public List<String> offLimitsLines()
	{
		return offLimits.stream().map(OffLimit::text).collect(Collectors.toList());
	}

	public List<String> logLines()
	{
		return log.isEmpty() ? Collections.singletonList(EMPTY_LOG) : log.stream().map(LogRow::text).collect(Collectors.toList());
	}
}
