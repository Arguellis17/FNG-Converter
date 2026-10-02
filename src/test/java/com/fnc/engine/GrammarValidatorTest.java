package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link GrammarValidator}.
 */
class GrammarValidatorTest {

    @Test
    void validGrammarPasses() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("A", List.of("a")),
                new Production("B", List.of("b"))
            ),
            "S"
        );

        assertTrue(new GrammarValidator().validate(grammar));
    }

    @Test
    void missingVariablesFails() {
        Grammar grammar = new Grammar(
            Set.of(),
            Set.of("a", "b"),
            List.of(new Production("S", List.of("a"))),
            "S"
        );

        assertFalse(new GrammarValidator().validate(grammar));
    }

    @Test
    void missingTerminalsFails() {
        Grammar grammar = new Grammar(
            Set.of("S"),
            Set.of(),
            List.of(new Production("S", List.of("S"))),
            "S"
        );

        assertFalse(new GrammarValidator().validate(grammar));
    }

    @Test
    void missingStartSymbolFails() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(new Production("S", List.of("a"))),
            null
        );

        assertFalse(new GrammarValidator().validate(grammar));
    }

    @Test
    void invalidStartSymbolFails() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(new Production("S", List.of("a"))),
            "X" // X is not in variables
        );

        assertFalse(new GrammarValidator().validate(grammar));
    }

    @Test
    void undeclaredSymbolInProductionFails() {
        Grammar grammar = new Grammar(
            Set.of("S"),
            Set.of("a"),
            List.of(new Production("S", List.of("a", "B"))), // B not declared
            "S"
        );

        assertFalse(new GrammarValidator().validate(grammar));
    }

    @Test
    void duplicateProductionsWarn() {
        Grammar grammar = new Grammar(
            Set.of("S"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("a")),
                new Production("S", List.of("a")) // duplicate
            ),
            "S"
        );

        GrammarValidator validator = new GrammarValidator();
        assertTrue(validator.validate(grammar));
        assertFalse(validator.getWarnings().isEmpty(),
                "Se esperaba una advertencia por producción duplicada");
    }

    @Test
    void overlapVariablesTerminalsFails() {
        Grammar grammar = new Grammar(
            Set.of("S", "a"), // 'a' is both variable and terminal
            Set.of("a", "b"),
            List.of(new Production("S", List.of("a"))),
            "S"
        );

        assertFalse(new GrammarValidator().validate(grammar));
    }
}
