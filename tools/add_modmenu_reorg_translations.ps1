$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$langDir = Join-Path $root 'src/main/resources/assets/skyblockmulti/lang'

$english = [ordered]@{
    'skyblockmulti.config.tab.difficulty' = 'Difficulty'
    'skyblockmulti.config.trees.vanilla' = 'Vanilla trees'
    'skyblockmulti.config.trees.modded' = 'Modded trees'
    'skyblockmulti.config.trees.vanilla.summary' = '%s vanilla tree types available.'
    'skyblockmulti.config.trees.modded.summary' = '%s tree types supplied by mods or datapacks.'
    'skyblockmulti.config.difficulty.title' = 'Difficulty and progression'
}
$spanish = [ordered]@{
    'skyblockmulti.config.tab.difficulty' = 'Dificultad'
    'skyblockmulti.config.trees.vanilla' = 'Árboles vanilla'
    'skyblockmulti.config.trees.modded' = 'Árboles de mods'
    'skyblockmulti.config.trees.vanilla.summary' = '%s tipos de árboles vanilla disponibles.'
    'skyblockmulti.config.trees.modded.summary' = '%s tipos de árboles aportados por mods o datapacks.'
    'skyblockmulti.config.difficulty.title' = 'Dificultad y progresión'
}

Get-ChildItem -LiteralPath $langDir -Filter '*.json' | ForEach-Object {
    $text = [IO.File]::ReadAllText($_.FullName, [Text.Encoding]::UTF8).TrimEnd()
    $isEnglish = $_.BaseName.StartsWith('en_')
    $text = if ($isEnglish) {
        $text.Replace('OpenPAC · minimum difficulty after leaving a party: %s',
                'Minimum difficulty after leaving a party: %s')
    } else {
        $text.Replace('OpenPAC · dificultad mínima al salir de una party: %s',
                'Dificultad mínima al salir de una party: %s')
    }
    $values = if ($isEnglish) { $english } else { $spanish }
    $missing = $values.GetEnumerator() | Where-Object { -not $text.Contains('"' + $_.Key + '"') }
    if ($missing.Count -gt 0) {
        $lines = foreach ($entry in $missing) {
            $escaped = $entry.Value.Replace('\', '\\').Replace('"', '\"')
            "  `"$($entry.Key)`": `"$escaped`""
        }
        $closing = $text.LastIndexOf('}')
        $text = $text.Substring(0, $closing).TrimEnd() + ",`n" + ($lines -join ",`n") + "`n}"
    }
    [IO.File]::WriteAllText($_.FullName, $text + "`n", [Text.UTF8Encoding]::new($false))
}
