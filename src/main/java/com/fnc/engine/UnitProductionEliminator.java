package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.*;

/**
 * Eliminates unit productions from a context-free grammar.
 *
 * A unit production is of the form: A → B
 * where both A and B are variables (non-terminals).
 *
 * Algorithm:
 * 1. Find all unit pairs (A, B) such that A ⇒* B through unit productions
 * 2. For each unit pair (A, B), add all non-unit productions of B to A
 * 3. Remove all unit productions
 *
 * Reference: Introduction to Automata Theory, Languages, and Computation
 *            (Hopcroft, Motwani, Ullman) - Chapter 6
 */
public class UnitProductionEliminator {

    private Grammar originalGrammar;
    private Grammar transformedGrammar;
    private final List<String> productionsRemoved;
    private final List<String> productionsAdded;

    public UnitProductionEliminator() {
        this.productionsRemoved = new ArrayList<>();
        this.productionsAdded = new ArrayList<>();
    }

    /**
     * Eliminate unit productions from the grammar.
     * @param grammar the original grammar (should have no ε-productions)
     * @return a TransformationStep with before/after grammars
     */
    public TransformationStep eliminate(Grammar grammar) {
        this.originalGrammar = grammar.copy();
        this.productionsRemoved.clear();
        this.productionsAdded.clear();

        // Step 1: Find all unit pairs
        Map<String, Set<String>> unitPairs = findUnitPairs(grammar);

        // Step 2: Create grammar without unit productions
        transformedGrammar = createGrammarWithoutUnitProductions(grammar, unitPairs);

        List<String> details = new ArrayList<>();
        String description;
        if (productionsRemoved.isEmpty()) {
            description = "No se han identificado producciones unitarias, no hay "
                    + "cambios en la gramática. Se pasa al siguiente paso.";
        } else {
            description = "Se eliminan las producciones unitarias: en cada variable "
                    + "se reemplaza la unitaria por las producciones del símbolo destino.";
            for (String removed : productionsRemoved) {
                String[] sides = removed.split(" -> ", 2);
                String left = sides[0];
                String right = sides.length > 1 ? sides[1] : "";
                details.add("Eliminando unitaria: " + left + "→" + right + ". "
                        + "En " + left + " se reemplaza la " + right + " por sus producciones.");
            }
        }

        return new TransformationStep(
            "Eliminación de unitarias",
            description,
            originalGrammar,
            transformedGrammar,
            productionsRemoved,
            productionsAdded,
            details
        );
    }

    /**
     * Find all unit pairs (A, B) such that A ⇒* B through unit productions.
     *
     * Algorithm:
     * 1. For each A → B, add (A, B) to unit pairs
     * 2. Repeat until no more changes:
     *    If (A, B) is a pair and B → C is a unit production,
     *    add (A, C) to unit pairs
     */
    private Map<String, Set<String>> findUnitPairs(Grammar grammar) {
        // Linked structures: deterministic order (visited set, no cycles loop).
        Map<String, Set<String>> unitPairs = new LinkedHashMap<>();

        // Initialize with direct unit productions, in production order.
        for (Production p : grammar.getProductions()) {
            if (p.isUnitary()) {
                String left = p.getLeftSide();
                String right = p.getRightSide().get(0);

                unitPairs.computeIfAbsent(left, k -> new LinkedHashSet<>()).add(right);
            }
        }

        // Transitive closure.
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Map.Entry<String, Set<String>> entry : unitPairs.entrySet()) {
                String a = entry.getKey();
                List<String> bSet = new ArrayList<>(entry.getValue());

                for (String b : bSet) {
                    // If (A, B) exists and B → C is unit, add (A, C)
                    Set<String> cSet = unitPairs.getOrDefault(b, Collections.emptySet());
                    for (String c : cSet) {
                        if (!unitPairs.get(a).contains(c)) {
                            unitPairs.get(a).add(c);
                            changed = true;
                        }
                    }
                }
            }
        }

        return unitPairs;
    }

    /**
     * Create a new grammar without unit productions. Each unit production
     * A→B is replaced in place by B's non-unit productions (in order).
     */
    private Grammar createGrammarWithoutUnitProductions(Grammar grammar,
                                                         Map<String, Set<String>> unitPairs) {
        Grammar newGrammar = grammar.copy();
        List<Production> newProductions = new ArrayList<>();
        Set<String> processedProductions = new HashSet<>();
        for (Production p : grammar.getProductions()) {
            processedProductions.add(p.toString());
        }

        for (Production p : grammar.getProductions()) {
            // Replace unit productions in place.
            if (p.isUnitary()) {
                productionsRemoved.add(p.toString());
                String a = p.getLeftSide();
                for (String b : unitPairs.getOrDefault(a, Collections.emptySet())) {
                    for (Production q : grammar.getProductions()) {
                        if (q.getLeftSide().equals(b) && !q.isUnitary()) {
                            Production newProd = new Production(a, q.getRightSide());
                            String key = newProd.toString();
                            if (!processedProductions.contains(key)) {
                                newProductions.add(newProd);
                                processedProductions.add(key);
                                productionsAdded.add(newProd.toString());
                            }
                        }
                    }
                }
                continue;
            }

            // Add non-unit productions as-is.
            String key = p.toString();
            if (!newProductions.contains(p)) {
                newProductions.add(p);
            }
        }

        newGrammar.setProductions(newProductions);
        return newGrammar;
    }

    // Getters
    public List<String> getProductionsRemoved() {
        return productionsRemoved;
    }

    public List<String> getProductionsAdded() {
        return productionsAdded;
    }

    public Grammar getTransformedGrammar() {
        return transformedGrammar;
    }
}
