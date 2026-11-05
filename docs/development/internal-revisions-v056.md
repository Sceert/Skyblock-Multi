# v056 — entrega segura ante fallo de escritura y flujo inicial

Nota documental del 9 de octubre: la confirmación del usuario recogida en
ROADMAP.md el 4 de octubre cierra las pruebas generales de End/progresión/portal.
La lista final de este informe conserva el estado histórico de la ejecución;
consultar el roadmap para pendientes actuales.

- El Vault guarda intención de entrega antes de dar objetos. Si falla la escritura,
  conserva la cola y no entrega nada. Una entrega interrumpida queda retenida con
  `@inflight`, también tras reiniciar, y no se reclama automáticamente otra vez.
  El contenido original permanece en el registro para revisión administrativa.
- Se solicita guardar al jugador antes de finalizar la reclamación. No se promete
  una transacción atómica entre inventario, objetos soltados y registro: la recuperación
  automática tras cualquier caída sigue pendiente. No borrar marcadores a ciegas.
- El reingreso tras party permite caminar; mantiene protección temporal y el reset
  existente para quien no tiene OWN.
- Libro oficial al llegar al Refugio; marca persistente evita duplicarlo al asignar
  isla o reconectar. El reset completo sin OWN renueva el libro tras limpiar inventario.
- Mensajes del Vault en los 13 idiomas y actualización del estado del roadmap.

Validación del 4 de octubre de 2026:

- 13 comprobaciones de `PendingAudit` aprobadas: fallo real de escritura, ausencia
  de entrega, reclamación posterior sin duplicación, retención tras recargas,
  compatibilidad legacy y movimiento durante selección.
- 23 comprobaciones de `CoreWorldAudit` aprobadas: llegada con libro, asignación
  sin duplicarlo, cierre/reapertura, OWN/ACTIVE, geometría y protección.
- 8 comprobaciones de `VaultAudit` aprobadas: callbacks reales, deduplicación de
  interacción, suplementos offline y múltiples premios pendientes.
- Total: 44 aprobadas, cero fallidas en las ejecuciones definitivas de cada suite.
  El primer intento de Core falló porque el propio arnés reemplazaba el libro de
  la mano por piedra; ahora conserva y restaura la mano tras probar protección.
- Build normal correcto y diez verificaciones de dominio. Validación JSON y las
  dos claves nuevas presentes en los 13 idiomas. `git diff --check` correcto.

Evidencia: `build/audit-v056-pending-and-core-initial.log` (Pending aprobado;
Core anterior a corregir el arnés), `build/audit-v056-core.log` y
`build/audit-v056-vault.log`.

JAR: `build/libs/Skyblock-Multi-0.2.4-v056-beta.jar`, metadata verificada y sin
clases de auditoría. SHA-256:
`722F8F63CDD41320DEDE95AA33C22F115C63F76C00A4DED398B9FE203412B813`.
No instalado en perfiles personales, sin publicación ni push.

Pendientes: recuperación automática plenamente transaccional del Vault y prueba
de caída real; integración NP/OpenPAC con varios jugadores; recarga real de
datapacks y sus opciones aún sin aplicación; distribución dedicada independiente;
migración integral legacy, matriz de anillos, progresión End, protección de frontera
y accesibilidad. No se presentan como aprobados por este lote.
