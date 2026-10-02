package com.fnc.model;

import java.util.List;

/**
 * Represents a single transformation step in the grammar conversion process.
 * Stores the grammar before and after the transformation, along with
 * a description of what was done.
 */
public class TransformationStep {
    private final String stepName;
    private final String description;
    private final Grammar grammarBefore;
    private final Grammar grammarAfter;
    private final List<String> productionsRemoved;
    private final List<String> productionsAdded;
    /**
     * Líneas de detalle elemento por elemento (ej: "Se elimina la
     * variable inútil: F", "Eliminando vacío: B→ε"). Si el paso no
     * encontró nada que transformar, contiene el aviso correspondiente.
     * Pueden ser bloques de varias líneas (ej: un Pn intermedio).
     */
    private final List<String> details;
    /**
     * Número en el menú lateral del archivo (0 a 6). -1 = sin número fijo
     * (modo paso a paso: el panel numera por posición).
     */
    private final int menuNumber;
    /**
     * Cuerpo personalizado para el área de texto. Si está presente, el panel
     * lo muestra en lugar de los bloques Sigma antes/después
     * (ej: paso 6 Organización gramática).
     */
    private final String customBody;

    public TransformationStep(String stepName, String description,
                              Grammar grammarBefore, Grammar grammarAfter,
                              List<String> productionsRemoved,
                              List<String> productionsAdded) {
        this(stepName, description, grammarBefore, grammarAfter,
                productionsRemoved, productionsAdded, List.of(), -1, null);
    }

    public TransformationStep(String stepName, String description,
                              Grammar grammarBefore, Grammar grammarAfter,
                              List<String> productionsRemoved,
                              List<String> productionsAdded,
                              List<String> details) {
        this(stepName, description, grammarBefore, grammarAfter,
                productionsRemoved, productionsAdded, details, -1, null);
    }

    public TransformationStep(String stepName, String description,
                              Grammar grammarBefore, Grammar grammarAfter,
                              List<String> productionsRemoved,
                              List<String> productionsAdded,
                              List<String> details,
                              int menuNumber,
                              String customBody) {
        this.stepName = stepName;
        this.description = description;
        this.grammarBefore = grammarBefore;
        this.grammarAfter = grammarAfter;
        this.productionsRemoved = productionsRemoved;
        this.productionsAdded = productionsAdded;
        this.details = details == null ? List.of() : List.copyOf(details);
        this.menuNumber = menuNumber;
        this.customBody = customBody;
    }

    // Getters
    public String getStepName() {
        return stepName;
    }

    public String getDescription() {
        return description;
    }

    public Grammar getGrammarBefore() {
        return grammarBefore;
    }

    public Grammar getGrammarAfter() {
        return grammarAfter;
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

    public int getMenuNumber() {
        return menuNumber;
    }

    public String getCustomBody() {
        return customBody;
    }

    /**
     * Indica si el paso realmente transformó algo.
     */
    public boolean hasChanges() {
        return !productionsRemoved.isEmpty() || !productionsAdded.isEmpty();
    }

    /**
     * Format the step as a readable string for display.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(stepName).append(" ===\n");
        sb.append(description).append("\n\n");

        if (!productionsRemoved.isEmpty()) {
            sb.append("Producciones eliminadas:\n");
            productionsRemoved.forEach(p -> sb.append("  - ").append(p).append("\n"));
        }

        if (!productionsAdded.isEmpty()) {
            sb.append("Producciones agregadas:\n");
            productionsAdded.forEach(p -> sb.append("  + ").append(p).append("\n"));
        }

        return sb.toString();
    }
}
