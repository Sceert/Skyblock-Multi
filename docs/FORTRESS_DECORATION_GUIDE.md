# Guía para decorar la fortaleza sin romper su lógica

La fortaleza puede decorarse directamente en un mundo nuevo. El arte y la lógica se integrarán como dos capas: primero se colocará el schematic visual y después Skyblock Multi colocará los bloques y entidades funcionales. No incluyas entidades al copiar el schematic.

## Flujo recomendado con WorldEdit

1. Genera un mundo nuevo con la versión actual del mod.
2. Decora la fortaleza, conservando despejadas las zonas funcionales indicadas abajo.
3. Selecciona aproximadamente desde `-72 4 -72` hasta `72 36 72`.
4. Colócate exactamente en `0 14 0` antes de copiar; ese será el origen de referencia.
5. Ejecuta `//copy` sin `-e`. La opción `-e` copiaría entidades, por lo que no debe utilizarse.
6. Guarda con `//schem save skyblockmulti_fortress_art_v1 sponge`.
7. Entrega el archivo `.schem` junto con una captura desde el centro mirando al norte.

## Elementos funcionales que deben conservar espacio libre

### Sellos cardinales

| Sello | Armor Stand | Atril de pista |
|---|---:|---:|
| Cuero / norte | `0 16 -25` | `0 15 -26` |
| Oro / este | `25 16 0` | `26 15 0` |
| Hierro / sur | `0 16 25` | `0 15 26` |
| Diamante / oeste | `-25 16 0` | `-26 15 0` |

Deja libre un volumen de al menos `3 × 4 × 3` alrededor de cada Armor Stand. Al completar el sello, ese volumen se transforma en una cápsula de paneles de vidrio transparente que contiene un maniquí de Steve con la armadura entregada. El bloque superior central queda abierto para integrarlo con la decoración definitiva.

### Ofrendas intercardinales

- Cofres funcionales: `18 16 -18`, `18 16 18`, `-18 16 18`, `-18 16 -18`.
- Atriles: `15 16 -18`, `15 16 18`, `-15 16 18`, `-15 16 -18`.

### Pozos de lava renovable

- Fuentes: `0 15 -66`, `66 15 0`, `0 15 66`, `-66 15 0`.
- Cofres de catalizador: `0 16 -69`, `69 16 0`, `0 16 69`, `-69 16 0`.

### Cámara central y Portal del End

- Mantén libre el volumen `-12 4 -12` a `12 36 12`.
- No reemplaces los 12 marcos situados alrededor de `Y=6`.
- Las cuatro entradas cardinales y sus escaleras se abren mediante progresión; no cierres sus ejes.
- Los ocho faros finales aparecen alrededor del centro, entre radios 8 y 9, desde `Y=15` hasta el techo `Y=35`.

## Elementos seguros para modificar

- Paleta de paredes, pisos y techo fuera de las zonas anteriores.
- Arcos, columnas decorativas, ruinas y relieves.
- Cadenas, rejas, losas, escaleras y muros decorativos.
- Iluminación ambiental que no bloquee los haces de los ocho faros.
- Lava y fuego puramente decorativos fuera de las fuentes renovables.

No añadas un spawner de silverfish. Evita cofres o Armor Stands decorativos dentro de las coordenadas funcionales para que no se confundan con los objetivos de progresión.
