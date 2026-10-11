package com.fnc.testsupport;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias para {@link LanguageEnumerator}, con los vectores de la
 * especificación FNG.
 */
class LanguageEnumeratorTest {

    @Test
    void rightRecursionUpToThreeSymbols() {
        Grammar grammar = new Grammar(
            Set.of("S"),
            Set.of("a"),
            List.of(
                new Production("S", List.of("a", "S")),
                new Production("S", List.of("a"))
            ),
            "S"
        );

        assertEquals(Set.of("a", "aa", "aaa"), LanguageEnumerator.generate(grammar, 3));
    }

    @Test
    void epsilonAndNestedRecursionUpToFourSymbols() {
        Grammar grammar = new Grammar(
            Set.of("S"),
            Set.of("a", "b"),
            List.of(
                new Production("S", List.of("a", "S", "b")),
                new Production("S", List.of("ε"))
            ),
            "S"
        );

        assertEquals(Set.of("", "ab", "aabb"), LanguageEnumerator.generate(grammar, 4));
    }

    @Test
    void indirectAndImmediateRecursionCounts232Strings() {
        Grammar grammar = new Grammar(
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

        assertEquals(232, LanguageEnumerator.generate(grammar, 11).size());
    }

    @Test
    void indirectRecursionCounts151Strings() {
        Grammar grammar = new Grammar(
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

        assertEquals(151, LanguageEnumerator.generate(grammar, 11).size());
    }
}
