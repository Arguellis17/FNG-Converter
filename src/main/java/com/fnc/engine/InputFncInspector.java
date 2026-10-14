package com.fnc.engine;

import com.fnc.model.Grammar;
import com.fnc.model.Production;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Inspector de la forma normal de Chomsky para la gramática de entrada.
 * No muta la gramática recibida y clasifica símbolos mediante
 * grammar.getVariables() / grammar.getTerminals().
 */
public class InputFncInspector {

    /**
     * Estado de FNC de la gramática de entrada.
     */
    public enum FncStatus {
        /**
         * Toda producción es A → B C (exactamente 2 variables) o A → a (un solo terminal).
         * S → ε se permite solo si S no aparece en ningún lado derecho.
         */
        STANDARD,
        /**
         * No es STANDARD, pero toda producción tiene como máximo 2 símbolos,
         * no hay unitarias y no hay ε salvo S → ε.
         */
        COURSE,
        /**
         * Cualquier otro caso.
         */
        NOT_FNC
    }

    private final List<String> violations = new ArrayList<>();
    private FncStatus status = FncStatus.STANDARD;

    /**
     * Inspecciona la gramática para determinar si cumple FNC estándar o del curso.
     *
     * @param g gramática a inspeccionar (no se modifica)
     * @return estado de FNC detectado
     */
    public FncStatus inspect(Grammar g) {
        violations.clear();
        if (g == null) {
            status = FncStatus.NOT_FNC;
            return status;
        }

        Set<String> variables = new LinkedHashSet<>(g.getVariables());
        Set<String> terminals = new LinkedHashSet<>(g.getTerminals());
        String startSymbol = g.getStartSymbol();
        List<Production> productions = g.getProductions();

        boolean hasViolation = false;
        boolean hasMoreThanTwo = false;
        boolean hasUnitary = false;
        boolean hasEpsilonNonStart = false;

        for (Production p : productions) {
            List<String> rightSide = p.getRightSide();
            String leftSide = p.getLeftSide();
            int size = rightSide.size();

            // Verificar ε no permitida salvo S → ε
            boolean isEpsilon = rightSide.isEmpty() || rightSide.contains("ε");
            if (isEpsilon) {
                if (leftSide == null || !leftSide.equals(startSymbol)) {
                    hasEpsilonNonStart = true;
                }
                // S → ε es permitido; no genera violación en el formato pedido
            }

            // Más de 2 símbolos
            if (size > 2) {
                hasMoreThanTwo = true;
                violations.add("La producción " + p.toString() + " tiene más de 2 símbolos.");
                hasViolation = true;
                continue;
            }

            // Unitaria: un solo símbolo y es variable
            if (size == 1) {
                String s = rightSide.get(0);
                if (s != null && !s.equals("ε") && variables.contains(s)) {
                    hasUnitary = true;
                    violations.add("La producción " + p.toString() + " es unitaria.");
                    hasViolation = true;
                    continue;
                }
            }

            // Producción nula explícita (ε) no en S: reportar como nula
            if (isEpsilon && (leftSide != null && !leftSide.equals(startSymbol))) {
                violations.add("La producción " + p.toString() + " es nula.");
                hasViolation = true;
                continue;
            }
        }

        // Determinar status
        if (hasViolation || hasEpsilonNonStart) {
            // NOT_FNC por violaciones encontradas
            status = FncStatus.NOT_FNC;
            return status;
        }

        // Sin violaciones formales: verificar STANDARD vs COURSE
        // STANDARD: toda producción A → B C (2 variables) o A → a (1 terminal)
        // Si S → ε existe, S no debe aparecer en ningún lado derecho.
        boolean allStandardForm = true;
        // Verificar condición de ε para STANDARD
        boolean hasStartEpsilon = false;
        for (Production p : productions) {
            List<String> rightSide = p.getRightSide();
            boolean isEpsilon = rightSide.isEmpty() || rightSide.contains("ε");
            if (isEpsilon && p.getLeftSide() != null && p.getLeftSide().equals(startSymbol)) {
                hasStartEpsilon = true;
                break;
            }
        }
        if (hasStartEpsilon) {
            // S no debe aparecer en RHS
            for (Production p : productions) {
                for (String s : p.getRightSide()) {
                    if (s != null && s.equals(startSymbol)) {
                        allStandardForm = false;
                        break;
                    }
                }
                if (!allStandardForm) {
                    break;
                }
            }
        }
        if (allStandardForm) {
            for (Production p : productions) {
                List<String> rightSide = p.getRightSide();
                int size = rightSide.size();
                if (size == 0) {
                    continue;
                }
                if (size == 1) {
                    String s = rightSide.get(0);
                    if (s == null || s.equals("ε")) {
                        continue;
                    }
                    if (variables.contains(s)) {
                        allStandardForm = false;
                        break;
                    }
                } else if (size == 2) {
                    String s1 = rightSide.get(0);
                    String s2 = rightSide.get(1);
                    boolean s1Var = s1 != null && variables.contains(s1);
                    boolean s2Var = s2 != null && variables.contains(s2);
                    if (!(s1Var && s2Var)) {
                        allStandardForm = false;
                        break;
                    }
                }
            }
        }

        if (allStandardForm) {
            status = FncStatus.STANDARD;
        } else {
            // cumple COURSE: <=2 símbolos, sin unitarias, sin ε no válida
            status = FncStatus.COURSE;
        }
        return status;
    }

    /**
     * @return lista de violaciones detectadas tras la última inspección.
     */
    public List<String> getViolations() {
        return new ArrayList<>(violations);
    }
}
