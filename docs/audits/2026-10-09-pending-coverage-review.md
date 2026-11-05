# Revisión de pendientes y cobertura — 9 de octubre de 2026

Versión inspeccionada: 0.2.4-v056-beta, rama feature/world-foundation.
Se revisaron registros, arneses y código actual; se añadieron pruebas optativas,
sin modificar gameplay, persistencia, versión, perfiles personales ni publicaciones.
Esto no es una aprobación global de todos los escenarios pendientes.

## Resultados nuevos

### Fallo reproducido: agua entrando por la frontera del Nexo

`ProtectionBoundaryAudit` crea un mundo desechable y dos canales comparables.
La flor en (45,170,0) está protegida; el agua se coloca fuera, en (46,170,0).
El control exterior usa X=600/601. Ambas flores tienen suelo de césped y el arnés
comprueba explícitamente que pueden sobrevivir antes de colocar el agua.
Tras 80 ticks, el agua elimina la flor protegida. El control exterior también
recibe agua, por lo que el resultado no depende de un canal que no esté activo.

Resultado definitivo: **8 comprobaciones, 7 aprobadas y 1 fallida**:

- Clasificación de X=45 y X=46: aprobada.
- Suelo válido de ambas flores: aprobado.
- Conservación de flor protegida ante agua exterior: FALLA.
- Flujo vanilla del control exterior: aprobado.
- Pistón exterior intentando mover un bloque protegido: bloqueado correctamente.
- Pistón del control exterior: funcional.

La primera pasada tenía suelo de piedra; se corrigió el fixture y se repitió
con suelo válido antes de concluir que existe el fallo. Solo el resultado final
de ocho comprobaciones constituye la reproducción válida.

El código protege usos de cubos mediante callbacks en NexusFoundation, pero no
hay una barrera equivalente al flujo ambiental de agua. La corrección debe actuar
sobre el destino protegido del fluido y conservar el comportamiento exterior.
No se probó lava, otras orientaciones, todas las alturas ni daño a entidades.
No se implementó una corrección en esta auditoría.

Evidencia: `build/audit-20261009-boundary-final.log`.
Arnés: `src/gametest/java/cl/treecs/skyblockmulti/panels/ProtectionBoundaryAudit.java`.

### Servidor separado y recarga real del datapack

Se inició `runServer` en otro proceso JVM con Fabric en entorno SERVER, sin cliente
integrado y con escucha en 127.0.0.1 y puerto asignado automáticamente.
Se usó un directorio nuevo dentro de build, no un mundo existente. La aceptación
de EULA para servidores desechables está documentada en la auditoría v054.

Se copió el datapack de ejemplo de `docs/datapacks/examples/infernal-api-v1` al
directorio de datapacks del mundo de prueba. El log muestra una arena y un trial
validados al arrancar. Tras el mensaje Done, el controlador envió `reload` y esperó
la segunda validación de una arena y un trial antes de comprobar que Overworld,
Nether y End estaban disponibles y enviar `stop`. El servidor guardó y cerró bien.

Resultado: **BUILD SUCCESSFUL**. Evidencia: `build/audit-20261009-independent.log`.
Directorio de esta ejecución: `build/audit-independent-20261009`.
El lanzador reutilizable ahora elige un directorio nuevo con UUID en cada ejecución.
El primer intento se detuvo preventivamente porque Loom había creado el directorio
vacío; se corrigió el guard para rechazar estado previo, no la carpeta vacía.

Esto confirma arranque con classpath de desarrollo exclusivamente servidor y
recarga desde disco del ejemplo válido. No certifica instalación limpia del JAR
distribuido, modificaciones en caliente del datapack, reload durante un combate,
packs inválidos, múltiples addons ni conexión de clientes a este servidor.

## Cobertura histórica comprobada en los registros

| Área | Evidencia existente | Qué queda fuera |
|---|---|---|
| Trials y árboles v055 | 247 PASS: 13 contrato dinámico, 13 ciclo dinámico, 34 trials incorporados, 187 árboles; sin FAIL en el log final | Balance humano, variantes externas y opciones no aplicadas |
| Correcciones v056 | 13 PendingAudit + 23 CoreWorldAudit + 8 VaultAudit = 44 PASS en las ejecuciones definitivas | Caída real del proceso/OS y matriz real de proveedores |
| Protección ambiental v053 | 12 PASS: explosiones, fuego y pistones dentro/fuera | Fluidos y fronteras adicionales; una frontera de agua falla ahora |
| Servidor dedicado del arnés v055 | 4 PASS: generadores de tres dimensiones y primera conexión | Instalación distribuida independiente y multijugador con proveedores |
| API de parties | Dobles para prioridad, solo fallback y liderazgo; coordinador prueba cancelación, expiración, estado obsoleto, OWN adquirido después de la oferta y journal/persistencia | Callbacks reales de NP/OpenPAC, expulsión offline, reinicios y desconexiones reales |
| Anillos | Verificación de 8/16/24, dispersión, 80/128, determinismo y límites técnicos | Matriz completa de asignación/overflow en mundos y defaults cambiados tras reinicio |
| Configuración de árboles | Pruebas de migración JSON/TOML, backup exacto, idempotencia y entrada corrupta | Migración integral de un perfil con mundo, addons y todas las configuraciones |

