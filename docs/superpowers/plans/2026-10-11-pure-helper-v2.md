# Pure Helper v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace v1 with a guard-based plugin: combat-style, quest/diary and lamp guards over one cap model, on-screen alerts, and a single-page panel.

**Architecture:** `Build` (cap math), `StyleTable` and `RewardRules` are plain Java with no client calls and carry the unit tests. Each guard is one class: a static pure decision method (tested) plus thin event wiring. `Alerts` fans one alert out to banner, infobox, chat, notification and `RiskLog`. The panel only reads snapshots.

**Tech Stack:** Java 11 (`options.release.set(11)` — no records, no switch expressions, no text blocks), RuneLite client `latest.release`, Lombok, JUnit 4.12, Gson (injected).

**Spec:** `docs/superpowers/specs/2026-10-11-pure-helper-rewrite-design.md`

## Global Constraints

- Package `com.yumesplugins.purehelper`, one purpose per file. `@ConfigGroup("purehelper")`, new keys only.
- No network, no files written, no reflection, no `new Gson()` (inject `Gson`), no IO on the client thread.
- Gameval constants only (`InterfaceID.*`, `VarbitID.*`, `VarPlayerID.*`); widgets by component ID.
- Override everywhere is **holding Shift** (`client.isKeyPressed(KeyCode.KC_SHIFT)`).
- One accent colour (config, default `#E5484D`), used only for danger. Everything else `ColorScheme`.
- Panel 225 px wide (`PluginPanel`), 8 px grid, 10 px padding.
- `shutDown()` undoes every widget change and removes overlays/infoboxes.
- Commit after each task on `rewrite/v2` and push. No Hub PR in this plan — that is a separate, user-gated step.

## Review Focus

1. **Before login** (stats all 0, login screen) — nothing hides, tints or alerts. → Task 2 `Build.EMPTY` test, Task 6/7 decision tests with `EMPTY`.
2. **Cap raised to 99 mid-session** — hidden style buttons reappear without relogging. → Task 6 test `raisingCapUnhides`.
3. **Weapon with no style enum** (`-1`, not 22 or 30) — nothing hidden, no exception. → Task 3 test `unknownWeaponHasNoStyles`.
4. **Quest names that don't match the client's quest list** (apostrophes, subquests) — still listed in Off-limits, never crash the tinting. → Task 4 test `everyQuestNameIsKnownOrAllowlisted`.
5. **Plugin turned off while a button is hidden** — combat tab fully restored. → Task 6 test `resetUnhidesAll` + Task 10 in-game check.

---

### Task 1: Clean slate and skeleton

**Files:**
- Delete: every file in `src/main/java/com/yumesplugins/purehelper/` except `PureHelperPlugin.java`, `PureHelperConfig.java` (both rewritten); `src/test/java/.../QuestRiskEvaluatorTest.java`; `src/main/resources/*_icon.png` (7 files); `src/main/resources/tab-icons/`
- Keep: `src/main/resources/quest-filter-rules.json`, `achievement-diary-rules.json`, `src/test/java/.../PureHelperPluginTest.java` (the `runClient` launcher)
- Rewrite: `PureHelperPlugin.java`, `PureHelperConfig.java`, `runelite-plugin.properties`
- Create: `src/test/java/com/yumesplugins/purehelper/HubRulesTest.java`, `HubPackagingTest.java` from `O:/nexus_workspace/.claude/skills/runelite-plugin/templates/` with `package PACKAGE;` → `package com.yumesplugins.purehelper;`

**Interfaces — Produces:**
- `PureHelperConfig` items (sections `caps`, `guards`, `alerts`, `look`):
  - `int attackCap()`, `strengthCap()`, `defenceCap()`, `hitpointsCap()`, `rangedCap()`, `magicCap()`, `prayerCap()` — `@Range(min = 1, max = 99)`; defaults 99 except `defenceCap` 1, `prayerCap` 1.
  - `boolean combatGuard()`, `questGuard()`, `lampGuard()` — default true.
  - `boolean dangerBanner()` default true; `Notification styleNotification()` default `Notification.ON` with flash; `Notification blockNotification()` default `Notification.OFF`.
  - `Color accentColor()` default `#E5484D`.
- `PureHelperPlugin`: lifecycle only for now; `@Provides PureHelperConfig`.

