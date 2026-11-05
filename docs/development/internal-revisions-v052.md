# v052 — Estado y protocolo del panel de islas

La versión se obtiene de `gradle.properties`. No es una publicación pública.

- Estado lógico del panel extraído a `IslandPanelState`. Pestaña, página, paginación del texto y operación
  pendiente sobreviven a cambios de foco y minimización. El panel actual no tiene
  campos editables ni inventario: no se inventan borradores ni objetos de cursor.
- Protocolos `island_panel_v3` y `island_panel_request_v2`: sesión y solicitud
  correlacionadas, confirmación separable de una actualización pasiva, rechazo
  explícito, timeout y descarte de respuestas de sesiones anteriores. También se
  confirma `selection`, aunque abra una pantalla exclusiva. Requiere v052 en ambos
  extremos; no se cambia persistencia ni reglas de permisos/acciones.
- Pruebas del modelo en el build y pruebas de cliente optativas en un mundo temporal.
