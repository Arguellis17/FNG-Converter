package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputFncInspectorTest {

    private Grammar makeGrammar(List<Production> productions, String start) {
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        LinkedHashSet<String> terminals = new LinkedHashSet<>();
        for (Production p : productions) {
            variables.add(p.getLeftSide());
            for (String s : p.getRightSide()) {
                if (s == null || s.equals("ε")) {
                    continue;
                }
                if (Character.isUpperCase(s.charAt(0))) {
                    variables.add(s);
                } else {
                    terminals.add(s);
                }
            }
        }
        Grammar g = new Grammar(new LinkedHashSet<>(variables),
                new LinkedHashSet<>(terminals),
                new ArrayList<>(productions),
                start == null ? productions.isEmpty() ? "S" : productions.get(0).getLeftSide() : start);
        return g;
    }

    @Test
    void standard1() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B")),
                new Production("S", Arrays.asList("a")),
                new Production("A", Arrays.asList("a")),
                new Production("B", Arrays.asList("b")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        assertEquals(InputFncInspector.FncStatus.STANDARD, ins.inspect(g));
    }

    @Test
    void standardWithEpsilon() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B")),
                new Production("S", Arrays.asList("ε")),
                new Production("A", Arrays.asList("a")),
                new Production("B", Arrays.asList("b")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        assertEquals(InputFncInspector.FncStatus.STANDARD, ins.inspect(g));
    }

    @Test
    void courseExample() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("S", "A")),
                new Production("S", Arrays.asList("ε")),
                new Production("A", Arrays.asList("a")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        assertEquals(InputFncInspector.FncStatus.COURSE, ins.inspect(g));
    }

    @Test
    void courseCourseExampleFromSpec() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B")),
                new Production("S", Arrays.asList("2")),
                new Production("A", Arrays.asList("S", "1")),
                new Production("A", Arrays.asList("1")),
                new Production("B", Arrays.asList("B", "2")),
                new Production("B", Arrays.asList("2")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        assertEquals(InputFncInspector.FncStatus.COURSE, ins.inspect(g));
    }

    @Test
    void notFncThreeSymbols() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B", "C")),
                new Production("S", Arrays.asList("a")),
                new Production("A", Arrays.asList("a")),
                new Production("B", Arrays.asList("b")),
                new Production("C", Arrays.asList("a")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        InputFncInspector.FncStatus st = ins.inspect(g);
        assertEquals(InputFncInspector.FncStatus.NOT_FNC, st);
        assertTrue(ins.getViolations().stream()
                .anyMatch(v -> v.contains("más de 2 símbolos")));
    }

    @Test
    void notFncUnitary() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A")),
                new Production("A", Arrays.asList("a")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        assertEquals(InputFncInspector.FncStatus.NOT_FNC, ins.inspect(g));
        assertTrue(ins.getViolations().stream()
                .anyMatch(v -> v.contains("unitaria")));
    }

    @Test
    void notFncEpsilonInNonStart() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("a")),
                new Production("A", Arrays.asList("ε")));
        Grammar g = makeGrammar(p, "S");
        InputFncInspector ins = new InputFncInspector();
        assertEquals(InputFncInspector.FncStatus.NOT_FNC, ins.inspect(g));
        assertTrue(ins.getViolations().stream()
                .anyMatch(v -> v.contains("nula")));
    }

    @Test
    void doesNotMutateInput() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B")),
                new Production("A", Arrays.asList("a")));
        Grammar g = makeGrammar(p, "S");
        Grammar copy = g.copy();
        InputFncInspector ins = new InputFncInspector();
        ins.inspect(g);
        assertEquals(copy.toString(), g.toString());
    }
}
