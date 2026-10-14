package com.fnc.engine;

import com.fnc.model.ConversionTarget;
import com.fnc.model.Grammar;
import com.fnc.model.TransformationStep;

import java.util.List;

/**
 * Fachada que elige el destino de la conversión.
 * <p>
 * Delega en el servicio correspondiente según el {@link ConversionTarget}:
 * {@link ConversionTarget#CHOMSKY} usa el pipeline FNC (pasos 0 a 6) y
 * {@link ConversionTarget#GREIBACH} usa el pipeline FNG completo (pasos 0 a 13).
 */
public class ConversionService {

    private final GrammarService grammarService;
    private final GreibachService greibachService;

    /**
     * Crea la fachada con servicios nuevos.
     */
    public ConversionService() {
        this(new GrammarService(), new GreibachService());
    }

    /**
     * Crea la fachada con los servicios indicados.
     *
     * @param grammarService   pipeline FNC
     * @param greibachService  pipeline FNG completo
     */
    public ConversionService(GrammarService grammarService, GreibachService greibachService) {
        this.grammarService = grammarService;
        this.greibachService = greibachService;
    }

    /**
     * Ejecuta la conversión hacia el destino indicado.
     *
     * @param input  gramática de entrada (no se muta)
     * @param target destino de la conversión
     * @return la lista de pasos correspondiente al destino
     * @throws IllegalArgumentException si {@code target} es {@code null}
     */
    public List<TransformationStep> convert(Grammar input, ConversionTarget target) {
        if (target == null) {
            throw new IllegalArgumentException(
                    "El destino de la conversión no puede ser nulo.");
        }
        if (target == ConversionTarget.CHOMSKY) {
            return grammarService.convertFull(input);
        }
        return greibachService.convertFull(input);
    }
}
