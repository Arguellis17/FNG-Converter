package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import com.fnc.testsupport.LanguageEnumerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link BackwardSubstitutor} (paso 11), con los vectores
 * de la especificación FNG. Cada fixture construye directamente la gramática de
 * salida del paso 10 y su orden de variables / auxiliares.
 */
class BackwardSubstitutorTest {

    /** G1: cambian S, A.1 y B.1 en ese orden. */
    @Test
    void g1SubstitutesSThenAuxiliaries() {
        List<TransformationStep> steps = substitute(g1After10(), orderG1(), List.of("A.1", "B.1"));

        assertEquals(3, steps.size());

        TransformationStep stepS = steps.get(0);
        assertEquals("Sustitución hacia atrás — S", stepS.getStepName());
        assertEquals(11, stepS.getMenuNumber());
        assertEquals(List.of("Sustituyendo A en S: S → A B."), stepS.getDetails());
        assertEquals(List.of("S -> 2 T1 B", "S -> 1 B", "S -> 2 T1 A.1 B", "S -> 1 A.1 B", "S -> 2"),
                productionsOf(stepS.getGrammarAfter(), "S"));

        TransformationStep stepA1 = steps.get(1);
        assertEquals("Sustitución hacia atrás — A.1", stepA1.getStepName());
        assertEquals(List.of("Sustituyendo B en A.1: A.1 → B T1, A.1 → B T1 A.1."),
                stepA1.getDetails());
        assertEquals(List.of("A.1 -> 2 T1", "A.1 -> 2 B.1 T1", "A.1 -> 2 T1 A.1", "A.1 -> 2 B.1 T1 A.1"),
                productionsOf(stepA1.getGrammarAfter(), "A.1"));
        assertEquals(productionsOf(stepS.getGrammarAfter(), "S"),
                productionsOf(stepA1.getGrammarBefore(), "S"));

        TransformationStep stepB1 = steps.get(2);
        assertEquals("Sustitución hacia atrás — B.1", stepB1.getStepName());
        assertEquals(List.of("Sustituyendo T2 en B.1: B.1 → T2, B.1 → T2 B.1."),
                stepB1.getDetails());
        assertEquals(List.of("B.1 -> 2", "B.1 -> 2 B.1"),
                productionsOf(stepB1.getGrammarAfter(), "B.1"));
    }

    /** G2: cambian A, S y B.1 en ese orden. */
    @Test
    void g2SubstitutesAThenSThenAuxiliary() {
        List<TransformationStep> steps = substitute(g2After10(), List.of("S", "A", "B"), List.of("B.1"));

        assertEquals(3, steps.size());

        TransformationStep stepA = steps.get(0);
        assertEquals("Sustitución hacia atrás — A", stepA.getStepName());
        assertEquals(List.of("Sustituyendo B en A: A → B S."), stepA.getDetails());
        assertEquals(List.of("A -> b B A S", "A -> a S", "A -> b B A B.1 S", "A -> a B.1 S", "A -> b"),
                productionsOf(stepA.getGrammarAfter(), "A"));

        TransformationStep stepS = steps.get(1);
        assertEquals("Sustitución hacia atrás — S", stepS.getStepName());
        assertEquals(List.of("Sustituyendo A en S: S → A B."), stepS.getDetails());
        assertEquals(List.of("S -> b B A S B", "S -> a S B", "S -> b B A B.1 S B", "S -> a B.1 S B", "S -> b B"),
                productionsOf(stepS.getGrammarAfter(), "S"));

        TransformationStep stepB1 = steps.get(2);
        assertEquals("Sustitución hacia atrás — B.1", stepB1.getStepName());
        assertEquals(List.of("Sustituyendo S en B.1: B.1 → S B A, B.1 → S B A B.1."),
                stepB1.getDetails());
        assertEquals(List.of(
                "B.1 -> b B A S B B A",
                "B.1 -> a S B B A",
                "B.1 -> b B A B.1 S B B A",
                "B.1 -> a B.1 S B B A",
                "B.1 -> b B B A",
                "B.1 -> b B A S B B A B.1",
                "B.1 -> a S B B A B.1",
                "B.1 -> b B A B.1 S B B A B.1",
                "B.1 -> a B.1 S B B A B.1",
                "B.1 -> b B B A B.1"),
                productionsOf(stepB1.getGrammarAfter(), "B.1"));
    }