Estos son resultados históricos acotados, no ejecuciones nuevas de todas las suites.
Los fallos antiguos de Extreme, familias infernales, demora de spawn, libro inicial,
inmovilidad de reingreso y escritura previa del Vault no se reabren sin evidencia.
Las confirmaciones del usuario sobre End, idiomas y recetas se conservan.

## Revisión estática de pendientes reales

1. **Loot original de trials de datapacks y jugadores offline.**
   InfernalTrialRunner.reward (línea 535) solo ejecuta `loot give` cuando
   getPlayer(participant) devuelve un jugador conectado. No encola ese loot en el
   ledger personal. Los suplementos de addons pasan por completeAddonTrial y son
   otra vía. La limitación sigue presente en el código actual; no es un fallo nuevo
   de suplementos offline ni una prueba nueva de desconexión durante combate.
2. **Contrato infernal parcialmente aplicado.** La búsqueda de consumidores y la
   lectura del spawn/reward confirman que baseline_structure, alliance, drop_policy,
   player_targeting_only, equipment.drop_chance, equipment.components y
   rewards.per_participant no se aplican por el runner. persistent se sustituye
   por PersistenceRequired:1b; spread cae en la rama random; effects.icon tampoco
   se usa en el comando de efectos. spawn_delay_ticks y herencia de hijos sí tienen
   implementación y pruebas v055, por lo que no deben listarse entre los ignorados.
3. **Vault tras una caída real.** La intención @inflight se guarda antes de entregar,
   se solicita guardar al jugador y las entregas interrumpidas se retienen para
   revisión. RewardFile usa archivo temporal, copia .bak y movimiento atómico;
   no fuerza flush duradero al dispositivo ni constituye una transacción con
   inventario/entidades soltadas. No se ejecutó una caída real ni se aprueba
   recuperación automática. Conservar marcadores y reconciliar entregas.
4. **Interacciones destructivas con entidades.** La protección nativa revisada
   registra callbacks de bloques/ítems; no se encontró cobertura equivalente con
   AttackEntityCallback/UseEntityCallback o mixins de marcos/armor stands. Esto es
   un hueco de cobertura que necesita reproducción, no un fallo ejecutado aquí.
5. **NP/OpenPAC.** Las pruebas con dobles y el código de prioridad/claim no sustituyen
   la matriz con mods reales y varios jugadores. No se probaron ni modificaron
   claims de un mundo existente.
6. **Administración de islas.** No se encontró la API completa de comandos para
   asignar, transferir, liberar propiedad/slots y limpiar regiones con vista previa
   recuperable. Sigue siendo funcionalidad por diseñar, no prueba fallida.

## Orden recomendado

1. Corregir flujo de agua hacia destinos protegidos y ampliar a lava/fronteras.
2. Resolver el contrato infernal ignorado y la entrega del loot original offline.
3. Diseñar recuperación del Vault y ejecutar fallos de proceso en mundos desechables.
4. Matriz real NP/OpenPAC y distribución limpia, después migración/anillos.
5. Nuevas funciones administrativas y balance cuando la seguridad esté cubierta.

## Reproducción y verificación final

PowerShell, Java 25, desde la raíz del repositorio:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.3'
& 'C:/Gradle/bin/gradle.bat' runClientGameTest '-Ppanels_game_test=boundary-audit' -I tools/audit-pending.init.gradle --offline
& 'C:/Gradle/bin/gradle.bat' runServer -I tools/audit-pending.init.gradle --offline
& 'C:/Gradle/bin/gradle.bat' build --offline --stacktrace
```

El primer comando termina con AssertionError por el fallo de protección conocido,
no por compilación. El segundo envía reload y stop automáticamente. Los arneses
solo se activan con la propiedad optativa y están fuera del JAR normal.

Build final: BUILD SUCCESSFUL, 17 tareas UP-TO-DATE; no se forzaron otra vez las
diez verificaciones de dominio. Evidencia: `build/audit-20261009-build.log`.
JAR v056: mismo SHA-256 registrado en la entrega anterior:
`722F8F63CDD41320DEDE95AA33C22F115C63F76C00A4DED398B9FE203412B813`.
No contiene las clases PanelClientTest, PendingAudit ni ProtectionBoundaryAudit.
Los logs en build son locales y se pueden perder con clean; este informe conserva
el alcance y los resultados. No se entregó una revisión de implementación nueva.
