package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import com.fnc.testsupport.LanguageEnumerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración manual de la cadena FNG completa (pasos 7 a 13), sin
 * {@code GrammarService}: enlaza cada fase usando el {@code grammarAfter} del
 * último paso producido, con un único {@link AuxiliaryNameRegistry}.
 */
class GreibachEngineChainTest {

    @Test
    void g1ProducesDocumentedFinalAndStepSequence() {
        Result result = run(g1());

        assertEquals(List.of(7, 8, 9, 10, 10, 11, 11, 11, 12, 13), menuNumbers(result.steps));
        assertEquals(List.of(
                        "S -> 2 T1 B | 1 B | 2 T1 A.1 B | 1 A.1 B | 2",
                        "B -> 2 | 2 B.1",
                        "T1 -> 1",
                        "A.1 -> 2 T1 | 2 B.1 T1 | 2 T1 A.1 | 2 B.1 T1 A.1",
                        "B.1 -> 2 | 2 B.1"),
                productionLines(result.finalStep.getCustomBody()));
        assertTrue(result.finalizer.isValid());
    }

    @Test
    void g2StaysValidWithoutRemovingVariables() {
        Result result = run(g2());

        assertTrue(result.finalizer.isValid());
        assertEquals(
                result.stepBeforeFinal(result, 11).getGrammarAfter().getVariables(),
                result.stepBeforeFinal(result, 12).getGrammarAfter().getVariables());
    }

    @Test
    void g3ReinsertsEpsilonAndProducesDocumentedFinal() {
        Result result = run(g3());

        assertTrue(result.finalizer.isEmptyStringReinserted());
        assertTrue(result.finalizer.isValid());
        assertEquals(List.of(
                        "S -> a X1 | a Tb | ε",
                        "X1 -> a X1 Tb | a Tb Tb",
                        "Tb -> b"),
                productionLines(result.finalStep.getCustomBody()));
    }

    @Test
    void g4WithoutAuxiliariesProducesDocumentedFinal() {
        Result result = run(g4());

        assertTrue(result.finalizer.isValid());
        assertEquals(List.of("S -> a S | a"), productionLines(result.finalStep.getCustomBody()));
    }

    @Test
    void languageIsPreservedUpToLengthNine() {
        assertLanguagePreserved(g1(), run(g1()));
        assertLanguagePreserved(g2(), run(g2()));
        assertLanguagePreserved(g3(), run(g3()));
        assertLanguagePreserved(g4(), run(g4()));
    }

    @Test
    void isDeterministicAcrossRuns() {
        assertSameRun(run(g1()), run(g1()));
        assertSameRun(run(g2()), run(g2()));
        assertSameRun(run(g3()), run(g3()));
        assertSameRun(run(g4()), run(g4()));
    }

    private static void assertLanguagePreserved(Grammar input, Result result) {
        Set<String> expected = LanguageEnumerator.generate(input, 9);
        if (!result.finalizer.isEmptyStringReinserted()) {
            expected.remove("");
        }
        assertEquals(expected, LanguageEnumerator.generate(result.finalStep.getGrammarAfter(), 9));
    }

    private static void assertSameRun(Result first, Result second) {
        assertEquals(first.steps.size(), second.steps.size());
        for (int i = 0; i < first.steps.size(); i++) {
            TransformationStep a = first.steps.get(i);
            TransformationStep b = second.steps.get(i);
            assertEquals(a.getMenuNumber(), b.getMenuNumber());
            assertEquals(a.getStepName(), b.getStepName());
            assertEquals(productionStrings(a.getGrammarBefore()), productionStrings(b.getGrammarBefore()));
            assertEquals(productionStrings(a.getGrammarAfter()), productionStrings(b.getGrammarAfter()));
            assertEquals(a.getDetails(), b.getDetails());
        }
        assertEquals(first.finalStep.getCustomBody(), second.finalStep.getCustomBody());
    }

