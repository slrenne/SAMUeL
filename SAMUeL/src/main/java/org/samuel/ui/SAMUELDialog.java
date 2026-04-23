package org.samuel.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import org.samuel.prompts.PromptBuilder;

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
            boolean generateManuscript,
            String outputType
    ) { }

    public Optional<Config> showAndWait(PromptBuilder promptBuilder) {
        Dialog<Config> dialog = new Dialog<>();
        dialog.setTitle("SAMUeL configuration");
        ButtonType runButton = new ButtonType("Run", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(runButton, ButtonType.CANCEL);

        SettingsPanel panel = new SettingsPanel();
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
                        panel.isGenerateManuscript(),
                        panel.getOutputType()
                );
            }
            return null;
        });
        return dialog.showAndWait();
    }
}