    /** G3: cambian X1 y S (sin auxiliares). */
    @Test
    void g3SubstitutesX1ThenS() {
        List<TransformationStep> steps = substitute(g3After10(), List.of("S", "X1", "Ta", "Tb"), List.of());

        assertEquals(2, steps.size());

        TransformationStep stepX1 = steps.get(0);
        assertEquals("Sustitución hacia atrás — X1", stepX1.getStepName());
        assertEquals(List.of("Sustituyendo Ta en X1: X1 → Ta X1 Tb, X1 → Ta Tb Tb."),
                stepX1.getDetails());
        assertEquals(List.of("X1 -> a X1 Tb", "X1 -> a Tb Tb"),
                productionsOf(stepX1.getGrammarAfter(), "X1"));

        TransformationStep stepS = steps.get(1);
        assertEquals("Sustitución hacia atrás — S", stepS.getStepName());
        assertEquals(List.of("Sustituyendo Ta en S: S → Ta X1, S → Ta Tb."), stepS.getDetails());
        assertEquals(List.of("S -> a X1", "S -> a Tb"),
                productionsOf(stepS.getGrammarAfter(), "S"));
    }

    /** G4: solo cambia S. */
    @Test
    void g4SubstitutesS() {
        List<TransformationStep> steps = substitute(g4After10(), List.of("S", "Ta"), List.of());

        assertEquals(1, steps.size());
        TransformationStep stepS = steps.get(0);
        assertEquals("Sustitución hacia atrás — S", stepS.getStepName());
        assertEquals(List.of("Sustituyendo Ta en S: S → Ta S."), stepS.getDetails());
        assertEquals(List.of("S -> a S", "S -> a"),
                productionsOf(stepS.getGrammarAfter(), "S"));
    }

    /** Sin cambios: un único paso marcado como sin cambios. */
    @Test
    void noChangeEmitsSingleStep() {
        Grammar alreadyTerminal = new Grammar(
                Set.of("S"), Set.of("a"),
                List.of(new Production("S", List.of("a", "S")),
                        new Production("S", List.of("a"))),
                "S");

        List<TransformationStep> steps = substitute(alreadyTerminal, List.of("S"), List.of());

        assertEquals(1, steps.size());
        TransformationStep step = steps.get(0);
        assertEquals("Sustitución hacia atrás", step.getStepName());
        assertFalse(step.hasChanges());
        assertEquals(11, step.getMenuNumber());
        assertTrue(step.getDetails().contains(
                "Ninguna producción empieza con variable, no hay cambios en la gramática. "
                + "Se pasa al siguiente paso."));
        assertEquals(productionStrings(alreadyTerminal), productionStrings(step.getGrammarAfter()));
    }

    /** Guarda: recursividad izquierda no eliminada no lanza excepción ni cuelga. */
    @Test
    void guardStopsRecursionWithoutException() {
        Grammar recursive = new Grammar(
                Set.of("S"), Set.of("a", "b"),
                List.of(new Production("S", List.of("S", "a")),
                        new Production("S", List.of("b"))),
                "S");

        List<TransformationStep> steps = assertDoesNotThrow(
                () -> substitute(recursive, List.of("S"), List.of()));

        assertFalse(steps.isEmpty());
        boolean hasGuardWarning = steps.stream()
                .flatMap(step -> step.getDetails().stream())
                .anyMatch(detail -> detail.startsWith("✘ "));
        assertTrue(hasGuardWarning);
    }

    /** RNF06: el lenguaje no cambia tras el paso 11 (N = 9). */
    @Test
    void languageIsPreserved() {
        assertLanguagePreserved(g1After10(), orderG1(), List.of("A.1", "B.1"));
        assertLanguagePreserved(g2After10(), List.of("S", "A", "B"), List.of("B.1"));
        assertLanguagePreserved(g3After10(), List.of("S", "X1", "Ta", "Tb"), List.of());
        assertLanguagePreserved(g4After10(), List.of("S", "Ta"), List.of());
    }

    /** Propiedad: tras el paso, ninguna producción empieza con una variable. */
    @Test
    void noProductionStartsWithVariable() {
        assertNoProductionStartsWithVariable(substitute(g1After10(), orderG1(), List.of("A.1", "B.1")));
        assertNoProductionStartsWithVariable(substitute(g2After10(), List.of("S", "A", "B"), List.of("B.1")));
        assertNoProductionStartsWithVariable(substitute(g3After10(), List.of("S", "X1", "Ta", "Tb"), List.of()));
    }

    /** Punto 0: productionsRemoved/productionsAdded son el delta neto del paso. */
    @Test
    void removedAndAddedAreNetDelta() {
        List<TransformationStep> steps = substitute(g1After10(), orderG1(), List.of("A.1", "B.1"));

        TransformationStep stepS = steps.get(0);
        assertEquals(List.of("S -> A B"), stepS.getProductionsRemoved());
        assertEquals(List.of("S -> 2 T1 B", "S -> 1 B", "S -> 2 T1 A.1 B", "S -> 1 A.1 B"),
                stepS.getProductionsAdded());
    }

