package com.fnc.engine;

import com.fnc.model.ConversionTarget;
import com.fnc.model.Grammar;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link ConversionService}: la fachada que elige el destino.
 */
class ConversionServiceTest {

    @Test
    void chomskyDelegatesToTheFncPipeline() {
        List<TransformationStep> viaConversion =
                new ConversionService().convert(g1(), ConversionTarget.CHOMSKY);
        List<TransformationStep> viaGrammarService =
                new GrammarService().convertFull(g1());

        assertEquals(7, viaConversion.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), menuNumbers(viaConversion));
        assertEquals(runSignature(viaGrammarService), runSignature(viaConversion));
    }

    @Test
    void greibachDelegatesToTheFngPipeline() {
        List<TransformationStep> viaConversion =
                new ConversionService().convert(g1(), ConversionTarget.GREIBACH);
        List<TransformationStep> viaGreibachService =
                new GreibachService().convertFull(g1());

        assertEquals(17, viaConversion.size());
        assertEquals(runSignature(viaGreibachService), runSignature(viaConversion));
    }

    @Test
    void nullTargetThrows() {
        ConversionService service = new ConversionService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.convert(g1(), null));
        assertTrue(error.getMessage().contains("destino"));
    }

    private static List<String> runSignature(List<TransformationStep> steps) {
        List<String> signature = new ArrayList<>();
        for (TransformationStep step : steps) {
            signature.add(step.getMenuNumber() + "|" + step.getStepName()
                    + "|" + step.getDescription() + "|" + step.getDetails()
                    + "|" + productionStrings(step.getGrammarAfter()));
        }
        return signature;
    }

    private static List<String> productionStrings(Grammar grammar) {
        List<String> lines = new ArrayList<>();
        for (Production production : grammar.getProductions()) {
            lines.add(production.toString());
        }
        return lines;
    }

    private static List<Integer> menuNumbers(List<TransformationStep> steps) {
        List<Integer> numbers = new ArrayList<>();
        for (TransformationStep step : steps) {
            numbers.add(step.getMenuNumber());
        }
        return numbers;
    }

    private static Grammar g1() {
        return new Grammar(
                new LinkedHashSet<>(List.of("S", "A", "B")),
                new LinkedHashSet<>(List.of("1", "2")),
                List.of(
                        new Production("S", List.of("A", "B")),
                        new Production("S", List.of("2")),
                        new Production("A", List.of("S", "1")),
                        new Production("A", List.of("1")),
                        new Production("B", List.of("B", "2")),
                        new Production("B", List.of("2"))),
                "S");
    }
}
