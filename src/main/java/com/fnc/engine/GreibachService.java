package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.TransformationStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio del pipeline FNG completo: encadena el pipeline FNC existente
 * (pasos 0 a 6, {@link GrammarService}) con el motor FNG nuevo (pasos 7 a 13)
 * en una única {@code List<TransformationStep>}.
 * <p>
 * Flujo:
 * <pre>
 * input
 *   │
 *   ├─ GrammarService.convertFull(input) ── pasos 0..6 (sin cambios)
 *   │        └─ si terminó detenido por lenguaje vacío:
 *   │              pasos 7..13 "detenidos" (mismo patrón que GrammarService) → fin
 *   │
 *   ├─ FncComplianceChecker.check(paso 6) ── paso 7
 *   │        └─ si !isCompliant(): pasos 8..13 "detenidos" (no cumple la FNC) → fin
 *   │
 *   ├─ TerminalToVariableConverter(registry).convert ── paso 8
 *   ├─ VariableOrderer(terminales).order ────────────── paso 9
 *   ├─ LeftRecursionEliminator(order, registry).eliminate ── pasos 10 (0..n)
 *   ├─ BackwardSubstitutor(order, V.1).substitute ──── pasos 11 (0..n)
 *   ├─ UnreachableVariableEliminator.eliminate ─────── paso 12
 *   └─ GreibachFinalizer.validateAndOrganize ───────── paso 13
 * </pre>
 * <p>
 * No muta la gramática de entrada (cada componente trabaja sobre copias) y es
 * determinista (RNF10).
 */
public class GreibachService {

    /** Sufijo idéntico al de {@link GrammarService} para los pasos detenidos. */
    private static final String STOPPED_SUFFIX =
            " (proceso detenido: lenguaje vacío).";
    /** Sufijo propio para cuando la gramática no cumple la FNC del curso. */
    private static final String NOT_FNC_SUFFIX =
            " (proceso detenido: la gramática no cumple la FNC del curso).";

    private static final String[] EMPTY_LANGUAGE_STEP_NAMES = {
            "Validación de FNC",
            "Terminales a variables (FNC estándar)",
            "Ordenamiento de variables",
            "Eliminación de recursividad izquierda",
            "Sustitución hacia atrás",
            "Eliminación de variables inalcanzables",
            "Validación final de FNG y organización"
    };

    private final GrammarService grammarService;

    /**
     * Crea el servicio con un {@link GrammarService} nuevo.
     */
    public GreibachService() {
        this(new GrammarService());
    }

    /**
     * Crea el servicio con el {@link GrammarService} indicado.
     *
     * @param grammarService pipeline FNC a reutilizar (pasos 0 a 6)
     */
    public GreibachService(GrammarService grammarService) {
        this.grammarService = grammarService;
    }

    /**
     * Ejecuta el pipeline completo (pasos 0 a 13) sobre una copia de la entrada.
     *
     * @param input gramática de entrada (no se muta)
     * @return lista con los pasos 0 a 13, en orden
     */
    public List<TransformationStep> convertFull(Grammar input) {
        List<TransformationStep> fncSteps = grammarService.convertFull(input);
        List<TransformationStep> steps = new ArrayList<>(fncSteps);

        Grammar finalFnc = last(fncSteps).getGrammarAfter();

        // (c) Detección robusta del lenguaje vacío: si algún paso del pipeline FNC
        // quedó detenido, su description termina con el sufijo de proceso detenido.
        if (isStoppedByEmptyLanguage(fncSteps)) {
            List<String> details = List.of(
                    "El lenguaje generado es vacío: " + finalFnc.getStartSymbol()
                    + " no genera cadenas. Se detiene el proceso.");
            for (int i = 0; i < EMPTY_LANGUAGE_STEP_NAMES.length; i++) {
                steps.add(stoppedStep(7 + i, EMPTY_LANGUAGE_STEP_NAMES[i],
                        finalFnc, STOPPED_SUFFIX, details));
            }
            return steps;
        }

        // Paso 7: validación de FNC del curso.
        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step7 = checker.check(finalFnc);
        steps.add(step7);
        Grammar g7 = step7.getGrammarAfter();

        // Si no cumple la FNC del curso, no se puede continuar.
        if (!checker.isCompliant()) {
            List<String> details = List.of(
                    "La gramática no cumple la FNC del curso: no se puede continuar "
                    + "con la conversión a FNG.");
            for (int menuNumber = 8; menuNumber <= 13; menuNumber++) {
                steps.add(stoppedStep(menuNumber,
                        EMPTY_LANGUAGE_STEP_NAMES[menuNumber - 7],
                        g7, NOT_FNC_SUFFIX, details));
            }
            return steps;
        }

        // Paso 8: terminales a variables (una sola instancia de registry).
        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(g7);
        TransformationStep step8 = new TerminalToVariableConverter(registry).convert(g7);
        steps.add(step8);
        Grammar g8 = step8.getGrammarAfter();

        // Paso 9: ordenamiento de variables (no cambia la gramática).
        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        TransformationStep step9 = orderer.order(g8);
        steps.add(step9);

        // Pasos 10: recursividad izquierda (entrada = gramática del paso 8).
        LeftRecursionEliminator leftRecursion =
                new LeftRecursionEliminator(orderer.getOrder(), registry);
        List<TransformationStep> steps10 = leftRecursion.eliminate(g8);
        steps.addAll(steps10);
        Grammar g10 = last(steps10).getGrammarAfter();

        // Pasos 11: sustitución hacia atrás.
        BackwardSubstitutor backward =
                new BackwardSubstitutor(orderer.getOrder(), leftRecursion.getCreatedVariables());
        List<TransformationStep> steps11 = backward.substitute(g10);
        steps.addAll(steps11);
        Grammar g11 = last(steps11).getGrammarAfter();

        // Paso 12: variables inalcanzables (reutilizado, renumerado a 12).
        TransformationStep step12 = new UnreachableVariableEliminator()
                .eliminate(g11)
                .withMenuNumber(12);
        steps.add(step12);
        Grammar g12 = step12.getGrammarAfter();

        // Paso 13: validación final y organización.
        GreibachFinalizer finalizer = new GreibachFinalizer(
                checker.isStartNullable(),
                orderer.getOrder(),
                List.copyOf(registry.terminalVariables().values()),
                leftRecursion.getCreatedVariables());
        steps.add(finalizer.validateAndOrganize(g12));

        return steps;
    }

    private static boolean isStoppedByEmptyLanguage(List<TransformationStep> steps) {
        for (TransformationStep step : steps) {
            String description = step.getDescription();
            if (description != null && description.endsWith(STOPPED_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    private static TransformationStep stoppedStep(int menuNumber, String name,
                                                  Grammar grammar, String suffix,
                                                  List<String> details) {
        return new TransformationStep(
                name,
                "Sin cambios en la gramática" + suffix,
                grammar.copy(), grammar.copy(),
                List.of(), List.of(), details, menuNumber, null);
    }

    private static TransformationStep last(List<TransformationStep> steps) {
        return steps.get(steps.size() - 1);
    }
}