    /** El paso no muta la gramática recibida. */
    @Test
    void substituteDoesNotMutateInput() {
        Grammar input = g1After10();
        List<String> productionsBefore = productionStrings(input);
        Set<String> variablesBefore = Set.copyOf(input.getVariables());

        substitute(input, orderG1(), List.of("A.1", "B.1"));

        assertEquals(productionsBefore, productionStrings(input));
        assertEquals(variablesBefore, Set.copyOf(input.getVariables()));
    }

    /** RNF10: dos ejecuciones producen los mismos pasos. */
    @Test
    void substitutesReproducibly() {
        List<TransformationStep> first = substitute(g1After10(), orderG1(), List.of("A.1", "B.1"));
        List<TransformationStep> second = substitute(g1After10(), orderG1(), List.of("A.1", "B.1"));

        assertEquals(productionStrings(finalGrammar(first)), productionStrings(finalGrammar(second)));
        assertEquals(allDetails(first), allDetails(second));
    }

    private static List<TransformationStep> substitute(Grammar grammar, List<String> order,
                                                       List<String> auxiliaryVariables) {
        return new BackwardSubstitutor(order, auxiliaryVariables).substitute(grammar);
    }

    private static void assertLanguagePreserved(Grammar input, List<String> order,
                                                List<String> auxiliaryVariables) {
        List<TransformationStep> steps = substitute(input, order, auxiliaryVariables);
        assertEquals(
                LanguageEnumerator.generate(input, 9),
                LanguageEnumerator.generate(finalGrammar(steps), 9));
    }

    private static void assertNoProductionStartsWithVariable(List<TransformationStep> steps) {
        Grammar finalGrammar = finalGrammar(steps);
        for (Production production : finalGrammar.getProductions()) {
            if (production.getRightSide().isEmpty()) {
                continue;
            }
            assertFalse(finalGrammar.getVariables().contains(production.getRightSide().get(0)),
                    "Producción que empieza con variable: " + production);
        }
    }

    private static Grammar finalGrammar(List<TransformationStep> steps) {
        return steps.get(steps.size() - 1).getGrammarAfter();
    }

    private static List<String> allDetails(List<TransformationStep> steps) {
        return steps.stream().flatMap(step -> step.getDetails().stream()).collect(Collectors.toList());
    }

    private static List<String> productionsOf(Grammar grammar, String variable) {
        return grammar.getProductions().stream()
                .filter(p -> p.getLeftSide().equals(variable))
                .map(Production::toString)
                .toList();
    }

    private static List<String> productionStrings(Grammar grammar) {
        return grammar.getProductions().stream().map(Production::toString).toList();
    }

    private static List<String> orderG1() {
        return List.of("S", "A", "B", "T1", "T2");
    }

    private static Grammar g1After10() {
        return new Grammar(
            Set.of("S", "A", "B", "T1", "T2", "A.1", "B.1"),
            Set.of("1", "2"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("S", List.of("2")),
                new Production("A", List.of("2", "T1")),
                new Production("A", List.of("1")),
                new Production("A", List.of("2", "T1", "A.1")),
                new Production("A", List.of("1", "A.1")),
                new Production("B", List.of("2")),
                new Production("B", List.of("2", "B.1")),
                new Production("T1", List.of("1")),
                new Production("T2", List.of("2")),
                new Production("A.1", List.of("B", "T1")),
                new Production("A.1", List.of("B", "T1", "A.1")),
                new Production("B.1", List.of("T2")),
                new Production("B.1", List.of("T2", "B.1"))
            ),
            "S"
        );
    }

    private static Grammar g2After10() {
        return new Grammar(
            Set.of("S", "A", "B", "B.1"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("A", List.of("B", "S")),
                new Production("A", List.of("b")),
                new Production("B", List.of("b", "B", "A")),
                new Production("B", List.of("a")),
                new Production("B", List.of("b", "B", "A", "B.1")),
                new Production("B", List.of("a", "B.1")),
                new Production("B.1", List.of("S", "B", "A")),
                new Production("B.1", List.of("S", "B", "A", "B.1"))
            ),
            "S"
        );
    }

    private static Grammar g3After10() {
        return new Grammar(
            Set.of("S", "X1", "Ta", "Tb"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("Ta", "X1")),
                new Production("S", List.of("Ta", "Tb")),
                new Production("X1", List.of("Ta", "X1", "Tb")),
                new Production("X1", List.of("Ta", "Tb", "Tb")),
                new Production("Ta", List.of("a")),
                new Production("Tb", List.of("b"))
            ),
            "S"
        );
    }

    private static Grammar g4After10() {
        return new Grammar(
            Set.of("S", "Ta"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("Ta", "S")),
                new Production("S", List.of("a")),
                new Production("Ta", List.of("a"))
            ),
            "S"
        );
    }
}
