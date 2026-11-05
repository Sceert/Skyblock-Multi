# GitHub and Modrinth preparation — reviewed 2026-10-06

Base inspected: branch `feature/world-foundation`, HEAD `38c3611` (0.2.3 beta release),
local development `0.2.4-v056-beta`. No staging, commit, push, tag or publication performed.

## GitHub selection

Include changed/new production sources and resources, 13 locale files, all
required API/helper classes, tests, opt-in gametest sources/resources, build
configuration, reproducible generators and current documentation. Include the
six deliberate deletions too; uploading only changed files leaves obsolete files.
The initial status contained 117 modified, 6 deleted and 172 new entries; the
inventory records individual paths rather than relying on directory-level counts.

Do not commit build outputs/JARs, Gradle caches, runtime worlds, accepted eula.txt,
logs, local configurations, credentials or personal agent context. Existing ignore
rules cover the usual build/run/cache locations. Release JARs belong in release
attachments/Modrinth, not in source history.
The already tracked `gradle/wrapper/gradle-wrapper.jar` is a build-tool exception:
keep it versioned; it is not a generated mod binary.

Review separately the archived agent instructions and project skill copy under
`docs/development`: they are historical tooling context, not runtime dependencies.
Keep maintenance scripts if they remain useful; generated slot functions and the
generator must travel together. Do not run old translation migration scripts as
part of publication. No broad staging command was executed.

The review found workstation paths in some internal docs, not evidence of exposed
credentials. This is a targeted publication review, not a comprehensive secret scan.

## Blocking build portability issue

`build.gradle` conditionally loads the newest JAR from
`../Nexus Parties/build/libs`. `NexusPartiesPartyProvider.java` imports
`cl.treecs.nexusparties.NexusParties`, `PartyEvent` and `PartyEvents` directly.
The GitHub workflow checks out only this repository and does not fetch that API.
Consequently its clean checkout lacks the compile dependency. Local successful
builds do not validate this CI case.

Before calling the source ready for GitHub CI: provide a pinned, retrievable public
Nexus Parties API artifact or explicitly build a pinned dependency in CI. Keep it
compile-only so optional runtime compatibility is preserved. The dependency source
and immutable revision must be verified; do not upload a random local binary or
silently use whichever development JAR happens to be newest.

Public availability checked on 2026-10-06: the GitHub API returned 404 for
`Treecs/Nexus-Parties` (the repository linked in the local mod metadata), and
Modrinth returned 404 for the `nexus-parties` project slug. Modrinth search for
"Nexus Parties" returned no results. This does not establish that the project
does not exist: it may be private or published under another identifier. No
public, pinned dependency was verified, so the CI portability issue remains open.

Loom currently uses `1.17-SNAPSHOT`; pin a verified available version for reproducible
release builds. CI specifies Gradle 9.5.0 whereas recent local evidence used 9.7.0:
run the final candidate using the intended CI configuration before publication.

## Modrinth fields and artifacts

- Proposed summary: Configurable multiplayer Skyblock with personal islands,
  party support, Infernal Trials and datapack-driven progression for Fabric.
- Body: `modrinth-description-draft.md`.
- Version changelog: `modrinth-changelog-draft.md`.
- Loader/game: Fabric / Minecraft 26.2. Release channel: beta.
- Required dependencies: Fabric API, Cloth Config. Optional: Mod Menu, Nexus Parties,
  Open Parties and Claims, JEI. Addons remain separate projects.
- Client and server installation required; Java 25 and Fabric Loader >=0.19.3.
- Current internal artifact: `build/libs/Skyblock-Multi-0.2.4-v056-beta.jar`.
  SHA-256: `722F8F63CDD41320DEDE95AA33C22F115C63F76C00A4DED398B9FE203412B813`.
  Do not upload a sources JAR as the playable mod.
- Keep published-only CHANGELOG.md unchanged until an actual public release.
  The next public version has not been selected or created by this preparation.

Live body reviewed: https://modrinth.com/mod/skyblock-multi on 2026-10-04.
The public Modrinth versions API was retrieved on 2026-10-06 and confirms
`0.2.3-beta` as the latest listed version (version ID `MD8xeq09`), followed by
`0.2.2-beta` and `0.2.0-beta`.
The live body is bilingual and still describes 8/16/24 capacity, optional client
installation and requirements without Cloth Config. Update both languages:
128-position layouts, required client/server installation and Cloth Config;
add native panel, Nexus Parties priority, Infernal Vault and addon APIs. Also review
environment metadata, which currently shows server-side alongside client/server.

Confirm the release version, artwork/screenshots and final dependency links
before publishing. Do not describe future admin commands or private arenas
as implemented, or promise exactly-once delivery across every crash scenario.