    private static Result run(Grammar input) {
        FncComplianceChecker checker = new FncComplianceChecker();
        TransformationStep step7 = checker.check(input);

        AuxiliaryNameRegistry registry = new AuxiliaryNameRegistry(step7.getGrammarAfter());
        TransformationStep step8 = new TerminalToVariableConverter(registry)
                .convert(step7.getGrammarAfter());

        VariableOrderer orderer = new VariableOrderer(registry.terminalVariables().values());
        TransformationStep step9 = orderer.order(step8.getGrammarAfter());
        List<String> order = orderer.getOrder();

        LeftRecursionEliminator leftRecursion = new LeftRecursionEliminator(order, registry);
        List<TransformationStep> steps10 = leftRecursion.eliminate(step9.getGrammarAfter());

        BackwardSubstitutor backward = new BackwardSubstitutor(
                order, leftRecursion.getCreatedVariables());
        List<TransformationStep> steps11 = backward.substitute(
                last(steps10).getGrammarAfter());

        TransformationStep step12 = new UnreachableVariableEliminator()
                .eliminate(last(steps11).getGrammarAfter())
                .withMenuNumber(12);

        GreibachFinalizer finalizer = new GreibachFinalizer(
                checker.isStartNullable(),
                order,
                new ArrayList<>(registry.terminalVariables().values()),
                leftRecursion.getCreatedVariables());
        TransformationStep step13 = finalizer.validateAndOrganize(step12.getGrammarAfter());

        List<TransformationStep> steps = new ArrayList<>();
        steps.add(step7);
        steps.add(step8);
        steps.add(step9);
        steps.addAll(steps10);
        steps.addAll(steps11);
        steps.add(step12);
        steps.add(step13);

        return new Result(steps, step13, finalizer);
    }

    private static TransformationStep last(List<TransformationStep> steps) {
        return steps.get(steps.size() - 1);
    }

    private static List<Integer> menuNumbers(List<TransformationStep> steps) {
        List<Integer> numbers = new ArrayList<>();
        for (TransformationStep step : steps) {
            numbers.add(step.getMenuNumber());
        }
        return numbers;
    }

    private static List<String> productionLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.contains(" -> ")) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    private static List<String> productionStrings(Grammar grammar) {
        List<String> lines = new ArrayList<>();
        for (Production production : grammar.getProductions()) {
            lines.add(production.toString());
        }
        return lines;
    }

    private static Grammar g1() {
        return grammar(
                List.of("S", "A", "B"),
                List.of("1", "2"),
                List.of(
                        production("S", "A", "B"),
                        production("S", "2"),
                        production("A", "S", "1"),
                        production("A", "1"),
                        production("B", "B", "2"),
                        production("B", "2")));
    }

    private static Grammar g2() {
        return grammar(
                List.of("S", "A", "B"),
                List.of("a", "b"),
                List.of(
                        production("S", "A", "B"),
                        production("A", "B", "S"),
                        production("A", "b"),
                        production("B", "S", "A"),
                        production("B", "a")));
    }

    private static Grammar g3() {
        return grammar(
                List.of("S", "X1"),
                List.of("a", "b"),
                List.of(
                        production("S", "a", "X1"),
                        production("S", "a", "b"),
                        production("S"),
                        production("X1", "S", "b")));
    }

    private static Grammar g4() {
        return grammar(
                List.of("S"),
                List.of("a"),
                List.of(production("S", "a", "S"), production("S", "a")));
    }

    private static Grammar grammar(List<String> variables, List<String> terminals,
                                   List<Production> productions) {
        return new Grammar(
                new LinkedHashSet<>(variables),
                new LinkedHashSet<>(terminals),
                productions,
                "S");
    }

    private static Production production(String left, String... right) {
        return new Production(left, List.of(right));
    }

    private static final class Result {
        private final List<TransformationStep> steps;
        private final TransformationStep finalStep;
        private final GreibachFinalizer finalizer;

        private Result(List<TransformationStep> steps, TransformationStep finalStep,
                       GreibachFinalizer finalizer) {
            this.steps = steps;
            this.finalStep = finalStep;
            this.finalizer = finalizer;
        }

        private TransformationStep stepBeforeFinal(Result result, int menuNumber) {
            for (int i = result.steps.size() - 1; i >= 0; i--) {
                if (result.steps.get(i).getMenuNumber() == menuNumber) {
                    return result.steps.get(i);
                }
            }
            throw new IllegalStateException("No se encontró el paso " + menuNumber);
        }
    }
}
