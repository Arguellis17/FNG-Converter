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
        this.rightSide = rightSide;
        this.isNull = rightSide.isEmpty() || rightSide.contains("ε");
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

    public boolean isTerminal() {
        return rightSide.size() == 1 && Character.isLowerCase(rightSide.get(0).charAt(0));
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
