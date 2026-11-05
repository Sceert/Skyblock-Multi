# Estado actual y entrada rápida

Actualizado: 2026-11-04. Migración local a Minecraft 26.3 verificada en v057.

## Fuentes autoritativas

- Versión y dependencias: [gradle.properties](../../gradle.properties).
- Pendientes y confirmaciones actuales: [ROADMAP.md](../../ROADMAP.md).
- Contrato completo: [project-rules.md](project-rules.md).
- Última entrega documentada: [v057](internal-revisions-v057.md).
- Uso de Gemini: [gemini-support.md](gemini-support.md).
- Cobertura revisada y pruebas nuevas: [auditoría 2026-10-09](../audits/2026-10-09-pending-coverage-review.md).
- API y formatos: [docs/datapacks](../datapacks/).

## Punto de partida

Rama inspeccionada: feature/world-foundation. Último commit observado:
38c3611 (0.2.3 beta). Versión local: 0.2.4-v057-beta, Minecraft 26.3.
Consultar Git y gradle.properties de nuevo al iniciar; estos datos son históricos.

El desarrollo posterior a 0.2.3 sigue acumulado en el worktree. No asumir que
un archivo sin seguimiento es prescindible ni que todo el diff pertenece a la
nueva tarea. Revisar y separar por tema antes de convertirlo en commits normales.

v056 registra un build correcto, diez verificaciones de dominio y 44 aserciones
aprobadas el 4 de octubre. No se repitieron en esta reorganización documental.
El roadmap recoge confirmaciones posteriores: End/progresión/portal, idiomas y
recetas de addons funcionan. No tratarlos como defectos abiertos por leer un
informe anterior. Waystones ya tiene un addon local implementado.

## Prioridad propuesta para siguientes tareas

1. Revisar el desarrollo acumulado por subsistema y crear commits locales
   coherentes, con comprobaciones proporcionales. El checkpoint de recuperación
   no sustituye esta revisión ni certifica el código.
2. Vault: investigar recuperación tras caída real; inventario y ledger no son
   atómicos. No borrar @inflight ni reproducir entregas sin reconciliarlas.
3. Multijugador real NP/OpenPAC: OWN, expulsión offline, disolución, prioridad
   del proveedor y claim permanente.
4. Fronteras de protección, migración legacy, matriz de anillos, recarga real de
   datapacks y distribución dedicada independiente: cobertura pendiente.
5. Comandos administrativos: definir vista previa, blancos exactos, límites,
   persistencia y recuperación antes de implementar operaciones destructivas.

Son prioridades propuestas, no autorización para ejecutar cada tarea.
La auditoría del 9 de octubre confirma un fallo de agua entrando al Nexo y
recomienda corregirlo primero. También acredita arranque en JVM servidor separada
y reload del ejemplo infernal desde disco; no cierra instalación limpia del JAR,
recuperación del Vault ni la matriz real de proveedores. Consultar el informe
enlazado antes de repetir pruebas antiguas.
La división gradual de SkyblockMultiMod.java debe acompañar un cambio acotado
con sus pruebas; evitar una reescritura general para ahorrar contexto.

## Flujo de trabajo

Una tarea por comportamiento: síntoma u objetivo, resultado esperado y alcance.
Consultar esta entrada, el apartado del roadmap y las reglas del subsistema.
Leer código y pruebas pertinentes; historial solo si falta explicar una conducta.
Separar defecto reproducido, cobertura no confirmada e idea futura.
Para una entrega de implementación, actualizar versión y notas internas; para
esta reorganización documental se conserva v056 y no se genera ni instala JAR.

## Checkpoint local

Los puntos de recuperación se guardan en refs/checkpoints/ con un índice temporal,
sin modificar el índice habitual ni limpiar el worktree. Incluyen archivos
versionados y nuevos no ignorados; no incluyen build/, run/, logs/, AGENTS.md
ignorado ni archivos externos. No son backup de mundos ni publicación.

Listar: git for-each-ref refs/checkpoints/
Inspeccionar: git show --stat <referencia>
Recuperar primero archivos concretos en una copia separada; no ejecutar un reset
o restore amplio sobre el worktree con cambios del usuario.
