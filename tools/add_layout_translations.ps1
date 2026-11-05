$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$langDir = Join-Path $root 'src/main/resources/assets/skyblockmulti/lang'

$english = [ordered]@{
    'skyblockmulti.config.tab.layout' = 'Rings'
    'skyblockmulti.config.layout.title' = 'Island ring layout'
    'skyblockmulti.config.layout.ring.button' = 'Ring %s: %s'
    'skyblockmulti.config.layout.capacity.button' = 'Islands: %s'
    'skyblockmulti.config.layout.policy.button' = 'Allocation: %s'
    'skyblockmulti.config.layout.policy.balanced' = 'Balanced'
    'skyblockmulti.config.layout.policy.inner_first' = 'Inner first'
    'skyblockmulti.config.layout.policy.outer_first' = 'Outer first'
    'skyblockmulti.config.layout.policy.difficulty' = 'By difficulty'
    'skyblockmulti.config.layout.policy.random_balanced' = 'Random balanced'
    'skyblockmulti.config.layout.technical.button' = 'Technical capacity (up to 128): %s'
    'skyblockmulti.config.layout.new_world_only' = 'Applies only before the first island is assigned.'
    'skyblockmulti.config.layout.summary' = '%s islands selected among %s enabled positions.'
    'skyblockmulti.config.status.invalid_layout' = 'Invalid ring or capacity combination.'
}
$spanish = [ordered]@{
    'skyblockmulti.config.tab.layout' = 'Anillos'
    'skyblockmulti.config.layout.title' = 'Distribución de anillos de islas'
    'skyblockmulti.config.layout.ring.button' = 'Anillo %s: %s'
    'skyblockmulti.config.layout.capacity.button' = 'Islas: %s'
    'skyblockmulti.config.layout.policy.button' = 'Asignación: %s'
    'skyblockmulti.config.layout.policy.balanced' = 'Equilibrada'
    'skyblockmulti.config.layout.policy.inner_first' = 'Interior primero'
    'skyblockmulti.config.layout.policy.outer_first' = 'Exterior primero'
    'skyblockmulti.config.layout.policy.difficulty' = 'Según dificultad'
    'skyblockmulti.config.layout.policy.random_balanced' = 'Aleatoria equilibrada'
    'skyblockmulti.config.layout.technical.button' = 'Capacidad técnica (hasta 128): %s'
    'skyblockmulti.config.layout.new_world_only' = 'Se aplica únicamente antes de asignar la primera isla.'
    'skyblockmulti.config.layout.summary' = '%s islas seleccionadas entre %s posiciones activas.'
    'skyblockmulti.config.status.invalid_layout' = 'La combinación de anillos y capacidad no es válida.'
}

Get-ChildItem -LiteralPath $langDir -Filter '*.json' | ForEach-Object {
    $text = [IO.File]::ReadAllText($_.FullName, [Text.Encoding]::UTF8).TrimEnd()
    if ($text.Contains('"skyblockmulti.config.tab.layout"')) { return }
    $values = if ($_.BaseName.StartsWith('en_')) { $english } else { $spanish }
    $lines = foreach ($entry in $values.GetEnumerator()) {
        $escaped = $entry.Value.Replace('\', '\\').Replace('"', '\"')
        "  `"$($entry.Key)`": `"$escaped`""
    }
    $closing = $text.LastIndexOf('}')
    $prefix = $text.Substring(0, $closing).TrimEnd()
    $insert = ",`n" + ($lines -join ",`n") + "`n}"
    [IO.File]::WriteAllText($_.FullName, $prefix + $insert + "`n", [Text.UTF8Encoding]::new($false))
}