- [ ] **Step 1:** Delete the files listed above; write the skeleton plugin and config.
- [ ] **Step 2:** `runelite-plugin.properties` description: `Guards pure builds: hides combat styles, blocks XP lamps and flags quests that would level a protected skill.`; tags `pure,pvp,combat,quest,lamp`.
- [ ] **Step 3:** Run `./gradlew build`. Expected: BUILD SUCCESSFUL, `HubRulesTest` and `HubPackagingTest` pass.
- [ ] **Step 4:** Commit `Start v2: remove v1 sources, add config skeleton and Hub tests.`

### Task 2: `Build`

**Files:** Create `Build.java`; Test `BuildTest.java`

**Interfaces — Produces:**
- `enum CombatSkill { ATTACK, STRENGTH, DEFENCE, HITPOINTS, RANGED, MAGIC, PRAYER; Skill skill(); }`  (nested in `Build` or own file — own file `CombatSkill.java`)
- `final class Build`:
  - `static final Build EMPTY` — allows everything, `atCap` false for all (not logged in).
  - `static Build of(Map<CombatSkill,Integer> caps, Map<CombatSkill,Integer> xp)`
  - `int cap(CombatSkill)`, `int ceiling(CombatSkill)` (`Integer.MAX_VALUE` when cap 99), `boolean protects(CombatSkill)` (cap < 99), `boolean atCap(CombatSkill)`, `boolean allows(CombatSkill, int xpGain)`, `int xpLeft(CombatSkill)`, `int level(CombatSkill)`
  - `Optional<CombatSkill> nearestCap()` — protected, not at cap, within 10% of the ceiling's XP span; smallest `xpLeft` wins.
- Uses `net.runelite.api.Experience.getXpForLevel` / `getLevelForXp` (static, no client).

- [ ] **Step 1: Failing tests** `BuildTest`:
  - `ceilingIsLastXpBeforeNextLevel`: cap 1 → 82; cap 2 → 173; cap 98 → `getXpForLevel(99) - 1` = 13034430; cap 99 → `Integer.MAX_VALUE`.
  - `allowsUpToCeilingOnly`: defence cap 1, xp 0 → `allows(DEFENCE, 82)` true, `allows(DEFENCE, 83)` false.
  - `atCapWhenXpAtCeiling`: attack cap 60, xp at level 50 → false; xp = `ceiling` → true.
  - `emptyBuildAllowsEverything`: `Build.EMPTY.atCap(x)` false and `allows(x, 1_000_000)` true for every skill.
  - `nearestCapPicksSmallestXpLeft`: attack cap 60 at 95% of span, strength cap 70 at 99% → STRENGTH.
- [ ] **Step 2:** `./gradlew test --tests '*BuildTest'` → FAIL (class missing).
- [ ] **Step 3:** Implement `CombatSkill`, `Build`.
- [ ] **Step 4:** Same command → PASS.
- [ ] **Step 5:** Commit `Add Build cap model.`

### Task 3: `StyleTable`

**Files:** Create `StyleTable.java`; Test `StyleTableTest.java`

**Interfaces — Produces:**
- `enum Style { ACCURATE, AGGRESSIVE, DEFENSIVE, CONTROLLED, RANGING, LONGRANGE, CASTING, DEFENSIVE_CASTING, OTHER; Set<CombatSkill> trains(); }` — every non-OTHER style also trains HITPOINTS. ACCURATE→ATTACK, AGGRESSIVE→STRENGTH, DEFENSIVE→DEFENCE, CONTROLLED→ATTACK+STRENGTH+DEFENCE, RANGING→RANGED, LONGRANGE→RANGED+DEFENCE, CASTING→MAGIC, DEFENSIVE_CASTING→MAGIC+DEFENCE.
- `static Style fromStructName(String)` — trim, spaces→`_`, upper-case, `valueOf`; anything else OTHER.
- `static Style[] slots(int weaponCategory, List<String> structNames)` — index = combat tab slot; slot 5 named "Defensive" becomes DEFENSIVE_CASTING. `structNames` empty (enum −1) → fallbacks copied from v1: category 22 → `{ACCURATE, AGGRESSIVE, null, DEFENSIVE, CASTING, DEFENSIVE_CASTING}`, category 30 → `{ACCURATE, AGGRESSIVE, AGGRESSIVE, DEFENSIVE}`, else empty array.
- `static int activeSlot(int comMode, int autocastDefMode)` — `comMode == 4 ? 4 + autocastDefMode : comMode`.
- `static final int[][] SLOT_COMPONENTS` — slot → `InterfaceID.CombatInterface` components: 0 `_0`; 1 `_1`; 2 `_2`; 3 `_3`; 4 `AUTOCAST_BUTTONS`; 5 `AUTOCAST_DEFENSIVE`, `DEFENSIVE_CONTAINER_GRAPHIC0`, `DEFENSIVE_CONTAINER_GRAPHIC1`, `DEFENSIVE_CONTAINER_TEXT2`.

