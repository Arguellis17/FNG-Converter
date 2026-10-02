package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link UselessVariableEliminator}.
 * Casos basados en Hopcroft, Motwani, Ullman (Introduction to Automata Theory).
 */
class UselessVariableEliminatorTest {

    /**
     * B solo tiene B → ε. Como ε cuenta como cadena generada (ε ∈ T*),
     * B es generadora y se conserva (solo la quita el paso de nulas).
     */
    @Test
    void epsilonOnlyVariableIsGenerating() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("a")),
                new Production("B", List.of()) // B → ε (generadora vía ε)
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertTrue(eliminator.getGeneratingVariables().contains("B"),
                "B debe ser generadora (B → ε)");
        assertTrue(hasProduction(result, "B", List.of()),
                "B → ε debe conservarse en este paso");
    }

    /** F no tiene producciones: es inútil aunque esté declarada. */
    @Test
    void simpleNonGenerating() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "F"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("a"))
                // F sin producciones → inútil
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertFalse(eliminator.getGeneratingVariables().contains("F"),
                "F no debe ser generadora");
        assertFalse(result.getVariables().contains("F"),
                "F debe salir del conjunto de variables");
    }

    /** Cadena S → A → B → C → a: todas son generadoras. */
    @Test
    void indirectGeneratingChain() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("B")),
                new Production("B", List.of("C")),
                new Production("C", List.of("a"))
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        eliminator.eliminate(grammar);

        assertTrue(eliminator.getGeneratingVariables().contains("S"), "S es generadora");
        assertTrue(eliminator.getGeneratingVariables().contains("A"), "A es generadora");
        assertTrue(eliminator.getGeneratingVariables().contains("B"), "B es generadora");
        assertTrue(eliminator.getGeneratingVariables().contains("C"), "C es generadora");
    }

    /** C no tiene producciones: S, A, B y C son no generadoras. */
    @Test
    void chainOfNonGenerating() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("B")),
                new Production("B", List.of("C"))
                // C has no productions → non-generating
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertFalse(eliminator.getGeneratingVariables().contains("S"), "S no es generadora");
        assertFalse(eliminator.getGeneratingVariables().contains("A"), "A no es generadora");
        assertFalse(eliminator.getGeneratingVariables().contains("B"), "B no es generadora");
        assertFalse(eliminator.getGeneratingVariables().contains("C"), "C no es generadora");
        assertTrue(result.getProductions().isEmpty(),
                "Todas las producciones deben eliminarse");
    }

    /** Gramática donde todas las variables son generadoras. */
    @Test
    void noNonGenerating() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        eliminator.eliminate(grammar);

        assertTrue(eliminator.getGeneratingVariables().contains("S"), "S es generadora");
        assertTrue(eliminator.getGeneratingVariables().contains("A"), "A es generadora");
        assertTrue(eliminator.getProductionsRemoved().isEmpty(),
                "No debe eliminarse ninguna producción");
    }

    /**
     * S → A | B, A → a, B → C (C no generadora).
     * B se vuelve no generadora; S y A siguen generadoras.
     */
    @Test
    void mixedProductions() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("S", List.of("B")),
                new Production("A", List.of("a")),
                new Production("B", List.of("C"))
                // C has no productions → non-generating
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertTrue(eliminator.getGeneratingVariables().contains("S"), "S es generadora");
        assertTrue(eliminator.getGeneratingVariables().contains("A"), "A es generadora");
        assertFalse(eliminator.getGeneratingVariables().contains("B"), "B no es generadora");
        assertFalse(eliminator.getGeneratingVariables().contains("C"), "C no es generadora");

        assertTrue(hasProduction(result, "S", List.of("A")), "S → A debe conservarse");
        assertTrue(hasProduction(result, "A", List.of("a")), "A → a debe conservarse");
        assertFalse(hasProduction(result, "S", List.of("B")), "S → B debe eliminarse");
    }

    /**
     * El ε de una variable generadora NO se elimina aquí.
     * Solo lo elimina el paso de producciones nulas.
     * Ej: E con E → a | ε conserva el ε; F (sin producciones) sale.
     * (Aporte de Juan: fix paso de inútiles ya no elimina producciones vacías.)
     */
    @Test
    void epsilonPreservedForGeneratingVariables() {
        Grammar grammar = new Grammar(
            Set.of("S", "E", "F"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("E")),
                new Production("E", List.of("a")),
                new Production("E", List.of()) // E → ε
                // F sin producciones → inútil
            ),
            "S"
        );

        UselessVariableEliminator eliminator = new UselessVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertTrue(eliminator.getGeneratingVariables().contains("E"),
                "E sigue siendo generadora");
        assertTrue(hasProduction(result, "E", List.of()),
                "E → ε se conserva");
        assertFalse(eliminator.getProductionsRemoved().stream()
                        .anyMatch(s -> s.equals("E -> ε")),
                "E → ε no debe aparecer como eliminada");
    }

    private static boolean hasProduction(Grammar grammar, String left, List<String> right) {
        return grammar.getProductions().stream()
                .anyMatch(p -> p.getLeftSide().equals(left)
                        && p.getRightSide().equals(right));
    }
}
