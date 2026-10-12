# Pure Helper v2 — rewrite design

Date: 2026-10-11 · Branch: `rewrite/v2` (from `pr/pure-helper-final`) · Status: draft for review

## Why

The v1 code is hard to work in (a 1,088-line plugin class, a four-tab panel), the in-game
experience is wrong, the scope changes, and the plugin is not on the Plugin Hub. Hub PR #11252
was closed by us on 2026-03-28 while awaiting review — there is no reviewer feedback to design
around. v2 is a fresh rewrite in the same repo; v1 source is deleted, not refactored.

## Scope

| v1 feature | v2 |
|---|---|
| Combat style guard | kept, rebuilt |
| Quest / diary risk | kept, rebuilt; bundled JSON data kept |
| Risk log | kept, in-memory |
| Build presets | **dropped** |
| Four-tab panel | replaced by a single-page panel |
| — | **new:** lamp / reward guard |

Out of scope: non-combat skill caps, persisted history, build presets, any network access.

## Principles

- Warnings live in the game, on the dangerous thing. The panel is supporting information.
- **Block + override** where a single dangerous click exists; warn where it doesn't.
- One override everywhere: **hold Shift**. Each guard also has its own on/off toggle.
- Decision logic is plain Java with no RuneLite types, so it is unit-tested.

## 1. Build model

**Config** (`@ConfigGroup("purehelper")`, new keys; v1 keys are left orphaned and ignored):
a "Protected skills" section with a level cap per combat skill — Attack, Strength, Defence,
Hitpoints, Ranged, Magic, Prayer. Range 1–99; 99 means unprotected. Defaults: Defence 1,
Prayer 1, the rest 99.

**`Build`** — immutable, no RuneLite types. Built from the caps plus current XP per skill.

- `ceiling(skill)` = `xpForLevel(cap + 1) - 1` (99 → unbounded).
- `allows(skill, xpGain)` → `currentXp + xpGain <= ceiling`.
- `atCap(skill)` → `currentXp >= ceiling(skill)` — used where the gain size is unknown
  (a combat style gives XP indefinitely). Attack capped at 60 with level 50 is not at cap,
  so its styles stay available until 60.
- `xpLeft(skill)` for the panel.

Rebuilt on `StatChanged` and `ConfigChanged`; guards always read the current instance.
Nothing is written to disk besides RuneLite config.

## 2. Guards

Each guard is one class, asks the `Build`, and reports catches to `RiskLog`.

### CombatGuard

- Inputs: `VarPlayerID.COM_MODE`, `VarbitID.COMBAT_WEAPON_CATEGORY`, `VarbitID.AUTOCAST_DEFMODE`.
- `StyleTable` (pure data): (weapon category, style index, defensive autocast) → skills trained.
- **Block:** combat-tab style buttons whose skills include one `atCap` are hidden. Holding
  Shift shows them. Same mechanism as core's Attack Styles plugin. Stays on in the Wilderness
  and PvP worlds: the Hub's PvP rule covers Attack/Cast menu options on players, not the local
  combat-style buttons. Pre-answered in the PR body.
- **Warn:** if the active style is dangerous anyway (e.g. a weapon switch lands on a
  defensive style), an infobox plus one chat line, and a `RiskLog` entry.
- `shutDown()` unhides every component it hid.

### QuestGuard (quests and diaries)

- Data: existing `quest-filter-rules.json` (157 quests) and `achievement-diary-rules.json`
  (12 diaries × 4 tiers). New loader, off the client thread, at startup.
- Only **fixed** rewards count; choice rewards are the LampGuard's job.
- **Warn only:** in the quest list, quests whose fixed XP fails `Build.allows` are tinted
  and carry a tooltip with the reason (`+2,000 Defence XP`). There is no single "start quest"
  click to intercept — quests start through NPC-specific dialogue — so there is no block.
- Also feeds the panel's Off-limits section.

### LampGuard (lamps, books, diary reward choices)

- Watches the skill-choice interface used by XP lamps and books.
- **Block:** protected skills (`atCap`, or failing `allows` when the amount is known) get a
  danger mark. With one of them selected, the Confirm click is consumed and one chat line says
  why. Shift + Confirm goes through and is logged as an override.

