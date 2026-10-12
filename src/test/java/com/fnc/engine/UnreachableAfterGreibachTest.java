package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del paso 12: la eliminación de inalcanzables reutilizada después del
 * paso 11 (FNG), renumerada con {@code withMenuNumber(12)}.
 */
class UnreachableAfterGreibachTest {

    @Test
    void removesUnreachableVariablesInDeclarationOrder() {
        Grammar grammar = g1AfterStep11();

        TransformationStep step = new UnreachableVariableEliminator()
                .eliminate(grammar)
                .withMenuNumber(12);

        assertEquals(12, step.getMenuNumber());
        assertEquals("Eliminación de variables inalcanzables", step.getStepName());
        assertTrue(step.hasChanges());
        assertEquals(List.of(
                        "Se elimina la variable inalcanzable: A",
                        "Se elimina la variable inalcanzable: T2"),
                step.getDetails());

        assertFalse(step.getGrammarAfter().getVariables().contains("A"));
        assertFalse(step.getGrammarAfter().getVariables().contains("T2"));
        assertTrue(step.getGrammarAfter().getVariables().contains("S"));
        assertTrue(step.getGrammarAfter().getVariables().contains("B"));
    }

    private static Grammar g1AfterStep11() {
        return new Grammar(
                new LinkedHashSet<>(List.of("S", "A", "B", "T1", "T2", "A.1", "B.1")),
                new LinkedHashSet<>(List.of("1", "2")),
                List.of(
                        production("S", "2", "T1", "B"),
                        production("S", "1", "B"),
                        production("S", "2", "T1", "A.1", "B"),
                        production("S", "1", "A.1", "B"),
                        production("S", "2"),
                        production("A", "2", "T1"),
                        production("A", "1"),
                        production("A", "2", "T1", "A.1"),
                        production("A", "1", "A.1"),
                        production("B", "2"),
                        production("B", "2", "B.1"),
                        production("T1", "1"),
                        production("T2", "2"),
                        production("A.1", "2", "T1"),
                        production("A.1", "2", "B.1", "T1"),
                        production("A.1", "2", "T1", "A.1"),
                        production("A.1", "2", "B.1", "T1", "A.1"),
                        production("B.1", "2"),
                        production("B.1", "2", "B.1")),
                "S");
    }

    private static Production production(String left, String... right) {
        return new Production(left, List.of(right));
    }
}
