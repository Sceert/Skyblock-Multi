# Compact island panel

Open with **I** (configurable in Controls) or `/skyblockmulti panel`.
Opening displays the live panel as a HUD overlay without taking keyboard or mouse
control. Press the panel key again to interact with its buttons and drag its title;
press it once more to return to gameplay while retaining the panel. Close/Escape
dismisses it; Minimize retains the small reminder. Other screens temporarily hide
the overlay. Server snapshots update the retained panel without opening a screen.
The panel defaults to 260 GUI units wide and fits its content up to 240 units high.
Facts use two columns, muted field labels and bold values with an accented score.
Minecraft's GUI scale determines its physical size. It does not blur, dim or pause the world.
While interacting with buttons, normal screen input applies. Use the panel key or minimize to regain
movement; a small HUD reminder shows the configured reopen key. Open chat to click
the reminder with the mouse, or use that key while playing. Close (or Escape)
dismisses the panel and reminder without answering any pending decision.
Drag the title bar to move the window. Its relative position is retained across
the panel and selection screens for the current client session, and clamped when
the window or GUI scale changes. Chat hints resolve the actual client keybinding.

Mod Menu → Skyblock Multi → Island panel configures width (220–420), maximum
height (210–360), six RGB colors and the minimized box's X/Y position (0–100%).
The header's C button also opens this category, which appears before Addons.
These local preferences live in `config/skyblockmulti/client.toml`, section
`[island_panel]`, independently of server/world settings. Missing/invalid settings
use defaults, dimensions and positions are bounded, and saving preserves unrelated
sections and a `.bak` copy. Without Mod Menu, edit the file and restart the client.
The panel clamps to the viewport and paginates overflowing information/decisions.
Client and server should use the same revision; structured fact rows use the v2
panel snapshot channel.

- **Island:** personal island number, ring, recorded starter tree, difficulty and
  personal progression score. A different active island has a separate detail page. Also includes the
  pending tree/difficulty selection. Tree selection retains addon search,
  pagination, enabled-state filtering, localized names and Random.
- **Group:** island owner and group name from the active provider only. Hidden
  without a compatible group provider; players outside a group see "None".
- **Notices:** individual island choice, group-home decision, Infernal Trial
  abandonment vote/request and personal pending reward count. Multiple notices
  and long localized text use pagination within the same window.
- Accept/reject explanations distinguish players with and without a personal
  island. Long decisions must be read through to the last page before answering.
- Closing/minimizing never submits a decision. Existing server expiry rules
  still apply. Reopening reads current state rather than reviving old actions.
- Rewards are collected only at the Infernal Vault; the panel provides no new
  free travel, claim, regeneration, ownership-transfer or reset command.

Snapshots update twice per second while subscribed. Each button response carries
a revision and an opaque action ID. The server reconstructs the current snapshot,
rejects stale/unadvertised responses, invalidates consumed revisions and invokes
only an allowlisted existing player command. Disconnect/close clears subscriptions.
Existing island/world persistence formats are unchanged. A separate versioned
`skyblockmulti_island_trees_v1.properties` file stores selected namespaced tree IDs
by slot and pending selections by player, with an atomic replacement and backup.
Slot identity preserves the original tree across ownership transfers. Old islands
without recorded metadata show "Not recorded"; blocks are never inspected to guess.

The score is **personal progression**, not a party or block-value leaderboard:
10 points for each completed, visible Skyblock Multi advancement with a parent.
Hidden, root, technical and other namespaces' advancements do not count. Progress
is read from the normal advancement save, so reopening/rejoining cannot award
duplicate points. Revoked advancements stop counting. No new reward is granted.

## Focused manual verification

1. Open using the configured key and chat link, test GUI scales and Spanish/English;
   drag to each edge, resize and reopen. Rebind the key and check a new chat hint.
   Paginate long decision text. Minimize and verify movement and HUD hint.
   Compare Close (no hint) with Minimize; reopen by key and by clicking from chat.
   Customize all colors, minimum/maximum sizes and HUD positions in Mod Menu;
   restart to verify persistence and check long tree/group names at large GUI scales.
2. On a new player, choose an enabled vanilla/addon tree or Random, close and
   resume, choose difficulty and inspect the Extreme warning before confirming.
3. Test island accept/reject with and without OWN, then a leadership home decision.
   Change party membership or expire a decision while the panel is open; old
   buttons must not move the player. Repeated clicks must not repeat travel.
4. During a trial, request abandonment and accept/reject as eligible participants;
   confirm unrelated players and spectators have no vote actions. Reopen after
   voting and after vote expiry.
5. Complete a trial, inspect the reward count, collect once at the Vault and verify
   the panel updates. Reconnect and confirm pending rewards still appear.
6. Check number/ring boundaries, choose a vanilla/addon/random tree on a new island,
   restart, and transfer ownership. The starter tree must remain tied to the slot.
   An old unrecorded island should show "Not recorded". Finish a visible milestone
   and verify +10 points exactly once; no points for a secret or root advancement.
7. Test no group mod (hidden tab), Nexus Parties, OpenPAC fallback and no membership.
   Group content must contain only owner and group, never a mixed provider list.

Automated protocol scenarios run in `verifyIslandPanel` as part of `build`.
Persistence and score policy scenarios run in `verifyIslandDetails`.
