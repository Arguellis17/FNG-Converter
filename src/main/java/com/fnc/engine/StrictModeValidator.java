package com.fnc.engine;

import com.fnc.model.ConversionTarget;
import com.fnc.model.Grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validador del modo estricto según el enunciado.
 */
public class StrictModeValidator {

    /**
     * Valida la gramática en modo estricto.
     *
     * @param g            gramática a validar
     * @param target       destino de conversión
     * @param autoDeclared mapa de símbolos auto-declarados
     * @return lista de errores (vacía si no hay)
     */
    public static List<String> validate(Grammar g, ConversionTarget target,
                                       Map<String, String> autoDeclared) {
        List<String> errors = new ArrayList<>();
        if (g == null) {
            return errors;
        }

        // 1. Símbolos auto-declarados
        if (autoDeclared != null) {
            for (Map.Entry<String, String> e : autoDeclared.entrySet()) {
                String symbol = e.getKey();
                errors.add("Error (modo estricto): el símbolo '" + symbol + "' no fue declarado.");
            }
        }

        // 2. Si destino es GREIBACH, verificar FNC
        if (target == ConversionTarget.GREIBACH) {
            InputFncInspector inspector = new InputFncInspector();
            InputFncInspector.FncStatus status = inspector.inspect(g);
            if (status == InputFncInspector.FncStatus.NOT_FNC) {
                errors.add("Error (modo estricto): la gramática de entrada no está en FNC.");
                List<String> v = inspector.getViolations();
                int limit = Math.min(3, v.size());
                for (int i = 0; i < limit; i++) {
                    errors.add(v.get(i));
                }
            }
        }
        return errors;
    }
}
