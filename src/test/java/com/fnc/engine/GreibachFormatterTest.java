package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.GrammarFormatter;
import com.fnc.model.Production;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link GreibachFormatter} (salida del paso 13).
 */
class GreibachFormatterTest {

    private static final List<String> ORDER = List.of("S", "A", "B", "T1", "T2");
    private static final List<String> TERMINAL_VARS = List.of("T1", "T2");
    private static final List<String> RECURSION_VARS = List.of("A.1", "B.1");

    @Test
    void g1FinalUsesHeaderOrderAndAuxiliaryLines() {
        Grammar grammar = g1Final(List.of("S", "B", "T1", "A.1", "B.1"));

        String text = GreibachFormatter.formatFinalResult(
                grammar, ORDER, TERMINAL_VARS, RECURSION_VARS);

        assertTrue(text.contains(GrammarFormatter.formatHeader(grammar)));

        List<String> expectedLines = GrammarFormatter.formatProductionLines(grammar);
        for (String line : expectedLines) {
            assertTrue(text.contains(line), "Falta la línea: " + line);
        }

        List<String> order = List.of("S -> ", "B -> ", "T1 -> ", "A.1 -> ", "B.1 -> ");
        int previous = -1;
        for (String marker : order) {
            int index = text.indexOf(marker);
            assertTrue(index > previous, "Orden incorrecto en: " + marker);
            previous = index;
        }

        assertTrue(text.contains("Variables auxiliares de terminales: T1."));
        assertTrue(text.contains("Variables auxiliares de recursividad izquierda: A.1, B.1."));
    }

    @Test
    void orderComesFromTheListNotFromDeclaration() {
        Grammar first = g1Final(List.of("S", "B", "T1", "A.1", "B.1"));
        Grammar second = g1Final(List.of("B.1", "A.1", "T1", "B", "S"));

        String firstText = GreibachFormatter.formatFinalResult(
                first, ORDER, TERMINAL_VARS, RECURSION_VARS);
        String secondText = GreibachFormatter.formatFinalResult(
                second, ORDER, TERMINAL_VARS, RECURSION_VARS);

        assertEquals(List.of("S -> 2 T1 B | 1 B | 2 T1 A.1 B | 1 A.1 B | 2",
                        "B -> 2 | 2 B.1",
                        "T1 -> 1",
                        "A.1 -> 2 T1 | 2 B.1 T1 | 2 T1 A.1 | 2 B.1 T1 A.1",
                        "B.1 -> 2 | 2 B.1"),
                productionLines(firstText));
        assertEquals(productionLines(firstText), productionLines(secondText));
    }

    @Test
    void withoutAuxiliariesOmitsAuxiliaryLines() {
        Grammar grammar = new Grammar(
                new LinkedHashSet<>(List.of("S")),
                new LinkedHashSet<>(List.of("a")),
                List.of(production("S", "a", "S"), production("S", "a")),
                "S");

        String text = GreibachFormatter.formatFinalResult(
                grammar, List.of("S"), List.of(), List.of());

        assertFalse(text.contains("Variables auxiliares de terminales:"));
        assertFalse(text.contains("Variables auxiliares de recursividad izquierda:"));
        assertTrue(text.contains("S -> a S | a"));
    }

    private static List<String> productionLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.contains(" -> ")) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    private static Grammar g1Final(List<String> variablesOrder) {
        return new Grammar(
                new LinkedHashSet<>(variablesOrder),
                new LinkedHashSet<>(List.of("1", "2")),
                List.of(
                        production("S", "2", "T1", "B"),
                        production("S", "1", "B"),
                        production("S", "2", "T1", "A.1", "B"),
                        production("S", "1", "A.1", "B"),
                        production("S", "2"),
                        production("B", "2"),
                        production("B", "2", "B.1"),
                        production("T1", "1"),
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
