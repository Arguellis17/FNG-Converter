package com.fnc.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas unitarias para {@link ConversionTarget}, en especial su etiqueta
 * visible usada por el selector de destino de la interfaz.
 */
class ConversionTargetTest {

    @Test
    void chomskyLabelIsFncChomsky() {
        assertEquals("FNC (Chomsky)", ConversionTarget.CHOMSKY.getLabel());
    }

    @Test
    void greibachLabelIsFngGreibach() {
        assertEquals("FNG (Greibach)", ConversionTarget.GREIBACH.getLabel());
    }
}