## 3. Risk log and panel

**`RiskLog`** — in-memory ring of the last 50 entries: time, guard, what, overridden (bool).
Session only.

**Panel** — one page, no tabs, read-only (settings stay in RuneLite config). 225 px wide.

```
┌──────────────────────────────┐
│  BUILD INTACT                │  hero, changes with state
│  0 slips this session        │
│                              │
│ [Att][Str][Def][HP]          │  core skill icons, 2 rows
│ [Rng][Mag][Pra]              │  2px bar = progress to cap; lock badge at cap
│                              │
│ ── OFF-LIMITS ────────────── │
│  23 quests · 4 diary tiers  ▸│  collapsed by default
│ ── THIS SESSION ──────────── │
│  14:02  Combat  Defensive    │
│         blocked              │
│  13:47  Lamp  Prayer  ⇧      │  ⇧ = Shift override
└──────────────────────────────┘
```

- **Hero** is the most pressing live fact, in priority order: a guard that just fired
  (briefly) → a skill within 10% of its cap (`ATTACK 58/60 · 12,430 XP left`) →
  `BUILD INTACT` with the session slip count. Bold RuneScape font, the only large text.
- **Skill tiles** use core `SkillIconManager`; the seven bundled `*_icon.png` are deleted.
  At cap: lock badge and 60% opacity, so state reads in shape and brightness, not colour
  alone. Tooltip: level, cap, XP left.
- **One accent colour**, reserved for danger (block, slip, near-cap). Everything else is
  RuneLite `ColorScheme`.
- **Off-limits** expands to quests and diary tiers sorted by severity, one line each with
  the reason.
- **Empty log** is one muted line: "Nothing caught yet."
- 8 px grid, 10 px panel padding (matches core panels such as XP Tracker). Section labels
  small, grey, uppercase, with a rule.
- Done means it looks right in game: screenshots of all three hero states, collapsed and
  expanded Off-limits, empty and full log.

## 4. Testing and Hub

**Unit tests** (JUnit, no client):

- `Build`: ceiling at caps 1, 2, 98, 99; `allows` at exactly the ceiling and one past it.
- `StyleTable`: every weapon category maps to the expected skills, including defensive autocast.
- Rule loader: every bundled quest and diary entry parses; fixed vs choice rewards split.
- Guard decisions as pure functions (style dangerous? lamp confirm blocked? override?).
- `HubRulesTest` and `HubPackagingTest` from the `runelite-plugin` skill templates.

**In game:** `gradlew runClient` per `.claude/rules/plugin-testing.md` (jx credential
switch first). Checklist: style hiding + Shift reveal, weapon-switch warning, quest list
tints and tooltips, lamp Confirm block + Shift override, `shutDown` restores the combat tab,
panel states above.

**Hub:** same hub name `pure-helper`, new PR via the skill's `hub-branch.sh`. The PR body
pre-answers: no network access; menu/widget changes are local only (style hiding mirrors
core Attack Styles; the lamp Confirm consume is local); bundled quest/diary data was compiled
from the OSRS Wiki (attributed in the README); nothing written to disk; `shutDown` undoes all
widget changes. `runelite-plugin.properties` description is rewritten to match v2.

## Code layout

One package `com.yumesplugins.purehelper`, one purpose per file:

| File | Purpose |
|---|---|
| `PureHelperPlugin` | wiring only: lifecycle, event fan-out to guards |
| `PureHelperConfig` | caps + per-guard toggles + accent colour |
| `Build` | cap math |
| `StyleTable` | style → skills data |
| `RewardRules` | quest/diary JSON loader and query |
| `CombatGuard`, `QuestGuard`, `LampGuard` | one guard each |
| `RiskLog` | session log |
| `PureHelperPanel` | the panel |

## Open risks

- Exact gameval IDs for the lamp/book skill-choice interface need confirming in the dev
  client before LampGuard is built.
- Panel polish is the riskiest part; expect iteration from in-game screenshots.
