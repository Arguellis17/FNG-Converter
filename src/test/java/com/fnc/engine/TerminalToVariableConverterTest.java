package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link TerminalToVariableConverter} (paso 8), con los
 * vectores de la especificación FNG.
 */
class TerminalToVariableConverterTest {

    /** G1: se crean T1 y T2 en ese orden y la gramática resultante es exacta. */
    @Test
    void g1CreatesT1AndT2InOrder() {
        Grammar g1 = g1();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(g1);
        TransformationStep step = new TerminalToVariableConverter(registry).convert(g1);

        assertTrue(step.hasChanges());
        assertEquals(List.of(
                "S -> A B", "S -> 2", "A -> S T1", "A -> 1",
                "B -> B T2", "B -> 2", "T1 -> 1", "T2 -> 2"),
                productionStrings(step.getGrammarAfter()));
        assertEquals(List.of("T1", "T2"), List.copyOf(registry.terminalVariables().values()));
        assertTrue(step.getGrammarAfter().getVariables().containsAll(List.of("T1", "T2")));
        assertTrue(step.getDetails().contains(
                "Se identificaron terminales dentro de producciones de 2 símbolos: 1, 2. "
                + "Se crean las variables T1→1, T2→2."));
        assertTrue(step.getDetails().contains(
                "Validación FNC estándar: toda producción es A → BC o A → a."));
    }

    /** G2 ya está en FNC estándar: sin cambios. */
    @Test
    void g2HasNoChanges() {
        Grammar g2 = g2();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(g2);
        TransformationStep step = new TerminalToVariableConverter(registry).convert(g2);

        assertFalse(step.hasChanges());
        assertEquals(productionStrings(g2), productionStrings(step.getGrammarAfter()));
        assertTrue(step.getDetails().contains(
                "No se han identificado terminales dentro de producciones de 2 símbolos, "
                + "no hay cambios en la gramática. Se pasa al siguiente paso."));
        assertTrue(step.getDetails().contains(
                "Validación FNC estándar: toda producción es A → BC o A → a."));
    }

    /** G3: Ta se crea antes que Tb. */
    @Test
    void g3CreatesTaBeforeTb() {
        Grammar g3 = g3WithoutEpsilon();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(g3);
        TransformationStep step = new TerminalToVariableConverter(registry).convert(g3);

        assertEquals(List.of(
                "S -> Ta X1", "S -> Ta Tb", "X1 -> S Tb", "Ta -> a", "Tb -> b"),
                productionStrings(step.getGrammarAfter()));
        assertEquals(List.of("Ta", "Tb"), List.copyOf(registry.terminalVariables().values()));
    }

    /** Terminal en la primera posición de la producción. */
    @Test
    void terminalInFirstPosition() {
        Grammar grammar = new Grammar(
            Set.of("S", "B"),
            Set.of("a", "b", "1", "2"),
            List.of(
                new Production("S", List.of("a", "B")),
                new Production("S", List.of("b")),
                new Production("B", List.of("B", "2")),
                new Production("B", List.of("1"))
            ),
            "S"
        );

        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(grammar);
        TransformationStep step = new TerminalToVariableConverter(registry).convert(grammar);

        assertEquals(List.of(
                "S -> Ta B", "S -> b", "B -> B T2", "B -> 1", "Ta -> a", "T2 -> 2"),
                productionStrings(step.getGrammarAfter()));
    }

    /** Mensaje singular con un solo terminal. */
    @Test
    void singleTerminalMessage() {
        Grammar grammar = new Grammar(
            Set.of("S", "A"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("A", "a")),
                new Production("A", List.of("b"))
            ),
            "S"
        );

        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(grammar);
        TransformationStep step = new TerminalToVariableConverter(registry).convert(grammar);

        assertTrue(step.getDetails().contains(
                "Se identificó un terminal dentro de una producción de 2 símbolos: a. "
                + "Se crea la variable Ta→a."));
    }

    /** La conversión no muta la gramática recibida. */
    @Test
    void convertDoesNotMutateInput() {
        Grammar g1 = g1();
        List<String> productionsBefore = productionStrings(g1);
        Set<String> variablesBefore = new LinkedHashSet<>(g1.getVariables());

        new TerminalToVariableConverter(new AuxiliaryNameRegistry(g1)).convert(g1);

        assertEquals(productionsBefore, productionStrings(g1));
        assertEquals(variablesBefore, g1.getVariables());
    }

    private static List<String> productionStrings(Grammar grammar) {
        return grammar.getProductions().stream().map(Production::toString).toList();
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
}
