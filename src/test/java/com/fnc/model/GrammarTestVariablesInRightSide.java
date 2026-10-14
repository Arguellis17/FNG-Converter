package com.fnc.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GrammarTestVariablesInRightSide {

    @Test
    void returnsLinkedHashSetInOrderOfAppearance() {
        Grammar g = new Grammar();
        g.setStartSymbol("S");
        g.setVariables(new LinkedHashSet<>(Arrays.asList("S", "B", "A")));
        g.setTerminals(new LinkedHashSet<>(Arrays.asList("a", "b")));
        g.setProductions(Arrays.asList(
                new Production("S", Arrays.asList("B", "a")),
                new Production("S", Arrays.asList("A", "b"))));
        Set<String> res = g.getVariablesInRightSide();
        assertTrue(res instanceof LinkedHashSet);
        assertEquals(Arrays.asList("B", "A"), new java.util.ArrayList<>(res));
    }
}
