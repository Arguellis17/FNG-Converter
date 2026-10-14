package com.fnc.engine;

import com.fnc.model.ConversionTarget;
import com.fnc.model.Grammar;
import com.fnc.model.Production;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrictModeValidatorTest {

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
        return new Grammar(new LinkedHashSet<>(variables),
                new LinkedHashSet<>(terminals),
                new ArrayList<>(productions),
                start == null ? productions.isEmpty() ? "S" : productions.get(0).getLeftSide() : start);
    }

    @Test
    void emptyWhenOk() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B")),
                new Production("S", Arrays.asList("a")),
                new Production("A", Arrays.asList("a")),
                new Production("B", Arrays.asList("b")));
        Grammar g = makeGrammar(p, "S");
        Map<String, String> auto = new LinkedHashMap<>();
        List<String> errs = StrictModeValidator.validate(g, ConversionTarget.GREIBACH, auto);
        assertTrue(errs.isEmpty());
    }

    @Test
    void autoDeclaredSymbol() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B")));
        Grammar g = makeGrammar(p, "S");
        Map<String, String> auto = new LinkedHashMap<>();
        auto.put("B", "variable");
        List<String> errs = StrictModeValidator.validate(g, ConversionTarget.GREIBACH, auto);
        assertFalse(errs.isEmpty());
        assertTrue(errs.get(0).contains("no fue declarado"));
    }

    @Test
    void greibachNotFnc() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B", "C")),
                new Production("S", Arrays.asList("a")));
        Grammar g = makeGrammar(p, "S");
        Map<String, String> auto = new LinkedHashMap<>();
        List<String> errs = StrictModeValidator.validate(g, ConversionTarget.GREIBACH, auto);
        assertFalse(errs.isEmpty());
        assertTrue(errs.stream().anyMatch(e -> e.contains("no está en FNC")));
    }

    @Test
    void chomskyDoesNotCheckFnc() {
        List<Production> p = Arrays.asList(
                new Production("S", Arrays.asList("A", "B", "C")));
        Grammar g = makeGrammar(p, "S");
        Map<String, String> auto = new LinkedHashMap<>();
        List<String> errs = StrictModeValidator.validate(g, ConversionTarget.CHOMSKY, auto);
        assertTrue(errs.isEmpty());
    }
}
