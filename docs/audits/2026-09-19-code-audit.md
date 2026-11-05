# Auditoría de código — 19 de septiembre de 2026

## Alcance y estado

Revisión transversal del estado de trabajo en `feature/world-foundation`: configuración,
migraciones, recetas, interfaz Cloth Config, traducciones, protección regional,
participantes y recompensas infernales, proveedores de parties, paquetes de red,
geometría dimensional, tareas de verificación y CI. La versión se obtiene de
`gradle.properties`; no se cambió ni se publicó una versión durante esta auditoría.
Se conservaron los numerosos cambios que ya existían sin confirmar.

Esta revisión no equivale a comprobar cada comportamiento dentro de Minecraft ni a
un perfil de rendimiento con jugadores. Los pendientes concretos están separados de
las correcciones verificadas.

## Correcciones realizadas

| Área | Problema y cambio |
| --- | --- |
| Recetas | Nueva subsección **Dificultad → Crafteos**. Un interruptor, activado por defecto, controla las 23 recetas añadidas de materiales vanilla. Se conservan las recetas originales de Minecraft, las de otros namespaces y los objetos de progresión del Nexo. |
| Persistencia del interruptor | `[difficulty.crafting].vanilla_recipes_enabled` en `general.toml`, creado también sin Mod Menu. Se conserva al guardar las otras opciones y se aplica al cargar recetas en el servidor. La UI impide cambiarlo con un mundo abierto. |
| Rendimiento de recarga | El filtro de addons toma una instantánea de las opciones una vez por recarga. Cada archivo de addon se lee una vez para construirla, en lugar de releerlo por cada receta y función declarada. No se atribuye una mejora porcentual sin benchmark. |
| TOML general | La dificultad mínima se escribía sin comilla inicial. La salida nueva es válida y fue inspeccionada en una inicialización real del mod. |
| Migración general | Se conservan radio, capacidad, anillos y política configurados; antes se sustituían por los valores predeterminados. Las geometrías no admitidas detienen la migración antes de tocar el origen. Se conserva una copia `.pre-toml.bak`. |
| Migración de árboles | Las opciones TOML existentes tienen prioridad sobre el JSON antiguo; se conservan las opciones presentes en uno solo de los archivos. Se guarda copia del JSON y un JSON inválido no se elimina. |
| Migración de addons | No se retira el archivo antiguo si el guardado falla. Se guardan copias de migración, se preservan claves existentes no declaradas actualmente y los ajustes actuales tienen prioridad sobre el archivo antiguo. Un error de lectura/JSON ya no se convierte silenciosamente en opciones vacías. |
| Pistones | La protección comprueba todas las posiciones que el resolver va a mover o destruir, sus destinos y las ramas de slime/miel. Antes solo comprobaba el pistón y su vecino inmediato. Se utiliza el resultado del resolver existente, sin calcular de nuevo el movimiento. |
| Fuego | Se comprueba el bloque que va a quemarse o prenderse desde fuera de la región. Antes únicamente se cancelaba el tick del fuego que ya estaba dentro. |
| Participantes infernales | Con proveedor de parties activo, un activador sin grupo participa solo. Los jugadores ajenos ya no entran por el fallback de proximidad. La regla es compartida por pruebas integradas y de datapack. |
| Funciones infernales | Se retiró la asignación indiscriminada del ID de ejecución a todos los jugadores cercanos, que contradecía la selección Java. El abandono considera participantes no espectadores; las pruebas dinámicas tampoco toman la dificultad de un jugador ajeno. |
| API de Nexus Parties | La comparación directa con `API_VERSION`, una constante Java, desaparecía del bytecode por inlining. Ahora se lee una vez la constante de la instalación real antes de registrar el proveedor y los eventos. Los eventos adicionales ajenos al contrato se ignoran. |
| Documentación y herramientas | README actualizado y rutas TOML sincronizadas en `E:/GitHub/AGENTS.md`. No se encontraron `SKILL.md` propios del repositorio; no se instalaron ni modificaron skills globales sin necesidad. |

## Verificación realizada

- Gradle `build --stacktrace` final satisfactorio con Java 25, Loom 1.17.21 y
  Gradle local 9.7.0. CI sigue declarando Gradle 9.5.0; no se comprobó esa ejecución remota.
- Pasan las verificaciones de árboles, API infernal, proveedores de parties,
  geometría radial, regiones lógicas, transferencias de OWN y recetas vanilla.
- Pruebas nuevas: lectura/escritura del interruptor, tablas sin duplicar, aislamiento
  de namespaces y cobertura exacta de los resultados vanilla/progresión; migración
  de geometría; migración de árboles con prioridad TOML, respaldo, idempotencia y
  conservación ante JSON inválido; selección de participantes con y sin proveedor.
