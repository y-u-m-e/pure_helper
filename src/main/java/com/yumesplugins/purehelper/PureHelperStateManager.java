package com.yumesplugins.purehelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class PureHelperStateManager
{
	static final int STATE_SCHEMA_VERSION = 1;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path STATE_FILE = Path.of(
		System.getProperty("user.home"),
		".runelite",
		"pure-helper-state.json");

	private State state;
	private boolean loaded;

	synchronized void seedSkillStateIfMissing(String avoidedSkillsCsv, String protectedSkillCapsCsv)
	{
		ensureLoaded();
		boolean changed = false;
		if (isBlank(state.avoidedSkillsCsv) && !isBlank(avoidedSkillsCsv))
		{
			state.avoidedSkillsCsv = avoidedSkillsCsv;
			changed = true;
		}
		if (isBlank(state.protectedSkillCapsCsv) && !isBlank(protectedSkillCapsCsv))
		{
			state.protectedSkillCapsCsv = protectedSkillCapsCsv;
			changed = true;
		}
		if (changed)
		{
			saveState();
		}
	}

	synchronized String getAvoidedSkillsCsv()
	{
		ensureLoaded();
		return state.avoidedSkillsCsv == null ? "" : state.avoidedSkillsCsv;
	}

	synchronized String getProtectedSkillCapsCsv()
	{
		ensureLoaded();
		return state.protectedSkillCapsCsv == null ? "" : state.protectedSkillCapsCsv;
	}

	synchronized void setSkillState(String avoidedSkillsCsv, String protectedSkillCapsCsv)
	{
		ensureLoaded();
		state.avoidedSkillsCsv = avoidedSkillsCsv == null ? "" : avoidedSkillsCsv;
		state.protectedSkillCapsCsv = protectedSkillCapsCsv == null ? "" : protectedSkillCapsCsv;
		saveState();
	}

	synchronized SkillState getSkillStateOrFallback(String fallbackAvoidedSkillsCsv, String fallbackProtectedSkillCapsCsv)
	{
		ensureLoaded();
		String avoidedSkillsCsv = isBlank(state.avoidedSkillsCsv) ? fallbackAvoidedSkillsCsv : state.avoidedSkillsCsv;
		String protectedSkillCapsCsv = isBlank(state.protectedSkillCapsCsv) ? fallbackProtectedSkillCapsCsv : state.protectedSkillCapsCsv;
		return new SkillState(
			avoidedSkillsCsv == null ? "" : avoidedSkillsCsv,
			protectedSkillCapsCsv == null ? "" : protectedSkillCapsCsv);
	}

	synchronized boolean isCompactRows()
	{
		ensureLoaded();
		return state.compactRows;
	}

	synchronized void setCompactRows(boolean compactRows)
	{
		ensureLoaded();
		state.compactRows = compactRows;
		saveState();
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

	synchronized boolean isSkillSelectionLocked()
	{
		ensureLoaded();
		return state.skillSelectionLocked;
	}

	synchronized void setSkillSelectionLocked(boolean skillSelectionLocked)
	{
		ensureLoaded();
		state.skillSelectionLocked = skillSelectionLocked;
		saveState();
	}

	synchronized boolean isDiarySectionCollapsed(String diaryName, boolean defaultCollapsed)
	{
		ensureLoaded();
		if (diaryName == null || diaryName.isBlank())
		{
			return defaultCollapsed;
		}
		Boolean collapsed = state.collapsedDiarySections.get(diaryName);
		return collapsed == null ? defaultCollapsed : collapsed;
	}

	synchronized void setDiarySectionCollapsed(String diaryName, boolean collapsed)
	{
		if (diaryName == null || diaryName.isBlank())
		{
			return;
		}

		ensureLoaded();
		state.collapsedDiarySections.put(diaryName, collapsed);
		saveState();
	}

	synchronized void resetState(String fallbackAvoidedSkillsCsv, String fallbackProtectedSkillCapsCsv)
	{
		state = new State();
		state.avoidedSkillsCsv = fallbackAvoidedSkillsCsv == null ? "" : fallbackAvoidedSkillsCsv;
		state.protectedSkillCapsCsv = fallbackProtectedSkillCapsCsv == null ? "" : fallbackProtectedSkillCapsCsv;
		loaded = true;
		saveState();
	}

	synchronized void exportBuildPreset(Path outputFile, String profileName, String notes) throws IOException
	{
		ensureLoaded();
		BuildPresetFile presetFile = new BuildPresetFile();
		presetFile.schema = "pure-helper.build-preset.v1";
		presetFile.profileName = isBlank(profileName) ? "Custom build" : profileName;
		presetFile.notes = notes == null ? "" : notes;
		presetFile.avoidedSkillsCsv = state.avoidedSkillsCsv == null ? "" : state.avoidedSkillsCsv;
		presetFile.protectedSkillCapsCsv = state.protectedSkillCapsCsv == null ? "" : state.protectedSkillCapsCsv;

		Objects.requireNonNull(outputFile, "outputFile");
		Files.createDirectories(outputFile.getParent());
		try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8))
		{
			GSON.toJson(presetFile, writer);
		}
	}

	synchronized boolean importBuildPreset(Path inputFile) throws IOException
	{
		Objects.requireNonNull(inputFile, "inputFile");
		if (!Files.exists(inputFile))
		{
			return false;
		}

		try (Reader reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8))
		{
			BuildPresetFile imported = GSON.fromJson(reader, BuildPresetFile.class);
			if (imported == null)
			{
				return false;
			}
			if (!isBlank(imported.schema) && !imported.schema.startsWith("pure-helper.build-preset."))
			{
				return false;
			}

			ensureLoaded();
			state.avoidedSkillsCsv = imported.avoidedSkillsCsv == null ? "" : imported.avoidedSkillsCsv;
			state.protectedSkillCapsCsv = imported.protectedSkillCapsCsv == null ? "" : imported.protectedSkillCapsCsv;
			saveState();
			return true;
		}
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
			State loadedState = GSON.fromJson(reader, State.class);
			if (loadedState != null)
			{
				state = loadedState;
				if (state.schemaVersion <= 0)
				{
					state.schemaVersion = STATE_SCHEMA_VERSION;
				}
				if (state.collapsedDiarySections == null)
				{
					state.collapsedDiarySections = new HashMap<>();
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
				GSON.toJson(state, writer);
			}
		}
		catch (IOException ex)
		{
			log.warn("Failed saving Pure Helper state file: {}", STATE_FILE, ex);
		}
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.isBlank();
	}

	private static class State
	{
		private int schemaVersion = STATE_SCHEMA_VERSION;
		private String avoidedSkillsCsv = "";
		private String protectedSkillCapsCsv = "";
		private boolean compactRows;
		private boolean collapseNotDoableQuestList = true;
		private boolean collapseNotDoableDiaryList = true;
		private boolean skillSelectionLocked;
		private Map<String, Boolean> collapsedDiarySections = new HashMap<>();
	}

	static final class SkillState
	{
		final String avoidedSkillsCsv;
		final String protectedSkillCapsCsv;

		private SkillState(String avoidedSkillsCsv, String protectedSkillCapsCsv)
		{
			this.avoidedSkillsCsv = avoidedSkillsCsv;
			this.protectedSkillCapsCsv = protectedSkillCapsCsv;
		}
	}

	private static class BuildPresetFile
	{
		private String schema;
		private String profileName;
		private String notes;
		private String avoidedSkillsCsv;
		private String protectedSkillCapsCsv;
	}
}
