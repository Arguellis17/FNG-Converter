package com.fnc.ui;

import com.fnc.engine.GrammarService;
import com.fnc.engine.GrammarValidator;
import com.fnc.model.Grammar;
import com.fnc.model.TransformationStep;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;

import java.util.List;

/**
 * Ventana principal del aplicativo (menú + paneles).
 * Ofrece modo automático (proceso completo) y modo paso a paso
 * (botón "Ejecutar el paso a paso" + "Continuar"), ambos sobre el
 * pipeline de {@link GrammarService}.
 */
public class MainFrame extends BorderPane {

    private final GrammarInputPanel inputPanel;
    private final TransformationPanel transformationPanel;
    private final Label lblStatus;

    /** Gramática sobre la que se aplica la siguiente etapa (modo paso a paso). */
    private Grammar workingGrammar;

    /** Pasos calculados de la sesión paso a paso en curso (null si no hay). */
    private List<TransformationStep> pendingSteps;
    /** Índice del último paso ya mostrado (-1 si no hay sesión activa). */
    private int currentStepIndex = -1;

    /** True cuando la gramática del formulario pasa la validación silenciosa. */
    private final BooleanProperty grammarValid = new SimpleBooleanProperty(false);
    /** True cuando la sesión paso a paso aún tiene un siguiente paso. */
    private final BooleanProperty hasNextStep = new SimpleBooleanProperty(false);

    public MainFrame() {
        inputPanel = new GrammarInputPanel();
        transformationPanel = new TransformationPanel();
        lblStatus = new Label("Ingrese una gramática para comenzar.");
        lblStatus.getStyleClass().add("status-bar");

        inputPanel.setOnValidGrammar(() -> {
            try {
                workingGrammar = inputPanel.parseGrammar();
                setStatus("Gramática válida. Puede ejecutar el proceso completo o paso a paso.");
            } catch (IllegalArgumentException ex) {
                setStatus("Error: " + ex.getMessage());
            }
        });

        Button btnFull = new Button("▶ Proceso completo");
        btnFull.getStyleClass().add("primary-button");
        btnFull.setOnAction(e -> runFullProcess());

        Button btnStepByStep = new Button("Ejecutar el paso a paso");
        btnStepByStep.setOnAction(e -> startStepByStep());
        // Solo se habilita cuando la gramática pasa la validación silenciosa.
        btnStepByStep.disableProperty().bind(grammarValid.not());

        Button btnContinue = new Button("Continuar");
        btnContinue.setOnAction(e -> continueStepByStep());
        // Requiere gramática válida y que quede un siguiente paso por mostrar.
        btnContinue.disableProperty().bind(grammarValid.not().or(hasNextStep.not()));

        // La validez la dicta el panel de entrada (validación silenciosa).
        grammarValid.bind(inputPanel.validProperty());

        transformationPanel.setActionButtons(btnFull, btnStepByStep, btnContinue);

        setTop(buildMenuBar());
        setLeft(inputPanel);
        inputPanel.setPrefWidth(340);

        setCenter(transformationPanel);

        setBottom(lblStatus);
        BorderPane.setMargin(lblStatus, new Insets(4, 10, 4, 10));
    }

    private MenuBar buildMenuBar() {
        Menu menuArchivo = new Menu("Archivo");

        MenuItem itemNueva = new MenuItem("Nueva gramática");
        itemNueva.setOnAction(e -> newGrammar());

        MenuItem itemSalir = new MenuItem("Salir");
        itemSalir.setOnAction(e -> System.exit(0));

        menuArchivo.getItems().addAll(itemNueva, itemSalir);

        Menu menuAyuda = new Menu("Ayuda");
        MenuItem itemAcerca = new MenuItem("Acerca de");
        itemAcerca.setOnAction(e -> showAbout());
        menuAyuda.getItems().add(itemAcerca);

        return new MenuBar(menuArchivo, menuAyuda);
    }

