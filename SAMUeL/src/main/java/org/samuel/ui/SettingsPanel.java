package org.samuel.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * JavaFX settings panel for SAMUeL runtime options.
 */
public class SettingsPanel extends VBox {

    private final ComboBox<String> modelTypeCombo = new ComboBox<>();
    private final Spinner<Integer> tileSizeSpinner = new Spinner<>(256, 4096, 1024, 64);
    private final Spinner<Integer> overlapSpinner = new Spinner<>(0, 1024, 128, 16);
    private final Spinner<Integer> minAreaSpinner = new Spinner<>(0, Integer.MAX_VALUE, 100, 10);
    private final CheckBox useGpuCheck = new CheckBox("Use GPU (if available)");
    private final CheckBox saveMasksCheck = new CheckBox("Save masks to disk");
    private final ComboBox<String> outputTypeCombo = new ComboBox<>();
    private final ComboBox<String> targetModeCombo = new ComboBox<>();
    private final CheckBox usePromptClassCheck = new CheckBox("Use annotations with class 'Prompt' or 'prompt'");
    private final TextField backendUrlField = new TextField("http://127.0.0.1:8000");
    private final Button installDepsButton = new Button("Install Python Dependencies");
    private final CheckBox autoStartBackendCheck = new CheckBox("Auto-start Python backend");
    private final TextField pythonExeField = new TextField("python");
    private final TextField backendDirField = new TextField("python-backend");

    public SettingsPanel() {
        setSpacing(10);
        setPadding(new Insets(10));

        // Model settings
        TitledPane modelPane = new TitledPane("SAM Model", createModelGrid());
        modelPane.setCollapsible(false);

        // Processing settings
        TitledPane processingPane = new TitledPane("Processing", createProcessingGrid());
        processingPane.setCollapsible(false);

        // Output settings
        TitledPane outputPane = new TitledPane("Output", createOutputGrid());
        outputPane.setCollapsible(false);

        // Backend settings
        TitledPane backendPane = new TitledPane("Python Backend", createBackendGrid());
        backendPane.setCollapsible(false);

        getChildren().addAll(modelPane, processingPane, outputPane, backendPane);

        // Initialize values
        modelTypeCombo.getItems().addAll("vit_h", "vit_l", "vit_b");
        modelTypeCombo.setValue("vit_h");
        modelTypeCombo.setTooltip(new Tooltip("SAM model size: vit_h (huge) for best quality, vit_b (base) for speed"));

        outputTypeCombo.getItems().addAll("detections", "annotations");
        outputTypeCombo.setValue("detections");
        outputTypeCombo.setTooltip(new Tooltip("Output as detections (for counting) or annotations (for further editing)"));

        targetModeCombo.getItems().addAll("selected", "all_annotations", "current_annotation", "whole_image");
        targetModeCombo.setValue("selected");
        targetModeCombo.setTooltip(new Tooltip("Which regions to segment: selected objects, all annotations, current selection, or entire image"));

        useGpuCheck.setSelected(true);
        useGpuCheck.setTooltip(new Tooltip("Use CUDA GPU if available for faster inference"));

        saveMasksCheck.setSelected(true);
        saveMasksCheck.setTooltip(new Tooltip("Save raw mask images and data for debugging"));

        usePromptClassCheck.setText("Use annotations with class 'Prompt' or 'prompt'");
        usePromptClassCheck.setSelected(true);
        usePromptClassCheck.setTooltip(new Tooltip("Use annotations labeled as prompts instead of selected objects"));

        backendUrlField.setTooltip(new Tooltip("URL of the Python FastAPI backend"));
        autoStartBackendCheck.setSelected(false);
        autoStartBackendCheck.setTooltip(new Tooltip("Automatically start the Python backend if not running"));
        pythonExeField.setTooltip(new Tooltip("Path to Python executable (leave as 'python' for auto-detection)"));
        backendDirField.setTooltip(new Tooltip("Directory containing Python backend files"));
        installDepsButton.setTooltip(new Tooltip("Install Python dependencies in a virtual environment"));
    }

    private GridPane createModelGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Model type:"), 0, 0);
        grid.add(modelTypeCombo, 1, 0);
        grid.add(useGpuCheck, 1, 1);
        return grid;
    }

    private GridPane createProcessingGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Tile size:"), 0, 0);
        grid.add(tileSizeSpinner, 1, 0);
        grid.add(new Label("Tile overlap:"), 0, 1);
        grid.add(overlapSpinner, 1, 1);
        grid.add(new Label("Min mask area:"), 0, 2);
        grid.add(minAreaSpinner, 1, 2);
        grid.add(new Label("Target regions:"), 0, 3);
        grid.add(targetModeCombo, 1, 3);
        grid.add(usePromptClassCheck, 1, 4);
        return grid;
    }

    private GridPane createOutputGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Output type:"), 0, 0);
        grid.add(outputTypeCombo, 1, 0);
        grid.add(saveMasksCheck, 1, 1);
        return grid;
    }

    private GridPane createBackendGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Backend URL:"), 0, 0);
        grid.add(backendUrlField, 1, 0);
        grid.add(autoStartBackendCheck, 1, 1);
        grid.add(new Label("Python exe:"), 0, 2);
        grid.add(pythonExeField, 1, 2);
        grid.add(new Label("Backend dir:"), 0, 3);
        grid.add(backendDirField, 1, 3);
        grid.add(installDepsButton, 1, 4);
        return grid;
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
