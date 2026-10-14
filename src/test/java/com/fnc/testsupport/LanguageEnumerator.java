package com.fnc.testsupport;

import com.fnc.model.Grammar;
import com.fnc.model.Production;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilidad de prueba que enumera el lenguaje de una gramática libre de
 * contexto hasta una longitud máxima (RNF06).
 * <p>
 * Calcula por punto fijo, para cada variable, el conjunto de cadenas de solo
 * terminales que puede derivar con a lo sumo {@code maxLength} símbolos. La
 * cadena vacía ε se representa con {@code ""}.
 * <p>
 * El símbolo se clasifica como variable si está en {@code grammar.getVariables()};
 * en caso contrario se trata como terminal. La longitud se mide en caracteres,
 * lo que coincide con el número de símbolos para las gramáticas de prueba del
 * proyecto, cuyos terminales son de un solo carácter.
 */
public final class LanguageEnumerator {

    private LanguageEnumerator() {
    }

    /**
     * Devuelve todas las cadenas de hasta {@code maxLength} símbolos derivables
     * desde el símbolo inicial de la gramática.
     *
     * @param grammar   gramática libre de contexto
     * @param maxLength longitud máxima, en símbolos, de las cadenas
     * @return conjunto de cadenas en orden de aparición (puede incluir {@code ""})
     */
    public static Set<String> generate(Grammar grammar, int maxLength) {
        Map<String, Set<String>> languages = new LinkedHashMap<>();
        for (String variable : grammar.getVariables()) {
            languages.put(variable, new LinkedHashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production production : grammar.getProductions()) {
                Set<String> derived = derive(production.getRightSide(), grammar, languages, maxLength);
                Set<String> target = languages.computeIfAbsent(
                        production.getLeftSide(), key -> new LinkedHashSet<>());
                for (String word : derived) {
                    if (target.add(word)) {
                        changed = true;
                    }
                }
            }
        }

        Set<String> start = languages.get(grammar.getStartSymbol());
        return start == null ? new LinkedHashSet<>() : new LinkedHashSet<>(start);
    }

    private static Set<String> derive(List<String> rightSide, Grammar grammar,
                                      Map<String, Set<String>> languages, int maxLength) {
        Set<String> partial = new LinkedHashSet<>();
        partial.add("");

        for (String symbol : rightSide) {
            Set<String> symbolStrings = symbolStrings(symbol, grammar, languages);
            if (symbolStrings.isEmpty()) {
                return new LinkedHashSet<>();
            }
            Set<String> combined = new LinkedHashSet<>();
            for (String prefix : partial) {
                for (String suffix : symbolStrings) {
                    String candidate = prefix + suffix;
                    if (candidate.length() <= maxLength) {
                        combined.add(candidate);
                    }
                }
            }
            if (combined.isEmpty()) {
                return combined;
            }
            partial = combined;
        }
        return partial;
    }

    private static Set<String> symbolStrings(String symbol, Grammar grammar,
                                             Map<String, Set<String>> languages) {
        if (symbol.equals("ε")) {
            Set<String> epsilon = new LinkedHashSet<>();
            epsilon.add("");
            return epsilon;
        }
        if (grammar.getVariables().contains(symbol)) {
            Set<String> language = languages.get(symbol);
            return language == null ? new LinkedHashSet<>() : language;
        }
        Set<String> terminal = new LinkedHashSet<>();
        terminal.add(symbol);
        return terminal;
    }
}
