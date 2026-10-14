package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del paso 13: {@link GreibachFinalizer}.
 */
class GreibachFinalizerTest {

    private static final List<String> ORDER = List.of("S", "A", "B", "T1", "T2");
    private static final List<String> TERMINAL_VARS = List.of("T1", "T2");
    private static final List<String> RECURSION_VARS = List.of("A.1", "B.1");

    private static final String REINSERTED =
            "Se reincorpora S → ε: S no aparece en ningún lado derecho.";
    private static final String NOT_REINSERTED =
            "La FNG no puede generar la cadena vacía sin un símbolo inicial nuevo; "
            + "el lenguaje es equivalente salvo ε.";

    @Test
    void validG1WithoutReinsertion() {
        GreibachFinalizer finalizer = new GreibachFinalizer(
                false, ORDER, TERMINAL_VARS, RECURSION_VARS);

        TransformationStep step = finalizer.validateAndOrganize(g1Final());

        assertTrue(finalizer.isValid());
        assertFalse(finalizer.isEmptyStringReinserted());
        assertFalse(step.hasChanges());
        assertEquals(13, step.getMenuNumber());
        assertEquals("Validación final de FNG y organización", step.getStepName());
        assertEquals(List.of(
                        "Validación final de FNG:",
                        "✔ Toda producción empieza con un terminal seguido solo de variables.",
                        "✔ Todos los símbolos están declarados.",
                        "✔ Todas las variables tienen producciones.",
                        "Resultado: la gramática está en Forma Normal de Greibach."),
                step.getDetails());
    }

    @Test
    void reinsertsEpsilonWhenStartNullableAndStartNotUsed() {
        Grammar g3 = new Grammar(
                new LinkedHashSet<>(List.of("S", "X1", "Tb")),
                new LinkedHashSet<>(List.of("a", "b")),
                List.of(
                        production("S", "a", "X1"),
                        production("S", "a", "Tb"),
                        production("X1", "a", "X1", "Tb"),
                        production("X1", "a", "Tb", "Tb"),
                        production("Tb", "b")),
                "S");

        GreibachFinalizer finalizer = new GreibachFinalizer(
                true, List.of("S", "X1"), List.of("Tb"), List.of());

        TransformationStep step = finalizer.validateAndOrganize(g3);

        assertTrue(finalizer.isEmptyStringReinserted());
        assertTrue(finalizer.isValid());
        assertTrue(step.hasChanges());
        assertEquals(List.of("S -> ε"), step.getProductionsAdded());
        assertEquals(
                List.of("S -> a X1 | a Tb | ε", "X1 -> a X1 Tb | a Tb Tb", "Tb -> b"),
                productionLines(step.getCustomBody()));
        assertTrue(productionStrings(step.getGrammarAfter()).contains("S -> ε"));
        assertEquals(REINSERTED, step.getDetails().get(0));
    }

    @Test
    void doesNotReinsertWhenStartAppearsOnRightSide() {
        Grammar grammar = new Grammar(
                new LinkedHashSet<>(List.of("S")),
                new LinkedHashSet<>(List.of("a")),
                List.of(production("S", "a", "S"), production("S", "a")),
                "S");

        GreibachFinalizer finalizer = new GreibachFinalizer(
                true, List.of("S"), List.of(), List.of());

        TransformationStep step = finalizer.validateAndOrganize(grammar);

        assertFalse(finalizer.isEmptyStringReinserted());
        assertFalse(step.hasChanges());
        assertTrue(finalizer.isValid());
        assertEquals(NOT_REINSERTED, step.getDetails().get(0));
    }

    @Test
    void reportsViolationsWithoutThrowing() {
        GreibachFinalizer badShape = new GreibachFinalizer(
                false, List.of("S", "A"), List.of(), List.of());
        TransformationStep shapeStep = badShape.validateAndOrganize(new Grammar(
                new LinkedHashSet<>(List.of("S", "A")),
                new LinkedHashSet<>(List.of("a")),
                List.of(production("S", "A", "a"), production("A", "a")),
                "S"));
        assertFalse(badShape.isValid());
        assertTrue(shapeStep.getDetails().contains(
                "✘ La producción S → A a no cumple Y → x α."));
        assertTrue(shapeStep.getDetails().contains(
                "Resultado: la gramática NO está en Forma Normal de Greibach."));

        GreibachFinalizer badTail = new GreibachFinalizer(
                false, List.of("S"), List.of(), List.of());
        TransformationStep tailStep = badTail.validateAndOrganize(new Grammar(
                new LinkedHashSet<>(List.of("S")),
                new LinkedHashSet<>(List.of("a", "b")),
                List.of(production("S", "a", "b")),
                "S"));
        assertFalse(badTail.isValid());
        assertTrue(tailStep.getDetails().contains(
                "✘ La producción S → a b no cumple Y → x α."));

        GreibachFinalizer emptyVariable = new GreibachFinalizer(
                false, List.of("S", "B"), List.of(), List.of());
        TransformationStep emptyStep = emptyVariable.validateAndOrganize(new Grammar(
                new LinkedHashSet<>(List.of("S", "B")),
                new LinkedHashSet<>(List.of("a")),
                List.of(production("S", "a", "B")),
                "S"));
        assertFalse(emptyVariable.isValid());
        assertTrue(emptyStep.getDetails().contains(
                "✘ La variable B no tiene producciones."));
    }

    @Test
    void doesNotMutateInputAndIsDeterministic() {
        Grammar grammar = g1Final();
        List<String> snapshot = productionStrings(grammar);

        GreibachFinalizer first = new GreibachFinalizer(
                false, ORDER, TERMINAL_VARS, RECURSION_VARS);
        TransformationStep firstStep = first.validateAndOrganize(grammar);
        GreibachFinalizer second = new GreibachFinalizer(
                false, ORDER, TERMINAL_VARS, RECURSION_VARS);
        TransformationStep secondStep = second.validateAndOrganize(grammar);

        assertEquals(snapshot, productionStrings(grammar));
        assertEquals(productionStrings(firstStep.getGrammarAfter()),
                productionStrings(secondStep.getGrammarAfter()));
        assertEquals(firstStep.getDetails(), secondStep.getDetails());
        assertEquals(firstStep.getCustomBody(), secondStep.getCustomBody());
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

    private static Grammar g1Final() {
        return new Grammar(
                new LinkedHashSet<>(List.of("S", "B", "T1", "A.1", "B.1")),
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
