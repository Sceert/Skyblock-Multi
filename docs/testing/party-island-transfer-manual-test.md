# Pruebas manuales pendientes: anillos y transferencia NP/SM

Estado: **traspaso habitual de party e isla con NP confirmado por el usuario**
(17 de septiembre de 2026). Las variantes de fallo, recuperación y los casos
individuales no expresamente comprobados siguen pendientes. La implementación NP
está documentada en su repositorio; sus pruebas automáticas y las de SM no
confirman estos casos reales. No marcar esta matriz como aprobada sin registrar
el resultado de cada escenario.

## Configurable rings

- No mode selector or compatible 8/16/24 control in Cloth Config, in English and
  Spanish. New TOML uses rings only and no `layout_mode`.
- Configure capacity 8, 16 and 24 through rings, including a sparse Ring 3 layout.
  Verify stable IDs, base radius, angular offsets and difficulty overflow.
- Assign an island, restart and change global defaults: established world ring
  mask, capacity, radius, policy and coordinates must remain locked.
- Verify 80-slot normal / 128-slot technical limits and invalid empty rings.
- Start without NP/OpenPAC: solo creation and all ring policies remain usable.

## Verificación manual de transferencia NP/SM

### Nuevas pruebas: propiedad personal sin NP y mensajes

- Sin NP, con OpenPAC: crear grupo e invitar un miembro. Comprobar el mensaje
  de isla personal; salir o expulsar al dueño del hogar después de cambiar el
  líder. El dueño conserva OWN, construcciones y slot, y vuelve a su isla.
- Miembros restantes con OWN vuelven a ella; quienes no tengan OWN se trasladan
  a la isla válida del líder actual. Verificar ACTIVE y punto de respawn.
- Repetir sin OWN del líder: miembros sin OWN siguen el reset normal al Último
  Refugio (Y=161) y selección. Los miembros con OWN conservan su isla y datos.
- Desconectar al dueño sin quitarlo del grupo: conservar el hogar, sin reset.
  Expulsarlo estando desconectado: resolver el hogar mediante membresía OpenPAC.
- Desconectar un miembro antes de la salida del dueño, reiniciar y reconectar:
  resolver su destino anterior. Tras el traslado/reset, esperar varios ticks,
  morir y reconectar: no repetir el reset ni crear teletransportes reutilizables.
- Con NP, y después con NP + OpenPAC: comprobar el mensaje de coordinación
  SM/NP al formar/unirse al grupo. No cambia OWN ni abre confirmaciones. Repetir
  el traspaso confirmado: solo NP administra la autorización adicional.
- Proveedor externo que no consulta miembros offline: no confundir ausencia
  de conexión con salida. Su adaptador debe implementar `isMember` para resolver
  inmediatamente expulsiones offline; una respuesta desconocida conserva el hogar.

Usar mundo de prueba con copia de seguridad, NP con esta implementación y SM
con contrato público IslandTransferApi v1. No hace falta OpenPAC. Repetir con
OpenPAC para verificar que únicamente NP sea el proveedor autoritativo.

1. A tiene OWN y PARTY_HOME; B no tiene OWN. Invitar B. Desde comando y desde
   ambos menús de transferencia, comprobar la advertencia literal y un solo paso
   adicional de autorización NP. Antes de confirmar, liderazgo, OWN y PARTY_HOME
   siguen iguales. SM no debe abrir otra confirmación para esta excepción.
2. Confirmar el UUID recibido como A. Verificar líder B, OWN B del mismo slot,
   coordenadas y dificultad, sin OWN A sobre ese slot. A sigue miembro con acceso
   normal a PARTY_HOME. Comprobar construcciones, ocupación de slot y datos SM intactos.
3. A sale: sin otra OWN válida, SM lo lleva al domo y aplica su reset normal.
   B y la party conservan la isla. Verificar también muerte/reconexión antes de salir.
4. Cancelar otra oferta y repetir dejando pasar 60 segundos. No cambian líder,
   OWN ni PARTY_HOME. Confirmar UUID cancelado/caducado falla.
5. B ya tiene OWN propia: transferencia social normal, ninguna advertencia
   adicional NP ni sobrescritura de OWN. Verificar el flujo ordinario PARTY_HOME SM.
   Repetir con A sin OWN: dependencia SM pendiente descrita en el contrato.
6. Cambiar líder, destinatario, membresía, OWN, nombre o PARTY_HOME entre oferta
   y autorización. La confirmación anterior falla; ningún UUID autoriza otro
   destinatario. Crear dos ofertas y confirmar la primera. Repetir autorización
   ya completada y probar como B: nunca se ejecuta otra transferencia.
7. Desconectar A o B antes de confirmar. Reconectar y reutilizar UUID: falla.
   Reiniciar con oferta pendiente: no revive la autorización.
8. Inyectar fallos de escritura antes de la decisión SM y durante preparación NP.
   Conservar liderazgo, OWN y PARTY_HOME. Después de corregir el fallo se solicita
   una oferta nueva, no se reutiliza la autorización anterior.
9. En entorno de prueba, interrumpir después de decisión duradera SM, después
   de commit NP y después de proyección SM. Debe requerir recuperación; no otra
   confirmación. Si no completa inmediatamente, miembros desconectados y party
   reservada; impedir leave/kick/disband/transfer/admin. Reiniciar con ambos mods:
   recuperar exactamente el estado decidido, sin duplicar notificaciones ni
   deshacer un cambio posterior al reproducir recibos antiguos.
10. Reiniciar después del éxito y tras una transferencia social posterior.
    Comprobar que los diarios no restauran un líder o PARTY_HOME anteriores.
11. NP sin SM, incluido servidor dedicado: liderazgo normal, sin confirmación
    de isla. SM instalado sin contrato compatible: rechazo claro, sin cambio parcial.
12. Expulsiones, disoluciones y transferencias administrativas no generan esta
    autorización ni transfieren OWN. Mantener sus reglas existentes.

Las pruebas automáticas NP usan un doble del contrato SM para verificar rutas
de autorización y errores, y persistencia real NP en disco. No prueban el
scoreboard/proyección real SM, el reset al domo, renderizado del chat ni el orden
de callbacks durante un arranque real de Fabric. Esos casos requieren esta matriz.

### Dependencia SM pendiente y comprobaciones complementarias

- El caso 5 con A sin OWN queda pendiente del preview SM: debe distinguir
  transferencia social segura de indisponibilidad, sin debilitar el rechazo de
  propiedad ambigua. Véase
  [Dependencia pendiente en SM](../../../Nexus%20Parties/docs/SKYBLOCK_MULTI_INTEGRATION.md).
- Después de una cesión confirmada, probar también expulsión del antiguo líder
  y disolución: sin OWN, aplicar su reset existente y llegada a Last Refuge en
  Y=161, conservando la isla del nuevo propietario.
- Completar la selección de una isla nueva para A y reiniciar: reproducir
  recibos anteriores no debe borrar ni sustituir esa nueva OWN.
- Con NP y OpenPAC, mantener intacto el claim permanente del Nexus y comprobar
  que esta operación no crea ni transfiere claims de islas.
