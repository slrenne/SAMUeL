package org.samuel.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Setup wizard for SAMUeL Python backend and SAM models.
 */
public class SetupWizard extends Dialog<Void> {

    private final TextField pythonPathField = new TextField();
    private final TextField backendDirField = new TextField();
    private final CheckBox installDepsCheck = new CheckBox("Install Python dependencies");
    private final CheckBox downloadModelsCheck = new CheckBox("Download SAM models");

    public SetupWizard() {
        setTitle("SAMUeL Setup Wizard");
        setHeaderText("Configure Python backend and download models");

        ButtonType setupButton = new ButtonType("Setup", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(setupButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Python executable
        GridPane pythonGrid = new GridPane();
        pythonGrid.setHgap(8);
        pythonGrid.setVgap(8);
        pythonGrid.add(new Label("Python executable:"), 0, 0);
        pythonGrid.add(pythonPathField, 1, 0);
        Button browsePython = new Button("Browse...");
        browsePython.setOnAction(e -> browsePythonExecutable());
        pythonGrid.add(browsePython, 2, 0);

        // Backend directory
        GridPane backendGrid = new GridPane();
        backendGrid.setHgap(8);
        backendGrid.setVgap(8);
        backendGrid.add(new Label("Backend directory:"), 0, 0);
        backendGrid.add(backendDirField, 1, 0);
        Button browseBackend = new Button("Browse...");
        browseBackend.setOnAction(e -> browseBackendDirectory());
        backendGrid.add(browseBackend, 2, 0);

        // Options
        installDepsCheck.setSelected(true);
        downloadModelsCheck.setSelected(true);

        content.getChildren().addAll(
            new Label("This wizard will help you set up the Python backend for SAMUeL."),
            pythonGrid,
            backendGrid,
            new Separator(),
            installDepsCheck,
            downloadModelsCheck
        );

        getDialogPane().setContent(content);

        // Auto-detect Python
        autoDetectPython();

        // Auto-detect backend dir
        autoDetectBackendDir();

        setResultConverter(buttonType -> {
            if (buttonType == setupButton) {
                performSetup();
            }
            return null;
        });
    }

    private void browsePythonExecutable() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Python Executable");
        File file = chooser.showOpenDialog(getOwner());
        if (file != null) {
            pythonPathField.setText(file.getAbsolutePath());
        }
    }

    private void browseBackendDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backend Directory");
        File dir = chooser.showDialog(getOwner());
        if (dir != null) {
            backendDirField.setText(dir.getAbsolutePath());
        }
    }

    private void autoDetectPython() {
        // Try common locations
        String[] candidates = {
            "python",
            "python3",
            System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python313\\python.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
            System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe"
        };

        for (String candidate : candidates) {
            if (isPythonAvailable(candidate)) {
                pythonPathField.setText(candidate);
                break;
            }
        }
    }

    private void autoDetectBackendDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        Path backendDir = currentDir.resolve("python-backend");
        if (Files.exists(backendDir)) {
            backendDirField.setText(backendDir.toString());
        }
    }

    private boolean isPythonAvailable(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void performSetup() {
        String message = "Setup configuration:\n";
        message += "Python: " + pythonPathField.getText() + "\n";
        message += "Backend: " + backendDirField.getText() + "\n";
        message += "Install deps: " + installDepsCheck.isSelected() + "\n";
        message += "Download models: " + downloadModelsCheck.isSelected() + "\n\n";

        // Copy backend files if directory is specified
        if (!backendDirField.getText().trim().isEmpty()) {
            try {
                java.nio.file.Path targetDir = java.nio.file.Paths.get(backendDirField.getText().trim());
                java.nio.file.Files.createDirectories(targetDir);

                String[] files = {"server.py", "sam_model.py", "tile_inference.py", "manuscript_generator.py", "requirements.txt"};
                for (String file : files) {
                    try (java.io.InputStream inputStream = getClass().getResourceAsStream("/python-backend/" + file)) {
                        if (inputStream != null) {
                            java.nio.file.Files.copy(inputStream, targetDir.resolve(file), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            message += "Copied " + file + " to " + targetDir + "\n";
                        }
                    }
                }
                message += "\nBackend files copied successfully!\n";
            } catch (Exception e) {
                message += "Error copying backend files: " + e.getMessage() + "\n";
            }
        }

        if (installDepsCheck.isSelected()) {
            message += "\nTo install dependencies manually:\n";
            message += "1. Create virtual environment: " + pythonPathField.getText() + " -m venv .venv\n";
            message += "2. Activate: .venv\\Scripts\\Activate.ps1 (Windows) or source .venv/bin/activate (Linux/Mac)\n";
            message += "3. Install: pip install -r requirements.txt\n";
        }

        if (downloadModelsCheck.isSelected()) {
            message += "\nSAM models will be downloaded automatically on first use.\n";
        }

        message += "\nTo start the backend manually:\n";
        message += "cd \"" + backendDirField.getText() + "\"\n";
        message += pythonPathField.getText() + " -m uvicorn server:app --host 127.0.0.1 --port 8000\n";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("SAMUeL Setup");
        alert.setHeaderText("Setup configuration saved");
        alert.setContentText(message);
        alert.showAndWait();
    }
}