The client-side read (`EnumID.WEAPON_STYLES` → struct `ParamID.ATTACK_STYLE_NAME`) lives in `CombatGuard` (Task 6) and passes names in.

- [ ] **Step 1: Failing tests:** `everyStyleTrainsHitpoints`; `longrangeTrainsDefence`; `slotFiveDefensiveIsDefensiveCasting`; `unknownWeaponHasNoStyles` (`slots(99, emptyList())` length 0); `staffFallback` (category 22 slot 2 null); `activeSlotAddsDefensiveAutocast` (`activeSlot(4,1)` = 5, `activeSlot(1,1)` = 1); `unknownNameIsOther`.
- [ ] **Step 2:** run `--tests '*StyleTableTest'` → FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** → PASS.
- [ ] **Step 5:** Commit `Add StyleTable.`

### Task 4: `RewardRules`

**Files:** Create `RewardRules.java`; Test `RewardRulesTest.java`

**Interfaces — Produces:**
- `final class RewardRules`:
  - `static RewardRules load(Gson gson, Reader quests, Reader diaries)` — parses the two bundled files; ignores skills outside `CombatSkill`; keeps only `fixedRewards`.
  - `static final class Entry { String name; Kind kind /* QUEST, DIARY */; Map<CombatSkill,Integer> fixedXp; }`
  - `List<Entry> quests()`, `List<Entry> diaryTiers()` (name `"<Diary> <Tier>"`, e.g. `"Ardougne Hard"`).
  - `static List<Breach> breaches(List<Entry> entries, Build build, Set<String> excludedNames)` — entries with any `fixedXp` failing `build.allows`; `Breach { Entry entry; CombatSkill worst; int xp; }`, sorted by `xp` beyond ceiling, largest first; reason text `"+2,000 Defence XP"` from `Breach.reason()`.
- Loaded once in `startUp` on the plugin's executor (`ScheduledExecutorService` injected), never on the client thread.

- [ ] **Step 1: Failing tests:**
  - `loadsAllBundledEntries`: 157 quests, 48 diary tiers (12 × 4).
  - `aSoulsBaneIsADefenceBreachOnA1Def`: Build defence cap 1 → breaches contain "A Soul's Bane", worst DEFENCE, reason `"+500 Defence XP"`.
  - `choiceRewardsAreIgnored`: "A Tail of Two Cats" never appears in breaches.
  - `excludedNamesAreSkipped`.
  - `everyQuestNameIsKnownOrAllowlisted`: every quest name equals some `net.runelite.api.Quest.getName()`, or is in a `KNOWN_UNMATCHED` set written in the test with the mismatches the run reveals.
- [ ] **Step 2:** run `--tests '*RewardRulesTest'` → FAIL.
- [ ] **Step 3:** Implement with Gson's `JsonParser`/model classes, `Reader` via `getResourceAsStream` in the plugin.
- [ ] **Step 4:** → PASS. Fill `KNOWN_UNMATCHED` from the failure output only if the names are genuinely different spellings; fix the JSON if they are typos.
- [ ] **Step 5:** Commit `Add RewardRules loader.`

### Task 5: `RiskLog`, `Alerts`, `DangerOverlay`

**Files:** Create `RiskLog.java`, `Alerts.java`, `DangerOverlay.java`; Test `RiskLogTest.java`. Update spec code-layout table with `Alerts` (one line).

**Interfaces — Produces:**
- `RiskLog`: `synchronized void add(Entry e)`; `synchronized List<Entry> snapshot()` (newest first, max 50); `int slips()` (entries where a dangerous state happened, overridden or not); `Entry { Instant time; String guard; String what; boolean overridden; }`.
- `Alerts` (singleton, injected): `void persistent(String key, String text)` / `void clear(String key)` — banner stays until cleared; `void flash(String text)` — banner ~3 s; `void notify(Notification n, String text)` via `Notifier`; `void chat(String text)` via `ChatMessageManager` (`ChatMessageType.GAMEMESSAGE`, accent-coloured); every call that represents a catch also `log.add(...)`. Respects `dangerBanner()`.
- `DangerOverlay extends Overlay`: `OverlayPosition.TOP_CENTER`, accent-filled `PanelComponent`, bold text, renders the current banner text from `Alerts` or nothing.

