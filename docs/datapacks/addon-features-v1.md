# Configurable addon features v1

Optional addons can expose switches in Skyblock Multi's **ADDONS** Mod Menu tab
without adding addon-specific code to the main mod. The declaration belongs in
the addon's `fabric.mod.json` under `custom.skyblockmulti.addon_features`.

```json
{
  "custom": {
    "skyblockmulti": {
      "addon_features": [
        {
          "id": "decoration_recipes",
          "type": "recipes",
          "name_translation_key": "skyblockmulti_bop.config.decoration_recipes",
          "enabled_by_default": true,
          "reload_required": true,
          "recipe_namespaces": ["skyblockmulti_bop"],
          "recipe_path_prefixes": ["decoration/"]
        }
      ]
    }
  }
}
```

Additional Java-backed capabilities use the same UI and persistence contract:

```json
{
  "id": "wandering_trader_items",
  "type": "wandering_trader",
  "name_translation_key": "example_addon.config.wandering_trader_items",
  "enabled_by_default": true,
  "reload_required": false
}
```

```json
{
  "id": "starter_waystone",
  "type": "starter_chest",
  "name_translation_key": "example_addon.config.starter_waystone",
  "enabled_by_default": false,
  "reload_required": false
}
```

```json
{
  "id": "rare_challenge_loot",
  "type": "challenge_loot",
  "name_translation_key": "example_addon.config.rare_challenge_loot",
  "enabled_by_default": true,
  "reload_required": true
}
```

The supported feature types are:

- `recipes`: managed directly by Skyblock Multi using the declared recipe scope.
- `starter_chest`: optional island starter-chest contents implemented by the
  declaring addon. This switch is independent from challenge rewards.
- `wandering_trader`: optional offers implemented by the declaring addon.
- `challenge_loot`: optional rewards for infernal or future challenges implemented
  by the declaring addon.
- `other`: an extension switch for another addon-owned capability.

Java addons query a non-recipe feature at runtime with its fully qualified ID:

```java
boolean enabled = AddonFeatureApi.isEnabled(
        "example_addon:rare_challenge_loot"
);
```

The public Java entry point is
`cl.treecs.skyblockmulti.api.addon.AddonFeatureApi`; `API_VERSION` is currently
`1`. Addons should not import implementation classes from
`cl.treecs.skyblockmulti.addon`.

Skyblock Multi deliberately does not interpret or register unknown item IDs from
the manifest. The addon remains responsible for validating its offers and loot,
and must skip registration or reward generation while its feature is disabled.
This prevents a missing optional mod or renamed item from breaking the core mod.

## Fields

- `id`: stable feature ID local to the addon. Skyblock Multi stores it as
  `<addon_mod_id>:<id>`.
- `type`: `recipes`, `starter_chest`, `wandering_trader`, `challenge_loot` or
  `other`.
- `name_translation_key`: preferred client-localized label supplied by the addon.
  A literal `name` may be used as a fallback.
- `enabled_by_default`: state used until an administrator changes it.
- `reload_required`: tells the UI that data must be reloaded or the world restarted.
- `recipe_namespaces`: recipe ID namespaces owned by a `recipes` feature.
- `recipe_path_prefixes`: prefixes within those namespaces. Every namespace/prefix
  combination belongs to that recipe feature.

States are stored per addon in
`config/skyblockmulti/addons/<addon_mod_id>.toml`. Dedicated servers create
these editable files automatically at startup, without requiring Mod Menu.
Disabling a recipe
feature removes its matching recipes while Minecraft prepares recipe data, so it
affects crafting and recipe viewers rather than merely hiding buttons. Existing
items are never removed.

Addon translation keys must be provided by the addon itself. This keeps names,
recipes and compatibility data outside the Skyblock Multi core.
