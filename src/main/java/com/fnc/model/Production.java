package com.fnc.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a production rule in a context-free grammar.
 * Format: leftSide -> rightSide
 * Example: A -> aB | b
 */
public class Production {
    private final String leftSide;
    private final List<String> rightSide;
    private boolean isNull; // epsilon production

    public Production(String leftSide, List<String> rightSide) {
        this.leftSide = leftSide;
        this.rightSide = rightSide == null ? List.of() : List.copyOf(rightSide);
        this.isNull = this.rightSide.isEmpty() || this.rightSide.contains("ε");
    }

    public String getLeftSide() {
        return leftSide;
    }

    public List<String> getRightSide() {
        return rightSide;
    }

    public boolean isNull() {
        return isNull;
    }

    public boolean isUnitary() {
        return rightSide.size() == 1 && Character.isUpperCase(rightSide.get(0).charAt(0));
    }

    public boolean isBinary() {
        return rightSide.size() == 2;
    }

    /**
     * Indica si la producción es de un solo símbolo y ese símbolo es un
     * terminal. Se considera terminal a todo símbolo que no empieza con
     * mayúscula y que no es la cadena vacía ε. Así se reconocen como
     * terminales no solo las letras minúsculas, sino también dígitos y otros
     * símbolos (por ejemplo {@code 1}, {@code 2}) que antes no se detectaban.
     */
    public boolean isTerminal() {
        if (rightSide.size() != 1) {
            return false;
        }
        String symbol = rightSide.get(0);
        if (symbol.isEmpty() || symbol.equals("ε")) {
            return false;
        }
        return !Character.isUpperCase(symbol.charAt(0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return Objects.equals(leftSide, that.leftSide) &&
               Objects.equals(rightSide, that.rightSide);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftSide, rightSide);
    }

    @Override
    public String toString() {
        String right = rightSide.isEmpty() ? "ε" : String.join(" ", rightSide);
        return leftSide + " -> " + right;
    }
}
