package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.GrammarFormatter;
import com.fnc.model.TransformationStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs the full normalization pipeline from the course file
 * (Entrenamiento Algoritmo), returning one step per side-menu entry:
 *
 * 0 Definición de gramática · 1 Variables inútiles · 2 Variables
 * inalcanzables · 3 Unitarias · 4 Producciones nulas · 5 FNC ·
 * 6 Organización gramática.
 *
 * Approved behaviors:
 * - Undefined symbols are auto-declared first (uppercase → variable),
 *   reported in step 0 (rule 2 of the file).
 * - S→ε is preserved by the null step with a notice (case c).
 * - If the start symbol is useless (empty language), the process stops
 *   after step 1 with a notice (case d).
 * - Inside step 4, after the nullables: second unit pass (case a),
 *   then useless, then unreachable (case b) — each only if it reports
 *   changes, with its usual message.
 *
 * Everything is deterministic code; no language models involved.
 */
public class GrammarService {

    private static final String STOPPED_SUFFIX =
            " (proceso detenido: lenguaje vacío).";

    /**
     * Run the complete pipeline on a copy of the given grammar.
     */
    public List<TransformationStep> convertFull(Grammar input) {
        List<TransformationStep> steps = new ArrayList<>();
        Grammar working = input.copy();

        // Step 0: definition (auto-declare undefined symbols first).
        Map<String, String> autoDeclared = working.autoDeclareMissingSymbols();
        List<String> step0Details = new ArrayList<>();
        for (Map.Entry<String, String> entry : autoDeclared.entrySet()) {
            step0Details.add("Info: símbolo '" + entry.getKey() + "' no declarado: "
                    + "se asumió como " + entry.getValue() + ".");
        }
        steps.add(new TransformationStep(
                "Definición de gramática",
                "Gramática ingresada por el usuario.",
                working.copy(), working.copy(),
                List.of(), List.of(), step0Details, 0, null));

        // Step 1: useless.
        UselessVariableEliminator useless = new UselessVariableEliminator();
        TransformationStep uselessStep = withMenu(useless.eliminate(working), 1);
        steps.add(uselessStep);
        working = uselessStep.getGrammarAfter();

        // Case (d): useless start symbol = empty language → stop here.
        if (!useless.getGeneratingVariables().contains(working.getStartSymbol())) {
            List<String> stopDetails = List.of(
                    "El lenguaje generado es vacío: " + working.getStartSymbol()
                    + " no genera cadenas. Se detiene el proceso.");
            steps.add(stoppedStep(2, "Eliminación de variables inalcanzables", working, stopDetails));
            steps.add(stoppedStep(3, "Eliminación de unitarias", working, stopDetails));
            steps.add(stoppedStep(4, "Eliminación de producciones nulas (vacío)", working, stopDetails));
            steps.add(stoppedStep(5, "Aplicando Forma Normal de Chomsky", working, stopDetails));
            steps.add(organizationStep(working, List.of(), stopDetails));
            return steps;
        }

        // Step 2: unreachable.
        UnreachableVariableEliminator unreachable = new UnreachableVariableEliminator();
        TransformationStep unreachableStep = withMenu(unreachable.eliminate(working), 2);
        steps.add(unreachableStep);
        working = unreachableStep.getGrammarAfter();

        // Step 3: units.
        UnitProductionEliminator units = new UnitProductionEliminator();
        TransformationStep unitStep = withMenu(units.eliminate(working), 3);
        steps.add(unitStep);
        working = unitStep.getGrammarAfter();

        // Step 4: nulls + approved cleanups (a, b).
        working = runNullStepWithCleanups(working, steps);

        // Step 5: CNF.
        ChomskyNormalFormConverter cnf = new ChomskyNormalFormConverter();
        TransformationStep cnfStep = withMenu(cnf.convert(working), 5);
        steps.add(cnfStep);
        working = cnfStep.getGrammarAfter();

        // Step 6: organization (final layout, section 5 style).
        steps.add(organizationStep(working, cnf.getCreatedVariables(), List.of()));

        return steps;
    }

