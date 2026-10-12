package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import com.fnc.testsupport.LanguageEnumerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link LeftRecursionEliminator} (paso 10), con los
 * vectores de la especificación FNG. Cada caso usa un único
 * {@link AuxiliaryNameRegistry} creado con la gramática previa al paso 8,
 * compartido con {@link TerminalToVariableConverter}, como hará el servicio.
 */
class LeftRecursionEliminatorTest {

    /** G1: cambian A y B (dos pasos), se crean A.1 y B.1. */
    @Test
    void g1EliminatesRecursionInAAndB() {
        Fixture fixture = run(g1());
        List<TransformationStep> steps = fixture.steps();

        assertEquals(2, steps.size());

        TransformationStep stepA = steps.get(0);
        assertEquals("Eliminación de recursividad izquierda — A", stepA.getStepName());
        assertEquals(10, stepA.getMenuNumber());
        assertEquals(List.of(
                "Procesando A (variable 2 de 5).",
                "Reemplazando S en A: A → S T1.",
                "Eliminando recursividad inmediata: A → A B T1. Se crea la variable A.1."),
                stepA.getDetails());
        assertEquals(List.of("A -> 2 T1", "A -> 1", "A -> 2 T1 A.1", "A -> 1 A.1"),
                productionsOf(stepA.getGrammarAfter(), "A"));
        assertEquals(List.of("A.1 -> B T1", "A.1 -> B T1 A.1"),
                productionsOf(stepA.getGrammarAfter(), "A.1"));

        TransformationStep stepB = steps.get(1);
        assertEquals("Eliminación de recursividad izquierda — B", stepB.getStepName());
        assertEquals(List.of(
                "Procesando B (variable 3 de 5).",
                "Eliminando recursividad inmediata: B → B T2. Se crea la variable B.1."),
                stepB.getDetails());
        assertEquals(productionsOf(stepA.getGrammarAfter(), "A"),
                productionsOf(stepB.getGrammarBefore(), "A"));
        assertEquals(productionsOf(stepB.getGrammarAfter(), "B"), List.of("B -> 2", "B -> 2 B.1"));
        assertEquals(productionsOf(stepB.getGrammarAfter(), "B.1"),
                List.of("B.1 -> T2", "B.1 -> T2 B.1"));

        assertEquals(List.of("A.1", "B.1"), fixture.eliminator().getCreatedVariables());
        assertEquals(2, fixture.registry().recursionVariables().size());
    }

    /** G2: solo cambia B (un paso). */
    @Test
    void g2EliminatesRecursionInB() {
        Fixture fixture = run(g2());
        List<TransformationStep> steps = fixture.steps();

        assertEquals(1, steps.size());
        TransformationStep stepB = steps.get(0);
        assertEquals("Eliminación de recursividad izquierda — B", stepB.getStepName());
        assertEquals(List.of(
                "Procesando B (variable 3 de 3).",
                "Reemplazando S en B: B → S A.",
                "Reemplazando A en B: B → A B A.",
                "Eliminando recursividad inmediata: B → B S B A. Se crea la variable B.1."),
                stepB.getDetails());
        assertEquals(List.of("B -> b B A", "B -> a", "B -> b B A B.1", "B -> a B.1"),
                productionsOf(stepB.getGrammarAfter(), "B"));
        assertEquals(List.of("B.1 -> S B A", "B.1 -> S B A B.1"),
                productionsOf(stepB.getGrammarAfter(), "B.1"));
        assertEquals(List.of("B.1"), fixture.eliminator().getCreatedVariables());
        assertEquals(1, fixture.registry().recursionVariables().size());
    }

    /** G3: solo cambia X1 por sustitución, sin crear auxiliar. */
    @Test
    void g3EliminatesRecursionInX1BySubstitution() {
        Fixture fixture = run(g3WithoutEpsilon());
        List<TransformationStep> steps = fixture.steps();

        assertEquals(1, steps.size());
        TransformationStep stepX1 = steps.get(0);
        assertEquals("Eliminación de recursividad izquierda — X1", stepX1.getStepName());
        assertEquals(List.of(
                "Procesando X1 (variable 2 de 4).",
                "Reemplazando S en X1: X1 → S Tb."),
                stepX1.getDetails());
        assertEquals(List.of("X1 -> Ta X1 Tb", "X1 -> Ta Tb Tb"),
                productionsOf(stepX1.getGrammarAfter(), "X1"));
        assertTrue(fixture.eliminator().getCreatedVariables().isEmpty());
        assertTrue(fixture.registry().recursionVariables().isEmpty());
    }

    /** G4: ninguna recursividad, un único paso sin cambios. */
    @Test
    void g4WithoutRecursionEmitsSingleNoChangeStep() {
        Fixture fixture = run(g4());
        List<TransformationStep> steps = fixture.steps();

        assertEquals(1, steps.size());
        TransformationStep step = steps.get(0);
        assertFalse(step.hasChanges());
        assertEquals(10, step.getMenuNumber());
        assertTrue(step.getDetails().contains(
                "No se han identificado recursividades a la izquierda, no hay cambios "
                + "en la gramática. Se pasa al siguiente paso."));
        assertTrue(fixture.eliminator().getCreatedVariables().isEmpty());
        assertEquals(productionStrings(fixture.after8()), productionStrings(step.getGrammarAfter()));
    }

