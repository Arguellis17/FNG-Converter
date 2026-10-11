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
 * Paso 8 del pipeline FNG: convierte a la FNC estándar los terminales que
 * aparecen dentro de producciones de exactamente 2 símbolos.
 * <p>
 * En cada producción de 2 símbolos, todo terminal (en cualquier posición) se
 * reemplaza por su variable auxiliar {@code Tt} dada por
 * {@link AuxiliaryNameRegistry#terminalVariable(String)}, y se agrega la
 * producción {@code Tt → t} una sola vez por terminal, declarando {@code Tt} en
 * el conjunto de variables resultante. Las producciones de un solo terminal no
 * se tocan. Al terminar, toda producción es {@code A → BC} o {@code A → a}.
 */
public class TerminalToVariableConverter {

    private static final int MENU_NUMBER = 8;
    private static final String STEP_NAME = "Terminales a variables (FNC estándar)";
    private static final String FINAL_VALIDATION =
            "Validación FNC estándar: toda producción es A → BC o A → a.";
    private static final String NO_CHANGES =
            "No se han identificado terminales dentro de producciones de 2 símbolos, "
            + "no hay cambios en la gramática. Se pasa al siguiente paso.";

    private final AuxiliaryNameRegistry registry;

    /**
     * Crea el conversor con el registro de nombres auxiliares indicado.
     *
     * @param registry registro que entrega los nombres {@code Tt}
     */
    public TerminalToVariableConverter(AuxiliaryNameRegistry registry) {
        this.registry = registry;
    }

    /**
     * Convierte los terminales dentro de producciones de 2 símbolos sin mutar la
     * gramática recibida.
     *
     * @param grammar gramática de entrada (salida del paso 7)
     * @return el {@link TransformationStep} del paso 8
     */
    public TransformationStep convert(Grammar grammar) {
        Grammar before = grammar.copy();

        List<Production> newProductions = new ArrayList<>();
        List<String> productionsRemoved = new ArrayList<>();
        List<String> productionsAdded = new ArrayList<>();
        Map<String, String> createdTerminalVariables = new LinkedHashMap<>();

        for (Production production : grammar.getProductions()) {
            List<String> rightSide = production.getRightSide();
            if (rightSide.size() != 2) {
                newProductions.add(production);
                continue;
            }

            List<String> newRightSide = new ArrayList<>();
            boolean changed = false;
            for (String symbol : rightSide) {
                if (grammar.getTerminals().contains(symbol)) {
                    String variable = createdTerminalVariables.computeIfAbsent(
                            symbol, registry::terminalVariable);
                    newRightSide.add(variable);
                    changed = true;
                } else {
                    newRightSide.add(symbol);
                }
            }

            if (changed) {
                productionsRemoved.add(production.toString());
                Production replaced = new Production(production.getLeftSide(), newRightSide);
                newProductions.add(replaced);
                productionsAdded.add(replaced.toString());
            } else {
                newProductions.add(production);
            }
        }

        for (Map.Entry<String, String> entry : createdTerminalVariables.entrySet()) {
            Production terminalProduction = new Production(
                    entry.getValue(), List.of(entry.getKey()));
            if (!newProductions.contains(terminalProduction)) {
                newProductions.add(terminalProduction);
                productionsAdded.add(terminalProduction.toString());
            }
        }

        Grammar after = grammar.copy();
        after.setProductions(newProductions);
        Set<String> newVariables = new LinkedHashSet<>(grammar.getVariables());
        newVariables.addAll(createdTerminalVariables.values());
        after.setVariables(newVariables);

        String message = describeChanges(createdTerminalVariables);
        List<String> details = new ArrayList<>();
        details.add(message);
        details.add(FINAL_VALIDATION);

        return new TransformationStep(
                STEP_NAME,
                message,
                before,
                after,
                productionsRemoved,
                productionsAdded,
                details,
                MENU_NUMBER,
                null);
    }

    private String describeChanges(Map<String, String> createdTerminalVariables) {
        if (createdTerminalVariables.isEmpty()) {
            return NO_CHANGES;
        }

        List<String> terminals = new ArrayList<>(createdTerminalVariables.keySet());
        List<String> mappings = new ArrayList<>();
        for (Map.Entry<String, String> entry : createdTerminalVariables.entrySet()) {
            mappings.add(entry.getValue() + "→" + entry.getKey());
        }

        if (terminals.size() == 1) {
            return "Se identificó un terminal dentro de una producción de 2 símbolos: "
                    + terminals.get(0) + ". Se crea la variable " + mappings.get(0) + ".";
        }
        return "Se identificaron terminales dentro de producciones de 2 símbolos: "
                + String.join(", ", terminals) + ". Se crean las variables "
                + String.join(", ", mappings) + ".";
    }
}
