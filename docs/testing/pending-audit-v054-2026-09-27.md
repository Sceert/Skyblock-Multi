# Continuación de auditoría — v054 — 2026-09-27

Seguimiento: los tres puntos agrupados en las 14 aserciones fueron tratados en
`../development/internal-revisions-v055.md`. El usuario confirmó que Extreme
no entrega nada y que la limpieza regional debe retirar familias de cubos.
Este informe conserva los resultados históricos y sus expectativas originales.

Revisión de `0.2.4-v054-beta` en `feature/world-foundation`, Minecraft 26.2,
Fabric Loader 0.19.3, Fabric API 0.156.0+26.2 y Java 25. Conserva los cambios
preexistentes del checkout. No modifica gameplay, versión ni perfiles personales.
Los arneses están en `src/gametest` y se activan explícitamente; no forman parte
del JAR normal. Todos los escenarios usan mundos desechables del servidor integrado.

Complementa `pending-audit-2026-09-27.md`; sus resultados v053 siguen siendo
históricos, no una certificación repetida sobre v054.

## Resultados cerrados

| Suite | Aprobadas | Fallidas | Alcance |
|---|---:|---:|---|
| Árboles | 176 | 11 | 11 árboles × 4 dificultades; funciones reales de base, árbol, cofre y stock; contenido y protección contra reinicialización. |
| Panel nativo | 11 | 0 | Minimizar/restaurar conserva instancia; configuración y regreso; controles dentro de 854×480, 426×240 y 320×180; X, reapertura, Escape y desconexión. |
| Ciclo infernal dinámico | 13 | 0 | Activación, dos fases, campeón, snapshot solo, bloqueo de spawn, victoria, cooldown, reinicio y abandono. |
| Contrato infernal dinámico | 1 | 3 | Fixture ajeno presente; apropiación y eliminación de slime ajeno; retraso de aparición ignorado. |
| Dedicado, tras aceptación de EULA | 4 | 0 | Preset Overworld, Nether void, End normal y conexión inicial Skyblock. |
| **Total** | **205** | **14** | **219 aserciones; las 14 fallidas representan tres problemas distintos.** |

Verificación final: `gradle build --offline` terminó correctamente (17 tareas,
10 ejecutadas y 7 UP-TO-DATE); pasaron las diez verificaciones de dominio.
`git diff --check` no detectó errores de whitespace. No se repitió un clean build.

El panel se probó mediante entradas reales de teclado y botones. Las tres
dimensiones se aplicaron a `Screen.resize` y se midieron los controles; no equivalen
a cambiar físicamente el tamaño de ventana ni a una revisión visual de cada idioma.
Se recuperó el log completo de la ejecución interrumpida: las 11 aserciones
pasaron y Minecraft cerró. No se recuperó el código de salida original de Gradle.

Las pruebas dinámicas usan el parser de producción y su registro de definiciones,
inyectado en el mundo de prueba. Ejecutan el runner real por ticks. No comprueban
la instalación ni `/reload` de un datapack desde disco. La victoria se provoca
retirando exclusivamente los enemigos de prueba, no mediante combate humano.
El ciclo normal terminó con `BUILD SUCCESSFUL`; la suite negativa compiló y
terminó con `AssertionError` por los tres incumplimientos reproducidos.

## Hallazgos

### 1. Slime ajeno incorporado y eliminado por un encuentro dinámico

Prioridad alta: afecta entidades que no pertenecen a la prueba.

`InfernalTrialRunner.trackSplitChildren` toma cualquier `Slime` vivo dentro del
radio que carezca de la etiqueta de victoria. No verifica procedencia. La opción
`children_count_for_victory` es true por defecto, incluso para una fase de blazes.

Reproducción: arena con `cleanup_mobs=false`, tres blazes y un slime tamaño cero
preexistente identificado con `audit_unrelated`. El log registra tres invocaciones
pero cuatro enemigos detectados. El slime recibe la etiqueta dinámica y muere
cuando `abandonActive` elimina las entidades de esa ejecución. No es un descendiente
de los mobs de la prueba. La eliminación del slime y su apropiación son dos
aserciones fallidas de un mismo problema.

