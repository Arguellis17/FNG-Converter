package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.GrammarFormatter;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.*;

/**
 * Eliminates null (ε) productions from a context-free grammar,
 * following the course file (Entrenamiento Algoritmo):
 *
 * - The nullable set is computed ONCE at the start of the step.
 * - Nullables are processed one by one, in order of appearance.
 *   When processing V ("Eliminando vacío: V→ε"), only V is touched:
 *   V→ε is removed and variants without V are added to every production
 *   containing V (all combinations). Other lines stay intact.
 * - Empty variants are discarded: an ε is never re-added.
 * - S→ε is preserved (case c: the language contains the empty string),
 *   with a notice; if S appears on a right side, a textbook warning
 *   is added (no new start symbol is created).
 *
 * Reference: Entrenamiento Algoritmo (paso 4).
 */
public class NullProductionEliminator {

    private Grammar originalGrammar;
    private Grammar transformedGrammar;
    private final List<String> productionsRemoved;
    private final List<String> productionsAdded;
    private final List<String> details;
    private final Set<String> nullableVariables;

    public NullProductionEliminator() {
        this.productionsRemoved = new ArrayList<>();
        this.productionsAdded = new ArrayList<>();
        this.details = new ArrayList<>();
        this.nullableVariables = new LinkedHashSet<>();
    }

    /**
     * Eliminate null productions from the grammar.
     * @param grammar the original grammar
     * @return a TransformationStep with before/after grammars
     */
    public TransformationStep eliminate(Grammar grammar) {
        this.originalGrammar = grammar.copy();
        this.productionsRemoved.clear();
        this.productionsAdded.clear();
        this.details.clear();
        this.nullableVariables.clear();

        // Nullable set computed ONCE, in order of appearance.
        findNullableVariables(grammar);

        String description;
        if (nullableVariables.isEmpty()) {
            description = "No se han identificado producciones nulas, no hay "
                    + "cambios en la gramática. Se pasa al siguiente paso.";
            transformedGrammar = grammar.copy();
        } else {
            description = "Se eliminan las producciones nulas: se procesa cada "
                    + "variable anulable y se generan las variantes sin ella.";
            details.add("Nulables: " + String.join(" y ", nullableVariables) + ".");
            transformedGrammar = processNullablesOneByOne(grammar);
        }

        return new TransformationStep(
            "Eliminación de producciones nulas (vacío)",
            description,
            originalGrammar,
            transformedGrammar,
            productionsRemoved,
            productionsAdded,
            details
        );
    }

    /**
     * Find all variables that can derive ε, in order of appearance:
     * first the ones with a direct V→ε (production order), then the
     * indirectly nullable ones in discovery order.
     */
    private void findNullableVariables(Grammar grammar) {
        // Direct: V→ε in production order.
        for (Production p : grammar.getProductions()) {
            if (p.isNull() && !nullableVariables.contains(p.getLeftSide())) {
                nullableVariables.add(p.getLeftSide());
            }
        }

        // Indirect: V → X₁...Xₙ with all Xᵢ already nullable.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production p : grammar.getProductions()) {
                if (p.isNull() || nullableVariables.contains(p.getLeftSide())) {
                    continue;
                }
                boolean allNullable = true;
                for (String symbol : p.getRightSide()) {
                    if (!nullableVariables.contains(symbol)) {
                        allNullable = false;
                        break;
                    }
                }
                if (allNullable) {
                    nullableVariables.add(p.getLeftSide());
                    changed = true;
                }
            }
        }
    }

    /**
     * Process each nullable once, over the CURRENT grammar.
     * Only the variable being processed is touched.
     */
    private Grammar processNullablesOneByOne(Grammar grammar) {
        Grammar current = grammar.copy();

        for (String nullable : nullableVariables) {
            boolean isStart = nullable.equals(current.getStartSymbol());

            if (isStart) {
                // Case (c): S→ε is preserved, never removed.
                details.add("Se conserva " + nullable + "→ε: "
                        + "el lenguaje contiene la cadena vacía.");
                if (appearsOnRightSide(current, nullable)) {
                    details.add("Aviso: " + nullable + " aparece en el lado derecho de "
                            + "alguna producción; la FNC de libro pediría un símbolo "
                            + "inicial nuevo (no se agrega).");
                }
            } else {
                removeEpsilonProduction(current, nullable);
                details.add("Eliminando vacío: " + nullable + "→ε");
            }

            // Variants without V in every production containing V.
            addVariantsWithout(current, nullable);

            details.add(GrammarFormatter.formatFull(current));
        }

        return current;
    }

    private boolean appearsOnRightSide(Grammar grammar, String symbol) {
        for (Production p : grammar.getProductions()) {
            if (p.getRightSide().contains(symbol)) {
                return true;
            }
        }
        return false;
    }

    private void removeEpsilonProduction(Grammar grammar, String nullable) {
        Iterator<Production> it = grammar.getProductions().iterator();
        while (it.hasNext()) {
            Production p = it.next();
            if (p.getLeftSide().equals(nullable) && p.isNull()) {
                productionsRemoved.add(p.toString());
                it.remove();
            }
        }
    }

    /**
     * For each production containing V, add all variants with V removed
     * (all combinations of its occurrences) right after their source
     * production, like the file (A→BB1 gives A→BB1 | B1 | 1).
     * Empty variants are discarded: an ε is never re-added.
     * The production itself always stays.
     */
    private void addVariantsWithout(Grammar grammar, String nullable) {
        List<Production> snapshot = new ArrayList<>(grammar.getProductions());
        List<Production> rebuilt = new ArrayList<>();
        Set<String> known = new HashSet<>();
        for (Production p : snapshot) {
            known.add(p.toString());
        }

        for (Production p : snapshot) {
            rebuilt.add(p);
            List<String> rightSide = p.getRightSide();
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < rightSide.size(); i++) {
                if (rightSide.get(i).equals(nullable)) {
                    positions.add(i);
                }
            }
            if (positions.isEmpty()) {
                continue;
            }

            int totalCombinations = 1 << positions.size();
            for (int mask = 1; mask < totalCombinations; mask++) {
                Set<Integer> toRemove = new HashSet<>();
                for (int i = 0; i < positions.size(); i++) {
                    if ((mask & (1 << i)) != 0) {
                        toRemove.add(positions.get(i));
                    }
                }
                List<String> variant = new ArrayList<>();
                for (int i = 0; i < rightSide.size(); i++) {
                    if (!toRemove.contains(i)) {
                        variant.add(rightSide.get(i));
                    }
                }
                // Empty variants are discarded: ε is never re-added.
                if (variant.isEmpty()) {
                    continue;
                }
                Production newProd = new Production(p.getLeftSide(), variant);
                if (!known.contains(newProd.toString())) {
                    rebuilt.add(newProd);
                    known.add(newProd.toString());
                    productionsAdded.add(newProd.toString());
                }
            }
        }
        grammar.setProductions(rebuilt);
    }

    // Getters
    public Set<String> getNullableVariables() {
        return nullableVariables;
    }

    public List<String> getProductionsRemoved() {
        return productionsRemoved;
    }

    public List<String> getProductionsAdded() {
        return productionsAdded;
    }

    public List<String> getDetails() {
        return details;
    }

    public Grammar getTransformedGrammar() {
        return transformedGrammar;
    }
}
