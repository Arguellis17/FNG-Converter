package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.GrammarFormatter;
import com.fnc.model.Production;
import com.fnc.model.TransformationStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de punta a punta del pipeline completo
 * ({@link GrammarService}): ejemplo del archivo, caso F inalcanzable,
 * gramática SABCDE y caso del párrafo (b).
 */
class GrammarServiceTest {

    private static Set<String> ordered(String... symbols) {
        return new LinkedHashSet<>(List.of(symbols));
    }

    /** Gramática P0 del ejemplo del archivo (G no declarada a propósito). */
    private static Grammar fileExample() {
        return new Grammar(
            ordered("A", "B", "C", "D", "E", "F"),
            ordered("1", "2", "3"),
            new ArrayList<>(List.of(
                new Production("A", List.of("B", "B", "1")),
                new Production("A", List.of("E", "F", "2")),
                new Production("A", List.of("3")),
                new Production("B", List.of("C", "D", "E", "E", "1", "F")),
                new Production("B", List.of("C", "G")),
                new Production("B", List.of("F")),
                new Production("B", List.of("2", "D", "E")),
                new Production("B", List.of("2")),
                new Production("B", List.of()),
                new Production("C", List.of("E")),
                new Production("D", List.of("1", "2", "C")),
                new Production("D", List.of("B", "3", "C", "1")),
                new Production("D", List.of("2")),
                new Production("D", List.of()),
                new Production("E", List.of("D", "C", "D", "C", "2")),
                new Production("E", List.of("1"))
            )),
            "A"
        );
    }

    private static String full(String header, String... lines) {
        StringBuilder sb = new StringBuilder(header);
        for (String line : lines) {
            sb.append("\n").append(line);
        }
        return sb.toString();
    }

    private static final String H5 = "G={{A, B, C, D, E}, {1, 2, 3}, A, Sigma}";

