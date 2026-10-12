package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Paso 13 del pipeline FNG: validación final y organización.
 * <p>
 * Reincorpora {@code S → ε} cuando el paso 7 apartó la cadena vacía
 * ({@code startNullable}) y el símbolo inicial no aparece en ningún lado derecho;
 * valida que la gramática resultante esté en Forma Normal de Greibach; y arma el
 * cuerpo final con {@link GreibachFormatter}. Nunca lanza excepción: si hay
 * violaciones, las informa con líneas {@code "✘ "} y {@link #isValid()} devuelve
 * {@code false}.
 * <p>
 * Reglas de validación (clasificando con {@code grammar.getVariables()} /
 * {@code grammar.getTerminals()}): toda producción es {@code Y → x α} (primer
 * símbolo terminal, el resto variables), salvo la {@code S → ε} reincorporada; todos
 * los símbolos usados están declarados; y toda variable tiene al menos una
 * producción.
 */
public class GreibachFinalizer {

    private static final int MENU_NUMBER = 13;
    private static final String STEP_NAME = "Validación final de FNG y organización";
    private static final String REINSERTED_MESSAGE =
            "Se reincorpora S → ε: S no aparece en ningún lado derecho.";
    private static final String NOT_REINSERTED_MESSAGE =
            "La FNG no puede generar la cadena vacía sin un símbolo inicial nuevo; "
            + "el lenguaje es equivalente salvo ε.";
    private static final String VALID_MESSAGE =
            "Resultado: la gramática está en Forma Normal de Greibach.";
    private static final String INVALID_MESSAGE =
            "Resultado: la gramática NO está en Forma Normal de Greibach.";

    private final boolean startNullable;
    private final List<String> order;
    private final List<String> terminalVariables;
    private final List<String> recursionVariables;

    private boolean valid;
    private boolean emptyStringReinserted;

    /**
     * Crea el finalizador.
     *
     * @param startNullable      {@code true} si el paso 7 apartó {@code S → ε}
     * @param order              orden de variables del paso 9
     * @param terminalVariables  variables auxiliares {@code Tt} del paso 8
     * @param recursionVariables variables auxiliares {@code V.1} del paso 10
     */
    public GreibachFinalizer(boolean startNullable, List<String> order,
                             List<String> terminalVariables, List<String> recursionVariables) {
        this.startNullable = startNullable;
        this.order = new ArrayList<>(order);
        this.terminalVariables = new ArrayList<>(terminalVariables);
        this.recursionVariables = new ArrayList<>(recursionVariables);
    }

    /**
     * Valida y organiza la gramática recibida sin mutarla.
     *
     * @param grammar gramática de entrada (salida del paso 12)
     * @return el {@link TransformationStep} del paso 13
     */
    public TransformationStep validateAndOrganize(Grammar grammar) {
        Grammar before = grammar.copy();
        Grammar after = grammar.copy();
        List<String> productionsAdded = new ArrayList<>();
        List<String> details = new ArrayList<>();
        emptyStringReinserted = false;

        String start = grammar.getStartSymbol();
        if (startNullable && !startsOnRightSide(grammar, start)) {
            Production epsilon = new Production(start, List.of());
            insertAfterLastOf(after, start, epsilon);
            productionsAdded.add(epsilon.toString());
            emptyStringReinserted = true;
            details.add(REINSERTED_MESSAGE);
        } else if (startNullable) {
            details.add(NOT_REINSERTED_MESSAGE);
        }

        details.add("Validación final de FNG:");

        List<String> shapeViolations = new ArrayList<>();
        List<String> undeclared = new ArrayList<>();
        List<String> withoutProductions = new ArrayList<>();
        validateShape(after, shapeViolations);
        validateDeclared(after, undeclared);
        validateHaveProductions(after, withoutProductions);

        if (shapeViolations.isEmpty()) {
            details.add("✔ Toda producción empieza con un terminal seguido solo de variables.");
        } else {
            details.addAll(shapeViolations);
        }

        if (undeclared.isEmpty()) {
            details.add("✔ Todos los símbolos están declarados.");
        } else {
            details.addAll(undeclared);
        }

        if (withoutProductions.isEmpty()) {
            details.add("✔ Todas las variables tienen producciones.");
        } else {
            details.addAll(withoutProductions);
        }

        valid = shapeViolations.isEmpty() && undeclared.isEmpty() && withoutProductions.isEmpty();
        details.add(valid ? VALID_MESSAGE : INVALID_MESSAGE);

        String customBody = GreibachFormatter.formatFinalResult(
                after, order, terminalVariables, recursionVariables);

        return new TransformationStep(
                STEP_NAME,
                "Se revisa que la gramática esté en Forma Normal de Greibach y se arma "
                + "la salida final.",
                before,
                after,
                List.of(),
                productionsAdded,
                details,
                MENU_NUMBER,
                customBody);
    }

    /**
     * @return {@code true} si la gramática quedó en Forma Normal de Greibach
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return {@code true} si se reincorporó {@code S → ε}
     */
    public boolean isEmptyStringReinserted() {
        return emptyStringReinserted;
    }

    private static boolean startsOnRightSide(Grammar grammar, String symbol) {
        for (Production production : grammar.getProductions()) {
            if (production.getRightSide().contains(symbol)) {
                return true;
            }
        }
        return false;
    }

    private static void insertAfterLastOf(Grammar grammar, String variable, Production production) {
        List<Production> productions = grammar.getProductions();
        int last = -1;
        for (int i = 0; i < productions.size(); i++) {
            if (productions.get(i).getLeftSide().equals(variable)) {
                last = i;
            }
        }
        if (last < 0) {
            productions.add(production);
        } else {
            productions.add(last + 1, production);
        }
    }

    private static void validateShape(Grammar grammar, List<String> violations) {
        for (Production production : grammar.getProductions()) {
            List<String> rightSide = production.getRightSide();
            if (rightSide.isEmpty()) {
                if (!production.getLeftSide().equals(grammar.getStartSymbol())) {
                    violations.add(violationOf(production));
                }
                continue;
            }
            boolean ok = grammar.getTerminals().contains(rightSide.get(0));
            if (ok) {
                for (int i = 1; i < rightSide.size(); i++) {
                    if (!grammar.getVariables().contains(rightSide.get(i))) {
                        ok = false;
                        break;
                    }
                }
            }
            if (!ok) {
                violations.add(violationOf(production));
            }
        }
    }

    private static void validateDeclared(Grammar grammar, List<String> violations) {
        for (Production production : grammar.getProductions()) {
            if (!grammar.getVariables().contains(production.getLeftSide())) {
                violations.add("✘ Símbolo no declarado: " + production.getLeftSide() + ".");
            }
            for (String symbol : production.getRightSide()) {
                if (symbol == null || symbol.isEmpty() || symbol.equals("ε")) {
                    continue;
                }
                if (!grammar.getVariables().contains(symbol)
                        && !grammar.getTerminals().contains(symbol)) {
                    violations.add("✘ Símbolo no declarado: " + symbol + ".");
                }
            }
        }
    }

    private static void validateHaveProductions(Grammar grammar, List<String> violations) {
        for (String variable : grammar.getVariables()) {
            boolean has = false;
            for (Production production : grammar.getProductions()) {
                if (production.getLeftSide().equals(variable)) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                violations.add("✘ La variable " + variable + " no tiene producciones.");
            }
        }
    }

    private static String violationOf(Production production) {
        List<String> rightSide = production.getRightSide();
        String right = rightSide.isEmpty() ? "ε" : String.join(" ", rightSide);
        return "✘ La producción " + production.getLeftSide() + " → " + right
                + " no cumple Y → x α.";
    }
}
