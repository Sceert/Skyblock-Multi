# Auditoría de pendientes — 27 de septiembre de 2026

Estado: **pasada de pruebas con fallos reproducidos; no aprobación general del mod**.
Las cinco suites nuevas ejecutadas suman **88 comprobaciones: 83 aprobadas y
5 fallidas**, además de las pruebas de panel existentes. El intento dedicado
quedó bloqueado antes de ejecutar sus comprobaciones.
Rama `feature/world-foundation`, versión existente `0.2.4-v053-beta`.
Se conservaron los cambios locales previos. Esta sesión añade únicamente arneses
optativos de prueba y este informe; no corrige gameplay, instala JAR ni publica.
No se entrega una revisión de implementación nueva y `mod_version` no cambia.

## Fallos reproducidos

1. **Premio duplicable tras fallo de escritura del registro.**
   `InfernalRewardLedger.claim` retira el premio de memoria y entrega objetos antes
   de guardar. En el mundo desechable se prepara un premio de siete diamantes y
   se crea un directorio vacío exactamente en la ruta `.tmp` del registro para
   provocar un error real de escritura. Tras reclamar, retirar ese obstáculo y
   recargar el registro duradero, otra reclamación entrega nuevamente el premio.
   Prueba: `PendingAudit`, modo `audit`.
   Esto no es una prueba de corte eléctrico: se invoca `load` para reproducir la
   recuperación desde disco manteniendo el inventario. La limitación de atomicidad
   inventario/registro ya está reconocida en `docs/datapacks/addon-rewards-v1.md`.
   Sigue siendo un riesgo reproducible frente al requisito de entrega única.
   Corregirlo exige definir recuperación duradera; mover un `save` no demuestra
   por sí solo atomicidad entre inventario y registro.
2. **Reingreso tras party inmoviliza mediante teletransporte.**
   `player/freeze_selection.mcfunction:10` lleva al jugador con
   `skyblock_party_reentry` desde X=4.5 hasta X=0.5. La selección ordinaria mantiene
   X=4.5. Prueba directa de función, no de todos los callbacks del proveedor.
   Contradice el AGENTS vigente que exige selección libre y reset normal.
   El comportamiento está expresamente descrito en las notas antiguas v048;
   esas notas y la matriz manual antigua necesitan reconciliarse con el requisito.
3. **Libro entregado después de asignar isla.**
   En el preset real, al llegar al Refugio en estado de selección no hay libro.
   Tras elegir Mangrove y Hard aparece uno; no se duplica al reabrir.
   `slots/claim_generic.mcfunction:44` es el punto actual de entrega.
   El AGENTS vigente exige recibirlo al llegar, antes de seleccionar.
4. **Abandono elimina cubos de magma ajenos.**
   En el Nether desechable, un cubo pequeño con solo `audit_unrelated` situado
   dentro del radio desaparece al ejecutar `infernal_trial/abandon`.
   Un Blaze ajeno sobrevive. Tanto `abandon.mcfunction:2` como
   `cleanup_magma_cubes.mcfunction:2` seleccionan todos los cubos del radio,
   sin exigir pertenencia al encuentro.
5. **Abandono deja descendientes del encuentro.**
   Un cubo Size=2 con `skyblock_infernal_enemy`, invocado en (8,100,0), deja
   descendientes vivos con esa etiqueta tras ejecutar abandono y esperar 100 ticks.
   El registro final muestra supervivientes en (8.11,100.5,0.11) y
   (8.89,100.5,0.11). Las seis pasadas tempranas no acreditan limpieza completa.
   Prueba de la función real en un Nether desechable con chunks forzados;
   no se afirma haber reproducido todas las posiciones del Coliseo.

## Ejecución y resultados

Comandos desde la raíz del repositorio, con Java 25:

```powershell
& 'C:/Gradle/bin/gradle.bat' build --stacktrace
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=standalone' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=integrated' '-Pfabric_api_version=0.158.0+26.2' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=core-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=trials-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=protection-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=vault-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=dedicated-audit' --offline
```

Las pruebas nuevas viven en `src/gametest`, se activan solo mediante el modo
correspondiente y no se incluyen en el JAR normal. Las suites con fallos lanzan
AssertionError al finalizar: su salida no cero es un resultado de auditoría,
no un fallo de compilación del mod. Se corrigieron errores del arnés durante su
preparación; en particular la fase 2 debe probar `spawn_phase_2_start`, que también
equipa enemigos, y la probabilidad de propagación debe consultar un destino de aire.
Los falsos positivos iniciales de esos dos casos no se reportan como bugs.