- [ ] **Step 1: Failing tests** `RiskLogTest`: `keepsNewestFifty` (add 60 → snapshot size 50, first is the 60th); `slipsCountsOverrides`.
- [ ] **Step 2:** run → FAIL. **Step 3:** implement all three. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add risk log, alerts and danger banner.`

### Task 6: `CombatGuard`

**Files:** Create `CombatGuard.java`; Test `CombatGuardTest.java`; Modify `PureHelperPlugin.java` (subscribe + forward `VarbitChanged`, `ScriptPostFired` for the combat tab redraw script as v1 did, `ClientTick` for Shift edges, `ConfigChanged`, `StatChanged`).

**Interfaces:**
- Consumes: `Build`, `StyleTable`, `Alerts`.
- Produces:
  - `static Set<Integer> dangerousSlots(Style[] slots, Build build)` — slots whose `trains()` includes a skill with `build.atCap`.
  - `static Set<Integer> componentsToHide(Set<Integer> dangerous, boolean shiftHeld, boolean enabled)` — empty when Shift held or disabled.
  - Instance: `void refresh()` (client thread: read category, struct names, `activeSlot`; apply hiding; raise/clear the `"combat"` persistent banner with text `"<STYLE> STYLE — trains <Skill> (cap <n>)"`, notify `styleNotification()` once per entry into a dangerous style, chat line, log), `void reset()` (unhide all components it hid, clear banner).

- [ ] **Step 1: Failing tests:** `defensiveIsDangerousOnA1Def`; `controlledIsDangerousWhenAnyTrainedSkillCapped`; `nothingDangerousOnEmptyBuild`; `shiftRevealsEverything`; `raisingCapUnhides` (cap 1 → slot 3 dangerous; same xp with cap 99 → none); `resetUnhidesAll` (guard with a fake hide-sink records hidden IDs; `reset()` unhides each).
- [ ] **Step 2:** run → FAIL. **Step 3:** implement; widget hiding via `widget.setHidden`, tracking which IDs it hid. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add combat style guard.`

### Task 7: `LampGuard`

**Files:** Create `LampGuard.java`; Test `LampGuardTest.java`; Modify `PureHelperPlugin.java` (`MenuOptionClicked`, `WidgetLoaded`/`WidgetClosed` for group of `InterfaceID.Xpreward.UNIVERSE`).

**Interfaces:**
- Consumes: `Build`, `Alerts`.
- Produces:
  - `static final Map<Integer, CombatSkill> SKILL_COMPONENTS` — `Xpreward.ATTACK`, `STRENGTH`, `RANGED`, `MAGIC`, `DEFENCE`, `HITPOINTS`, `PRAYER` → skill.
  - `static boolean blocksConfirm(CombatSkill selected, Build build, boolean shiftHeld)` — `selected != null && build.protects(selected) && !shiftHeld`. Lamp XP amounts aren't on the interface, so any protected skill is blocked (stricter than the spec's `atCap` wording; spec updated in Step 3).
  - Instance: remembers last clicked skill component; on a `CONFIRM` click that `blocksConfirm` → `event.consume()`, `alerts.flash("BLOCKED — <Skill> is protected. Hold Shift to confirm.")`, chat, `blockNotification()`, log; Shift-confirm on a protected skill → log `overridden=true`. Draws a danger outline on protected skill components while the interface is open (overlay over the widget bounds).
