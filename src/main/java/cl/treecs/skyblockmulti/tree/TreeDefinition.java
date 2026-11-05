package cl.treecs.skyblockmulti.tree;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identidad estable de una opción de árbol.
 *
 * <p>El valor {@code id} es la identidad persistente y extensible. Los campos
 * legacy conservan el contrato actual con la configuración y el datapack hasta
 * que esos sistemas puedan migrar a IDs con namespace.</p>
 */
public record TreeDefinition(
        String id,
        String configKey,
        String translationKey,
        int legacyTriggerValue,
        String legacyScoreHolder
) {
    private static final Pattern NAMESPACED_ID = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9_./-]+"
    );

    public TreeDefinition {
        id = requireNamespacedId(id);
        configKey = requireNonBlank(configKey, "configKey");
        translationKey = requireNonBlank(translationKey, "translationKey");
        legacyScoreHolder = requireNonBlank(legacyScoreHolder, "legacyScoreHolder");
        if (legacyTriggerValue <= 0) {
            throw new IllegalArgumentException("legacyTriggerValue debe ser positivo");
        }
    }

    public static String requireNamespacedId(String value) {
        String id = requireNonBlank(value, "id");
        if (!NAMESPACED_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("ID de árbol inválido: " + id);
        }
        return id;
    }

    private static String requireNonBlank(String value, String field) {
        String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return checked;
    }
}