| Área | Resultado y alcance |
|---|---|
| Build normal | Exitoso; 17 tareas UP-TO-DATE. Se reutilizaron las diez verificaciones vigentes de dominio, sin forzar su repetición. |
| Panel independiente | Cliente real, Fabric API 0.156.0; abrir/cerrar y pantalla no pausante aprobados. Sin NP/OpenPAC. |
| Persistencia de premios | 9 comprobaciones aprobadas, 2 fallidas incluyendo la selección de reingreso. Cola de dos premios, reclamación vacía, item de addon ausente, formato legacy y archivo corrupto comprobados. |
| Mundo Skyblock | 22 comprobaciones aprobadas, 1 fallida (libro inicial). Overworld/Nether flat, End noise, llegada, protección por callbacks, Mangrove/Hard, OWN/ACTIVE y bloqueo de geometría. |
| Reapertura del mundo | OWN y coordenadas conservadas; retorno a isla; bloques de prueba de isla y Nexus intactos; bloqueo persistente; sin duplicar libro. Es cierre/reapertura real del servidor integrado. |
| Trials I–X | Invocación de fase inicial y equipo aprobados; fases 2 de VII–X aprobadas; campeón y barra de salud aprobados. Dos fallos de limpieza descritos arriba. No es evaluación humana del balance ni una victoria completa de cada nivel. |
| Protección ambiental | Explosión real, combustión, probabilidad de propagación y resolución de pistones con piedra/slime/honey aprobadas dentro y fuera de la región. No cubre todas las orientaciones ni circuitos de frontera. |
| Vault / suplementos | 8 comprobaciones aprobadas: suplemento offline, dos runs pendientes, deduplicación tras recarga, offhand, doble callback en un tick, siguiente clic y aislamiento entre jugadores. Usa callbacks reales y UUID offline; no simula conexión multijugador humana. |
| Dedicado | Bloqueado: el directorio nuevo `build/run/clientGameTest/eula.txt` exige aceptación de EULA. El arnés finaliza con timeout. No se aceptó en nombre del usuario. |
| JSON / idiomas | 164 JSON válidos; 13 idiomas con las mismas 615 claves. Esto valida sintaxis y presencia, no calidad lingüística ni renderizado completo. |
| Funciones | 286 archivos, 1592 referencias locales literales a funciones; ninguna referencia ausente. No equivale a ejecutar todas las ramas. |
| JAR existente | ID/version correctos; no incorpora clases de los nuevos arneses. No se instaló en perfiles personales. |

SHA-256 del JAR existente `build/libs/Skyblock-Multi-0.2.4-v053-beta.jar`:
`16702638AFDFE2655C24A8D7EE07950068CDACF1D5F990CE42E9D73B2F910F2B`.

Evidencia local (el directorio build puede eliminarse con clean):

- `build/audit-2026-09-27-standalone.log`
- `build/audit-2026-09-27-integrated.log`
- `build/audit-2026-09-27-persistence.log` — `[PENDING-AUDIT]`
- `build/audit-2026-09-27-core.log` — `[CORE-AUDIT]`
- `build/audit-2026-09-27-trials.log` — `[TRIALS-AUDIT]`
- `build/audit-2026-09-27-protection.log` — `[PROTECTION-AUDIT]`
- `build/audit-2026-09-27-dedicated-blocked.log`
- `build/audit-2026-09-27-vault.log` — `[VAULT-AUDIT]`

Los avisos externos de OSHI/Perflib, autenticación Realms del usuario de desarrollo,
JEI/Amecs y opciones gráficas no se interpretaron como fallos de SkyblockMulti.
El intento de revisión complementaria por Gemini quedó sin resultado por permisos
de su modo headless; no se desactivaron sus controles.

## Matriz de pendientes restante

| Pendiente | Cobertura actual / siguiente comprobación necesaria |
|---|---|
| Anillos 8/16/24, dispersos, 80/128, overflow | Verificación de dominio vigente; esta sesión prueba una asignación real. Falta matriz de mundos para todas las políticas y cambios de defaults tras reinicio. |
| NP/SM traspaso, cancelación, timeout, estado obsoleto | Coordinador cubierto por pruebas con dobles. Falta matriz real de dos jugadores, cada proveedor y reinicios entre decisiones duraderas. No sustituirla por el build. |
| OWN/ACTIVE/PARTY_HOME al salir, expulsar, disolver | No aprobado globalmente. Incluye reingreso bloqueado confirmado arriba, líder sin OWN y expulsión offline con OpenPAC. |
| Cooldown y administración | Cooldown ya figura implementado en v038: roadmap está desactualizado. Faltan escenarios de tiempo real/lifecycle y comandos de intervención/liberación de slots si siguen sin implementar. |
| Claims OpenPAC | Falta servidor con OpenPAC y verificación de claim permanente, prioridad NP y ausencia de claims de islas. |
| Protección | Faltan fluidos con cliente, entidades destructivas, TNT/creeper/crystal por sus fuentes concretas, sticky pistons y ramas que crucen la frontera. La prueba de explosión usa el motor común. |
| Selección, árboles y dificultad | Mangrove/Hard real aprobado. Falta matriz de todos los árboles/dificultades, stock reproductivo completo, Random con addons y servidor sin Mod Menu. |
| TOML / migración / addons | Pruebas de codecs vigentes; falta migración integral de perfiles con datos legacy y addons presentes/ausentes, con copias. |
| Mod Menu | Revisión de código confirma bloqueo de toggles de árboles/addons al abrir mundo. Falta interacción visual en todas las escalas/idiomas. El roadmap de dos columnas es anterior al cambio a Cloth. |
| Infernal multiplayer | Falta balance con jugadores reales, exclusión de espectadores/ajenos bajo proveedores, desconexión durante cada fase y votaciones de abandono. |
| Infernal datapacks | Parsing vigente; falta encuentro dinámico completo con datapack de prueba, suplementos, cambios de dimensión y abandono. |
| Persistencia extrema | Fallo de escritura del Vault reproducido; no se realizó caída del proceso, corrupción de scoreboard ni corte entre commits NP/SM. |
| End / progreso | Se comprobó generador normal; falta viaje físico, avance secreto por libro y matriz de ojos 0/25/50/75 en mundos separados. |
| Accesibilidad / panel | Faltan narrador, IME si procede, Alt+Tab físico, latencia real y varias escalas con contenido largo. |
| Waystones, dimensión inundada, arenas privadas, rutas nuevas | Propuestas futuras; no son pruebas ejecutables de implementación actual. No se implementaron por esta petición de pruebas. |
| Recetas BOP / otros addons | Requieren su repositorio y sus definiciones instaladas; la auditoría del core no las declara aprobadas. |

La sesión no certifica que se hayan probado todos los pendientes ni que se haya
consumido toda la cuota. Los resultados y arneses quedan guardados para continuar
sin repetir las comprobaciones que ya pasaron.
