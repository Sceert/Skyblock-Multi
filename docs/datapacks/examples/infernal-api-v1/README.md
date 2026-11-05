# Infernal API v1 example

Datapack de validación para Minecraft 26.2 y Skyblock Multi 0.2.3-beta.

## Instalación

1. Copia `skyblockmulti-infernal-api-v1-example.zip` dentro de
   `<mundo>/datapacks/`.
2. Entra al mundo y ejecuta `/reload`.
3. Revisa `latest.log`. Debe mostrar:

   `API infernal: 1 arenas y 1 pruebas validadas.`

También puedes comprobar que Minecraft reconoce el paquete con `/datapack list`.

## Núcleo experimental

La futura activación usa polvo de blaze como ítem portador y estos datos:

```mcfunction
/give @s minecraft:blaze_powder[minecraft:custom_data={example:{infernal_trial:"ember_oath"}}]
```

En v014 el núcleo inicia el encuentro dinámico de dos etapas. Esta primera versión
ejecutable valida activación, pisos, cantidad y escalado, campeón, victoria,
recompensas, cooldown, abandono y restauración.
