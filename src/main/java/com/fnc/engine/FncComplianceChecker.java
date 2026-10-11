package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Paso 7 del pipeline FNG: valida que la gramática recibida esté en la Forma
 * Normal de Chomsky del curso.
 * <p>
 * Verifica que toda producción tenga como máximo 2 símbolos, que no haya
 * producciones unitarias, que no haya producciones ε salvo {@code S → ε}, que
 * todos los símbolos usados estén declarados y que no haya variables inútiles
 * ni inalcanzables (con los eliminadores existentes sobre una copia).
 * <p>
 * Si existe {@code S → ε} se aparta de {@link #check(Grammar) grammarAfter} y se
 * recuerda con {@link #isStartNullable()} para reincorporarlo en el paso 13. Si
 * alguna verificación falla, se informa con una línea {@code "✘ "} por violación
 * y {@link #isCompliant()} devuelve {@code false}; nunca se lanza excepción.
 */
public class FncComplianceChecker {

    private static final int MENU_NUMBER = 7;
    private static final String STEP_NAME = "Validación de FNC";

    private boolean compliant;
    private boolean startNullable;

    /**
     * Revisa la gramática recibida sin mutarla.
     *
     * @param fnc gramática a revisar
     * @return el {@link TransformationStep} del paso 7
     */
    public TransformationStep check(Grammar fnc) {
        Grammar before = fnc.copy();

        List<String> details = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        details.add("Producciones revisadas: " + fnc.getProductions().size() + ".");

        Set<String> declaredSymbols = new LinkedHashSet<>(fnc.getVariables());
        declaredSymbols.addAll(fnc.getTerminals());

        startNullable = false;
        for (Production production : fnc.getProductions()) {
            String left = production.getLeftSide();
            List<String> right = production.getRightSide();
            boolean startNull = production.isNull() && left.equals(fnc.getStartSymbol());
            if (startNull) {
                startNullable = true;
            }

            if (right.size() > 2) {
                violations.add("✘ La producción tiene más de 2 símbolos: " + production + ".");
            }
            if (production.isUnitary()) {
                violations.add("✘ La producción es unitaria: " + production + ".");
            }
            if (production.isNull() && !startNull) {
                violations.add("✘ La producción es ε y no es S → ε: " + production + ".");
            }
            if (!fnc.getVariables().contains(left)) {
                violations.add("✘ El lado izquierdo no es una variable declarada: " + left + ".");
            }
            for (String symbol : right) {
                if (symbol.equals("ε")) {
                    continue;
                }
                if (!declaredSymbols.contains(symbol)) {
                    violations.add("✘ Símbolo no declarado en la producción: "
                            + symbol + " en " + production + ".");
                }
            }
        }

        UselessVariableEliminator useless = new UselessVariableEliminator();
        TransformationStep uselessStep = useless.eliminate(fnc.copy());
        if (uselessStep.hasChanges()) {
            violations.add("✘ Hay variables inútiles: "
                    + String.join(", ", useless.getNonGeneratingVariables(fnc)) + ".");
        }

        UnreachableVariableEliminator unreachable = new UnreachableVariableEliminator();
        TransformationStep unreachableStep = unreachable.eliminate(fnc.copy());
        if (unreachableStep.hasChanges()) {
            violations.add("✘ Hay variables inalcanzables: "
                    + String.join(", ", unreachable.getUnreachableVariables(fnc)) + ".");
        }

        compliant = violations.isEmpty();
        details.addAll(violations);

        Grammar after = fnc.copy();
        if (compliant) {
            details.add("Resultado: la gramática está en FNC del curso.");
        }
        if (startNullable) {
            after.getProductions().removeIf(p -> p.isNull()
                    && p.getLeftSide().equals(fnc.getStartSymbol()));
            details.add("Se aparta S → ε: se reincorporará en el paso 13 si S no "
                    + "aparece en ningún lado derecho.");
        }

        return new TransformationStep(
                STEP_NAME,
                "Se revisa que la gramática esté en la Forma Normal de Chomsky del curso.",
                before,
                after,
                List.of(),
                List.of(),
                details,
                MENU_NUMBER,
                null);
    }

    /**
     * @return {@code true} si no se encontró ninguna violación de FNC.
     */
    public boolean isCompliant() {
        return compliant;
    }

    /**
     * @return {@code true} si se apartó {@code S → ε} de {@code grammarAfter}.
     */
    public boolean isStartNullable() {
        return startNullable;
    }
}
