package com.fnc.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link Production}, en especial la clasificación de
 * una producción de un solo símbolo como terminal.
 */
class ProductionTest {

    @Test
    void lowercaseSymbolIsTerminal() {
        assertTrue(new Production("A", List.of("a")).isTerminal());
    }

    @Test
    void digitSymbolIsTerminal() {
        assertTrue(new Production("A", List.of("1")).isTerminal());
    }

    @Test
    void uppercaseSymbolIsNotTerminal() {
        assertFalse(new Production("A", List.of("B")).isTerminal());
    }

    @Test
    void compoundVariableSymbolIsNotTerminal() {
        assertFalse(new Production("A", List.of("X1")).isTerminal());
    }

    @Test
    void epsilonIsNotTerminal() {
        assertFalse(new Production("A", List.of("ε")).isTerminal());
    }

    @Test
    void twoSymbolsIsNotTerminal() {
        assertFalse(new Production("A", List.of("a", "B")).isTerminal());
    }

    @Test
    void unitaryAndBinaryClassificationUnchanged() {
        Production unitary = new Production("A", List.of("B"));
        assertTrue(unitary.isUnitary());
        assertFalse(unitary.isBinary());

        Production binary = new Production("A", List.of("a", "B"));
        assertTrue(binary.isBinary());
        assertFalse(binary.isUnitary());
    }
}
