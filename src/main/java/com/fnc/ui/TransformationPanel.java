package com.fnc.ui;

import com.fnc.model.Grammar;
import com.fnc.model.GrammarFormatter;
import com.fnc.model.TransformationStep;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel que muestra el historial paso a paso de las transformaciones (RF18).
 *
 * - La lista de pasos es un {@link ListView} tipado con
 *   {@link TransformationStep}: al seleccionar un paso anterior, el detalle
 *   muestra exactamente el estado de la gramática en ese momento
 *   (máquina del tiempo).
 * - El detalle es una jerarquía visual dentro de un {@link ScrollPane}:
 *   título del paso, caja de diagnóstico, diff lado a lado
 *   (eliminadas/agregadas), separador y tarjeta con la gramática resultante.
 *
 * Distribución: arriba la lista de pasos con los botones de control a su
 * derecha; debajo, el detalle del paso seleccionado.
 */
public class TransformationPanel extends VBox {

    private final ListView<TransformationStep> stepList;
    private final ObservableList<TransformationStep> stepItems =
            FXCollections.observableArrayList();
    private final VBox detailContent = new VBox(16);
    private final Label lblCount;
    private final VBox buttonBox;
    private final List<TransformationStep> steps = new ArrayList<>();

    public TransformationPanel() {
        super(6);
        setPadding(new Insets(10));

        stepList = new ListView<>(stepItems);
        stepList.setPrefHeight(200);
        stepList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(TransformationStep step, boolean empty) {
                super.updateItem(step, empty);
                if (empty || step == null) {
                    setText(null);
                } else {
                    setText(labelFor(step, getIndex()));
                }
            }
        });
        // Máquina del tiempo: seleccionar un paso muestra su estado exacto.
        stepList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldStep, newStep) -> showStep(newStep));

        lblCount = new Label("Pasos: 0");
        lblCount.getStyleClass().add("panel-title");
        VBox stepsBox = new VBox(6, lblCount, stepList);
        HBox.setHgrow(stepsBox, Priority.ALWAYS);

        buttonBox = new VBox(8);
        buttonBox.setPadding(new Insets(24, 0, 0, 0));

        HBox topRow = new HBox(8, stepsBox, buttonBox);

        detailContent.setPadding(new Insets(16));
        ScrollPane detailScroll = new ScrollPane(detailContent);
        detailScroll.setFitToWidth(true);

        Label detailTitle = new Label("Detalle del paso");
        detailTitle.getStyleClass().add("panel-title");
        VBox detailSection = new VBox(6, detailTitle, detailScroll);
        VBox.setVgrow(detailScroll, Priority.ALWAYS);
        VBox.setVgrow(detailSection, Priority.ALWAYS);

        getChildren().addAll(topRow, detailSection);
        showPlaceholder();
    }

    private static String labelFor(TransformationStep step, int position) {
        int n = step.getMenuNumber() >= 0 ? step.getMenuNumber() : position + 1;
        return n + ". " + step.getStepName();
    }

    /**
     * Coloca los botones de control a la derecha de la lista de pasos.
     */
    public void setActionButtons(Button... buttons) {
        buttonBox.getChildren().setAll(buttons);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /** Reemplaza el historial completo de pasos. */
    public void setSteps(List<TransformationStep> newSteps) {
        steps.clear();
        stepItems.clear();
        if (newSteps != null) {
            steps.addAll(newSteps);
            stepItems.addAll(newSteps);
        }
        lblCount.setText("Pasos: " + steps.size());
        if (!steps.isEmpty()) {
            stepList.getSelectionModel().select(0);
        } else {
            showPlaceholder();
        }
    }

    /** Agrega un paso al final del historial. */
    public void addStep(TransformationStep step) {
        steps.add(step);
        stepItems.add(step);
        lblCount.setText("Pasos: " + steps.size());
        stepList.getSelectionModel().select(steps.size() - 1);
    }

    public List<TransformationStep> getSteps() {
        return List.copyOf(steps);
    }

    /** Limpia el historial de pasos. */
    public void clear() {
        setSteps(List.of());
    }

    private void showPlaceholder() {
        detailContent.getChildren().clear();
        Label placeholder = new Label("Seleccione un paso de la lista para ver su detalle.");
        placeholder.getStyleClass().add("diff-unchanged");
        placeholder.setWrapText(true);
        detailContent.getChildren().add(placeholder);
    }

    /**
     * Construye la jerarquía visual del paso dado: título, diagnóstico,
     * diff lado a lado, separador y tarjeta con la gramática resultante.
     */
    private void showStep(TransformationStep step) {
        detailContent.getChildren().clear();
        if (step == null) {
            showPlaceholder();
            return;
        }

        // 1. Título del paso.
        int position = steps.indexOf(step);
        Label title = new Label(labelFor(step, position));
        title.getStyleClass().add("step-title");
        title.setWrapText(true);

        // 2. Diagnóstico: qué se identificó en esta etapa.
        VBox diagnosticBox = new VBox(4);
        diagnosticBox.getStyleClass().add("diagnostic-box");
        Label diagnosis = new Label(step.getDescription());
        diagnosis.getStyleClass().add("diagnostic-text");
        diagnosis.setWrapText(true);
        diagnosticBox.getChildren().add(diagnosis);
        for (String detail : step.getDetails()) {
            Label bullet = new Label("• " + detail);
            bullet.getStyleClass().add("diagnostic-text");
            bullet.setWrapText(true);
            diagnosticBox.getChildren().add(bullet);
        }

        // 3. Transformaciones: diff lado a lado con TextFlow.
        VBox removedBox = buildDiffColumn("Producciones Eliminadas",
                step.getProductionsRemoved(), "diff-removed", "− ",
                "Sin producciones eliminadas.");
        VBox addedBox = buildDiffColumn("Producciones Agregadas",
                step.getProductionsAdded(), "diff-added", "+ ",
                "Sin producciones agregadas.");
        HBox diffRow = new HBox(16, removedBox, addedBox);

        // 4. Separador entre transformaciones y resultado.
        Separator separator = new Separator();

        // 5. Tarjeta con la gramática resultante G = (V, T, P, S).
        VBox resultCard = new VBox(6);
        resultCard.getStyleClass().add("result-card");
        Label resultTitle = new Label("Gramática Resultante");
        resultTitle.getStyleClass().add("section-title");
        TextFlow resultFlow = new TextFlow();
        resultFlow.getStyleClass().add("mono-flow");
        Grammar after = step.getGrammarAfter();
        String resultText = step.getCustomBody() != null
                ? step.getCustomBody()
                : (after == null ? "(sin gramática)" : GrammarFormatter.formatFull(after));
        resultFlow.getChildren().add(new Text(resultText));
        resultCard.getChildren().addAll(resultTitle, resultFlow);

        detailContent.getChildren().addAll(
                title, diagnosticBox, diffRow, separator, resultCard);
    }

    /**
     * Columna del diff: subtítulo + TextFlow con una línea por producción.
     */
    private VBox buildDiffColumn(String subtitle, List<String> productions,
                                 String styleClass, String prefix, String emptyMessage) {
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("diff-subtitle");
        subtitleLabel.setWrapText(true);
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("mono-flow");
        if (productions.isEmpty()) {
            Text empty = new Text(emptyMessage);
            empty.getStyleClass().add("diff-unchanged");
            flow.getChildren().add(empty);
        } else {
            for (String production : productions) {
                Text line = new Text(prefix + production + "\n");
                line.getStyleClass().add(styleClass);
                flow.getChildren().add(line);
            }
        }
        VBox column = new VBox(4, subtitleLabel, flow);
        HBox.setHgrow(column, Priority.ALWAYS);
        return column;
    }
}
