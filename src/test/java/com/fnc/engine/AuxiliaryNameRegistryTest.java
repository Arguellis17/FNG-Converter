package com.fnc.engine;

import com.fnc.model.Grammar;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias para {@link AuxiliaryNameRegistry}.
 */
class AuxiliaryNameRegistryTest {

    @Test
    void freeNameForTerminal() {
        AuxiliaryNameRegistry registry = registry(Set.of("S"), Set.of("a"));
        assertEquals("Ta", registry.terminalVariable("a"));
    }

    @Test
    void terminalNameTakenByVariableAppendsPrime() {
        AuxiliaryNameRegistry registry = registry(Set.of("S", "Tb"), Set.of("b"));
        assertEquals("Tb'", registry.terminalVariable("b"));
    }

    @Test
    void sameTerminalReturnsSameVariable() {
        AuxiliaryNameRegistry registry = registry(Set.of("S"), Set.of("a"));
        assertEquals("Ta", registry.terminalVariable("a"));
        assertEquals("Ta", registry.terminalVariable("a"));
    }

    @Test
    void recursionNameTakenByVariableAppendsPrime() {
        AuxiliaryNameRegistry registry = registry(Set.of("S", "A", "A.1"), Set.of("a"));
        assertEquals("A.1'", registry.recursionVariable("A"));
    }

    @Test
    void recursionNameTakenByPreviousAuxiliaryAppendsPrime() {
        AuxiliaryNameRegistry registry = registry(Set.of("S", "A"), Set.of("a"));
        assertEquals("A.1", registry.recursionVariable("A"));
        assertEquals("A.1'", registry.recursionVariable("A"));
    }

    @Test
    void creationOrderIsPreserved() {
        AuxiliaryNameRegistry registry = registry(Set.of("S", "A", "B"), Set.of("a", "b"));
        registry.terminalVariable("b");
        registry.terminalVariable("a");
        registry.recursionVariable("A");
        registry.recursionVariable("B");

        assertEquals(List.of("b", "a"), List.copyOf(registry.terminalVariables().keySet()));
        assertEquals("Tb", registry.terminalVariables().get("b"));
        assertEquals("Ta", registry.terminalVariables().get("a"));
        assertEquals(List.of("A.1", "B.1"), registry.recursionVariables());
    }

    private static AuxiliaryNameRegistry registry(Set<String> variables, Set<String> terminals) {
        return new AuxiliaryNameRegistry(new Grammar(variables, terminals, List.of(), "S"));
    }
}