    /** El ejemplo del archivo debe dar exactamente P1, P2, P3, P4. */
    @Test
    void fileExampleGivesExactIntermediateGrammars() {
        List<TransformationStep> steps = new GrammarService().convertFull(fileExample());

        assertEquals(7, steps.size(), "El menú lateral tiene 7 pasos (0 a 6)");
        assertEquals("Definición de gramática", steps.get(0).getStepName());
        assertEquals("Eliminación de variables inútiles", steps.get(1).getStepName());
        assertEquals("Eliminación de variables inalcanzables", steps.get(2).getStepName());
        assertEquals("Eliminación de unitarias", steps.get(3).getStepName());
        assertEquals("Eliminación de producciones nulas (vacío)", steps.get(4).getStepName());
        assertEquals("Aplicando Forma Normal de Chomsky", steps.get(5).getStepName());
        assertEquals("Organización gramática", steps.get(6).getStepName());
        for (int i = 0; i < 7; i++) {
            assertEquals(i, steps.get(i).getMenuNumber(), "Paso " + i + " numerado");
        }

        // G no declarada se asume como variable en el paso 0.
        assertTrue(String.join("\n", steps.get(0).getDetails()).contains(
                "Info: símbolo 'G' no declarado: se asumió como variable."));

        // P1: sin F, sin G, con ε conservados.
        assertEquals(full(H5,
                "Sigma=",
                "  A -> B B 1 | 3",
                "  B -> 2 D E | 2 | ε",
                "  C -> E",
                "  D -> 1 2 C | B 3 C 1 | 2 | ε",
                "  E -> D C D C 2 | 1"),
                GrammarFormatter.formatFull(steps.get(1).getGrammarAfter()));

        // Paso 2 sin cambios (mensaje del archivo).
        assertEquals("No se han identificado variables inalcanzables, no hay "
                + "cambios en la gramática. Se pasa al siguiente paso.",
                steps.get(2).getDescription());

        // P2: C→E reemplazada.
        assertEquals(full(H5,
                "Sigma=",
                "  A -> B B 1 | 3",
                "  B -> 2 D E | 2 | ε",
                "  C -> D C D C 2 | 1",
                "  D -> 1 2 C | B 3 C 1 | 2 | ε",
                "  E -> D C D C 2 | 1"),
                GrammarFormatter.formatFull(steps.get(3).getGrammarAfter()));
        assertTrue(String.join("\n", steps.get(3).getDetails())
                .contains("Eliminando unitaria: C→E"));

        // P4 final del paso 4 (= P4 del archivo).
        assertEquals(full(H5,
                "Sigma=",
                "  A -> B B 1 | B 1 | 1 | 3",
                "  B -> 2 D E | 2 E | 2",
                "  C -> D C D C 2 | C D C 2 | D C C 2 | C C 2 | 1",
                "  D -> 1 2 C | B 3 C 1 | 3 C 1 | 2",
                "  E -> D C D C 2 | C D C 2 | D C C 2 | C C 2 | 1"),
                GrammarFormatter.formatFull(steps.get(4).getGrammarAfter()));

        // Sub-bloques secuenciales dentro del paso 4 (P3 intermedio).
        String nullDetails = String.join("\n", steps.get(4).getDetails());
        assertTrue(nullDetails.contains("Nulables: B y D."));
        assertTrue(nullDetails.contains("Eliminando vacío: B→ε"));
        assertTrue(nullDetails.contains("Eliminando vacío: D→ε"));
        assertTrue(nullDetails.contains("D -> 1 2 C | B 3 C 1 | 3 C 1 | 2 | ε"),
                "P3 intermedio conserva D→ε");

        // Final: 22 Xn en orden, variables exactas.
        Grammar fin = steps.get(6).getGrammarAfter();
        Set<String> expectedVars = new LinkedHashSet<>(Set.of("A", "B", "C", "D", "E"));
        for (int i = 1; i <= 22; i++) {
            expectedVars.add("X" + i);
        }
        assertEquals(expectedVars, fin.getVariables());
        assertTrue(fin.getProductions().stream()
                .allMatch(p -> p.getRightSide().size() <= 2),
                "Todo mide máximo 2 símbolos");
        String finalBody = steps.get(6).getCustomBody();
        assertTrue(finalBody.contains("X1->B 1"), "Tabla final con X1");
        assertTrue(finalBody.contains("X22->C 2"), "Tabla final con X22");
    }

    /**
     * Caso con variable inalcanzable de la sección 3:
     * F → D1 | 2 que ninguna producción genera.
     */
    @Test
    void unreachableFCase() {
        Grammar grammar = new Grammar(
            ordered("A", "B", "D", "F"),
            ordered("1", "2"),
            new ArrayList<>(List.of(
                new Production("A", List.of("B", "1")),
                new Production("A", List.of("D", "2")),
                new Production("B", List.of("2")),
                new Production("B", List.of()),
                new Production("D", List.of("1")),
                new Production("F", List.of("D", "1")),
                new Production("F", List.of("2"))
            )),
            "A"
        );

        List<TransformationStep> steps = new GrammarService().convertFull(grammar);
        TransformationStep unreachable = steps.get(2);

        assertEquals("Se identificó como variable inalcanzable: F. "
                + "Se elimina F con sus producciones.", unreachable.getDescription());
        assertTrue(unreachable.getGrammarAfter().getProductions().stream()
                .noneMatch(p -> p.getLeftSide().equals("F")
                        || p.getRightSide().contains("F")),
                "F sale con sus producciones");
    }

