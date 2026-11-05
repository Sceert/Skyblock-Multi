# Internal revision v051

- Keep the island panel in the HUD while playing, without capturing movement,
  camera, attack or placement input. The configured panel key switches to and
  from interactive mode; only explicit interaction opens a screen.
- Keep the same panel and server subscription across mode switches, resizing
  its HUD view with the viewport. Incoming snapshots do not steal focus.
- Close/Escape clears the retained panel; Minimize keeps the existing reminder.
  Other screens hide the overlay, and disconnect clears client panel state.

Manual checks: open while moving, move/look/build with the HUD visible, toggle
interaction with a rebound key, drag/use buttons and return to play, resize GUI,
receive changed information/decisions during play, close/minimize/reopen,
open inventory/configuration and disconnect/reconnect.
