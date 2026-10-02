package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link UnreachableVariableEliminator}.
 * Casos basados en Hopcroft, Motwani, Ullman (Introduction to Automata Theory).
 */
class UnreachableVariableEliminatorTest {

    /** S → a, A → b (A es inalcanzable desde S). */
    @Test
    void simpleUnreachable() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        UnreachableVariableEliminator eliminator = new UnreachableVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertFalse(eliminator.getReachableVariables().contains("A"),
                "A debe ser inalcanzable");
        assertFalse(result.getProductions().stream()
                        .anyMatch(p -> p.getLeftSide().equals("A")),
                "Las producciones de A deben eliminarse");
    }

    /** S → A → B → a: todas alcanzables a través de la cadena. */
    @Test
    void allReachableThroughChain() {
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

        UnreachableVariableEliminator eliminator = new UnreachableVariableEliminator();
        eliminator.eliminate(grammar);

        assertTrue(eliminator.getReachableVariables().contains("S"), "S es alcanzable");
        assertTrue(eliminator.getReachableVariables().contains("A"), "A es alcanzable");
        assertTrue(eliminator.getReachableVariables().contains("B"), "B es alcanzable");
    }

    /** S → A, A → a, B → C (B y C inalcanzables). */
    @Test
    void chainOfUnreachable() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("a")),
                new Production("B", List.of("C")),
                new Production("C", List.of("a"))
            ),
            "S"
        );

        UnreachableVariableEliminator eliminator = new UnreachableVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertFalse(eliminator.getReachableVariables().contains("B"), "B es inalcanzable");
        assertFalse(eliminator.getReachableVariables().contains("C"), "C es inalcanzable");
        assertFalse(result.getProductions().stream()
                        .anyMatch(p -> p.getLeftSide().equals("B")),
                "Las producciones de B deben eliminarse");
        assertFalse(result.getProductions().stream()
                        .anyMatch(p -> p.getLeftSide().equals("C")),
                "Las producciones de C deben eliminarse");
    }

    /** Gramática donde todas las variables son alcanzables. */
    @Test
    void noUnreachable() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        UnreachableVariableEliminator eliminator = new UnreachableVariableEliminator();
        eliminator.eliminate(grammar);

        assertTrue(eliminator.getReachableVariables().contains("S"), "S es alcanzable");
        assertTrue(eliminator.getReachableVariables().contains("A"), "A es alcanzable");
        assertTrue(eliminator.getProductionsRemoved().isEmpty(),
                "No debe eliminarse ninguna producción");
    }

    /** S → A | B: A y B alcanzables por múltiples caminos. */
    @Test
    void multiplePaths() {
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

        UnreachableVariableEliminator eliminator = new UnreachableVariableEliminator();
        TransformationStep step = eliminator.eliminate(grammar);
        Grammar result = step.getGrammarAfter();

        assertTrue(eliminator.getReachableVariables().contains("A"), "A es alcanzable");
        assertTrue(eliminator.getReachableVariables().contains("B"), "B es alcanzable");
        assertEquals(grammar.getProductions().size(), result.getProductions().size(),
                "Todas las producciones deben conservarse");
    }
}
