package com.fnc.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Context-Free Grammar (CFG).
 * G = (V, T, P, S)
 * V: variables (non-terminals)
 * T: terminals
 * P: production rules
 * S: start symbol
 */
public class Grammar {
    private Set<String> variables;
    private Set<String> terminals;
    private List<Production> productions;
    private String startSymbol;

    public Grammar() {
        this.variables = new LinkedHashSet<>();
        this.terminals = new LinkedHashSet<>();
        this.productions = new ArrayList<>();
    }

    public Grammar(Set<String> variables, Set<String> terminals,
                   List<Production> productions, String startSymbol) {
        this.variables = new LinkedHashSet<>(variables);
        this.terminals = new LinkedHashSet<>(terminals);
        this.productions = new ArrayList<>(productions);
        this.startSymbol = startSymbol;
    }

    // Getters and Setters
    public Set<String> getVariables() {
        return variables;
    }

    public void setVariables(Set<String> variables) {
        this.variables = variables;
    }

    public Set<String> getTerminals() {
        return terminals;
    }

    public void setTerminals(Set<String> terminals) {
        this.terminals = terminals;
    }

    public List<Production> getProductions() {
        return productions;
    }

    public void setProductions(List<Production> productions) {
        this.productions = productions;
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    /**
     * Add a production rule to the grammar.
     */
    public void addProduction(Production production) {
        productions.add(production);
    }

    /**
     * Get all productions for a specific variable.
     */
    public List<Production> getProductionsFor(String variable) {
        return productions.stream()
                .filter(p -> p.getLeftSide().equals(variable))
                .collect(Collectors.toList());
    }

    /**
     * Get all variables that appear on the right side of any production.
     */
    public Set<String> getVariablesInRightSide() {
        return productions.stream()
                .flatMap(p -> p.getRightSide().stream())
                .filter(s -> variables.contains(s))
                .collect(Collectors.toSet());
    }

    /**
     * Registra automáticamente los símbolos usados en producciones pero no
     * declarados en V ni T, para no bloquear el proceso con un error:
     * mayúscula inicial -> variable, otro caso -> terminal.
     * Así, una variable usada pero sin producciones propias (ej: F en A -> F1)
     * entra al proceso y la elimina el paso de variables inútiles.
     * @return mapa símbolo -> "variable"|"terminal" en orden de aparición.
     */
    public Map<String, String> autoDeclareMissingSymbols() {
        Map<String, String> added = new LinkedHashMap<>();
        for (Production p : productions) {
            declareIfMissing(p.getLeftSide(), added);
            for (String s : p.getRightSide()) {
                if (s.equals("ε")) {
                    continue;
                }
                declareIfMissing(s, added);
            }
        }
        return added;
    }

    private void declareIfMissing(String symbol, Map<String, String> added) {
        if (symbol == null || symbol.isEmpty()
                || variables.contains(symbol) || terminals.contains(symbol)) {
            return;
        }
        if (Character.isUpperCase(symbol.charAt(0))) {
            variables.add(symbol);
            added.put(symbol, "variable");
        } else {
            terminals.add(symbol);
            added.put(symbol, "terminal");
        }
    }

    /**
     * Create a deep copy of this grammar.
     */
    public Grammar copy() {
        Grammar copy = new Grammar();
        copy.variables = new LinkedHashSet<>(this.variables);
        copy.terminals = new LinkedHashSet<>(this.terminals);
        copy.productions = new ArrayList<>(this.productions);
        copy.startSymbol = this.startSymbol;
        return copy;
    }

    /**
     * Format grammar as a readable string, grouping alternatives
     * by variable (ej: S -> A B | B).
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("G = (V, T, P, S)\n\n");
        sb.append("V = {").append(String.join(", ", variables)).append("}\n");
        sb.append("T = {").append(String.join(", ", terminals)).append("}\n");
        sb.append("S = ").append(startSymbol).append("\n\n");
        sb.append("P = {\n");
        for (String line : GrammarFormatter.formatProductionLines(this)) {
            sb.append("  ").append(line).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
