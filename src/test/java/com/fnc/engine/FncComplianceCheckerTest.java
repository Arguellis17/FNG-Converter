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
 * Pruebas unitarias para {@link FncComplianceChecker} (paso 7), con los vectores
 * de la especificación FNG.
 */
class FncComplianceCheckerTest {

    /** G1 cumple la FNC del curso y no tiene S → ε. */
    @Test
    void g1IsCompliantWithoutEpsilon() {
        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step = checker.check(g1());

        assertTrue(checker.isCompliant(), "G1 está en FNC");
        assertFalse(checker.isStartNullable(), "G1 no tiene S → ε");
        assertFalse(step.hasChanges(), "Sin S → ε no se aparta nada: sin cambios");
        assertTrue(step.getProductionsRemoved().isEmpty());
        assertTrue(step.getDetails().contains("Producciones revisadas: 6."));
        assertTrue(step.getDetails().contains(
                "Resultado: la gramática está en FNC del curso."));
        assertFalse(step.getDetails().stream().anyMatch(d -> d.startsWith("✘ ")));
    }

    /** G3 tiene S → ε: se aparta y grammarAfter queda sin él. */
    @Test
    void g3ApartaStartEpsilon() {
        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step = checker.check(g3());

        assertTrue(checker.isStartNullable(), "G3 tiene S → ε");
        assertTrue(checker.isCompliant(), "G3 cumple la FNC del curso");
        assertTrue(hasStartNull(step.getGrammarBefore()), "grammarBefore conserva S → ε");
        assertFalse(hasStartNull(step.getGrammarAfter()), "grammarAfter no debe tener S → ε");
        assertTrue(step.hasChanges(), "Apartar S → ε sí es un cambio");
        assertTrue(step.getProductionsRemoved().contains("S -> ε"),
                "La producción apartada debe registrarse en productionsRemoved");
        assertTrue(step.getDetails().contains(
                "Se aparta S → ε: se reincorporará en el paso 13 si S no aparece en "
                + "ningún lado derecho."));
    }

    /** G3: al apartar S → ε el paso reporta cambios y registra la producción. */
    @Test
    void g3ApartarEpsilonRegistraProduccionEliminada() {
        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step = checker.check(g3());

        assertEquals(List.of("S -> ε"), step.getProductionsRemoved());
        assertTrue(step.getProductionsAdded().isEmpty());
        assertTrue(step.hasChanges());
    }

    /** Producción de 3 símbolos: violación. */
    @Test
    void threeSymbolProductionIsViolation() {
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

        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step = checker.check(grammar);

        assertFalse(checker.isCompliant());
        assertTrue(hasViolationMarker(step));
    }

    /** Producción unitaria: violación. */
    @Test
    void unitaryProductionIsViolation() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("A")),
                new Production("A", List.of("a"))
            ),
            "S"
        );

        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step = checker.check(grammar);

        assertFalse(checker.isCompliant());
        assertTrue(hasViolationMarker(step));
    }

    /** Producción ε en una variable distinta de S: violación. */
    @Test
    void epsilonInNonStartVariableIsViolation() {
        Grammar grammar = new Grammar(
            Set.of("S", "B"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("a", "B")),
                new Production("B", List.of())
            ),
            "S"
        );

        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step = checker.check(grammar);

        assertFalse(checker.isCompliant());
        assertTrue(hasViolationMarker(step));
    }

    /** El chequeo no muta la gramática recibida. */
    @Test
    void checkDoesNotMutateInput() {
        Grammar g3 = g3();
        List<String> before = g3.getProductions().stream()
                .map(Production::toString).toList();

        new FncComplianceChecker().check(g3);

        assertEquals(before, g3.getProductions().stream()
                .map(Production::toString).toList());
        assertTrue(hasStartNull(g3), "La entrada conserva S → ε");
    }

    private static boolean hasViolationMarker(TransformationStep step) {
        return step.getDetails().stream().anyMatch(d -> d.startsWith("✘ "));
    }

    private static boolean hasStartNull(Grammar grammar) {
        return grammar.getProductions().stream()
                .anyMatch(p -> p.isNull() && p.getLeftSide().equals(grammar.getStartSymbol()));
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

    private static Grammar g3() {
        return new Grammar(
            Set.of("S", "X1"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("a", "X1")),
                new Production("S", List.of("a", "b")),
                new Production("S", List.of()),
                new Production("X1", List.of("S", "b"))
            ),
            "S"
        );
    }
}
