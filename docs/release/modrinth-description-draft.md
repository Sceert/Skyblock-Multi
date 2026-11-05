# Skyblock Multi

## English

Configurable multiplayer Skyblock for **Minecraft Java 26.2 / Fabric**.
Start in the **Last Refuge**, choose a tree and difficulty, and establish your
island in a configurable ring layout around the **Ascension Nexus**.

## Build your own progression

- Eleven vanilla tree starts, Random selection and support for addon tree definitions.
- Four difficulties: Extreme, Hard, Standard and Easy. Extreme provides no vanilla
  starter supplies; obtaining replacement saplings is not guaranteed.
- Configurable radial island layouts with up to 128 positions.
- Void Overworld and Nether, with a normal vanilla End.
- Protected central Nexus, fortress progression, offering seals, recipes and lore.
- Infernal Trials with champions, health-based encounter progress and personal
  rewards collected from the Infernal Vault.

## Play solo or with a party

Your personal island remains distinct from the island selected for party play.
Optional party-provider priority is **Nexus Parties → Open Parties and Claims → solo**.
The native island panel provides island information and pending decisions without
depending on an external UI library. Open it with **I** or `/skyblockmulti panel`.

## Configure and extend

Cloth Config provides configuration screens; Mod Menu is optional. Global defaults
and per-world persisted settings are separate. Datapacks and addons can contribute
tree definitions, Infernal encounters and reward supplements through documented APIs.
Separate BOP and Waystones addons are optional and are not bundled with the core.
English and Spanish locale variants are included.

## Requirements

- Minecraft Java **26.2**, Java **25**, Fabric Loader **0.19.3 or newer**.
- **Fabric API** for 26.2 and **Cloth Config API 26.2.155 or newer**.
- Install Skyblock Multi and its required dependencies on both the server and clients.
- Optional: Mod Menu, Nexus Parties, Open Parties and Claims, JEI.

## Beta and world upgrades

Back up the complete world and configuration before updating. World geometry and
existing builds are preserved; do not downgrade worlds with newer reward data.
The current development build improves recovery from reward-write failures, but
inventory and reward records are not a single crash-atomic transaction. Interrupted
deliveries may require administrator review.

---

## Español

Skyblock configurable para **Minecraft Java 26.2 / Fabric**. Llega al **Último
Refugio**, elige árbol y dificultad, y desarrolla tu isla alrededor del
**Nexo del Ascenso**.

### Islas y progresión

- Once árboles vanilla, selección aleatoria y definiciones externas de addons.
- Extremo, Difícil, Estándar y Fácil. Extremo no entrega suministros vanilla;
  no se garantiza obtener retoños para volver a plantar.
- Anillos configurables con hasta 128 posiciones.
- Overworld y Nether vacíos; End con generación vanilla normal.
- Nexo protegido, fortaleza, sellos de progresión, recetas e historia del mundo.
- Desafíos Infernales con campeones, barras de salud y recompensas personales
  reclamadas en la Bóveda Infernal.

### Multijugador y configuración

La isla personal se mantiene separada de la isla elegida para la party.
La prioridad de proveedores opcionales es **Nexus Parties → Open Parties and
Claims → individual**. El panel nativo muestra islas y decisiones pendientes;
se abre con **I** o `/skyblockmulti panel`.

Cloth Config permite configurar el mod; Mod Menu es opcional. Los valores globales
y los datos persistentes de cada mundo se mantienen separados. Datapacks y addons
pueden aportar árboles, encuentros y suplementos de recompensas mediante APIs
documentadas. Los addons de BOP y Waystones son independientes y no están incluidos.
Incluye variantes de idioma en español e inglés.

### Requisitos

- Minecraft Java **26.2**, Java **25**, Fabric Loader **0.19.3 o posterior**.
- **Fabric API** para 26.2 y **Cloth Config API 26.2.155 o posterior**.
- Instalar Skyblock Multi y sus dependencias obligatorias en **servidor y clientes**.
- Opcionales: Mod Menu, Nexus Parties, Open Parties and Claims y JEI.

### Beta y actualización de mundos

Respalda el mundo completo y su configuración antes de actualizar. La geometría
y las construcciones existentes se conservan; no vuelvas a una versión anterior
con datos nuevos de recompensas. Se mejoró la protección ante errores de escritura,
pero inventario y registro no constituyen una sola transacción: una entrega
interrumpida puede requerir revisión administrativa.
