package com.yumesplugins.purehelper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.Notification;

/** One alert fanned out to banner, chat and notification; guards call record() once per catch. */
@Singleton
public class Alerts
{
	private static final Duration FLASH = Duration.ofSeconds(3);

	private final Notifier notifier;
	private final ChatMessageManager chatMessageManager;
	private final PureHelperConfig config;
	private final RiskLog riskLog;
	private final Clock clock;

	private final Map<String, String> persistent = new LinkedHashMap<>();
	private String flashText;
	private Instant flashUntil = Instant.MIN;

	@Inject
	Alerts(Notifier notifier, ChatMessageManager chatMessageManager, PureHelperConfig config, RiskLog riskLog)
	{
		this(notifier, chatMessageManager, config, riskLog, Clock.systemUTC());
	}

	Alerts(Notifier notifier, ChatMessageManager chatMessageManager, PureHelperConfig config, RiskLog riskLog, Clock clock)
	{
		this.notifier = notifier;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
		this.riskLog = riskLog;
		this.clock = clock;
	}

	public void record(String guard, String what, boolean blocked, boolean overridden)
	{
		riskLog.add(new RiskLog.Entry(clock.instant(), guard, what, blocked, overridden));
	}

	public synchronized void persistent(String key, String text)
	{
		persistent.put(key, text);
	}

	public synchronized void clear(String key)
	{
		persistent.remove(key);
	}

	public synchronized void flash(String text)
	{
		flashText = text;
		flashUntil = clock.instant().plus(FLASH);
	}

	public void notify(Notification n, String text)
	{
		notifier.notify(n, text);
	}

	public void chat(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder().append(config.accentColor(), text).build())
			.build());
	}

	/** Text for the banner, or null when there is nothing to show. */
	public synchronized String bannerText()
	{
		if (!config.dangerBanner())
		{
			return null;
		}
		if (flashText != null && clock.instant().isBefore(flashUntil))
		{
			return flashText;
		}
		String last = null;
		for (String t : persistent.values())
		{
			last = t;
		}
		return last;
	}
}
