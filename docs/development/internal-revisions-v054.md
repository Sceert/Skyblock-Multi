# 0.2.4-v054-beta — Panel nativo

El panel nativo registra directamente sus controles, eventos y HUD desde el
inicializador cliente.

IslandPanelScreen conserva sus controles, foco, minimización, configuración y
renderizado propios. No se cambian payloads, persistencia, permisos, lógica de
islas, protección, progresión, Nexus Foundation ni proveedores de grupos.
Se conservan las verificaciones del modelo y las auditorías del mod.

El artefacto de prueba se entrega en build/libs, sin instalarlo en perfiles
personales.

## Verificación

- `gradle build --stacktrace`: correcto; diez verificaciones de dominio y ciclo
  de vida aprobadas. Incluye proveedores de grupos, transferencias, regiones,
  islas, recetas, árboles y pruebas infernales. El primer intento restringido
  falló al cargar native-platform.dll; la ejecución externa autorizada pasó.
- `git diff --check`: correcto.
- Inspección del JAR: versión 0.2.4-v054-beta. Conserva
  NexusFoundation y metadatos de Nexus Parties/OpenPAC.
- `gradle runClientGameTest '-Ppanels_game_test=standalone' --offline`: correcto
  con Fabric API 0.156.0+26.2. Cliente real automatizado
  en mundo temporal: tecla I registrada, apertura del panel nativo, pantalla
  no pausante, vuelta al HUD y salida del mundo sin fallo.
- Log: `build/v054-native-client.log`. Captura inspeccionada:
  `build/run/clientGameTest/screenshots/0000_skyblock-panel-native.png`.
  Se observa un solo panel con sus controles nativos.

No se realizaron pruebas manuales en perfiles personales. Quedan pendientes
minimizar/recuperar, cerrar/reabrir, configuración/retorno y decisiones de grupos
con proveedores reales en esta revisión. La prueba automatizada de tecla I
comprueba volver al HUD, no el botón X ni todas las transiciones de foco.

Artefacto: `build/libs/Skyblock-Multi-0.2.4-v054-beta.jar`.
SHA-256: `DB611189A6E596A8DA06D83C70FB8483A7EB956E2B34E78FCE0AECF19EF7E768`.
