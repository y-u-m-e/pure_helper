package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@Slf4j
@PluginDescriptor(name = "Pure Helper")
public class PureHelperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PureHelperConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DangerOverlay dangerOverlay;

	@Inject
	private Alerts alerts;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private TooltipManager tooltipManager;

	private volatile Build build = Build.EMPTY;
	private CombatGuard combatGuard;
	private LampGuard lampGuard;
	private LampOverlay lampOverlay;
	private QuestGuard questGuard;
	private QuestTooltipOverlay questTooltipOverlay;

	@Override
	protected void startUp()
	{
		overlayManager.add(dangerOverlay);
		combatGuard = new CombatGuard(client, config, alerts, () -> build, (id, hidden) ->
		{
			Widget w = client.getWidget(id);
			if (w != null)
			{
				w.setHidden(hidden);
			}
		});
		lampGuard = new LampGuard(client, config, alerts, () -> build);
		lampOverlay = new LampOverlay(client, config, () -> build);
		overlayManager.add(lampOverlay);
		questGuard = new QuestGuard(client, config, () -> build);
		questTooltipOverlay = new QuestTooltipOverlay(client, tooltipManager, questGuard);
		overlayManager.add(questTooltipOverlay);
		final QuestGuard qg = questGuard;
		executor.execute(() ->
		{
			try (Reader quests = new InputStreamReader(getClass().getResourceAsStream("/quest-filter-rules.json"), StandardCharsets.UTF_8);
				Reader diaries = new InputStreamReader(getClass().getResourceAsStream("/achievement-diary-rules.json"), StandardCharsets.UTF_8))
			{
				qg.setRules(RewardRules.load(gson, quests, diaries));
			}
			catch (Exception ex)
			{
				log.warn("Could not load reward rules", ex);
				return;
			}
			clientThread.invoke(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					qg.refreshFinished();
				}
			});
		});
		clientThread.invoke(this::rebuildAndRefresh);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(dangerOverlay);
		overlayManager.remove(lampOverlay);
		overlayManager.remove(questTooltipOverlay);
		final CombatGuard g = combatGuard;
		final QuestGuard qg = questGuard;
		clientThread.invoke(() ->
		{
			g.reset();
			qg.reset();
			alerts.reset();
		});
	}

	private void rebuild()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			build = Build.EMPTY;
			return;
		}
		int[] caps = {config.attackCap(), config.strengthCap(), config.defenceCap(), config.hitpointsCap(),
			config.rangedCap(), config.magicCap(), config.prayerCap()};
		Map<CombatSkill, Integer> capMap = new EnumMap<>(CombatSkill.class);
		Map<CombatSkill, Integer> xpMap = new EnumMap<>(CombatSkill.class);
		for (CombatSkill s : CombatSkill.values())
		{
			capMap.put(s, caps[s.ordinal()]); // enum order matches the array above
			xpMap.put(s, client.getSkillExperience(s.skill()));
		}
		build = Build.of(capMap, xpMap);
	}

	private void rebuildAndRefresh()
	{
		rebuild();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			combatGuard.refresh();
			questGuard.refresh();
		}
		else
		{
			combatGuard.reset();
			questGuard.reset();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		if (e.getGameState() == GameState.LOGGED_IN || e.getGameState() == GameState.LOGIN_SCREEN)
		{
			rebuildAndRefresh();
			if (e.getGameState() == GameState.LOGGED_IN)
			{
				questGuard.refreshFinished();
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged e)
	{
		rebuildAndRefresh();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if ("purehelper".equals(e.getGroup()))
		{
			rebuildAndRefresh();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		if (e.getScriptId() == ScriptID.COMBAT_INTERFACE_SETUP)
		{
			combatGuard.refresh();
		}
		else if (e.getScriptId() == ScriptID.QUESTLIST_INIT)
		{
			questGuard.refreshFinished();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (e.getVarpId() == VarPlayerID.COM_MODE
			|| e.getVarbitId() == VarbitID.COMBAT_WEAPON_CATEGORY
			|| e.getVarbitId() == VarbitID.AUTOCAST_DEFMODE)
		{
			combatGuard.refresh();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick e)
	{
		combatGuard.onClientTick();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		lampGuard.onMenuOptionClicked(e);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded e)
	{
		if (e.getGroupId() == InterfaceID.XPREWARD)
		{
			lampGuard.clearSelection();
		}
		else if (e.getGroupId() == InterfaceID.QUESTLIST)
		{
			questGuard.refreshFinished();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed e)
	{
		if (e.getGroupId() == InterfaceID.XPREWARD)
		{
			lampGuard.clearSelection();
		}
	}

	@Provides
	PureHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PureHelperConfig.class);
	}
}