    /**
     * Step 4: null elimination, then — only if each reports changes —
     * new units (a), useless (b), unreachable (b), merged into this step.
     */
    private Grammar runNullStepWithCleanups(Grammar grammar, List<TransformationStep> steps) {
        NullProductionEliminator nulls = new NullProductionEliminator();
        TransformationStep nullStep = nulls.eliminate(grammar);
        Grammar current = nullStep.getGrammarAfter();

        List<String> removed = new ArrayList<>(nullStep.getProductionsRemoved());
        List<String> added = new ArrayList<>(nullStep.getProductionsAdded());
        List<String> details = new ArrayList<>(nullStep.getDetails());

        // (a) Second unit pass for units created by null elimination.
        UnitProductionEliminator unitsAgain = new UnitProductionEliminator();
        TransformationStep unitsStep = unitsAgain.eliminate(current);
        if (unitsStep.hasChanges()) {
            current = unitsStep.getGrammarAfter();
            removed.addAll(unitsStep.getProductionsRemoved());
            added.addAll(unitsStep.getProductionsAdded());
            details.add(unitsStep.getDescription());
            details.addAll(unitsStep.getDetails());
        }

        // (b2) Useless re-check (e.g. left without productions).
        UselessVariableEliminator uselessAgain = new UselessVariableEliminator();
        TransformationStep uselessStep = uselessAgain.eliminate(current);
        if (uselessStep.hasChanges()) {
            current = uselessStep.getGrammarAfter();
            removed.addAll(uselessStep.getProductionsRemoved());
            added.addAll(uselessStep.getProductionsAdded());
            details.add(uselessStep.getDescription());
            details.addAll(uselessStep.getDetails());
        }

        // (b3) Unreachable re-check.
        UnreachableVariableEliminator unreachableAgain = new UnreachableVariableEliminator();
        TransformationStep unreachableStep = unreachableAgain.eliminate(current);
        if (unreachableStep.hasChanges()) {
            current = unreachableStep.getGrammarAfter();
            removed.addAll(unreachableStep.getProductionsRemoved());
            added.addAll(unreachableStep.getProductionsAdded());
            details.add(unreachableStep.getDescription());
            details.addAll(unreachableStep.getDetails());
        }

        steps.add(new TransformationStep(
                nullStep.getStepName(), nullStep.getDescription(),
                nullStep.getGrammarBefore(), current,
                removed, added, details, 4, null));
        return current;
    }

    private TransformationStep withMenu(TransformationStep step, int menuNumber) {
        return new TransformationStep(
                step.getStepName(), step.getDescription(),
                step.getGrammarBefore(), step.getGrammarAfter(),
                step.getProductionsRemoved(), step.getProductionsAdded(),
                step.getDetails(), menuNumber, step.getCustomBody());
    }

    private TransformationStep stoppedStep(int menuNumber, String name,
                                           Grammar grammar, List<String> stopDetails) {
        return new TransformationStep(
                name,
                "Sin cambios en la gramática" + STOPPED_SUFFIX,
                grammar.copy(), grammar.copy(),
                List.of(), List.of(), stopDetails, menuNumber, null);
    }

    private TransformationStep organizationStep(Grammar grammar,
                                                List<String> createdXn,
                                                List<String> extraDetails) {
        List<String> details = new ArrayList<>(extraDetails);
        return new TransformationStep(
                "Organización gramática",
                "Gramática final completa en Forma Normal de Chomsky.",
                grammar.copy(), grammar.copy(),
                List.of(), List.of(), details, 6,
                GrammarFormatter.formatFinalResult(grammar, createdXn));
    }

    /** Expose the final-layout formatter for single-step mode reuse. */
    public static String finalLayout(Grammar grammar, List<String> createdXn) {
        return GrammarFormatter.formatFinalResult(grammar, createdXn);
    }
}
