package com.fnc.model;

/**
 * Destino de conversión elegido para una gramática.
 * <p>
 * Determina hasta dónde llega el pipeline encadenado de transformaciones:
 * {@link #CHOMSKY} lo detiene en la Forma Normal de Chomsky (pasos 0 a 6) y
 * {@link #GREIBACH} encadena esos pasos y continúa hasta la Forma Normal de
 * Greibach (pasos 7 a 13).
 */
public enum ConversionTarget {

    /** Forma Normal de Chomsky: solo el pipeline FNC (pasos 0 a 6). */
    CHOMSKY,

    /** Forma Normal de Greibach: pipeline FNC más el motor FNG (pasos 0 a 13). */
    GREIBACH
}
