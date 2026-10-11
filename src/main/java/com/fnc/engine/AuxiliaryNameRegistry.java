package com.fnc.engine;

import com.fnc.model.Grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registro de nombres auxiliares que usa el motor FNG (RNF08).
 * <p>
 * Genera y reserva nombres nuevos para las variables auxiliares de la
 * conversión a Forma Normal de Greibach, garantizando que ningún nombre se
 * repita. Los nombres ya tomados son las variables y terminales de la gramática
 * más cada auxiliar entregada por este registro; si un nombre está ocupado se
 * le agrega {@code '} hasta que sea único.
 * <ul>
 *   <li>{@link #terminalVariable(String)} entrega {@code Tt} para el terminal
 *       {@code t}. La misma llamada con el mismo terminal devuelve siempre la
 *       misma variable (una sola variable por terminal).</li>
 *   <li>{@link #recursionVariable(String)} entrega {@code V.1} para la variable
 *       {@code V}. Cada llamada crea un nombre auxiliar nuevo y único; el
 *       algoritmo la invoca una sola vez por variable.</li>
 * </ul>
 */
public class AuxiliaryNameRegistry {

    private final Set<String> takenNames;
    private final Map<String, String> terminalVariables = new LinkedHashMap<>();
    private final List<String> recursionVariables = new ArrayList<>();

    /**
     * Crea el registro tomando como nombres ocupados las variables y los
     * terminales de la gramática indicada.
     *
     * @param grammar gramática de trabajo del motor FNG
     */
    public AuxiliaryNameRegistry(Grammar grammar) {
        this.takenNames = new LinkedHashSet<>();
        this.takenNames.addAll(grammar.getVariables());
        this.takenNames.addAll(grammar.getTerminals());
    }

    /**
     * Devuelve la variable auxiliar del terminal indicado, con el formato
     * {@code Tt}. Es idempotente: la misma llamada con el mismo terminal
     * devuelve siempre la misma variable.
     *
     * @param terminal terminal para el que se pide la variable auxiliar
     * @return el nombre único de la variable auxiliar del terminal
     */
    public String terminalVariable(String terminal) {
        String existing = terminalVariables.get(terminal);
        if (existing != null) {
            return existing;
        }
        String name = unique("T" + terminal);
        takenNames.add(name);
        terminalVariables.put(terminal, name);
        return name;
    }

    /**
     * Devuelve un nombre auxiliar nuevo con el formato {@code V.1} para la
     * variable indicada. Si el nombre está tomado se le agregan {@code '}
     * hasta que sea único.
     *
     * @param variable variable cuya recursividad inmediata se está eliminando
     * @return el nombre único de la variable auxiliar
     */
    public String recursionVariable(String variable) {
        String name = unique(variable + ".1");
        takenNames.add(name);
        recursionVariables.add(name);
        return name;
    }

    /**
     * Devuelve las variables auxiliares de terminales creadas, en orden de
     * creación, como un mapa {@code terminal -> variable}.
     *
     * @return mapa no modificable en orden de creación
     */
    public Map<String, String> terminalVariables() {
        return Collections.unmodifiableMap(terminalVariables);
    }

    /**
     * Devuelve las variables auxiliares de recursividad creadas, en orden de
     * creación.
     *
     * @return lista no modificable en orden de creación
     */
    public List<String> recursionVariables() {
        return Collections.unmodifiableList(recursionVariables);
    }

    private String unique(String candidate) {
        String name = candidate;
        while (takenNames.contains(name)) {
            name = name + "'";
        }
        return name;
    }
}