    /** Ejecuta todas las etapas de una vez sobre la gramática del formulario. */
    private void runFullProcess() {
        Grammar grammar = parseAndValidate();
        if (grammar == null) {
            return;
        }

        resetStepByStep();

        List<TransformationStep> steps = new GrammarService().convertFull(grammar);
        Grammar current = steps.get(steps.size() - 1).getGrammarAfter();

        workingGrammar = current;
        transformationPanel.setSteps(steps);
        setStatus("Proceso completo: " + steps.size()
                + " pasos. Gramática final lista.");
    }

    /**
     * Inicia el modo paso a paso: calcula todos los pasos y muestra
     * únicamente el primero. Cada click en "Continuar" revela el siguiente.
     */
    private void startStepByStep() {
        Grammar grammar = parseAndValidate();
        if (grammar == null) {
            return;
        }

        pendingSteps = new GrammarService().convertFull(grammar);
        currentStepIndex = 0;

        transformationPanel.clear();
        revealCurrentStep();
        hasNextStep.set(pendingSteps.size() > 1);
    }

    /** Revela el siguiente paso de la sesión paso a paso en curso. */
    private void continueStepByStep() {
        if (pendingSteps == null || currentStepIndex < 0) {
            setStatus("Pulse primero \"Ejecutar el paso a paso\".");
            return;
        }
        if (currentStepIndex + 1 >= pendingSteps.size()) {
            setStatus("Ya se mostró el último paso. Gramática final lista.");
            return;
        }
        currentStepIndex++;
        revealCurrentStep();
    }

    /** Muestra el paso actual y actualiza la gramática de trabajo y el resultado. */
    private void revealCurrentStep() {
        TransformationStep step = pendingSteps.get(currentStepIndex);
        transformationPanel.addStep(step);
        workingGrammar = step.getGrammarAfter();
        hasNextStep.set(currentStepIndex + 1 < pendingSteps.size());
        setStatus("Paso " + (currentStepIndex + 1) + " de " + pendingSteps.size()
                + ": " + step.getStepName() + ". Pulse \"Continuar\".");
    }

    /** Reinicia la sesión paso a paso. */
    private void resetStepByStep() {
        pendingSteps = null;
        currentStepIndex = -1;
        hasNextStep.set(false);
    }

    /**
     * Registra atajos globales: F5 ejecuta el proceso completo y
     * F10 inicia el modo paso a paso.
     */
    public void registerAccelerators(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F5), this::runFullProcess);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F10), this::startStepByStep);
    }

    /**
     * Parsea y valida la gramática del formulario.
     * @return la gramática válida, o null si hay errores (ya reportados).
     */
    private Grammar parseAndValidate() {
        Grammar grammar;
        try {
            grammar = inputPanel.parseGrammar();
        } catch (IllegalArgumentException ex) {
            inputPanel.showValidationResult(false,
                    List.of("Error: " + ex.getMessage()), List.of());
            setStatus("Corrija la gramática antes de convertir.");
            return null;
        }

        GrammarValidator validator = new GrammarValidator();
        if (!validator.validate(grammar)) {
            inputPanel.showValidationResult(false,
                    validator.getErrors(), validator.getWarnings());
            setStatus("La gramática tiene errores. El proceso no puede comenzar.");
            return null;
        }
        inputPanel.showValidationResult(true, List.of(), validator.getWarnings());
        return grammar;
    }

    /** Limpia todo para ingresar una nueva gramática (RF20). */
    private void newGrammar() {
        inputPanel.clearAll();
        transformationPanel.clear();
        workingGrammar = null;
        resetStepByStep();
        setStatus("Ingrese una gramática para comenzar.");
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("FNC Converter");
        alert.setContentText("Aplicativo para depuración y conversión de Gramáticas "
                + "Libres de Contexto a Forma Normal de Chomsky.\nTeoría de la Computación - Microproyecto #1.");
        alert.showAndWait();
    }

    private void setStatus(String message) {
        lblStatus.setText(message);
    }
}
