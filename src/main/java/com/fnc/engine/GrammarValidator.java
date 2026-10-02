package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that a context-free grammar is correctly defined before transformation.
 *
 * Validation checks:
 * - Variables (non-terminals) are declared
 * - Terminals are declared
 * - Start symbol is declared and belongs to variables
 * - Productions have valid structure
 * - All symbols in productions are declared
 * - No duplicate productions
 * - Variables and terminals don't overlap
 */
public class GrammarValidator {

    private final List<String> errors;
    private final List<String> warnings;

    public GrammarValidator() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Validate the entire grammar.
     * @param grammar the grammar to validate
     * @return true if valid (no errors), false otherwise
     */
    public boolean validate(Grammar grammar) {
        errors.clear();
        warnings.clear();

        validateVariables(grammar);
        validateTerminals(grammar);
        validateStartSymbol(grammar);
        validateNoOverlap(grammar);
        validateProductions(grammar);
        validateDuplicateProductions(grammar);

        return errors.isEmpty();
    }

    /**
     * Check that at least one variable is declared.
     */
    private void validateVariables(Grammar grammar) {
        if (grammar.getVariables() == null || grammar.getVariables().isEmpty()) {
            errors.add("Error: No se han declarado variables (símbolos no terminales).");
            return;
        }

        // Check for empty or blank variable names
        for (String v : grammar.getVariables()) {
            if (v == null || v.trim().isEmpty()) {
                errors.add("Error: Existe una variable con nombre vacío.");
            }
        }
    }

    /**
     * Check that at least one terminal is declared.
     */
    private void validateTerminals(Grammar grammar) {
        if (grammar.getTerminals() == null || grammar.getTerminals().isEmpty()) {
            errors.add("Error: No se han declarado terminales.");
            return;
        }

        // Check for empty or blank terminal names
        for (String t : grammar.getTerminals()) {
            if (t == null || t.trim().isEmpty()) {
                errors.add("Error: Existe un terminal con nombre vacío.");
            }
        }
    }

    /**
     * Check that start symbol is declared and belongs to variables.
     */
    private void validateStartSymbol(Grammar grammar) {
        if (grammar.getStartSymbol() == null || grammar.getStartSymbol().isEmpty()) {
            errors.add("Error: No se ha definido el símbolo inicial.");
            return;
        }

        if (!grammar.getVariables().contains(grammar.getStartSymbol())) {
            errors.add("Error: el símbolo inicial '" + grammar.getStartSymbol()
                    + "' no pertenece al conjunto de variables.");
        }
    }

    /**
     * Check that variables and terminals don't overlap.
     */
    private void validateNoOverlap(Grammar grammar) {
        Set<String> overlap = new HashSet<>(grammar.getVariables());
        overlap.retainAll(grammar.getTerminals());

        if (!overlap.isEmpty()) {
            errors.add("Error: Los siguientes símbolos están en variables y terminales: " + overlap);
        }
    }

    /**
     * Validate structure of all productions.
     */
    private void validateProductions(Grammar grammar) {
        if (grammar.getProductions() == null || grammar.getProductions().isEmpty()) {
            errors.add("Error: No se han declarado producciones.");
            return;
        }

        for (Production p : grammar.getProductions()) {
            validateSingleProduction(grammar, p);
        }
    }

    /**
     * Validate a single production rule.
     */
    private void validateSingleProduction(Grammar grammar, Production p) {
        // Validate left side
        if (p.getLeftSide() == null || p.getLeftSide().isEmpty()) {
            errors.add("Error: Una producción tiene el lado izquierdo vacío.");
            return;
        }

        if (!grammar.getVariables().contains(p.getLeftSide())) {
            errors.add("Error: la variable '" + p.getLeftSide()
                    + "' en el lado izquierdo de la producción no fue declarada.");
            return;
        }

        // Validate right side
        if (p.getRightSide() == null) {
            errors.add("Error: la producción " + p + " tiene el lado derecho nulo.");
            return;
        }

        for (String symbol : p.getRightSide()) {
            // Epsilon is always allowed
            if (symbol.equals("ε") || symbol.equals("\u03B5")) {
                continue;
            }

            // Check symbol is declared
            if (!grammar.getVariables().contains(symbol)
                    && !grammar.getTerminals().contains(symbol)) {
                errors.add("Error: el símbolo '" + symbol
                        + "' utilizado en la producción " + p + " no fue declarado.");
            }
        }
    }

    /**
     * Check for duplicate productions.
     */
    private void validateDuplicateProductions(Grammar grammar) {
        Set<String> seen = new HashSet<>();

        for (Production p : grammar.getProductions()) {
            String key = p.toString();
            if (seen.contains(key)) {
                warnings.add("Advertencia: Producción duplicada ignorada: " + p);
            }
            seen.add(key);
        }
    }

    // Getters
    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Get a formatted validation report.
     */
    public String getReport() {
        StringBuilder sb = new StringBuilder();

        if (errors.isEmpty() && warnings.isEmpty()) {
            sb.append("✓ Gramática válida. No se encontraron errores.");
        } else {
            if (!errors.isEmpty()) {
                sb.append("ERRORES (").append(errors.size()).append("):\n");
                for (int i = 0; i < errors.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                if (!errors.isEmpty()) sb.append("\n");
                sb.append("ADVERTENCIAS (").append(warnings.size()).append("):\n");
                for (int i = 0; i < warnings.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(warnings.get(i)).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
