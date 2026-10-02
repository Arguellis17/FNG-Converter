package com.fnc.ui;

import com.fnc.model.Grammar;
import com.fnc.model.Production;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formulario para ingresar una Gramática Libre de Contexto G = (V, T, P, S).
 * Permite registrar variables, terminales, símbolo inicial y producciones
 * (RF01-RF05), validar la gramática y cargarla en el proceso de conversión.
 */
public class GrammarInputPanel extends VBox {

    private final TextField txtVariables;
    private final TextField txtTerminals;
    private final TextField txtStartSymbol;
    private final TextArea txtProductions;
    private final TextArea txtValidation;
    private final Button btnValidate;
    private final Button btnExample;
    private final Button btnClear;

    /** Callback que se ejecuta cuando la gramática fue validada con éxito. */
    private Runnable onValidGrammar;

    /** Símbolos auto-detectados del último parseo (símbolo -> variable|terminal). */
    private Map<String, String> lastAutoDeclared = new LinkedHashMap<>();

    /** True cuando la última validación silenciosa/explícita fue exitosa. */
    private final ReadOnlyBooleanWrapper grammarValid = new ReadOnlyBooleanWrapper(false);

    /** Debounce de 500 ms para la validación silenciosa mientras se escribe. */
    private final PauseTransition validationDebounce = new PauseTransition(Duration.millis(500));

    public GrammarInputPanel() {
        super(8);
        setPadding(new Insets(10));
        getStyleClass().add("panel");

        Label title = new Label("Entrada de la gramática G = (V, T, P, S)");
        title.getStyleClass().add("panel-title");

        txtVariables = new TextField();
        txtVariables.setPromptText("Variables separadas por coma. Ej: S, A, B");

        txtTerminals = new TextField();
        txtTerminals.setPromptText("Terminales separados por coma. Ej: a, b");

        txtStartSymbol = new TextField();
        txtStartSymbol.setPromptText("Símbolo inicial. Ej: S");

        txtProductions = new TextArea();
        txtProductions.setPromptText(
                "Una producción por línea (cada caracter es un símbolo). Ej:\nS -> AB | a\nA -> aA | ε");
        txtProductions.setPrefRowCount(10);
        txtProductions.getStyleClass().add("mono");
        VBox.setVgrow(txtProductions, Priority.ALWAYS);

        txtValidation = new TextArea();
        txtValidation.setEditable(false);
        txtValidation.setPrefRowCount(4);
        txtValidation.setPromptText("Aquí se mostrará el resultado de la validación.");
        txtValidation.getStyleClass().add("mono");

        btnValidate = new Button("Validar");
        btnValidate.setOnAction(e -> validateInput());

        btnExample = new Button("Ejemplo");
        btnExample.setOnAction(e -> loadExample());

        btnClear = new Button("Limpiar");
        btnClear.setOnAction(e -> clearAll());

        HBox buttons = new HBox(8, btnValidate, btnExample, btnClear);

        // Al escribir producciones: inferir V/T y revalidar con debounce.
        txtProductions.textProperty().addListener((obs, oldText, newText) -> {
            inferSymbolsFromProductions();
            scheduleSilentValidation();
        });
        // Los demás campos solo revalidan (ya participan en la inferencia).
        txtVariables.textProperty().addListener((obs, o, n) -> scheduleSilentValidation());
        txtTerminals.textProperty().addListener((obs, o, n) -> scheduleSilentValidation());
        txtStartSymbol.textProperty().addListener((obs, o, n) -> scheduleSilentValidation());

        getChildren().addAll(
                title,
                new Label("Variables (V):"), txtVariables,
                new Label("Terminales (T):"), txtTerminals,
                new Label("Símbolo inicial (S):"), txtStartSymbol,
                new Label("Producciones (P):"), txtProductions,
                buttons,
                new Label("Validación:"), txtValidation);
    }

    public void setOnValidGrammar(Runnable onValidGrammar) {
        this.onValidGrammar = onValidGrammar;
    }

    /** Propiedad observable: true si la gramática actual es válida. */
    public ReadOnlyBooleanProperty validProperty() {
        return grammarValid.getReadOnlyProperty();
    }

    /**
     * Valida el contenido del formulario y muestra el resultado.
     * @return true si la gramática es válida.
     */
    public boolean validateInput() {
        ValidationOutcome outcome = runValidation(
                txtVariables.getText(), txtTerminals.getText(),
                txtStartSymbol.getText(), txtProductions.getText());
        applyOutcome(outcome);
        if (outcome.valid() && onValidGrammar != null) {
            onValidGrammar.run();
        }
        return outcome.valid();
    }

