package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Paso 11 del pipeline FNG: sustitución hacia atrás.
 * <p>
 * Se recorren las variables del paso 9 de la última a la primera y, después, las
 * auxiliares {@code V.1} en su orden de creación. Para cada variable, <b>mientras</b>
 * alguna de sus producciones empiece con una variable {@code Y}, se reemplaza esa
 * producción por una por cada producción de {@code Y} (en la posición de la original
 * y sin duplicados). Al terminar, toda producción de la variable empieza con un
 * terminal.
 * <p>
 * Si por alguna razón una variable no logra completar la sustitución tras más
 * repeticiones que {@code variables + 1} (posible recursividad izquierda que el paso
 * 10 no pudo eliminar), se detiene esa variable, se registra el aviso
 * {@code "✘ No se pudo completar la sustitución en V: ..."} y se continúa con las
 * demás; nunca se lanza excepción.
 * <p>
 * Se emite un {@link TransformationStep} por cada variable que cambió; las que no
 * cambian no generan paso. Si ninguna cambió, se emite uno solo con el mensaje de
 * "sin cambios". Los {@code productionsRemoved} y {@code productionsAdded} son el
 * delta neto entre {@code grammarBefore} y {@code grammarAfter}. La gramática
 * recibida nunca se muta.
 */
public class BackwardSubstitutor {

    private static final int MENU_NUMBER = 11;
    private static final String NO_CHANGES =
            "Ninguna producción empieza con variable, no hay cambios en la gramática. "
            + "Se pasa al siguiente paso.";
    private static final String GUARD_MESSAGE_SUFFIX =
            ": posible recursividad izquierda sin eliminar.";

    private final List<String> order;
    private final List<String> auxiliaryVariables;

    /**
     * Crea el sustitutor con el orden de variables del paso 9 y las auxiliares
     * creadas en el paso 10.
     *
     * @param order              orden de variables del paso 9
     * @param auxiliaryVariables variables {@code V.1} creadas en el paso 10, en su
     *                           orden de creación
     */
    public BackwardSubstitutor(List<String> order, List<String> auxiliaryVariables) {
        this.order = new ArrayList<>(order);
        this.auxiliaryVariables = new ArrayList<>(auxiliaryVariables);
    }

    /**
     * Sustituye hacia atrás sin mutar la gramática recibida.
     *
     * @param grammar gramática de entrada (salida del paso 10)
     * @return un {@link TransformationStep} por variable que cambió, o uno solo
     *         sin cambios
     */
    public List<TransformationStep> substitute(Grammar grammar) {
        Grammar current = grammar.copy();
        List<TransformationStep> steps = new ArrayList<>();
        List<String> processOrder = new ArrayList<>();
        for (int i = order.size() - 1; i >= 0; i--) {
            processOrder.add(order.get(i));
        }
        processOrder.addAll(auxiliaryVariables);

        int guardLimit = current.getVariables().size() + 1;

        for (String variable : processOrder) {
            if (!current.getVariables().contains(variable)) {
                continue;
            }
            Grammar stepBefore = current.copy();
            Set<String> variables = new LinkedHashSet<>(current.getVariables());
            List<Production> productions = new ArrayList<>(current.getProductions());
            List<String> details = new ArrayList<>();

            int rounds = 0;
            while (hasVariableStart(productions, variable, variables)) {
                if (rounds >= guardLimit) {
                    details.add("✘ No se pudo completar la sustitución en "
                            + variable + GUARD_MESSAGE_SUFFIX);
                    break;
                }
                substituteRound(productions, variable, variables, details);
                rounds++;
            }

            if (details.isEmpty()) {
                continue;
            }

            List<String> beforeStrings = productionStrings(current.getProductions());
            List<String> afterStrings = productionStrings(productions);
            List<String> removed = difference(beforeStrings, afterStrings);
            List<String> added = difference(afterStrings, beforeStrings);

            current.setProductions(productions);
            steps.add(new TransformationStep(
                    "Sustitución hacia atrás — " + variable,
                    "Se reemplazan las producciones que empiezan con una variable por "
                    + "las producciones de esa variable, hasta que empiezan con un "
                    + "terminal.",
                    stepBefore,
                    current.copy(),
                    removed,
                    added,
                    details,
                    MENU_NUMBER,
                    null));
        }

        if (steps.isEmpty()) {
            Grammar snapshot = grammar.copy();
            steps.add(new TransformationStep(
                    "Sustitución hacia atrás",
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
     * Aplica una repetición ({@code mientras}): recorre todas las producciones de
     * la variable y reemplaza, en su posición, cada una que empiece con una
     * variable {@code Y} por una por cada producción de {@code Y} (sin duplicados).
     * Agrega un detalle por cada {@code Y}, con todas las producciones reemplazadas
     * de esa ronda separadas por comas.
     */
    private void substituteRound(List<Production> productions, String variable,
                                 Set<String> variables, List<String> details) {
        Set<String> kept = new LinkedHashSet<>();
        for (Production production : productions) {
            if (!isReplaceable(production, variable, variables)) {
                kept.add(production.toString());
            }
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        Set<String> addedKeys = new LinkedHashSet<>();
        List<Production> rebuilt = new ArrayList<>();

        for (Production production : productions) {
            if (!isReplaceable(production, variable, variables)) {
                rebuilt.add(production);
                continue;
            }
            String head = production.getRightSide().get(0);
            grouped.computeIfAbsent(head, key -> new ArrayList<>()).add(render(production));

            List<String> suffix = new ArrayList<>(
                    production.getRightSide().subList(1, production.getRightSide().size()));
            for (Production target : productionsWithLeft(productions, head)) {
                List<String> newRightSide = new ArrayList<>(target.getRightSide());
                newRightSide.addAll(suffix);
                Production replacement = new Production(variable, newRightSide);
                String key = replacement.toString();
                if (!kept.contains(key) && addedKeys.add(key)) {
                    rebuilt.add(replacement);
                }
            }
        }

        productions.clear();
        productions.addAll(rebuilt);

        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            details.add("Sustituyendo " + entry.getKey() + " en " + variable + ": "
                    + String.join(", ", entry.getValue()) + ".");
        }
    }

    private static boolean hasVariableStart(List<Production> productions, String variable,
                                            Set<String> variables) {
        for (Production production : productions) {
            if (isReplaceable(production, variable, variables)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReplaceable(Production production, String variable,
                                         Set<String> variables) {
        if (!production.getLeftSide().equals(variable)) {
            return false;
        }
        List<String> rightSide = production.getRightSide();
        if (rightSide.isEmpty()) {
            return false;
        }
        return variables.contains(rightSide.get(0));
    }

    private static List<Production> productionsWithLeft(List<Production> productions,
                                                        String leftSide) {
        List<Production> result = new ArrayList<>();
        for (Production production : productions) {
            if (production.getLeftSide().equals(leftSide)) {
                result.add(production);
            }
        }
        return result;
    }

    /**
     * Devuelve los elementos de {@code from} que no están en {@code other}, en su
     * orden y sin repetidos.
     */
    private static List<String> difference(List<String> from, List<String> other) {
        List<String> result = new ArrayList<>();
        for (String value : from) {
            if (!other.contains(value) && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static List<String> productionStrings(List<Production> productions) {
        List<String> result = new ArrayList<>();
        for (Production production : productions) {
            result.add(production.toString());
        }
        return result;
    }

    private static String render(Production production) {
        String right = production.getRightSide().isEmpty()
                ? "ε"
                : String.join(" ", production.getRightSide());
        return production.getLeftSide() + " → " + right;
    }
}