- [ ] **Step 1: Failing tests:** `blocksProtectedSkill`; `allowsUnprotectedSkill`; `shiftOverrides`; `noSelectionDoesNotBlock`; `emptyBuildNeverBlocks`.
- [ ] **Step 2:** run → FAIL. **Step 3:** implement; edit spec LampGuard bullet to "any protected skill (cap < 99) — lamp amounts aren't shown". **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add lamp guard.`

### Task 8: `QuestGuard`

**Files:** Create `QuestGuard.java`, `QuestTooltipOverlay.java`; Test `QuestGuardTest.java`; Modify `PureHelperPlugin.java` (`WidgetLoaded` for `InterfaceID.QUESTLIST`, `ScriptPostFired` for the quest-list rebuild, `GameStateChanged` LOGGED_IN).

**Interfaces:**
- Consumes: `RewardRules`, `Build`.
- Produces:
  - `static Map<String, Breach> tints(List<Breach> breaches)` — by quest name.
  - Instance: `List<Breach> offLimits()` (quests not `QuestState.FINISHED` + diary tiers, for the panel), refreshed on login, quest-list load and `Build` change; `void tintQuestList()` (children of `InterfaceID.Questlist.LIST` whose text matches → accent colour); tooltip overlay shows `Breach.reason()` on hover; `reset()` restores original colours.
  - shortcut: diary tiers have no completion check — completed tiers still show; add varbit checks if players ask.
- [ ] **Step 1: Failing tests:** `tintsOnlyBreachingQuests`; `finishedQuestsExcludedFromOffLimits` (pass a finished-names set).
- [ ] **Step 2:** run → FAIL. **Step 3:** implement. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add quest and diary guard.`

### Task 9: Panel

**Files:** Create `PureHelperPanel.java`, `PanelState.java`; Test `PanelStateTest.java`; Modify `PureHelperPlugin.java` (`NavigationButton`, push state on `GameTick` when changed).

**Interfaces:**
- Consumes: `Build`, `RiskLog.snapshot()`, `QuestGuard.offLimits()`, `SkillIconManager`.
- Produces:
  - `PanelState` (immutable, built on the client thread): `String heroTitle`, `String heroSub`, `boolean heroDanger`, per-skill tiles (`level`, `cap`, `xpLeft`, `progress 0..1`, `locked`), off-limits counts and lines, log lines.
  - `static PanelState of(Build build, List<RiskLog.Entry> log, List<Breach> offLimits, Instant now)` — hero priority: an entry < 5 s old → that entry, danger; `build.nearestCap()` → `"ATTACK 58/60"` / `"12,430 XP left"`, danger; else `"BUILD INTACT"` / `"<n> slips this session"` (`"0 slips this session"`).
  - `PureHelperPanel extends PluginPanel`: `void render(PanelState)` on the EDT. Hero in `FontManager.getRunescapeBoldFont()` larger size; 2 rows of tiles (4 + 3), 2 px progress bar, lock badge + 60% alpha at cap, tooltip `"Level 58 / cap 60 — 12,430 XP left"`; Off-limits header line `"23 quests · 4 diary tiers ▸"` toggling a list; log rows `HH:mm  Guard  what`, `⇧` suffix when overridden; empty log `"Nothing caught yet."`.
- [ ] **Step 1: Failing tests:** `heroShowsIntactWhenSafe`; `heroShowsNearestCap`; `recentEntryTakesHero`; `emptyLogLine`.
- [ ] **Step 2:** run → FAIL. **Step 3:** implement `PanelState`, then the panel. **Step 4:** run → PASS.
- [ ] **Step 5:** Commit `Add single-page panel.`

### Task 10: In-game verification, README

**Files:** Modify `README.md` (v2 usage; "Quest and diary reward data compiled from the Old School RuneScape Wiki" with link); Modify `PureHelperPlugin.java` only for fixes found.

- [ ] **Step 1:** `./gradlew build` → all tests pass.
- [ ] **Step 2:** Credential switch per `.claude/rules/plugin-testing.md` (`sh jagex_acc_swtcr/jx.sh list`, ask the user, `use`), then `./gradlew runClient`.
- [ ] **Step 3:** Checklist with the user, screenshots where noted:
  - defensive style hidden on a 1 def; Shift shows it; picking it → banner stays, flash notification once, chat line, log entry ⇧
  - weapon switch onto a dangerous style → banner until switched back
  - lamp: protected skills outlined; Confirm blocked with banner; Shift+Confirm goes through and logs ⇧
  - quest list: breaching unfinished quests tinted; hover tooltip reason
  - cap raised to 99 in config → buttons reappear at once
  - plugin disabled → combat tab and quest list fully restored, banner gone
  - panel screenshots: three hero states, Off-limits collapsed/expanded, empty and full log
- [ ] **Step 4:** Fix findings, re-run Step 1, commit `Fix in-game findings.` and push `rewrite/v2`.
