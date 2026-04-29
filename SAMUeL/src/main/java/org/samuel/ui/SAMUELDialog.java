package org.samuel.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import java.util.Optional;

/**
 * Main user dialog for SAMUeL configuration.
 */
public class SAMUELDialog {

    /**
     * Immutable dialog output.
     */
    public record Config(
            String modelType,
            int tileSize,
            int overlap,
            int minMaskArea,
            boolean useGpu,
            boolean saveMasks,
            String outputType,
            String targetMode,
            boolean usePromptClass,
            String backendUrl,
            boolean autoStartBackend,
            String pythonExe,
            String backendDir
    ) { }

    public Optional<Config> showAndWait(Runnable installDependenciesAction) {
        Dialog<Config> dialog = new Dialog<>();
        dialog.setTitle("SAMUeL configuration");
        ButtonType runButton = new ButtonType("Run", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(runButton, ButtonType.CANCEL);

        SettingsPanel panel = new SettingsPanel();
        panel.setOnInstallDependencies(installDependenciesAction);
        BorderPane root = new BorderPane(panel);
        dialog.getDialogPane().setContent(root);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == runButton) {
                return new Config(
                        panel.getModelType(),
                        panel.getTileSize(),
                        panel.getOverlap(),
                        panel.getMinArea(),
                        panel.isUseGpu(),
                        panel.isSaveMasks(),
                        panel.getOutputType(),
                        panel.getTargetMode(),
                        panel.isUsePromptClass(),
                        panel.getBackendUrl(),
                        panel.isAutoStartBackend(),
                        panel.getPythonExe(),
                        panel.getBackendDir()
                );
            }
            return null;
        });
        return dialog.showAndWait();
    }
}
