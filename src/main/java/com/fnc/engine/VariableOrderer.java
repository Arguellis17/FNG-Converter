package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Paso 9 del pipeline FNG: enumera las variables de la gramática.
 * <p>
 * El orden es: (1) {@code S} es la variable 1; (2) las demás variables (las
 * {@code Xn} incluidas, sin las {@code Tn}) por niveles desde {@code S}: se toma
 * una variable, se leen sus producciones de izquierda a derecha y cada variable
 * nueva que aparece se agrega al final de la lista; (3) las {@code Tn} al final,
 * en el orden en que se crearon.
 * <p>
 * Como defensa, si alguna variable declarada no quedara cubierta por el recorrido
 * ni fuera una {@code Tn}, se agrega al final en el orden de declaración. Este paso
 * no modifica la gramática: {@code grammarBefore} y {@code grammarAfter} son la
 * misma, y el paso no reporta cambios.
 */
public class VariableOrderer {

    private static final int MENU_NUMBER = 9;
    private static final String STEP_NAME = "Ordenamiento de variables";

    private final List<String> terminalVariables;
    private List<String> order = new ArrayList<>();

    /**
     * Crea el ordenador con las variables de terminales creadas en el paso 8,
     * en orden de creación.
     *
     * @param terminalVariables nombres {@code Tt}, en orden de creación
     */
    public VariableOrderer(Collection<String> terminalVariables) {
        this.terminalVariables = new ArrayList<>(terminalVariables);
    }

    /**
     * Enumera las variables de la gramática sin mutarla.
     *
     * @param grammar gramática de entrada (salida del paso 8)
     * @return el {@link TransformationStep} del paso 9
     */
    public TransformationStep order(Grammar grammar) {
        Grammar snapshot = grammar.copy();
        this.order = computeOrder(grammar);

        List<String> details = new ArrayList<>();
        details.add(buildMessage(order));

        return new TransformationStep(
                STEP_NAME,
                "Se enumeran las variables: S primero, luego las demás por niveles "
                + "desde S y al final las variables de terminales.",
                snapshot,
                snapshot,
                List.of(),
                List.of(),
                details,
                MENU_NUMBER,
                null);
    }

    /**
     * @return el orden de las variables calculado en la última llamada a
     *         {@link #order(Grammar)}
     */
    public List<String> getOrder() {
        return List.copyOf(order);
    }

    private List<String> computeOrder(Grammar grammar) {
        Set<String> terminalVars = new LinkedHashSet<>(terminalVariables);
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        String start = grammar.getStartSymbol();
        if (start != null && grammar.getVariables().contains(start)) {
            result.add(start);
            seen.add(start);
            queue.add(start);
        }

        while (!queue.isEmpty()) {
            String variable = queue.poll();
            for (Production production : grammar.getProductions()) {
                if (!production.getLeftSide().equals(variable)) {
                    continue;
                }
                for (String symbol : production.getRightSide()) {
                    if (!grammar.getVariables().contains(symbol)) {
                        continue;
                    }
                    if (terminalVars.contains(symbol)) {
                        continue;
                    }
                    if (seen.add(symbol)) {
                        result.add(symbol);
                        queue.add(symbol);
                    }
                }
            }
        }

        for (String terminalVariable : terminalVariables) {
            if (seen.add(terminalVariable)) {
                result.add(terminalVariable);
            }
        }

        for (String variable : grammar.getVariables()) {
            if (seen.add(variable)) {
                result.add(variable);
            }
        }

        return result;
    }

    private String buildMessage(List<String> variables) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < variables.size(); i++) {
            parts.add((i + 1) + ". " + variables.get(i));
        }
        return "Orden de las variables: " + String.join(", ", parts) + ".";
    }
}