    /** Recursividad inmediata en la primera variable. */
    @Test
    void recursionInFirstVariable() {
        Fixture fixture = run(recursionInStart());
        List<TransformationStep> steps = fixture.steps();

        assertEquals(1, steps.size());
        TransformationStep step = steps.get(0);
        assertEquals(List.of(
                "Procesando S (variable 1 de 2).",
                "Eliminando recursividad inmediata: S → S A. Se crea la variable S.1."),
                step.getDetails());
        assertEquals(List.of("S -> b", "S -> b S.1"),
                productionsOf(step.getGrammarAfter(), "S"));
        assertEquals(List.of("S.1 -> A", "S.1 -> A S.1"),
                productionsOf(step.getGrammarAfter(), "S.1"));
        assertEquals(List.of("S.1"), fixture.eliminator().getCreatedVariables());
    }

    /** RNF06: el lenguaje no cambia tras el paso 10 (N = 9). */
    @Test
    void languageIsPreserved() {
        assertLanguagePreserved(g1());
        assertLanguagePreserved(g2());
        assertLanguagePreserved(g3WithoutEpsilon());
        assertLanguagePreserved(recursionInStart());
    }

    /** Propiedad: ninguna producción Xi → Xj α de una variable original cumple j ≤ i. */
    @Test
    void noProductionStartsWithEarlierVariable() {
        Fixture g1Fixture = run(g1());
        assertTrue(noProductionStartsWithEarlierOrEqualVariable(
                finalGrammar(g1Fixture.steps()), g1Fixture.order()));

        Fixture g2Fixture = run(g2());
        assertTrue(noProductionStartsWithEarlierOrEqualVariable(
                finalGrammar(g2Fixture.steps()), g2Fixture.order()));
    }

    /** El paso no muta la gramática recibida. */
    @Test
    void eliminateDoesNotMutateInput() {
        Grammar input = g1();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(input);
        Grammar after8 = new TerminalToVariableConverter(registry).convert(input).getGrammarAfter();
        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        orderer.order(after8);
        List<String> productionsBefore = productionStrings(after8);
        Set<String> variablesBefore = Set.copyOf(after8.getVariables());

        new LeftRecursionEliminator(orderer.getOrder(), registry).eliminate(after8);

        assertEquals(productionsBefore, productionStrings(after8));
        assertEquals(variablesBefore, Set.copyOf(after8.getVariables()));
    }

    /** RNF10: dos ejecuciones producen la misma gramática y los mismos detalles. */
    @Test
    void eliminatesReproducibly() {
        Fixture first = run(g1());
        Fixture second = run(g1());

        assertEquals(productionStrings(finalGrammar(first.steps())),
                productionStrings(finalGrammar(second.steps())));
        assertEquals(allDetails(first.steps()), allDetails(second.steps()));
    }

    private static void assertLanguagePreserved(Grammar input) {
        Fixture fixture = run(input);
        assertEquals(
                LanguageEnumerator.generate(fixture.after8(), 9),
                LanguageEnumerator.generate(finalGrammar(fixture.steps()), 9));
    }

    private static boolean noProductionStartsWithEarlierOrEqualVariable(Grammar grammar,
                                                                        List<String> order) {
        for (Production production : grammar.getProductions()) {
            int i = order.indexOf(production.getLeftSide());
            if (i < 0 || production.getRightSide().isEmpty()) {
                continue;
            }
            int j = order.indexOf(production.getRightSide().get(0));
            if (j >= 0 && j <= i) {
                return false;
            }
        }
        return true;
    }

    private static Fixture run(Grammar input) {
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(input);
        Grammar after8 = new TerminalToVariableConverter(registry).convert(input).getGrammarAfter();
        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        orderer.order(after8);
        LeftRecursionEliminator eliminator =
                new LeftRecursionEliminator(orderer.getOrder(), registry);
        List<TransformationStep> steps = eliminator.eliminate(after8);
        return new Fixture(registry, after8, orderer.getOrder(), eliminator, steps);
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

    private record Fixture(AuxiliaryNameRegistry registry, Grammar after8, List<String> order,
                           LeftRecursionEliminator eliminator, List<TransformationStep> steps) {
    }

    private static Grammar g1() {
        return new Grammar(
            Set.of("S", "A", "B"),
            Set.of("1", "2"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("S", List.of("2")),
                new Production("A", List.of("S", "1")),
                new Production("A", List.of("1")),
                new Production("B", List.of("B", "2")),
                new Production("B", List.of("2"))
            ),
            "S"
        );
    }

    private static Grammar g2() {
        return new Grammar(
            Set.of("S", "A", "B"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "B")),
                new Production("A", List.of("B", "S")),
                new Production("A", List.of("b")),
                new Production("B", List.of("S", "A")),
                new Production("B", List.of("a"))
            ),
            "S"
        );
    }

    private static Grammar g3WithoutEpsilon() {
        return new Grammar(
            Set.of("S", "X1"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("a", "X1")),
                new Production("S", List.of("a", "b")),
                new Production("X1", List.of("S", "b"))
            ),
            "S"
        );
    }

    private static Grammar g4() {
        return new Grammar(
            Set.of("S"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("a", "S")),
                new Production("S", List.of("a"))
            ),
            "S"
        );
    }

    private static Grammar recursionInStart() {
        return new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("S", "A")),
                new Production("S", List.of("b")),
                new Production("A", List.of("a"))
            ),
            "S"
        );
    }
}
