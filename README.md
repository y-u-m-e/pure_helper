# Pure Helper

Pure Helper is a RuneLite external plugin focused on pure-account safety:

- combat style safety warnings
- quest and achievement diary risk checks against protected skills/caps
- local build preset import/export
- optional sidebar panel with safeguards and risk log

## Requirements

- Java 11+ (Java 17 recommended)
- RuneLite development environment (Gradle wrapper included)

## Run and Develop

- `./gradlew run`  
  Launches RuneLite in developer mode with this plugin loaded.

- `./gradlew classes`  
  Fast compile check for Java sources.

- `./gradlew clean test`  
  Full compile + unit test pass (recommended before PR).

- `./gradlew shadowJar`  
  Builds a fat JAR for sideload/manual testing workflows.

## Plugin Usage

After launching RuneLite with this plugin:

1. Open the Pure Helper sidebar panel.
2. Configure protected skills and optional level caps in `Skills`.
3. Configure safeguards and warning behavior in `Safeguards` and `Combat`.
4. Review risky style switches in `Risk Log`.
5. Optionally set `Accent color` in plugin config.

## Custom Tab Icons

Tab icons are loaded from:

`src/main/resources/tab-icons/`

Use these exact filenames:

- `safeguards.png`
- `combat.png`
- `skills.png`
- `log.png`

Recommended icon format:

- PNG with transparent background
- square source image
- high contrast for dark background

Missing files fall back to a neutral placeholder icon.

## Plugin Hub / PR Readiness

Before opening or updating a Plugin Hub PR:

1. Run `./gradlew clean test`
2. Verify no prohibited behavior per Jagex third-party guidelines
3. Confirm plugin metadata in `runelite-plugin.properties` is accurate
4. Update plugin-hub manifest commit hash to the latest plugin commit

Plugin Hub submission docs:

- [RuneLite Plugin Hub repository](https://github.com/runelite/plugin-hub)