    /** Gramática de entrenamiento S, A, B, C, D, E resuelta a mano. */
    @Test
    void trainingGrammarSABCDE() {
        Grammar grammar = new Grammar(
            ordered("S", "A", "B", "C", "D", "E"),
            ordered("1", "2", "3"),
            new ArrayList<>(List.of(
                new Production("S", List.of("A", "B", "1")),
                new Production("S", List.of("1", "C")),
                new Production("S", List.of("D", "2")),
                new Production("A", List.of("C")),
                new Production("A", List.of("1", "A")),
                new Production("B", List.of("2", "B")),
                new Production("B", List.of()),
                new Production("C", List.of("3")),
                new Production("C", List.of("1")),
                new Production("D", List.of("D", "1")),
                new Production("E", List.of("1", "2", "E")),
                new Production("E", List.of("3"))
            )),
            "S"
        );

        List<TransformationStep> steps = new GrammarService().convertFull(grammar);

        // Paso 1: solo D es inútil (B genera vía ε).
        assertEquals("Se identificó la variable inútil: D. "
                + "Se cancelan las producciones que contengan esta variable.",
                steps.get(1).getDescription());
        assertEquals(full("G={{S, A, B, C, E}, {1, 2, 3}, S, Sigma}",
                "Sigma=",
                "  S -> A B 1 | 1 C",
                "  A -> C | 1 A",
                "  B -> 2 B | ε",
                "  C -> 3 | 1",
                "  E -> 1 2 E | 3"),
                GrammarFormatter.formatFull(steps.get(1).getGrammarAfter()));

        // Paso 2: E inalcanzable.
        assertEquals("Se identificó como variable inalcanzable: E. "
                + "Se elimina E con sus producciones.", steps.get(2).getDescription());

        // Paso 3: unitaria A→C.
        assertTrue(String.join("\n", steps.get(3).getDetails())
                .contains("Eliminando unitaria: A→C"));

        // Paso 4: nulable B; S→A1 y B→2 agregadas.
        Grammar p4 = steps.get(4).getGrammarAfter();
        assertTrue(hasProduction(p4, "S", List.of("A", "1")), "S → A1");
        assertTrue(hasProduction(p4, "B", List.of("2")), "B → 2");
        assertTrue(String.join("\n", steps.get(4).getDetails())
                .contains("Eliminando vacío: B→ε"));

        // Paso 5: solo S→AB1 se parte (X1→B1, S→AX1).
        Grammar fin = steps.get(6).getGrammarAfter();
        assertEquals(Set.of("S", "A", "B", "C", "X1"), fin.getVariables());
        assertTrue(hasProduction(fin, "S", List.of("A", "X1")), "S → AX1");
        assertTrue(hasProduction(fin, "X1", List.of("B", "1")), "X1 → B1");
    }

    /**
     * Caso del párrafo (b): S → AB1 | 3, A → 2, B → ε.
     * Tras nulas B queda sin producciones ⇒ inútil ⇒ sale con S→AB1.
     * Final genera {21, 3}.
     */
    @Test
    void nullLeavesBWithoutProductions() {
        Grammar grammar = new Grammar(
            ordered("S", "A", "B"),
            ordered("1", "2", "3"),
            new ArrayList<>(List.of(
                new Production("S", List.of("A", "B", "1")),
                new Production("S", List.of("3")),
                new Production("A", List.of("2")),
                new Production("B", List.of())
            )),
            "S"
        );

        List<TransformationStep> steps = new GrammarService().convertFull(grammar);
        String nullDetails = String.join("\n", steps.get(4).getDetails());

        assertTrue(nullDetails.contains("Eliminando vacío: B→ε"));
        assertTrue(nullDetails.contains("Se identificó la variable inútil: B"),
                "B sin producciones se reporta como inútil dentro del paso 4");

        Grammar fin = steps.get(6).getGrammarAfter();
        assertTrue(hasProduction(fin, "S", List.of("A", "1")), "S → A1 (21)");
        assertTrue(hasProduction(fin, "S", List.of("3")), "S → 3");
        assertTrue(hasProduction(fin, "A", List.of("2")), "A → 2");
        assertTrue(fin.getProductions().stream()
                .noneMatch(p -> p.getLeftSide().equals("B")
                        || p.getRightSide().contains("B")),
                "B sale con S→AB1");
    }

    private static boolean hasProduction(Grammar grammar, String left, List<String> right) {
        return grammar.getProductions().stream()
                .anyMatch(p -> p.getLeftSide().equals(left)
                        && p.getRightSide().equals(right));
    }
}
