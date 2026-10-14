package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.GrammarFormatter;
import com.fnc.model.Production;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Formato de salida del paso 13 (FNG).
 * <p>
 * Reutiliza el estilo y los métodos públicos de {@link GrammarFormatter} sin
 * modificarlos: la cabecera es la de {@link GrammarFormatter#formatHeader} y las
 * producciones usan el formato de
 * {@link GrammarFormatter#formatProductionLines}, pero agrupadas en un orden
 * explícito: primero las variables de {@code order} que existan en la gramática,
 * después las de {@code recursionVariables} que existan, y al final cualquier
 * otra variable en su orden de declaración.
 * <p>
 * Tras las producciones se agrega una línea por cada familia auxiliar que tenga
 * elementos presentes en la gramática (si no hay ninguno, la línea se omite):
 * <pre>
 * Variables auxiliares de terminales: T1.
 * Variables auxiliares de recursividad izquierda: A.1, B.1.
 * </pre>
 */
public final class GreibachFormatter {

    private static final String TERMINAL_AUX_LABEL = "Variables auxiliares de terminales: ";
    private static final String RECURSION_AUX_LABEL =
            "Variables auxiliares de recursividad izquierda: ";

    private GreibachFormatter() {
    }

    /**
     * Arma el texto final de la FNG.
     *
     * @param grammar            gramática final (paso 13)
     * @param order              orden de variables del paso 9
     * @param terminalVariables  variables auxiliares {@code Tt} del paso 8
     * @param recursionVariables variables auxiliares {@code V.1} del paso 10
     * @return el texto de salida, sin espacios sobrantes al final
     */
    public static String formatFinalResult(Grammar grammar, List<String> order,
                                           List<String> terminalVariables,
                                           List<String> recursionVariables) {
        if (grammar == null) {
            return "(sin gramática)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(GrammarFormatter.formatHeader(grammar)).append("\n");
        sb.append("Sigma=\n");
        for (String line : productionLinesInOrder(grammar, order, recursionVariables)) {
            sb.append("  ").append(line).append("\n");
        }
        appendAuxiliaryLine(sb, TERMINAL_AUX_LABEL, terminalVariables, grammar);
        appendAuxiliaryLine(sb, RECURSION_AUX_LABEL, recursionVariables, grammar);
        return sb.toString().trim();
    }

    /**
     * Producciones agrupadas por variable, en el orden pedido, reutilizando
     * {@link GrammarFormatter#formatProductionLines} sobre una copia con las
     * variables ya ordenadas.
     */
    private static List<String> productionLinesInOrder(Grammar grammar, List<String> order,
                                                       List<String> recursionVariables) {
        Set<String> present = grammar.getVariables();
        Set<String> orderedVariables = new LinkedHashSet<>();
        for (String variable : order) {
            if (present.contains(variable)) {
                orderedVariables.add(variable);
            }
        }
        for (String variable : recursionVariables) {
            if (present.contains(variable)) {
                orderedVariables.add(variable);
            }
        }
        for (String variable : grammar.getVariables()) {
            orderedVariables.add(variable);
        }
        for (Production production : grammar.getProductions()) {
            orderedVariables.add(production.getLeftSide());
        }

        Grammar reordered = new Grammar(
                orderedVariables,
                new LinkedHashSet<>(grammar.getTerminals()),
                new ArrayList<>(grammar.getProductions()),
                grammar.getStartSymbol());
        return GrammarFormatter.formatProductionLines(reordered);
    }

    private static void appendAuxiliaryLine(StringBuilder sb, String label, List<String> names,
                                            Grammar grammar) {
        List<String> present = new ArrayList<>();
        for (String name : names) {
            if (grammar.getVariables().contains(name)) {
                present.add(name);
            }
        }
        if (present.isEmpty()) {
            return;
        }
        sb.append("\n").append(label).append(String.join(", ", present)).append(".\n");
    }
}
