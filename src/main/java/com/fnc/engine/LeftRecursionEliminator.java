package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Paso 10 del pipeline FNG: elimina la recursividad izquierda variable por
 * variable, siguiendo el orden del paso 9.
 * <p>
 * Para cada variable {@code Xi} (en el orden recibido):
 * <ol>
 *   <li><b>Sustitución.</b> Mientras {@code Xi} tenga una producción
 *       {@code Xi → Xj α} con {@code j < i}, se reemplaza esa producción por una
 *       por cada producción de {@code Xj} ({@code Xi → β α}). Se usan todas las
 *       producciones de {@code Xj}.</li>
 *   <li><b>Recursividad inmediata.</b> Si tras (1) {@code Xi} tiene producciones
 *       {@code Xi → Xi α} y otras que no empiezan por {@code Xi}, se reemplazan por
 *       {@code Xi → β1 | … | βm | β1 Xi.1 | … | βm Xi.1} y
 *       {@code Xi.1 → α1 | … | αk | α1 Xi.1 | … | αk Xi.1}, pidiendo {@code Xi.1}
 *       una sola vez a {@link AuxiliaryNameRegistry#recursionVariable(String)}.</li>
 * </ol>
 * Se emite un {@link TransformationStep} por cada variable que cambió; las
 * variables que no cambian no generan paso. Si ninguna cambió, se emite uno solo
 * con el mensaje de "sin cambios". La gramática recibida nunca se muta.
 */
public class LeftRecursionEliminator {

    private static final int MENU_NUMBER = 10;
    private static final String NO_CHANGES =
            "No se han identificado recursividades a la izquierda, no hay cambios "
            + "en la gramática. Se pasa al siguiente paso.";

    private final List<String> order;
    private final AuxiliaryNameRegistry registry;
    private List<String> createdVariables = new ArrayList<>();

    /**
     * Crea el eliminador con el orden de variables y el registro de nombres
     * auxiliares indicados.
     *
     * @param order    orden de variables del paso 9
     * @param registry registro que entrega los nombres {@code V.1}
     */
    public LeftRecursionEliminator(List<String> order, AuxiliaryNameRegistry registry) {
        this.order = new ArrayList<>(order);
        this.registry = registry;
    }

    /**
     * Elimina la recursividad izquierda sin mutar la gramática recibida.
     *
     * @param grammar gramática de entrada (salida del paso 9)
     * @return un {@link TransformationStep} por variable que cambió, o uno solo
     *         sin cambios
     */
    public List<TransformationStep> eliminate(Grammar grammar) {
        Grammar current = grammar.copy();
        List<TransformationStep> steps = new ArrayList<>();
        this.createdVariables = new ArrayList<>();
        int total = order.size();

        for (int i = 0; i < order.size(); i++) {
            String variable = order.get(i);
            Grammar stepBefore = current.copy();

            List<Production> productions = new ArrayList<>(current.getProductions());
            Set<String> variables = new LinkedHashSet<>(current.getVariables());
            List<String> removed = new ArrayList<>();
            List<String> added = new ArrayList<>();
            List<String> details = new ArrayList<>();

            boolean changed = substituteEarlier(productions, variable, i, removed, added, details);
            changed |= eliminateImmediateRecursion(
                    productions, variables, variable, removed, added, details);

            if (changed) {
                current.setProductions(productions);
                current.setVariables(variables);
                details.add(0, "Procesando " + variable + " (variable " + (i + 1)
                        + " de " + total + ").");
                steps.add(new TransformationStep(
                        "Eliminación de recursividad izquierda — " + variable,
                        "Se sustituyen las producciones y se elimina la recursividad "
                        + "inmediata de " + variable + ".",
                        stepBefore,
                        current.copy(),
                        removed,
                        added,
                        details,
                        MENU_NUMBER,
                        null));
            }
        }

        if (steps.isEmpty()) {
            Grammar snapshot = grammar.copy();
            steps.add(new TransformationStep(
                    "Eliminación de recursividad izquierda",
                    NO_CHANGES,
                    snapshot,
                    snapshot,
                    List.of(),
                    List.of(),
                    List.of(NO_CHANGES),
                    MENU_NUMBER,
                    null));
        }

        return steps;
    }

    /**
     * @return las variables {@code Xi.1} creadas en la última llamada a
     *         {@link #eliminate(Grammar)}, en orden de creación
     */
    public List<String> getCreatedVariables() {
        return List.copyOf(createdVariables);
    }

    /**
     * (a) Sustituye las producciones {@code Xi → Xj α} con {@code j < i} por una
     * por cada producción de {@code Xj}, en la posición de la producción original.
     */
    private boolean substituteEarlier(List<Production> productions, String variable, int index,
                                      List<String> removed, List<String> added,
                                      List<String> details) {
        boolean changed = false;
        boolean substituted = true;
        while (substituted) {
            substituted = false;
            for (int p = 0; p < productions.size(); p++) {
                Production production = productions.get(p);
                if (!production.getLeftSide().equals(variable)) {
                    continue;
                }
                List<String> rightSide = production.getRightSide();
                if (rightSide.isEmpty()) {
                    continue;
                }
                String first = rightSide.get(0);
                int j = order.indexOf(first);
                if (j < 0 || j >= index) {
                    continue;
                }

                changed = true;
                substituted = true;
                addUnique(removed, production.toString());
                details.add("Reemplazando " + first + " en " + variable + ": "
                        + render(production) + ".");

                List<Production> targetProductions = productionsWithLeft(productions, first);
                productions.remove(p);
                int insertAt = p;
                List<String> suffix = rightSide.subList(1, rightSide.size());
                for (Production target : targetProductions) {
                    List<String> newRightSide = new ArrayList<>(target.getRightSide());
                    newRightSide.addAll(suffix);
                    Production replacement = new Production(variable, newRightSide);
                    if (!productions.contains(replacement)) {
                        productions.add(insertAt, replacement);
                        insertAt++;
                        addUnique(added, replacement.toString());
                    }
                }
                break;
            }
        }
        return changed;
    }

    /**
     * (b) Reemplaza la recursividad inmediata de {@code Xi} por las producciones
     * base y sus versiones con la auxiliar {@code Xi.1}.
     */
    private boolean eliminateImmediateRecursion(List<Production> productions, Set<String> variables,
                                                String variable, List<String> removed,
                                                List<String> added, List<String> details) {
        List<Production> recursive = new ArrayList<>();
        List<Production> nonRecursive = new ArrayList<>();
        for (Production production : productions) {
            if (!production.getLeftSide().equals(variable)) {
                continue;
            }
            List<String> rightSide = production.getRightSide();
            if (!rightSide.isEmpty() && rightSide.get(0).equals(variable)) {
                recursive.add(production);
            } else {
                nonRecursive.add(production);
            }
        }

        if (recursive.isEmpty() || nonRecursive.isEmpty()) {
            return false;
        }

        String auxiliary = registry.recursionVariable(variable);
        createdVariables.add(auxiliary);
        variables.add(auxiliary);

        List<Production> newVariableProductions = new ArrayList<>();
        for (Production beta : nonRecursive) {
            newVariableProductions.add(new Production(variable, new ArrayList<>(beta.getRightSide())));
        }
        for (Production beta : nonRecursive) {
            List<String> rightSide = new ArrayList<>(beta.getRightSide());
            rightSide.add(auxiliary);
            newVariableProductions.add(new Production(variable, rightSide));
        }

        List<Production> auxiliaryProductions = new ArrayList<>();
        for (Production alpha : recursive) {
            auxiliaryProductions.add(new Production(auxiliary, suffixFromOne(alpha)));
        }
        for (Production alpha : recursive) {
            List<String> rightSide = suffixFromOne(alpha);
            rightSide.add(auxiliary);
            auxiliaryProductions.add(new Production(auxiliary, rightSide));
        }

        List<Production> oldProductions = new ArrayList<>();
        for (Production production : productions) {
            if (production.getLeftSide().equals(variable)) {
                oldProductions.add(production);
            }
        }
        for (Production production : oldProductions) {
            addUnique(removed, production.toString());
        }

        int insertAt = productions.indexOf(oldProductions.get(0));
        productions.removeIf(p -> p.getLeftSide().equals(variable));
        if (insertAt < 0 || insertAt > productions.size()) {
            insertAt = productions.size();
        }
        for (Production production : newVariableProductions) {
            if (!productions.contains(production)) {
                productions.add(insertAt, production);
                insertAt++;
                addUnique(added, production.toString());
            }
        }
        for (Production production : auxiliaryProductions) {
            if (!productions.contains(production)) {
                productions.add(production);
                addUnique(added, production.toString());
            }
        }

        List<String> recursiveTexts = new ArrayList<>();
        for (Production production : recursive) {
            recursiveTexts.add(render(production));
        }
        details.add("Eliminando recursividad inmediata: "
                + String.join(", ", recursiveTexts)
                + ". Se crea la variable " + auxiliary + ".");

        return true;
    }

    private static List<String> suffixFromOne(Production production) {
        return new ArrayList<>(production.getRightSide().subList(1, production.getRightSide().size()));
    }

    private static List<Production> productionsWithLeft(List<Production> productions, String leftSide) {
        List<Production> result = new ArrayList<>();
        for (Production production : productions) {
            if (production.getLeftSide().equals(leftSide)) {
                result.add(production);
            }
        }
        return result;
    }

    private static void addUnique(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    private static String render(Production production) {
        String right = production.getRightSide().isEmpty()
                ? "ε"
                : String.join(" ", production.getRightSide());
        return production.getLeftSide() + " → " + right;
    }
}
