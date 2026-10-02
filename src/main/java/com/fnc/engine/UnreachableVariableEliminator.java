package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.*;

/**
 * Eliminates unreachable variables from a context-free grammar.
 *
 * A variable is unreachable if it cannot be reached from the start symbol S.
 *
 * Algorithm:
 * 1. Mark S as reachable
 * 2. For each reachable variable A, if A → X₁X₂...Xₙ,
 *    mark X₁, X₂, ..., Xₙ as reachable
 * 3. Repeat until no more variables can be marked as reachable
 * 4. Remove all productions with unreachable variables on the left side
 *
 * Reference: Introduction to Automata Theory, Languages, and Computation
 *            (Hopcroft, Motwani, Ullman) - Chapter 6
 */
public class UnreachableVariableEliminator {

    private Grammar originalGrammar;
    private Grammar transformedGrammar;
    private final List<String> productionsRemoved;
    private final List<String> productionsAdded;
    private final Set<String> reachableVariables;

    public UnreachableVariableEliminator() {
        this.productionsRemoved = new ArrayList<>();
        this.productionsAdded = new ArrayList<>();
        this.reachableVariables = new LinkedHashSet<>();
    }

    /**
     * Eliminate unreachable variables from the grammar.
     * @param grammar the original grammar
     * @return a TransformationStep with before/after grammars
     */
    public TransformationStep eliminate(Grammar grammar) {
        this.originalGrammar = grammar.copy();
        this.productionsRemoved.clear();
        this.productionsAdded.clear();
        this.reachableVariables.clear();

        // Step 1: Find all reachable variables
        findReachableVariables(grammar);

        // Step 2: Create grammar without unreachable variables
        transformedGrammar = createGrammarWithoutUnreachable(grammar);

        List<String> details = new ArrayList<>();
        String description;
        Set<String> unreachable = new LinkedHashSet<>(grammar.getVariables());
        unreachable.removeAll(reachableVariables);
        if (unreachable.isEmpty()) {
            description = "No se han identificado variables inalcanzables, no hay "
                    + "cambios en la gramática. Se pasa al siguiente paso.";
        } else if (unreachable.size() == 1) {
            String u = unreachable.iterator().next();
            description = "Se identificó como variable inalcanzable: " + u + ". "
                    + "Se elimina " + u + " con sus producciones.";
            details.add("Se elimina la variable inalcanzable: " + u);
        } else {
            description = "Se identificaron como variables inalcanzables: "
                    + String.join(", ", unreachable) + ". Se eliminan con sus producciones.";
            for (String u : unreachable) {
                details.add("Se elimina la variable inalcanzable: " + u);
            }
        }

        return new TransformationStep(
            "Eliminación de variables inalcanzables",
            description,
            originalGrammar,
            transformedGrammar,
            productionsRemoved,
            productionsAdded,
            details
        );
    }

    /**
     * Find all variables reachable from the start symbol.
     *
     * Algorithm:
     * 1. Mark S as reachable
     * 2. Repeat until no more changes:
     *    For each reachable variable A and production A → X₁X₂...Xₙ,
     *    mark each Xᵢ as reachable
     */
    private void findReachableVariables(Grammar grammar) {
        // Start with the start symbol
        if (grammar.getStartSymbol() != null) {
            reachableVariables.add(grammar.getStartSymbol());
        }

        // Iteratively find reachable variables
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Production p : grammar.getProductions()) {
                // Only process productions of reachable variables
                if (!reachableVariables.contains(p.getLeftSide())) {
                    continue;
                }

                // Mark all variables on right side as reachable
                for (String symbol : p.getRightSide()) {
                    if (grammar.getVariables().contains(symbol)
                            && !reachableVariables.contains(symbol)) {
                        reachableVariables.add(symbol);
                        changed = true;
                    }
                }
            }
        }
    }

    /**
     * Create a new grammar without unreachable variables.
     */
    private Grammar createGrammarWithoutUnreachable(Grammar grammar) {
        Grammar newGrammar = grammar.copy();
        List<Production> newProductions = new ArrayList<>();
        Set<String> processedProductions = new HashSet<>();

        for (Production p : grammar.getProductions()) {
            // Skip productions with unreachable variable on left side
            if (!reachableVariables.contains(p.getLeftSide())) {
                productionsRemoved.add(p.toString());
                continue;
            }

            // Add reachable production
            String key = p.toString();
            if (!processedProductions.contains(key)) {
                newProductions.add(p);
                processedProductions.add(key);
            }
        }

        newGrammar.setProductions(newProductions);

        // Update variables to only include reachable ones
        Set<String> newVariables = new LinkedHashSet<>(grammar.getVariables());
        newVariables.retainAll(reachableVariables);
        newGrammar.setVariables(newVariables);

        return newGrammar;
    }

    // Getters
    public Set<String> getReachableVariables() {
        return reachableVariables;
    }

    public Set<String> getUnreachableVariables(Grammar grammar) {
        Set<String> unreachable = new HashSet<>(grammar.getVariables());
        unreachable.removeAll(reachableVariables);
        return unreachable;
    }

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
