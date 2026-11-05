# v055 — familias infernales y apariciones escalonadas

- Extreme conserva la regla aprobada: sin cofre ni suministros iniciales vanilla.
  README y arnés de árboles corregidos; no se añaden retoños.
- Slimes y magma cubes heredan pertenencia de su padre al dividirse. Los encuentros
  ya no reclutan cubos ajenos por proximidad durante el combate.
- La limpieza inicial mantiene el alcance regional; marca los cubos para evitar
  nuevas divisiones al morir. La limpieza dinámica retira la familia etiquetada
  y cancela apariciones pendientes.
- `spawn_delay_ticks` espacia las invocaciones dentro de cada entrada y bloquea
  la victoria mientras queden invocaciones pendientes.

Validación completada:

- `runClientGameTest -Ppanels_game_test=v055-regression --offline`: 247 aserciones
  aprobadas, cero fallidas (13 contrato dinámico, 13 ciclo dinámico, 34 trials
  incorporados y 187 árboles). Mundos desechables, sin perfiles personales.
- Se comprobó división real de slime y magma cube, herencia de etiquetas,
  eliminación sin nietos, limpieza inicial habilitada, aislamiento cuando está
  deshabilitada, intervalo de 200 ticks, bloqueo de victoria con cola pendiente,
  cancelación al abandonar y máximo de salud estable.
- `gradle build --offline`: correcto, incluidas las diez verificaciones de dominio.
- `git diff --check`: correcto. Log: `build/audit-v055-regression.log`.

JAR: `build/libs/Skyblock-Multi-0.2.4-v055-beta.jar`.
SHA-256: `9EB77FCF4E264DEF3A8E0AEBBAEF629EF9DF285EBF25C3B3C10F8C9DE2A50230`.
No se instaló en un perfil personal ni se publicó.

Los once fallos históricos de Extreme eran expectativas erróneas del arnés,
corregidas tras confirmar la regla con el usuario. La limpieza regional de cubos
en los trials incorporados también es intencional: no se exige conservar un cubo
ajeno dentro del área expresamente seleccionada para limpiar. Se mantiene esa
distinción respecto a apropiarse de fauna durante el combate.

Esta revisión no corrige los otros hallazgos históricos del Vault, libro inicial
o reingreso de party, ni implementa los demás campos opcionales del API infernal.
