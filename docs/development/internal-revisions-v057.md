# v057 — Minecraft 26.3

Entrega local de pruebas, 2026-11-04. Conserva el trabajo previo del worktree.

- Minecraft 26.3, Java 25, Gradle 9.7.0, Loom 1.17.21, Loader 0.20.1,
  Fabric API 0.165.2+26.3, Cloth Config 26.3.159, Mod Menu 21.0.0,
  JEI 31.17.0.108 y API OpenPAC 0.33.0.
- Atajos mediante InputConstants para SDL; API nueva de estructuras,
  visibilidad de avances y drop SERVER_ONLY para recompensas de servidor.
- El filtro de recetas se aplica al construir RecipeManager desde el registro
  recargable de 26.3. Mantiene los interruptores del núcleo y de addons.
- Actualiza el criterio recipe_crafted a recipes y el ejemplo infernal a
  formato de datapack 121.0. No cambia el formato propio de persistencia.

## Verificación

- Gradle build correcto, incluidas las diez verificaciones independientes.
- runClientGameTest -Ppanels_game_test=core-audit: 23 comprobaciones correctas;
  preset vacío Overworld/Nether, End normal, asignación, protecciones y
  conservación de OWN, construcciones, Nexo y geometría al reabrir el mundo.
- Prueba dedicada desde SM Waystone Addon con -PintegrationTest -PupgradeSuite:
  cuatro mods juntos, 11 árboles y 84 recetas BOP, 23 recetas gestionadas del
  núcleo; desactivación y restauración de recetas mediante dos recargas reales.
- La misma prueba confirma recompensas pendientes tras reinicio y consumo
  único de una reclamación sintética con entrega real de ítems al inventario.

## Límites

Mundos temporales de desarrollo; no se instalaron mods en perfiles personales.
Pendiente probar visualmente las marcas sobre otro jugador y el multijugador
real con OpenPAC/Xaero. Las limitaciones históricas del roadmap continúan;
esta migración no corrige el fallo conocido de entrada de agua al Nexo.
El análisis auxiliar de Gemini no produjo propuestas utilizables porque su
modo sin interfaz denegó una herramienta; se continuó con APIs y pruebas locales.

Fuentes: [Fabric 26.3](https://fabricmc.net/2026/09/15/263.html),
[cambios de Minecraft 26.3](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3).
