package com.yumesplugins.purehelper;

import net.runelite.api.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

/** Tints quests (and lists quests and diary tiers) whose fixed XP would break the build. Warns only; never blocks. */
public class QuestGuard
{
	private final Client client;
	private final PureHelperConfig config;
	private final Supplier<Build> build;

	private volatile RewardRules rules;
	private volatile List<RewardRules.Breach> offLimits = Collections.emptyList();
	private volatile Map<String, RewardRules.Breach> tints = Collections.emptyMap();
	private Set<String> finished = Collections.emptySet(); // client thread only
	/** Tinted widget key -> reason; read by the overlay on the render thread. */
	private final Map<Long, String> reasons = new ConcurrentHashMap<>();
	private final Map<Long, Integer> originalColors = new HashMap<>(); // client thread only

	QuestGuard(Client client, PureHelperConfig config, Supplier<Build> build)
	{
		this.client = client;
		this.config = config;
		this.build = build;
	}

	static Map<String, RewardRules.Breach> tints(List<RewardRules.Breach> breaches)
	{
		Map<String, RewardRules.Breach> out = new HashMap<>();
		for (RewardRules.Breach b : breaches)
		{
			out.put(b.getEntry().getName(), b);
		}
		return out;
	}

	static boolean shouldTint(boolean enabled, Build build)
	{
		return enabled && build != Build.EMPTY;
	}

	/** Colour to restore a row to, or null when the game has since re-coloured it (or we stored nothing). */
	static Integer restoreColor(int currentColor, int accentRgb, Integer original)
	{
		return original != null && currentColor == accentRgb ? original : null;
	}

	void setRules(RewardRules rules)
	{
		this.rules = rules;
	}

	/** Immutable snapshot; safe to read from Swing. */
	public List<RewardRules.Breach> offLimits()
	{
		return offLimits;
	}

	/** Client thread. Recompute the finished-quest set (runs a script per quest), then {@link #refresh()}. */
	void refreshFinished()
	{
		// every caller is a list (re)build: the game has re-coloured the rows, so stored colours are stale
		originalColors.clear();
		reasons.clear();
		RewardRules r = rules;
		if (r != null)
		{
			Set<String> names = new HashSet<>();
			for (RewardRules.Entry e : r.quests())
			{
				names.add(e.getName());
			}
			Set<String> done = new HashSet<>();
			for (Quest q : Quest.values())
			{
				if (names.contains(q.getName()) && q.getState(client) == QuestState.FINISHED)
				{
					done.add(q.getName());
				}
			}
			finished = done;
		}
		refresh();
	}

	/** Client thread. Recompute breaches from the cached finished set and re-tint. */
	void refresh()
	{
		RewardRules r = rules;
		if (r == null)
		{
			return;
		}
		Build b = build.get();
		List<RewardRules.Breach> quests = RewardRules.breaches(r.quests(), b, finished);
		List<RewardRules.Breach> all = new ArrayList<>(quests);
		// shortcut: diary completion is not checked, so finished tiers still show; add varbit checks if players ask
		all.addAll(RewardRules.breaches(r.diaryTiers(), b, Collections.emptySet()));
		offLimits = Collections.unmodifiableList(all);
		tints = shouldTint(config.questGuard(), b) ? tints(quests) : Collections.emptyMap();
		tintQuestList();
	}

	/** Client thread. Tint matching quest-list rows; restore rows that no longer match. */
	void tintQuestList()
	{
		Widget list = client.getWidget(InterfaceID.Questlist.LIST);
		if (list == null)
		{
			return;
		}
		List<Widget> texts = new ArrayList<>();
		collectTextWidgets(list, texts);
		Map<String, RewardRules.Breach> t = tints;
		int accent = config.accentColor().getRGB() & 0xFFFFFF;
		for (Widget w : texts)
		{
			long key = key(w);
			RewardRules.Breach breach = t.get(extractQuestName(w.getText()));
			if (breach != null)
			{
				if (w.getTextColor() != accent)
				{
					originalColors.put(key, w.getTextColor());
				}
				w.setTextColor(accent);
				reasons.put(key, breach.reason());
			}
			else
			{
				Integer restore = restoreColor(w.getTextColor(), accent, originalColors.remove(key));
				if (restore != null)
				{
					w.setTextColor(restore);
				}
				reasons.remove(key);
			}
		}
	}

	/** Reason of the tinted row under the mouse, or null. */
	String reasonAt(Point mouse)
	{
		Widget list = client.getWidget(InterfaceID.Questlist.LIST);
		if (mouse == null || reasons.isEmpty() || list == null || list.isHidden())
		{
			return null;
		}
		List<Widget> texts = new ArrayList<>();
		collectTextWidgets(list, texts);
		for (Widget w : texts)
		{
			String reason = reasons.get(key(w));
			if (reason != null && !w.isHidden() && w.getBounds().contains(mouse.getX(), mouse.getY()))
			{
				return reason;
			}
		}
		return null;
	}

	/** Client thread. Restore original colours and clear state. */
	void reset()
	{
		tints = Collections.emptyMap();
		Widget list = client.getWidget(InterfaceID.Questlist.LIST);
		if (list != null)
		{
			List<Widget> texts = new ArrayList<>();
			collectTextWidgets(list, texts);
			int accent = config.accentColor().getRGB() & 0xFFFFFF;
			for (Widget w : texts)
			{
				Integer restore = restoreColor(w.getTextColor(), accent, originalColors.get(key(w)));
				if (restore != null)
				{
					w.setTextColor(restore);
				}
			}
		}
		originalColors.clear();
		reasons.clear();
		offLimits = Collections.emptyList();
		finished = Collections.emptySet();
	}

	private static long key(Widget w)
	{
		return ((long) w.getId() << 32) | w.getIndex();
	}

	private static String extractQuestName(String text)
	{
		return text == null ? "" : text.replaceAll("<[^>]+>", "").trim();
	}

	private static void collectTextWidgets(Widget root, List<Widget> out)
	{
		if (root == null)
		{
			return;
		}
		if (root.getType() == WidgetType.TEXT)
		{
			out.add(root);
		}
		for (Widget[] children : new Widget[][]{root.getDynamicChildren(), root.getStaticChildren(), root.getNestedChildren()})
		{
			if (children != null)
			{
				for (Widget c : children)
				{
					collectTextWidgets(c, out);
				}
			}
		}
	}
}