    /** Resultado de parsear + validar una foto de los campos del formulario. */
    private record ValidationOutcome(boolean valid, String syntaxError,
                                     List<String> errors, List<String> notices) {
    }

    /** Gramática parseada junto con los símbolos auto-declarados. */
    private record ParsedGrammar(Grammar grammar, Map<String, String> autoDeclared) {
    }

    /**
     * Parsea y valida sin tocar la UI (apto para segundo plano).
     * Recibe fotos de los textos para no leer controles fuera del hilo FX.
     */
    private ValidationOutcome runValidation(String varsText, String termsText,
                                            String startText, String prodsText) {
        final ParsedGrammar parsed;
        try {
            parsed = parseFrom(varsText, termsText, startText, prodsText);
        } catch (IllegalArgumentException ex) {
            return new ValidationOutcome(false, ex.getMessage(), List.of(), List.of());
        }
        com.fnc.engine.GrammarValidator validator = new com.fnc.engine.GrammarValidator();
        boolean valid = validator.validate(parsed.grammar());
        List<String> notices = new ArrayList<>(validator.getWarnings());
        parsed.autoDeclared().forEach((sym, kind) -> notices.add(
                "Info: símbolo '" + sym + "' no declarado: se asumió como " + kind + "."));
        return new ValidationOutcome(valid, null, validator.getErrors(), notices);
    }

    /**
     * Aplica el resultado de una validación a la UI (debe correr en hilo FX):
     * mensajes, propiedad observable y borde rojo si hay error de sintaxis.
     */
    private void applyOutcome(ValidationOutcome outcome) {
        if (outcome.syntaxError() != null) {
            showValidationResult(false,
                    List.of("Error: " + outcome.syntaxError()), List.of());
            markSyntaxError(true);
        } else {
            showValidationResult(outcome.valid(), outcome.errors(), outcome.notices());
            markSyntaxError(false);
        }
        grammarValid.set(outcome.valid());
    }

    /** Marca/desmarca el borde rojo del área de producciones. */
    private void markSyntaxError(boolean error) {
        if (error) {
            if (!txtProductions.getStyleClass().contains("syntax-error")) {
                txtProductions.getStyleClass().add("syntax-error");
            }
        } else {
            txtProductions.getStyleClass().remove("syntax-error");
        }
    }

    /** Reprograma la validación silenciosa (debounce de 500 ms). */
    private void scheduleSilentValidation() {
        validationDebounce.setOnFinished(e -> {
            String varsText = txtVariables.getText();
            String termsText = txtTerminals.getText();
            String startText = txtStartSymbol.getText();
            String prodsText = txtProductions.getText();
            Task<ValidationOutcome> task = new Task<>() {
                @Override
                protected ValidationOutcome call() {
                    return runValidation(varsText, termsText, startText, prodsText);
                }
            };
            // setOnSucceeded corre en el hilo FX: seguro actualizar la UI.
            task.setOnSucceeded(ev -> applyOutcome(task.getValue()));
            Thread worker = new Thread(task, "silent-validation");
            worker.setDaemon(true);
            worker.start();
        });
        validationDebounce.playFromStart();
    }

    /**
     * Infiere Variables y Terminales desde las producciones usando la misma
     * regla de clasificación del motor (mayúscula inicial → variable,
     * en otro caso → terminal) y puebla los campos V y T.
     */
    private void inferSymbolsFromProductions() {
        final List<Production> productions;
        try {
            productions = parseProductions(txtProductions.getText());
        } catch (IllegalArgumentException ex) {
            return; // sintaxis incompleta: no tocar V/T hasta que sea parseable
        }
        Set<String> variables = new LinkedHashSet<>();
        Set<String> terminals = new LinkedHashSet<>();
        for (Production production : productions) {
            classifySymbol(production.getLeftSide(), variables, terminals);
            for (String symbol : production.getRightSide()) {
                classifySymbol(symbol, variables, terminals);
            }
        }
        txtVariables.setText(String.join(", ", variables));
        txtTerminals.setText(String.join(", ", terminals));
    }

    /** Clasifica un símbolo con la regla del motor (mayúscula → variable). */
    private void classifySymbol(String symbol, Set<String> variables, Set<String> terminals) {
        if (symbol == null || symbol.isEmpty() || symbol.equals("ε")) {
            return;
        }
        if (Character.isUpperCase(symbol.charAt(0))) {
            variables.add(symbol);
        } else {
            terminals.add(symbol);
        }
    }

