# Gemini support

## Gemini como apoyo de desarrollo

El usuario autoriza a Codex a usar Gemini mediante Antigravity CLI por iniciativa
propia durante el desarrollo solicitado, sin pedir autorizacion por cada consulta,
cuando permita reducir trabajo y consumo de tokens de Codex. Delegar subtareas
acotadas de diagnostico, analisis, propuestas de codigo o revision independiente;
resolver directamente tareas triviales si delegarlas añade mas contexto y coste.

Desde el directorio de este repositorio, ejecutar en PowerShell:

```powershell
& 'C:/Users/jsarm/OneDrive/Documentos/ChatGPT/Varios/gemini-integration/gemini.ps1' -Prompt 'Subtarea, contexto minimo, restricciones y resultado esperado'
```

- Indicar las versiones reales de Minecraft, loader y Java, los archivos relevantes
  y los invariantes afectados. Enviar fragmentos o diffs concretos, no el repositorio
  ni el historial completo. Agrupar preguntas relacionadas y pedir respuestas
  concisas con hallazgos, ubicaciones y propuestas verificables.
- Codex integra y verifica los resultados con las APIs y pruebas del proyecto.
  El lanzador usa modo plan: Gemini propone; Codex aplica cambios y conserva las
  reglas de versionado, compatibilidad, persistencia y publicacion.
- No repetir en ambos agentes el mismo trabajo salvo una revision independiente
  justificada. No delegar de forma recursiva: Antigravity no debe invocar este
  lanzador ni delegarse a si mismo.
- Usar la cuenta Google AI Plus y el flujo normal de permisos escalados cuando sea
  necesario para acceder a la sesion/red. No extraer credenciales ni enviar secretos.
  No activar API de pago, compras o sobreconsumo; ante cuota agotada, continuar
  con Codex. El ahorro depende de la tarea y no aumenta los limites de Codex.