### 2. `spawn_delay_ticks` aceptado pero ignorado

El parser acepta 200 ticks en una entrada de tres blazes. Los tres aparecen al
inicio de fase y ya existen a los 40 ticks. `startNextPhase` invoca todo el grupo
en un bucle síncrono sin leer ese campo. Contradice la descripción publicada de
apariciones escalonadas en `docs/datapacks/infernal-trials-v1-proposal.md`.

### 3. Contradicción documental del cofre Extreme

Los once árboles en Extreme no generan cofre. README describe «Empty chest»,
mientras `docs/datapacks/addon-rewards-v1.md` especifica que una selección vacía
solo genera cofre si un proveedor aporta objetos. Se registran once fallos frente
al README, no once bugs independientes ni una decisión de cambiar el diseño.
En estas ramas no se pudieron validar contenido ni stock dentro de un cofre
inexistente. Las otras 33 combinaciones sí validaron el contenido esperado,
stock reproductivo y conservación de objetos al repetir la inicialización.

## Revisión estática adicional

Se localizaron campos aceptados por el parser sin consumo en el runner:
`drop_policy`, `player_targeting_only`, `alliance`, `equipment.drop_chance`,
`equipment.components` y `rewards.per_participant`. `persistent` tampoco se lee:
el summon fija `PersistenceRequired:1b`. `placement=spread` cae en la selección
aleatoria usada por defecto. Estas observaciones de código requieren pruebas
específicas antes de describir sus efectos completos en juego.

Las loot tables de fase/final se entregan directamente a participantes online
en `reward`; no deben confundirse con suplementos persistentes del Vault,
que cuentan con una ruta distinta. No se certificó desconexión durante esa entrega.

## Reproducción y evidencia

```powershell
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=trees-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=native-panel-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=dynamic-audit' --offline
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=dynamic-contract-audit' --offline
```

Logs preservados (un `clean` puede borrarlos):

- `build/audit-v054-trees.log`
- `build/audit-v054-native-panel.log`
- `build/audit-v054-dynamic.log`
- `build/audit-v054-dynamic-contract.log`
- `build/audit-v054-dedicated.log`

### Desbloqueo del servidor dedicado

El usuario aceptó expresamente la EULA para el servidor de pruebas. Se cambió
`build/run/clientGameTest/eula.txt` a `eula=true`. El primer arranque desbloqueado
detectó un error del arnés: `level-type` no reemplaza el mundo que el builder ya
prepara. Se seleccionó también el preset Skyblock en `adjustSettings` y se limitó
la escucha a `127.0.0.1`. El reintento aprobó las cuatro aserciones, guardó el mundo,
cerró el servidor y terminó con `BUILD SUCCESSFUL`. No se clasifica el primer
resultado del Nether como bug del mod. Esta prueba usa el servidor dedicado del
arnés de Fabric dentro del proceso del cliente; no certifica distribución de
servidor independiente ni ausencia de dependencias de cliente en otro classpath.

## Pendientes que no quedan aprobados

- Fallos v053 de persistencia del Vault, libro inicial, reingreso de party y limpieza
  de magma cubes: documentados en el informe anterior; no corregidos ni revalidados aquí.
- Distribución dedicada independiente y matriz multijugador: no cubiertas por
  las cuatro comprobaciones del servidor dedicado del arnés.
- NP/OpenPAC y dos jugadores reales: transferencias, expulsión offline, prioridad,
  claim permanente, OWN/ACTIVE/PARTY_HOME y desconexiones.
- Recarga real de datapacks, recompensas dinámicas offline y campos de contrato
  señalados en la revisión estática.
- Matriz de políticas de anillos y geometría persistente, migración integral de
  perfiles legacy y addons, Random con definiciones externas.
- Viaje al End, avance secreto, portales con 0/25/50/75%; protección de frontera,
  fluidos y todas las fuentes concretas de daño.
- Balance humano, accesibilidad, narrador, todos los idiomas y escalas físicas.

La revisión adicional queda documentada y reproducible, pero no declara agotada
la cuota ni aprobados todos los pendientes del proyecto. Los fallos quedan abiertos
para una revisión de implementación con nueva versión, sin alterar v054 en esta auditoría.
