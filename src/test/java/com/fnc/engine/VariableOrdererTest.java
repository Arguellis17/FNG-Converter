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
 * Pruebas unitarias para {@link VariableOrderer} (paso 9), con los vectores de la
 * especificación FNG. Se usa un único {@link AuxiliaryNameRegistry} creado con la
 * gramática previa al paso 8, compartido con {@link TerminalToVariableConverter}.
 */
class VariableOrdererTest {

    /** G1: orden S, A, B, T1, T2 con el mensaje exacto. */
    @Test
    void g1OrdersStartThenLevelsThenTerminalVariables() {
        Grammar input = g1();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(input);
        Grammar after8 = new TerminalToVariableConverter(registry).convert(input).getGrammarAfter();

        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        TransformationStep step = orderer.order(after8);

        assertEquals(List.of("S", "A", "B", "T1", "T2"), orderer.getOrder());
        assertTrue(step.getDetails().contains(
                "Orden de las variables: 1. S, 2. A, 3. B, 4. T1, 5. T2."));
        assertFalse(step.hasChanges());
        assertEquals(9, step.getMenuNumber());
        assertEquals("Ordenamiento de variables", step.getStepName());
    }

    /** G2: sin variables de terminales, orden S, A, B. */
    @Test
    void g2OrdersStartThenLevels() {
        Grammar input = g2();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(input);
        Grammar after8 = new TerminalToVariableConverter(registry).convert(input).getGrammarAfter();

        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        TransformationStep step = orderer.order(after8);

        assertEquals(List.of("S", "A", "B"), orderer.getOrder());
        assertTrue(step.getDetails().contains("Orden de las variables: 1. S, 2. A, 3. B."));
    }

    /** G3: las variables de terminales Ta y Tb van al final. */
    @Test
    void g3OrdersX1BeforeTerminalVariables() {
        Grammar input = g3WithoutEpsilon();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(input);
        Grammar after8 = new TerminalToVariableConverter(registry).convert(input).getGrammarAfter();

        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        TransformationStep step = orderer.order(after8);

        assertEquals(List.of("S", "X1", "Ta", "Tb"), orderer.getOrder());
        assertTrue(step.getDetails().contains(
                "Orden de las variables: 1. S, 2. X1, 3. Ta, 4. Tb."));
    }

    /** El ordenamiento no muta la gramática ni reporta cambios. */
    @Test
    void orderDoesNotMutateInput() {
        Grammar input = g1();
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(input);
        Grammar after8 = new TerminalToVariableConverter(registry).convert(input).getGrammarAfter();
        List<String> productionsBefore = productionStrings(after8);
        Set<String> variablesBefore = Set.copyOf(after8.getVariables());

        new VariableOrderer(registry.terminalVariables().values()).order(after8);

        assertEquals(productionsBefore, productionStrings(after8));
        assertEquals(variablesBefore, Set.copyOf(after8.getVariables()));
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