    public void showValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        StringBuilder sb = new StringBuilder();
        if (valid) {
            sb.append("Gramática válida. Lista para convertir.");
            if (!warnings.isEmpty()) {
                sb.append("\n");
            }
            warnings.forEach(warn -> sb.append(warn).append("\n"));
        } else {
            errors.forEach(err -> sb.append(err).append("\n"));
            warnings.forEach(warn -> sb.append(warn).append("\n"));
        }
        txtValidation.setText(sb.toString().trim());
        txtValidation.getStyleClass().removeAll("validation-ok", "validation-error");
        txtValidation.getStyleClass().add(valid ? "validation-ok" : "validation-error");
    }

    /**
     * Construye la gramática a partir del formulario.
     * @throws IllegalArgumentException si el formato es incorrecto.
     */
    public Grammar parseGrammar() {
        ParsedGrammar parsed = parseFrom(
                txtVariables.getText(), txtTerminals.getText(),
                txtStartSymbol.getText(), txtProductions.getText());
        lastAutoDeclared = parsed.autoDeclared();
        return parsed.grammar();
    }

    /**
     * Construye la gramática desde textos dados (sin leer controles:
     * apto para segundo plano).
     * @throws IllegalArgumentException si el formato es incorrecto.
     */
    private ParsedGrammar parseFrom(String varsText, String termsText,
                                    String startText, String prodsText) {
        Set<String> variables = parseSymbolSet(varsText, "variables");
        Set<String> terminals = parseSymbolSet(termsText, "terminales");

        String start = startText == null ? "" : startText.trim();
        if (start.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el símbolo inicial.");
        }

        List<Production> productions = parseProductions(prodsText);
        if (productions.isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar al menos una producción.");
        }

        Grammar grammar = new Grammar(variables, terminals, productions, start);
        // No bloquear por símbolos no declarados: se asumen (mayúscula -> variable)
        // y el proceso los clasifica (ej: F sin producciones sale en inútiles).
        Map<String, String> autoDeclared = grammar.autoDeclareMissingSymbols();
        return new ParsedGrammar(grammar, autoDeclared);
    }

    /**
     * Parsea un conjunto de símbolos. Cada caracter es un símbolo, por lo que
     * "S, A, B" y "SAB" son equivalentes. Las comas y espacios son opcionales.
     */
    private Set<String> parseSymbolSet(String text, String setName) {
        if (text == null || text.replaceAll("[,\\s]", "").isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe ingresar el conjunto de " + setName + ".");
        }
        return text.replaceAll("[,\\s]", "").codePoints()
                .mapToObj(cp -> new String(Character.toChars(cp)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Parsea producciones con formato "A -> XY | a" (una por línea).
     * Cada caracter es un símbolo, así "B1C" equivale a "B 1 C".
     * Acepta "->" o "→", alternativas con "|" y "ε" como producción vacía.
     */
    private List<Production> parseProductions(String text) {
        List<Production> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] sides = line.split("->|→", 2);
            if (sides.length != 2) {
                throw new IllegalArgumentException(
                        "Línea " + (i + 1) + " sin formato 'Variable -> ...': " + line);
            }
            String left = sides[0].trim();
            if (left.isEmpty() || left.contains(" ")) {
                throw new IllegalArgumentException(
                        "Línea " + (i + 1) + " con lado izquierdo inválido: " + line);
            }
            String[] alternatives = sides[1].split("\\|", -1);
            for (String alt : alternatives) {
                String body = alt.replaceAll("\\s+", "");
                List<String> rightSide;
                if (body.isEmpty() || body.equals("ε") || body.equalsIgnoreCase("epsilon")) {
                    rightSide = List.of();
                } else {
                    rightSide = body.codePoints()
                            .mapToObj(cp -> new String(Character.toChars(cp)))
                            .collect(Collectors.toList());
                }
                result.add(new Production(left, rightSide));
            }
        }
        return result;
    }

    /** Carga una gramática de ejemplo para probar el aplicativo. */
    public void loadExample() {
        txtVariables.setText("S, A, B");
        txtTerminals.setText("a, b");
        txtStartSymbol.setText("S");
        txtProductions.setText("S -> A B | B\nA -> a A | a | ε\nB -> b B | b");
        txtValidation.clear();
    }

    /** Limpia todos los campos del formulario (RF20). */
    public void clearAll() {
        validationDebounce.stop();
        txtVariables.clear();
        txtTerminals.clear();
        txtStartSymbol.clear();
        txtProductions.clear();
        txtValidation.clear();
        markSyntaxError(false);
        grammarValid.set(false);
    }
}
