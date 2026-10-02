package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.*;

/**
 * Eliminates useless (non-generating) variables from a context-free grammar.
 *
 * A variable is useless if it cannot derive any terminal string.
 *
 * Algorithm:
 * 1. Find all generating variables:
 *    - A is generating if A → w where w consists only of terminals
 *    - A is generating if A → X₁X₂...Xₙ where all Xᵢ are generating
 * 2. Remove all productions with non-generating variables on the left side
 * 3. Remove all occurrences of non-generating variables on the right side
 *
 * Reference: Introduction to Automata Theory, Languages, and Computation
 *            (Hopcroft, Motwani, Ullman) - Chapter 6
 */
public class UselessVariableEliminator {

    private Grammar originalGrammar;
    private Grammar transformedGrammar;
    private final List<String> productionsRemoved;
    private final List<String> productionsAdded;
    private final Set<String> generatingVariables;

    public UselessVariableEliminator() {
        this.productionsRemoved = new ArrayList<>();
        this.productionsAdded = new ArrayList<>();
        this.generatingVariables = new LinkedHashSet<>();
    }

    /**
     * Eliminate useless (non-generating) variables from the grammar.
     * @param grammar the original grammar (should have no ε or unit productions)
     * @return a TransformationStep with before/after grammars
     */
    public TransformationStep eliminate(Grammar grammar) {
        this.originalGrammar = grammar.copy();
        this.productionsRemoved.clear();
        this.productionsAdded.clear();
        this.generatingVariables.clear();

        // Step 1: Find all generating variables
        findGeneratingVariables(grammar);

        // Step 2: Create grammar without non-generating variables
        transformedGrammar = createGrammarWithoutNonGenerating(grammar);

        List<String> details = new ArrayList<>();
        String description;
        Set<String> useless = new LinkedHashSet<>(grammar.getVariables());
        useless.removeAll(generatingVariables);
        if (useless.isEmpty()) {
            description = "No se han identificado variables inútiles, no hay "
                    + "cambios en la gramática. Se pasa al siguiente paso.";
        } else if (useless.size() == 1) {
            String u = useless.iterator().next();
            description = "Se identificó la variable inútil: " + u + ". "
                    + "Se cancelan las producciones que contengan esta variable.";
            details.add("Se elimina la variable inútil: " + u);
        } else {
            description = "Se identificaron las variables inútiles: "
                    + String.join(", ", useless) + ". "
                    + "Se cancelan las producciones que contengan estas variables.";
            for (String u : useless) {
                details.add("Se elimina la variable inútil: " + u);
            }
        }

        return new TransformationStep(
            "Eliminación de variables inútiles",
            description,
            originalGrammar,
            transformedGrammar,
            productionsRemoved,
            productionsAdded,
            details
        );
    }

    /**
     * Find all generating variables.
     *
     * Algorithm (entrenamiento: ε cuenta como cadena generada, pues ε ∈ T*):
     * 1. Mark A as generating if A → w where w ∈ T* (only terminals, incl ε)
     * 2. Repeat until no more changes:
     *    Mark A as generating if A → X₁X₂...Xₙ and all Xᵢ are generating
     */
    private void findGeneratingVariables(Grammar grammar) {
        Set<String> newGenerating = new LinkedHashSet<>();

        // Step 1: Find variables that directly generate terminals (ε counts)
        for (Production p : grammar.getProductions()) {
            boolean allTerminals = true;
            for (String symbol : p.getRightSide()) {
                if (!grammar.getTerminals().contains(symbol)) {
                    allTerminals = false;
                    break;
                }
            }

            if (allTerminals) {
                newGenerating.add(p.getLeftSide());
            }
        }

        // Step 2: Iteratively find indirectly generating variables
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Production p : grammar.getProductions()) {
                // Skip if already generating
                if (newGenerating.contains(p.getLeftSide())) continue;

                // Check if all symbols on right side are generating
                // (either terminal or generating variable; ε counts as generated)
                boolean allGenerating = true;
                for (String symbol : p.getRightSide()) {
                    if (!grammar.getTerminals().contains(symbol)
                            && !newGenerating.contains(symbol)) {
                        allGenerating = false;
                        break;
                    }
                }

                if (allGenerating) {
                    newGenerating.add(p.getLeftSide());
                    changed = true;
                }
            }
        }

        generatingVariables.addAll(newGenerating);
    }

    /**
     * Create a new grammar without non-generating variables.
     */
    private Grammar createGrammarWithoutNonGenerating(Grammar grammar) {
        Grammar newGrammar = grammar.copy();
        List<Production> newProductions = new ArrayList<>();
        Set<String> processedProductions = new HashSet<>();

        for (Production p : grammar.getProductions()) {
            // Skip productions with non-generating variable on left side
            if (!generatingVariables.contains(p.getLeftSide())) {
                productionsRemoved.add(p.toString());
                continue;
            }

            // NOTA: las producciones ε (vacío) NO se tocan aquí aunque su
            // variable sea generadora: solo las elimina el paso de
            // producciones nulas. Solo sale el ε de una variable que de por
            // sí es inútil (caso anterior).

            // Check if all symbols on right side are valid
            // (terminal or generating variable)
            boolean allValid = true;
            for (String symbol : p.getRightSide()) {
                if (!grammar.getTerminals().contains(symbol)
                        && !generatingVariables.contains(symbol)) {
                    allValid = false;
                    break;
                }
            }

            if (!allValid) {
                productionsRemoved.add(p.toString());
                continue;
            }

            // Add valid production
            String key = p.toString();
            if (!processedProductions.contains(key)) {
                newProductions.add(p);
                processedProductions.add(key);
            }
        }

        newGrammar.setProductions(newProductions);

        // Update variables to only include generating ones
        Set<String> newVariables = new LinkedHashSet<>(grammar.getVariables());
        newVariables.retainAll(generatingVariables);
        newGrammar.setVariables(newVariables);

        return newGrammar;
    }

    // Getters
    public Set<String> getGeneratingVariables() {
        return generatingVariables;
    }

    public Set<String> getNonGeneratingVariables(Grammar grammar) {
        Set<String> nonGenerating = new HashSet<>(grammar.getVariables());
        nonGenerating.removeAll(generatingVariables);
        return nonGenerating;
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
