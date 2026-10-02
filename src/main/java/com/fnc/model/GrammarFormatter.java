package com.fnc.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Formatea gramáticas en la notación formal usada en clase:
 *
 * <pre>
 * G={{A, B}, {a, b}, S, Sigma}
 * Sigma=
 *   S -> A B | B
 *   A -> a
 * </pre>
 *
 * Como los TextArea de JavaFX son texto plano (sin rich-text), el
 * "tachado" de producciones eliminadas se simula con el caracter
 * Unicode de tachado combinado U+0336 (ej: F&#x336;1&#x336;).
 */
public final class GrammarFormatter {

    /** Caracter combinado U+0336 que tacha el caracter anterior. */
    private static final char STRIKE = '̶';

    private GrammarFormatter() {
    }

    /**
     * Tacha un texto para texto plano (ej: "F1" -> "F&#x336;1&#x336;").
     */
    public static String strike(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i)).append(STRIKE);
        }
        return sb.toString();
    }

    /**
     * Cabecera formal: G={{V}, {T}, S, Sigma}.
     */
    public static String formatHeader(Grammar grammar) {
        if (grammar == null) {
            return "G={} (sin gramática)";
        }
        return "G={{" + String.join(", ", grammar.getVariables()) + "}, {"
                + String.join(", ", grammar.getTerminals()) + "}, "
                + grammar.getStartSymbol() + ", Sigma}";
    }

    /**
     * Producciones agrupadas por variable: "S -> A B | B".
     * Una alternativa vacía se muestra como "ε".
     */
    public static List<String> formatProductionLines(Grammar grammar) {
        List<String> lines = new ArrayList<>();
        if (grammar == null) {
            lines.add("(sin gramática)");
            return lines;
        }
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String v : grammar.getVariables()) {
            grouped.put(v, new ArrayList<>());
        }
        for (Production p : grammar.getProductions()) {
            grouped.computeIfAbsent(p.getLeftSide(), k -> new ArrayList<>())
                    .add(formatRightSide(p.getRightSide()));
        }
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            lines.add(entry.getKey() + " -> " + String.join(" | ", entry.getValue()));
        }
        if (lines.isEmpty()) {
            lines.add("(sin producciones)");
        }
        return lines;
    }

    private static String formatRightSide(List<String> rightSide) {
        if (rightSide == null || rightSide.isEmpty()) {
            return "ε";
        }
        return String.join(" ", rightSide);
    }

    /**
     * Gramática completa: cabecera + bloque Sigma.
     */
    public static String formatFull(Grammar grammar) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatHeader(grammar)).append("\n");
        sb.append("Sigma=\n");
        for (String line : formatProductionLines(grammar)) {
            sb.append("  ").append(line).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Bloque Sigma agrupado por variable, donde cada alternativa cuyo texto
     * ({@code Production.toString()}) esté en {@code struckProductionTexts}
     * aparece tachada dentro de su grupo. Ej:
     * <pre>
     *   S -> A B | B[tachada]
     * </pre>
     */
    public static String formatWithStruck(Grammar grammar, Set<String> struckProductionTexts) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatHeader(grammar)).append("\n");
        sb.append("Sigma=\n");
        if (grammar == null || grammar.getProductions().isEmpty()) {
            sb.append("  (sin producciones)");
            return sb.toString();
        }
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Production p : grammar.getProductions()) {
            String alt = formatRightSide(p.getRightSide());
            if (struckProductionTexts != null && struckProductionTexts.contains(p.toString())) {
                alt = strike(alt);
            }
            grouped.computeIfAbsent(p.getLeftSide(), k -> new ArrayList<>()).add(alt);
        }
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" -> ")
                    .append(String.join(" | ", entry.getValue())).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Bloque final estilo sección 5 del archivo (paso 6 Organización):
     * cabecera G = ({...}, {...}, S, P), producciones principales agrupadas
     * y tabla de Xn en filas de 5.
     * @param createdXn nombres de Xn en orden de creación.
     */
    public static String formatFinalResult(Grammar grammar, List<String> createdXn) {
        StringBuilder sb = new StringBuilder();
        if (grammar == null) {
            return "(sin gramática)";
        }
        sb.append("G = ({").append(String.join(",", grammar.getVariables())).append("}, {")
                .append(String.join(",", grammar.getTerminals())).append("}, ")
                .append(grammar.getStartSymbol()).append(", P)\n\n");
        java.util.Set<String> aux = createdXn == null
                ? java.util.Set.of() : new java.util.LinkedHashSet<>(createdXn);
        for (String line : formatProductionLines(grammar)) {
            String left = line.contains(" -> ") ? line.substring(0, line.indexOf(" -> ")) : line;
            if (!aux.contains(left)) {
                sb.append(line).append("\n");
            }
        }
        if (!aux.isEmpty()) {
            sb.append("\n");
            List<String> cells = new ArrayList<>();
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (Production p : grammar.getProductions()) {
                if (aux.contains(p.getLeftSide())) {
                    grouped.computeIfAbsent(p.getLeftSide(), k -> new ArrayList<>())
                            .add(formatRightSide(p.getRightSide()));
                }
            }
            for (String name : createdXn) {
                List<String> alts = grouped.getOrDefault(name, List.of());
                cells.add(name + "->" + String.join(" | ", alts));
            }
            for (int i = 0; i < cells.size(); i += 5) {
                List<String> row = cells.subList(i, Math.min(i + 5, cells.size()));
                int width = row.stream().mapToInt(String::length).max().orElse(0) + 4;
                for (String cell : row) {
                    sb.append(String.format("%-" + width + "s", cell));
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Agrupa textos de producciones individuales ("A -> X") por variable
     * ("A -> X | Y"). Sirve para mostrar listas de agregadas/eliminadas
     * en el formato Sigma que usa el curso.
     */
    public static List<String> groupProductionTexts(List<String> productionTexts) {
        List<String> lines = new ArrayList<>();
        if (productionTexts == null || productionTexts.isEmpty()) {
            return lines;
        }
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String text : productionTexts) {
            int arrow = text.indexOf(" -> ");
            if (arrow < 0) {
                grouped.computeIfAbsent(text, k -> new ArrayList<>());
                continue;
            }
            String left = text.substring(0, arrow);
            String right = text.substring(arrow + 4);
            grouped.computeIfAbsent(left, k -> new ArrayList<>()).add(right);
        }
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                lines.add(entry.getKey());
            } else {
                lines.add(entry.getKey() + " -> " + String.join(" | ", entry.getValue()));
            }
        }
        return lines;
    }
}
