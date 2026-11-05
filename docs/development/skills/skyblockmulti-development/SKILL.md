---
name: skyblockmulti-development
description: Maintain Skyblock Multi revision discipline when implementing, fixing, configuring or packaging the Sceert/SkyblockMulti Minecraft mod, including its local E:/GitHub/SkyblockMulti checkout. Does not apply to unrelated mods or discussion without implementation.
---

# Skyblock Multi development

Read the repository's applicable AGENTS.md before changing the mod.

For every new implementation revision delivered for testing, increment
`mod_version` in `gradle.properties`. This includes UI, configuration and resource
changes. Read the current value and existing build/internal revision names first;
when the version has `vNNN`, advance to the next unused internal revision while
preserving the development-version prefix and beta suffix. Never deliver different
changes under a previously delivered version. Build retries while finishing the
same revision retain that revision number.

`gradle.properties` is authoritative: derive artifact names and metadata from it.
Record internal changes under `docs/development/`, not in the published-only
CHANGELOG.md. Verify the version in the built JAR before handoff. A version bump
does not authorize publishing, pushing, tagging or installing into a game profile.

Keep this preference in the applicable repository AGENTS.md as well as this skill
so repository tasks retain it even if automatic skill selection does not trigger.
