package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link NullProductionEliminator}.
 * Casos basados en Hopcroft, Motwani, Ullman (Introduction to Automata Theory).
 */
class NullProductionEliminatorTest {

    /**
     * S → Aa, A → ε.
     * Esperado: S → Aa | a.
     */
    @Test
    void simpleNullProduction() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of()) // A → ε
            ),
            "S"
        );

        NullProductionEliminator eliminator = new NullProductionEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertTrue(eliminator.getNullableVariables().contains("A"),
                "A debe identificarse como anulable");
        assertTrue(hasProduction(result, "S", List.of("A", "a")),
                "La producción original S → Aa debe conservarse");
        assertTrue(hasProduction(result, "S", List.of("a")),
                "Debe agregarse S → a");
    }

    /**
     * S → AB, A → a | ε, B → b | ε.
     * Esperado: S → AB | A | B.
     */
    @Test
    void multipleNullables() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("A", List.of("a")),
                new Production("A", List.of()), // A → ε
                new Production("B", List.of("b")),
                new Production("B", List.of()) // B → ε
            ),
            "S"
        );

        NullProductionEliminator eliminator = new NullProductionEliminator();
        Grammar result = eliminator.eliminate(grammar).getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("A", "B")), "S → AB debe conservarse");
        assertTrue(hasProduction(result, "S", List.of("A")), "S → A debe agregarse");
        assertTrue(hasProduction(result, "S", List.of("B")), "S → B debe agregarse");
    }

    /** Gramática sin producciones nulas debe quedar intacta. */
    @Test
    void noNullProductions() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        NullProductionEliminator eliminator = new NullProductionEliminator();
        eliminator.eliminate(grammar);

        assertTrue(eliminator.getNullableVariables().isEmpty(),
                "No debe haber variables anulables");
        assertTrue(eliminator.getProductionsRemoved().isEmpty(),
                "No debe eliminarse ninguna producción");
    }

    /**
     * Anulable indirecta: S → Aa, A → B, B → ε.
     * A es anulable porque A → B y B → ε.
     */
    @Test
    void indirectNullProduction() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("B")),
                new Production("B", List.of()) // B → ε
            ),
            "S"
        );

        NullProductionEliminator eliminator = new NullProductionEliminator();
        Grammar result = eliminator.eliminate(grammar).getGrammarAfter();

        assertTrue(eliminator.getNullableVariables().contains("A"),
                "A debe ser anulable indirecta");
        assertTrue(hasProduction(result, "S", List.of("a")),
                "Debe agregarse S → a");
    }

    /** Si existe S → ε, debe preservarse en el resultado. */
    @Test
    void startSymbolEpsilon() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("S", List.of()), // S → ε
                new Production("A", List.of("a"))
            ),
            "S"
        );

        NullProductionEliminator eliminator = new NullProductionEliminator();
        Grammar result = eliminator.eliminate(grammar).getGrammarAfter();

        assertTrue(eliminator.getNullableVariables().contains("S"),
                "El símbolo inicial S debe ser anulable");
        assertTrue(hasProduction(result, "S", List.of()),
                "S → ε debe preservarse en el resultado");
    }

    private static boolean hasProduction(Grammar grammar, String left, List<String> right) {
        return grammar.getProductions().stream()
                .anyMatch(p -> p.getLeftSide().equals(left)
                        && p.getRightSide().equals(right));
    }
}
