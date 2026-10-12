package com.fnc.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Pruebas de {@link TransformationStep}, en particular la copia con nuevo
 * {@code menuNumber} (paso 12/13).
 */
class TransformationStepTest {

    @Test
    void withMenuNumberCopiesAllFieldsAndKeepsOriginal() {
        Grammar before = new Grammar(
                Set.of("S"), Set.of("a"),
                List.of(new Production("S", List.of("a"))), "S");
        Grammar after = before.copy();
        TransformationStep original = new TransformationStep(
                "Nombre", "Descripción", before, after,
                List.of("S -> a"), List.of("S -> a a"), List.of("detalle"), 7, "cuerpo");

        TransformationStep copy = original.withMenuNumber(12);

        assertNotSame(original, copy);
        assertEquals(12, copy.getMenuNumber());
        assertEquals(original.getStepName(), copy.getStepName());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getGrammarBefore(), copy.getGrammarBefore());
        assertEquals(original.getGrammarAfter(), copy.getGrammarAfter());
        assertEquals(original.getProductionsRemoved(), copy.getProductionsRemoved());
        assertEquals(original.getProductionsAdded(), copy.getProductionsAdded());
        assertEquals(original.getDetails(), copy.getDetails());
        assertEquals(original.getCustomBody(), copy.getCustomBody());
        assertEquals(original.hasChanges(), copy.hasChanges());

        assertEquals(7, original.getMenuNumber());
    }
}
