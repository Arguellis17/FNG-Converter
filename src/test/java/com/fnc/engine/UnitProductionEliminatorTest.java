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
 * Pruebas unitarias para {@link UnitProductionEliminator}.
 * Casos basados en Hopcroft, Motwani, Ullman (Introduction to Automata Theory).
 */
class UnitProductionEliminatorTest {

    /**
     * S → A, A → a.
     * Esperado: S → a, A → a.
     */
    @Test
    void simpleUnitProduction() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("a"))
            ),
            "S"
        );

        UnitProductionEliminator eliminator = new UnitProductionEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("a")), "Debe agregarse S → a");
        assertTrue(hasProduction(result, "A", List.of("a")), "A → a debe conservarse");
        assertFalse(hasProduction(result, "S", List.of("A")), "S → A debe eliminarse");
    }

    /**
     * Cadena S → A, A → B, B → a.
     * Esperado: S → a, A → a, B → a.
     */
    @Test
    void chainOfUnitProductions() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("B")),
                new Production("B", List.of("a"))
            ),
            "S"
        );

        UnitProductionEliminator eliminator = new UnitProductionEliminator();
        Grammar result = eliminator.eliminate(grammar).getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("a")), "S → a (cadena S→A→B→a)");
        assertTrue(hasProduction(result, "A", List.of("a")), "A → a (cadena A→B→a)");
        assertTrue(hasProduction(result, "B", List.of("a")), "B → a debe conservarse");
    }

    /**
     * Ciclo S → A, A → S, A → a.
     * Esperado: S → a, A → a.
     */
    @Test
    void cycleDetection() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("S")),
                new Production("A", List.of("a"))
            ),
            "S"
        );

        UnitProductionEliminator eliminator = new UnitProductionEliminator();
        Grammar result = eliminator.eliminate(grammar).getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("a")), "S → a (vía ciclo)");
        assertTrue(hasProduction(result, "A", List.of("a")), "A → a debe conservarse");
    }

    /** Gramática sin producciones unitarias debe quedar intacta. */
    @Test
    void noUnitProductions() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        UnitProductionEliminator eliminator = new UnitProductionEliminator();
        eliminator.eliminate(grammar);

        assertTrue(eliminator.getProductionsRemoved().isEmpty(),
                "No debe eliminarse ninguna producción");
        assertTrue(eliminator.getProductionsAdded().isEmpty(),
                "No debe agregarse ninguna producción");
    }

    /**
     * S → A | B, A → a, B → b.
     * Esperado: S → a | b, A → a, B → b.
     */
    @Test
    void multipleTargets() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A")),
                new Production("S", List.of("B")),
                new Production("A", List.of("a")),
                new Production("B", List.of("b"))
            ),
            "S"
        );

        UnitProductionEliminator eliminator = new UnitProductionEliminator();
        Grammar result = eliminator.eliminate(grammar).getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("a")), "S → a (desde A)");
        assertTrue(hasProduction(result, "S", List.of("b")), "S → b (desde B)");
    }

    private static boolean hasProduction(Grammar grammar, String left, List<String> right) {
        return grammar.getProductions().stream()
                .anyMatch(p -> p.getLeftSide().equals(left)
                        && p.getRightSide().equals(right));
    }
}
