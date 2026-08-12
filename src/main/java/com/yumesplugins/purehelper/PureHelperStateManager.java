package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Persists lightweight, non-configuration UI state for the informational sidebar
 * (currently just the collapse state of the "not doable" lists).
 */
@Slf4j
@Singleton
class PureHelperStateManager
{
	static final int STATE_SCHEMA_VERSION = 2;
	private static final Path STATE_FILE = Path.of(
		System.getProperty("user.home"),
		".runelite",
		"pure-helper-state.json");

	private final Gson gson;
	private State state;
	private boolean loaded;

	@Inject
	PureHelperStateManager(Gson gson)
	{
		this.gson = gson.newBuilder().setPrettyPrinting().create();
	}

	synchronized boolean isCollapseNotDoableQuestList()
	{
		ensureLoaded();
		return state.collapseNotDoableQuestList;
	}

	synchronized void setCollapseNotDoableQuestList(boolean collapseNotDoableQuestList)
	{
		ensureLoaded();
		state.collapseNotDoableQuestList = collapseNotDoableQuestList;
		saveState();
	}

	synchronized boolean isCollapseNotDoableDiaryList()
	{
		ensureLoaded();
		return state.collapseNotDoableDiaryList;
	}

	synchronized void setCollapseNotDoableDiaryList(boolean collapseNotDoableDiaryList)
	{
		ensureLoaded();
		state.collapseNotDoableDiaryList = collapseNotDoableDiaryList;
		saveState();
	}

	private void ensureLoaded()
	{
		if (loaded)
		{
			return;
		}
		loaded = true;

		state = new State();
		if (!Files.exists(STATE_FILE))
		{
			return;
		}

		try (Reader reader = Files.newBufferedReader(STATE_FILE, StandardCharsets.UTF_8))
		{
			State loadedState = gson.fromJson(reader, State.class);
			if (loadedState != null)
			{
				state = loadedState;
				if (state.schemaVersion <= 0)
				{
					state.schemaVersion = STATE_SCHEMA_VERSION;
				}
			}
		}
		catch (Exception ex)
		{
			log.warn("Failed loading Pure Helper state file: {}", STATE_FILE, ex);
			state = new State();
		}
	}

	private void saveState()
	{
		try
		{
			Files.createDirectories(STATE_FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(STATE_FILE, StandardCharsets.UTF_8))
			{
				gson.toJson(state, writer);
			}
		}
		catch (IOException ex)
		{
			log.warn("Failed saving Pure Helper state file: {}", STATE_FILE, ex);
		}
	}

	private static class State
	{
		private int schemaVersion = STATE_SCHEMA_VERSION;
		private boolean collapseNotDoableQuestList = true;
		private boolean collapseNotDoableDiaryList = true;
	}
}