- 164 archivos JSON analizados; los 13 idiomas tienen las mismas 544 claves.
- `git diff --check` sin errores de espacios.
- Inspección de APIs del JAR mapeado de Minecraft 26.2 para fuego y pistones.
- Arranque `runServer --initSettings` en `build/audit-server`: los entrypoints del
  mod se inicializan sin Nexus Parties, OpenPAC ni Mod Menu. Se generó e inspeccionó
  `general.toml`, incluyendo dificultad y crafteos. No se abrió un mundo y se mantuvo
  `eula=false`. Esto no prueba la recarga de recetas ni los movimientos dentro del juego.
- El primer intento de aislar el arranque mediante `JavaExec.workingDir` fue
  sobrescrito por Loom y leyó la configuración antigua de `run/`. Rechazó un radio
  1024 sin modificar el JSON original. Se eliminó únicamente el `general.toml`
  parcial de 44 bytes creado por ese intento; JEI volvió a escribir su archivo de
  configuración. El segundo intento configuró también `loom.runs.server.runDir` y
  terminó en la carpeta aislada. No se cargaron mundos en ninguno de los intentos.
- Se conservaron los avisos de obsolescencia de Cloth Config/Gradle y el aviso
  externo de JOML. El arranque también mostró avisos externos de JEI/Amecs y de
  contadores de rendimiento de Windows; no se modificó el sistema operativo.

## Pendientes de mayor importancia

1. **Recompensas y reinicios:** `InfernalRewardLedger` persiste recompensas pendientes
   y ejecuciones completadas, pero no el conjunto activo de participantes ni el
   registro de enemigos ya escalados. Un reinicio a mitad de una prueba necesita una
   recuperación explícita antes de garantizar recompensas y escalado correctos.
   Además, entregar objetos y guardar el ledger son operaciones separadas de la
   persistencia del jugador: no se ha demostrado exactamente-una-vez frente a una
   caída en mitad del cobro. Requiere una solución transaccional y pruebas de fallo.
2. **Loot de pruebas dinámicas:** `InfernalTrialRunner.reward` entrega tablas de loot
   directamente solo a participantes conectados. Ese loot de fase/final no comparte
   la cola persistente de recompensas del Vault y debe revisarse para desconexiones.
3. **CI reproducible:** el adaptador de Nexus Parties importa su API, pero
   `build.gradle` obtiene el JAR de un proyecto hermano local. El workflow no prepara
   esa dependencia. La compilación local no demuestra que una copia limpia de CI
   pueda compilar; hace falta una distribución reproducible de la API o preparar
   expresamente esa dependencia en CI.
4. **Protección completa:** faltan pruebas funcionales en los límites para flujo de
   fluidos desde fuera, interacciones destructivas con entidades y daño de explosión
   a entidades decorativas. Los cambios de esta auditoría cubren fuego y movimientos
   de pistón; no certifican los demás vectores por el solo hecho de compilar.
5. **Configuración:** los lectores TOML usan expresiones regulares, no un parser
   completo. Quedan por endurecer diagnóstico de sintaxis y escrituras atómicas de
   configuración. Radios antiguos fuera de los presets actuales, como 1024, se
   preservan y se rechazan en vez de convertirse silenciosamente; requieren decidir
   su compatibilidad de migración sin modificar geometría de mundos existentes.
6. **Rendimiento real:** las pruebas infernales todavía consultan entidades por tags
   y emiten comandos periódicos. Se necesita medir con partidas representativas
   antes de modificar frecuencias, estructuras de datos o semántica del encuentro.

## Pruebas manuales introducidas por esta revisión

1. Abrir Mod Menu antes de cargar el mundo, desactivar **Dificultad → Crafteos**,
   guardar, abrir un mundo y comprobar que una receta añadida de arena no funciona.
   Comprobar una receta original de Minecraft y un núcleo/ofrenda del Nexo.
   Volver al menú, activar, guardar y reabrir: la receta debe volver.
2. Repetir en servidor dedicado con `vanilla_recipes_enabled = false`; un cliente
   con la opción local activada no debe poder fabricar esas recetas.
3. En una copia de prueba, intentar empujar, extraer y destruir bloques a través
   del límite protegido con pistones normales, pegajosos y ramas de slime/miel.
   Comprobar también un mecanismo equivalente completamente fuera del Nexo.
4. Encender fuego fuera del límite junto a material inflamable protegido; verificar
   que no lo quema ni propaga fuego dentro y que el fuego exterior conserva su conducta.
5. Activar una prueba integrado/datapack sin party, pero con proveedor instalado y
   otro jugador cerca; solo el activador debe contar y recibir recompensas. Repetir
   con miembros de su party y con un jugador de otra party. Los ajenos/espectadores
   no deben impedir el abandono al marcharse los participantes.

No se instaló el JAR en perfiles de juego, no se hicieron commits ni pushes y no se
modificaron islas, claims, estructuras o formatos de estado de mundos existentes.
