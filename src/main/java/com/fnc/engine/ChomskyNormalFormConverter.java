package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.*;

/**
 * Converts a grammar to the course's Chomsky Normal Form
 * (Entrenamiento Algoritmo, paso 5):
 *
 * - Terminals are NEVER replaced by new variables.
 * - Only Xn variables are created, and only when splitting productions
 *   longer than 2 symbols (which may mix variables and terminals).
 * - No reuse: every partition creates a NEW Xn, even if another one
 *   already has the same body.
 * - Xn are numbered in creation order, traversing variables in the order
 *   their productions appear (top to bottom) and each production
 *   left to right. When splitting a production, the new Xn is numbered
 *   before splitting its own body (depth-first).
 * - Names already taken are skipped; a bare "X" is never generated.
 *
 * Splitting: A → X₁X₂...Xₙ becomes A → X₁Xn, Xn → X₂...Xₙ,
 * repeating on Xn until everything has at most 2 symbols.
 */
public class ChomskyNormalFormConverter {

    private Grammar originalGrammar;
    private Grammar transformedGrammar;
    private final List<String> productionsRemoved;
    private final List<String> productionsAdded;
    private final List<String> details;
    private final List<String> createdVariables;
    private final Set<String> takenNames;
    private int nextNumber;

    public ChomskyNormalFormConverter() {
        this.productionsRemoved = new ArrayList<>();
        this.productionsAdded = new ArrayList<>();
        this.details = new ArrayList<>();
        this.createdVariables = new ArrayList<>();
        this.takenNames = new HashSet<>();
        this.nextNumber = 1;
    }

    /**
     * Convert the grammar to Chomsky Normal Form.
     * @param grammar the grammar (cleaned by previous steps)
     * @return a TransformationStep with before/after grammars
     */
    public TransformationStep convert(Grammar grammar) {
        this.originalGrammar = grammar.copy();
        this.productionsRemoved.clear();
        this.productionsAdded.clear();
        this.details.clear();
        this.createdVariables.clear();
        this.takenNames.clear();
        this.takenNames.addAll(grammar.getVariables());
        this.takenNames.addAll(grammar.getTerminals());
        this.nextNumber = 1;

        transformedGrammar = splitLongProductions(grammar);

        String description;
        if (productionsRemoved.isEmpty()) {
            description = "No hay producciones de más de 2 símbolos, no hay "
                    + "cambios en la gramática. Se pasa al siguiente paso.";
        } else {
            description = "Se parten las producciones de más de 2 símbolos: se deja "
                    + "el primer símbolo y el resto pasa a una Xn nueva, hasta que "
                    + "todo tenga máximo 2 símbolos.";
        }

        return new TransformationStep(
            "Aplicando Forma Normal de Chomsky",
            description,
            originalGrammar,
            transformedGrammar,
            productionsRemoved,
            productionsAdded,
            details
        );
    }

    /**
     * Traverse productions in written order (top to bottom); split each
     * one longer than 2 symbols depth-first.
     */
    private Grammar splitLongProductions(Grammar grammar) {
        Grammar result = grammar.copy();
        List<Production> output = new ArrayList<>();

        String currentOwner = null;
        for (Production p : grammar.getProductions()) {
            if (!p.getLeftSide().equals(currentOwner)) {
                currentOwner = p.getLeftSide();
                details.add("Producciones de " + currentOwner + ":");
            }
            if (p.getRightSide().size() > 2) {
                productionsRemoved.add(p.toString());
                splitDepthFirst(output, p.getLeftSide(), p.getRightSide());
            } else {
                output.add(p);
                if (p.getRightSide().size() == 2) {
                    details.add(p + " (ya tiene 2 símbolos)");
                } else {
                    details.add(p.toString());
                }
            }
        }

        result.setProductions(output);
        Set<String> newVariables = new LinkedHashSet<>(grammar.getVariables());
        newVariables.addAll(createdVariables);
        result.setVariables(newVariables);
        return result;
    }

    /**
     * Split A → X₁X₂...Xₙ (n > 2) into A → X₁Xn and Xn → X₂...Xₙ,
     * numbering Xn before splitting its own body (depth-first).
     */
    private void splitDepthFirst(List<Production> output, String left, List<String> right) {
        String head = right.get(0);
        List<String> tail = new ArrayList<>(right.subList(1, right.size()));

        String xn = nextFreeName();
        details.add(left + "→" + String.join(" ", right)
                + ": " + xn + "→" + String.join(" ", tail)
                + ", " + left + "→" + head + " " + xn);

        Production headProd = new Production(left, List.of(head, xn));
        output.add(headProd);
        productionsAdded.add(headProd.toString());

        if (tail.size() > 2) {
            splitDepthFirst(output, xn, tail);
        } else {
            Production tailProd = new Production(xn, tail);
            output.add(tailProd);
            productionsAdded.add(tailProd.toString());
        }
    }

    /**
     * Next free Xn name, skipping taken ones. Never a bare "X".
     */
    private String nextFreeName() {
        String name;
        do {
            name = "X" + nextNumber;
            nextNumber++;
        } while (takenNames.contains(name));
        takenNames.add(name);
        createdVariables.add(name);
        return name;
    }

    // Getters
    public List<String> getProductionsRemoved() {
        return productionsRemoved;
    }

    public List<String> getProductionsAdded() {
        return productionsAdded;
    }

    public List<String> getDetails() {
        return details;
    }

    /** Xn created, in creation order. */
    public List<String> getCreatedVariables() {
        return createdVariables;
    }

    public Grammar getTransformedGrammar() {
        return transformedGrammar;
    }
}
