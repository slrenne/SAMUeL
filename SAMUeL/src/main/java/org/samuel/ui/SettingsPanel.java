package org.samuel.ui;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

/**
 * JavaFX settings panel for SAMUeL runtime options.
 */
public class SettingsPanel extends GridPane {

    private final ComboBox<String> modelTypeCombo = new ComboBox<>();
    private final Spinner<Integer> tileSizeSpinner = new Spinner<>(256, 4096, 1024, 64);
    private final Spinner<Integer> overlapSpinner = new Spinner<>(0, 1024, 128, 16);
    private final Spinner<Integer> minAreaSpinner = new Spinner<>(0, Integer.MAX_VALUE, 100, 10);
    private final CheckBox useGpuCheck = new CheckBox("Use GPU");
    private final CheckBox saveMasksCheck = new CheckBox("Save masks to disk");
    private final CheckBox generateManuscriptCheck = new CheckBox("Generate manuscript");
    private final ComboBox<String> outputTypeCombo = new ComboBox<>();
    private final ComboBox<String> targetModeCombo = new ComboBox<>();
    private final CheckBox usePromptClassCheck = new CheckBox("Use annotations labeled 'prompt'");
    private final TextField backendUrlField = new TextField("http://127.0.0.1:8000");
    private final Button installDepsButton = new Button("Install backend dependencies");
    private final CheckBox autoStartBackendCheck = new CheckBox("Auto-start Python backend");
    private final TextField pythonExeField = new TextField("python");
    private final TextField backendDirField = new TextField("python-backend");

    public SettingsPanel() {
        setHgap(8);
        setVgap(8);
        setPadding(new Insets(10));

        modelTypeCombo.getItems().addAll("vit_h", "vit_l", "vit_b");
        modelTypeCombo.setValue("vit_h");
        outputTypeCombo.getItems().addAll("detections", "annotations");
        outputTypeCombo.setValue("detections");
        targetModeCombo.getItems().addAll("selected", "all_annotations", "current_annotation", "whole_image");
        targetModeCombo.setValue("selected");
        useGpuCheck.setSelected(true);
        saveMasksCheck.setSelected(true);
        generateManuscriptCheck.setSelected(true);
        usePromptClassCheck.setText("Use annotations with class 'Prompt' or 'prompt'");
        usePromptClassCheck.setSelected(true);
        autoStartBackendCheck.setSelected(false);

        int row = 0;
        add(new Label("SAM model type"), 0, row);
        add(modelTypeCombo, 1, row++);
        add(new Label("Tile size"), 0, row);
        add(tileSizeSpinner, 1, row++);
        add(new Label("Tile overlap"), 0, row);
        add(overlapSpinner, 1, row++);
        add(new Label("Minimum mask area"), 0, row);
        add(minAreaSpinner, 1, row++);
        add(new Label("Output object type"), 0, row);
        add(outputTypeCombo, 1, row++);
        add(new Label("Target regions"), 0, row);
        add(targetModeCombo, 1, row++);
        add(new Label("Backend URL"), 0, row);
        add(backendUrlField, 1, row++);
        add(installDepsButton, 1, row++);
        add(autoStartBackendCheck, 1, row++);
        add(new Label("Python executable"), 0, row);
        add(pythonExeField, 1, row++);
        add(new Label("Backend directory"), 0, row);
        add(backendDirField, 1, row++);
        add(usePromptClassCheck, 1, row++);
        add(useGpuCheck, 1, row++);
        add(saveMasksCheck, 1, row++);
        add(generateManuscriptCheck, 1, row);
    }

    public String getModelType() {
        return modelTypeCombo.getValue();
    }

    public int getTileSize() {
        return tileSizeSpinner.getValue();
    }

    public int getOverlap() {
        return overlapSpinner.getValue();
    }

    public int getMinArea() {
        return minAreaSpinner.getValue();
    }

    public boolean isUseGpu() {
        return useGpuCheck.isSelected();
    }

    public boolean isSaveMasks() {
        return saveMasksCheck.isSelected();
    }

    public boolean isGenerateManuscript() {
        return generateManuscriptCheck.isSelected();
    }

    public String getOutputType() {
        return outputTypeCombo.getValue();
    }

    public String getTargetMode() {
        return targetModeCombo.getValue();
    }

    public boolean isUsePromptClass() {
        return usePromptClassCheck.isSelected();
    }

    public String getBackendUrl() {
        return backendUrlField.getText().trim();
    }

    public boolean isAutoStartBackend() {
        return autoStartBackendCheck.isSelected();
    }

    public String getPythonExe() {
        return pythonExeField.getText().trim();
    }

    public String getBackendDir() {
        return backendDirField.getText().trim();
    }

    public void setOnInstallDependencies(Runnable runnable) {
        installDepsButton.setOnAction(e -> runnable.run());
    }
}
