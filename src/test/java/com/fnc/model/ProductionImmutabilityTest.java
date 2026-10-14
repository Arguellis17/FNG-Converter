package com.fnc.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionImmutabilityTest {

    @Test
    void getRightSideIsUnmodifiable() {
        List<String> right = new ArrayList<>(Arrays.asList("A", "B"));
        Production p = new Production("S", right);
        assertThrows(UnsupportedOperationException.class, () -> p.getRightSide().add("C"));
    }

    @Test
    void originalListChangeDoesNotAffectProduction() {
        List<String> right = new ArrayList<>(Arrays.asList("A", "B"));
        Production p = new Production("S", right);
        right.add("C");
        assertEquals(Arrays.asList("A", "B"), p.getRightSide());
    }
}
