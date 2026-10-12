package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import com.fnc.testsupport.LanguageEnumerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link GreibachService}: pipeline completo (pasos 0 a 13).
 */
class GreibachServiceTest {

    private static final String FNG_RESULT =
            "Resultado: la gramática está en Forma Normal de Greibach.";

    @Test
    void g1RunsFromGeneralGrammarToGreibach() {
        List<TransformationStep> steps = new GreibachService().convertFull(g1());

        assertEquals(17, steps.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 11, 11, 11, 12, 13),
                menuNumbers(steps));

        TransformationStep last = steps.get(steps.size() - 1);
        assertEquals(List.of(
                        "S -> 2 T1 B | 1 B | 2 T1 A.1 B | 1 A.1 B | 2",
                        "B -> 2 | 2 B.1",
                        "T1 -> 1",
                        "A.1 -> 2 T1 | 2 B.1 T1 | 2 T1 A.1 | 2 B.1 T1 A.1",
                        "B.1 -> 2 | 2 B.1"),
                productionLines(last.getCustomBody()));
        assertTrue(last.getDetails().contains(FNG_RESULT));
    }

    @Test
    void generalGrammarWithEpsilonReachesGreibach() {
        List<TransformationStep> steps = new GreibachService().convertFull(aSb());

        assertEquals(15, steps.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 11, 12, 13),
                menuNumbers(steps));

        TransformationStep last = steps.get(steps.size() - 1);
        assertEquals(List.of(
                        "S -> a X1 | a Tb | ε",
                        "X1 -> a X1 Tb | a Tb Tb",
                        "Tb -> b"),
                productionLines(last.getCustomBody()));
        assertTrue(last.getDetails().contains(FNG_RESULT));
    }

    @Test
    void languageIsPreservedEndToEnd() {
        assertLanguagePreserved(aSb());
        assertLanguagePreserved(mixedNullsUnitsAndVariables());
        assertLanguagePreserved(g1());
    }

    @Test
    void emptyLanguageProducesSevenStoppedSteps() {
        List<TransformationStep> steps = new GreibachService().convertFull(emptyLanguageGrammar());

        assertEquals(14, steps.size());
        List<Integer> menus = menuNumbers(steps);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), menus);

        for (int i = 7; i <= 13; i++) {
            TransformationStep step = steps.get(i);
            assertEquals(i, step.getMenuNumber());
            assertFalse(step.hasChanges());
            assertTrue(step.getDescription().contains("proceso detenido: lenguaje vacío"),
                    "El paso " + i + " debería estar detenido: " + step.getDescription());
        }
    }

    @Test
    void inputIsNotMutated() {
        assertNotMutated(g1());
        assertNotMutated(aSb());
        assertNotMutated(emptyLanguageGrammar());
    }

    @Test
    void isDeterministic() {
        assertSameRun(g1());
        assertSameRun(aSb());
        assertSameRun(emptyLanguageGrammar());
    }

    @Test
    void menuNumbersAreNonDecreasingFromStepSeven() {
        for (Grammar input : List.of(g1(), aSb(), mixedNullsUnitsAndVariables())) {
            List<Integer> menus = menuNumbers(new GreibachService().convertFull(input));
            int previous = -1;
            for (int i = 7; i < menus.size(); i++) {
                assertTrue(menus.get(i) >= previous, "menuNumber decreció en el paso " + i);
                previous = menus.get(i);
            }
        }
    }

    private static void assertLanguagePreserved(Grammar input) {
        List<TransformationStep> steps = new GreibachService().convertFull(input);
        TransformationStep last = steps.get(steps.size() - 1);
        assertTrue(last.getDetails().contains(FNG_RESULT));

        Grammar finalGrammar = last.getGrammarAfter();
        Set<String> expected = LanguageEnumerator.generate(input, 9);
        if (!hasEpsilon(finalGrammar)) {
            expected.remove("");
        }
        assertEquals(expected, LanguageEnumerator.generate(finalGrammar, 9));
    }

    private static void assertNotMutated(Grammar input) {
        String before = input.toString();
        new GreibachService().convertFull(input);
        assertEquals(before, input.toString());
    }

    private static void assertSameRun(Grammar input) {
        List<TransformationStep> first = new GreibachService().convertFull(input);
        List<TransformationStep> second = new GreibachService().convertFull(input);
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getMenuNumber(), second.get(i).getMenuNumber());
            assertEquals(first.get(i).getDescription(), second.get(i).getDescription());
            assertEquals(first.get(i).getDetails(), second.get(i).getDetails());
            assertEquals(productionStrings(first.get(i).getGrammarBefore()),
                    productionStrings(second.get(i).getGrammarBefore()));
            assertEquals(productionStrings(first.get(i).getGrammarAfter()),
                    productionStrings(second.get(i).getGrammarAfter()));
        }
    }

    private static boolean hasEpsilon(Grammar grammar) {
        for (Production production : grammar.getProductions()) {
            if (production.getLeftSide().equals(grammar.getStartSymbol())
                    && production.getRightSide().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> menuNumbers(List<TransformationStep> steps) {
        List<Integer> numbers = new ArrayList<>();
        for (TransformationStep step : steps) {
            numbers.add(step.getMenuNumber());
        }
        return numbers;
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

    private static List<String> productionStrings(Grammar grammar) {
        List<String> lines = new ArrayList<>();
        for (Production production : grammar.getProductions()) {
            lines.add(production.toString());
        }
        return lines;
    }

    private static Grammar g1() {
        return grammar(List.of("S", "A", "B"), List.of("1", "2"), List.of(
                production("S", "A", "B"),
                production("S", "2"),
                production("A", "S", "1"),
                production("A", "1"),
                production("B", "B", "2"),
                production("B", "2")));
    }

    private static Grammar aSb() {
        return grammar(List.of("S"), List.of("a", "b"), List.of(
                production("S", "a", "S", "b"),
                production("S")));
    }

    private static Grammar mixedNullsUnitsAndVariables() {
        return grammar(List.of("S", "A", "B", "C"), List.of("a", "b", "c"), List.of(
                production("S", "A", "B"),
                production("S", "C"),
                production("A", "a", "A"),
                production("A"),
                production("B", "b", "B"),
                production("B", "b"),
                production("C", "c")));
    }

    private static Grammar emptyLanguageGrammar() {
        return grammar(List.of("S"), List.of("a"), List.of(
                production("S", "S", "a")));
    }

    private static Grammar grammar(List<String> variables, List<String> terminals,
                                   List<Production> productions) {
        return new Grammar(
                new LinkedHashSet<>(variables),
                new LinkedHashSet<>(terminals),
                productions,
                "S");
    }

    private static Production production(String left, String... right) {
        return new Production(left, List.of(right));
    }
}
