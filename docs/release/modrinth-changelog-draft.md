# Draft update notes — based on 0.2.4-v056-beta

Editorial draft for the next public beta; this is not a published release.
Choose the public version separately. Do not relabel the internal v056 JAR as a
different version without building and testing matching metadata.

## Highlights

- Native island panel for island status and pending decisions, with configurable
  appearance, minimizing/restoring and access to configuration.
- Expanded configurable ring layouts, allocation policies and optional party
  provider support, prioritizing Nexus Parties before OpenPAC.
- Generic addon feature switches, planting stock and personal reward supplements.
- Personal Infernal Vault, participant-aware encounters, health bars and abandonment
  flow, with persistent pending addon rewards for disconnected participants.

## Fixes and adjustments

- Slime and magma-cube descendants inherit encounter membership from their parent.
  Cleanup removes their families without creating new splits; unrelated nearby cubes
  are no longer adopted into encounters during combat.
- Datapack spawn intervals are honored. Encounters wait for pending spawns, and
  abandonment cancels them while preserving health-bar progress.
- Reward claims persist intent before granting items. Failed initial writes deliver
  nothing; interrupted deliveries are retained for review instead of automatically replayed.
- The lore book arrives at the Last Refuge before island selection and is not
  duplicated by normal island assignment or reconnection.
- Party reentry selection allows movement instead of repeatedly teleporting players.
- Extreme intentionally provides no vanilla starter chest or supplies.

## Upgrade notes and known limits

Back up worlds and configuration. Do not downgrade saves containing the updated
reward format. Full automatic recovery from arbitrary crashes is not guaranteed;
do not blindly clear interrupted-claim markers. Advanced island administration
commands are still pending. Integration testing with real multiplayer party
providers remains ongoing.

Development validation includes 247 checks in the v055 regression and 44 targeted
checks in v056, plus successful builds/domain checks. These are separate historical
runs, not a single complete certification of every feature on a public release.

## Notas en español

- Panel nativo de islas con información, decisiones pendientes, apariencia
  configurable, minimizar/restaurar y acceso a configuración.
- Anillos y políticas de asignación ampliados; prioridad de Nexus Parties sobre
  OpenPAC cuando hay un proveedor compatible instalado.
- Interruptores genéricos de addons, material de plantación y suplementos de premios.
- Bóveda Infernal personal, barras de salud, participación y abandono de encuentros.
- Los hijos de slimes y magma cubes heredan su pertenencia al encuentro; las
  limpiezas evitan nuevas divisiones y ya no se adoptan cubos ajenos por cercanía.
- Intervalos de aparición de datapacks respetados; no se completa una fase con
  apariciones pendientes y abandonar las cancela.
- Registro previo de reclamación: un fallo de escritura no entrega objetos;
  las entregas interrumpidas quedan retenidas para revisión.
- Libro al llegar al Refugio sin duplicarlo al asignar isla o reconectar;
  movimiento libre durante la selección de reingreso de party.
- Extremo conserva su regla: sin cofre ni suministros iniciales vanilla.

Respalda mundo y configuración antes de actualizar. La recuperación automática
ante cualquier caída no está garantizada. Los comandos administrativos avanzados
de islas y parte de la validación multijugador siguen pendientes.
