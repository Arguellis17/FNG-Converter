package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link ChomskyNormalFormConverter}
 * según el archivo (paso 5): sin reemplazo de terminales, Xn globales
 * sin reutilizar, numeradas en orden de creación.
 */
class ChomskyNormalFormConverterTest {

    /** Gramática ya en FNC (incluye variable+terminal) queda intacta. */
    @Test
    void alreadyInCnf() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("S", List.of("A", "b")),
                new Production("A", List.of("a")),
                new Production("B", List.of("b"))
            ),
            "S"
        );

        ChomskyNormalFormConverter converter = new ChomskyNormalFormConverter();
        TransformationStep step = converter.convert(grammar);

        assertTrue(converter.getProductionsRemoved().isEmpty(),
                "No debe eliminarse ninguna producción");
        assertTrue(converter.getCreatedVariables().isEmpty(),
                "No debe crearse ninguna Xn");
    }

    /**
     * Los terminales NO se reemplazan: S → Aa (binaria con terminal)
     * se conserva tal cual.
     */
    @Test
    void terminalsAreNotReplaced() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        ChomskyNormalFormConverter converter = new ChomskyNormalFormConverter();
        Grammar result = converter.convert(grammar).getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("A", "a")),
                "S → Aa debe conservarse (terminal no se reemplaza)");
        assertTrue(converter.getCreatedVariables().isEmpty(),
                "No debe crearse ninguna Xn");
    }

    /**
     * S → ABC se parte en S → AX1, X1 → BC (se deja el primer símbolo).
     */
    @Test
    void longProductionDecomposition() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C"),
            Set.of("a", "b", "c"),
            List.of(
                new Production("S", List.of("A", "B", "C")),
                new Production("A", List.of("a")),
                new Production("B", List.of("b")),
                new Production("C", List.of("c"))
            ),
            "S"
        );

        ChomskyNormalFormConverter converter = new ChomskyNormalFormConverter();
        Grammar result = converter.convert(grammar).getGrammarAfter();

        assertTrue(hasProduction(result, "S", List.of("A", "X1")),
                "S → AX1");
        assertTrue(hasProduction(result, "X1", List.of("B", "C")),
                "X1 → BC");
        assertEquals(List.of("X1"), converter.getCreatedVariables(),
                "Solo debe crearse X1");
    }

    /**
     * Sin reutilizar: dos colas iguales generan dos Xn distintas.
     * S → ABC, D → ABC ⇒ X1 → BC y X2 → BC.
     */
    @Test
    void noVariableReuse() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C", "D"),
            Set.of("a", "b", "c"),
            List.of(
                new Production("S", List.of("A", "B", "C")),
                new Production("D", List.of("A", "B", "C"))
            ),
            "S"
        );

        ChomskyNormalFormConverter converter = new ChomskyNormalFormConverter();
        Grammar result = converter.convert(grammar).getGrammarAfter();

        assertEquals(List.of("X1", "X2"), converter.getCreatedVariables(),
                "Cada partición crea su propia Xn");
        assertTrue(hasProduction(result, "X1", List.of("B", "C")), "X1 → BC");
        assertTrue(hasProduction(result, "X2", List.of("B", "C")), "X2 → BC");
    }

    /** Los nombres ocupados se saltan y nunca se genera "X" sola. */
    @Test
    void skipsTakenNames() {
        Grammar grammar = new Grammar(
            Set.of("S", "A", "B", "C", "X1"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A", "B", "C")),
                new Production("X1", List.of("a"))
            ),
            "S"
        );

        ChomskyNormalFormConverter converter = new ChomskyNormalFormConverter();
        converter.convert(grammar);

        assertEquals(List.of("X2"), converter.getCreatedVariables(),
                "X1 está ocupada: la primera libre es X2");
    }

    private static boolean hasProduction(Grammar grammar, String left, List<String> right) {
        return grammar.getProductions().stream()
                .anyMatch(p -> p.getLeftSide().equals(left)
                        && p.getRightSide().equals(right));
    }
}
