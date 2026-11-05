# Guardia liviana: solo ejecuta Skyblock Multi cuando el preset fue detectado al cargar.
execute if score #active sb3_const matches 1 run function skyblockmulti:tick_active
