package org.samuel;

import javafx.application.Platform;
import org.controlsfx.control.action.Action;
import org.samuel.inference.SAMClient;
import org.samuel.inference.SAMRequest;
import org.samuel.inference.SAMResponse;
import org.samuel.objects.MaskDecoder;
import org.samuel.objects.MaskToPathObject;
import org.samuel.prompts.AnnotationPromptConverter;
import org.samuel.prompts.PromptBuilder;
import org.samuel.tiling.Tile;
import org.samuel.tiling.TileGrid;
import org.samuel.tiling.TileManager;
import org.samuel.ui.SAMUELDialog;
import org.samuel.ui.SetupWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.tools.MenuTools;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.ROIs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Main command handler exposed in QuPath menu.
 */
public class SAMUELCommand {

    private static final Logger logger = LoggerFactory.getLogger(SAMUELCommand.class);
    private static final Preferences PREFS = Preferences.userNodeForPackage(SAMUELCommand.class);
    private static final String PREF_VENV_PYTHON = "venvPython";
    private final QuPathGUI quPathGUI;
    private Process lastBackendProcess;

    public SAMUELCommand(QuPathGUI quPathGUI) {
        this.quPathGUI = quPathGUI;
    }

    public static void install(QuPathGUI quPathGUI) {
        SAMUELCommand command = new SAMUELCommand(quPathGUI);
        Action action = ActionTools.createAction(command::run, "Run SAM segmentation");
        MenuTools.addMenuItems(quPathGUI.getMenu("Extensions>SAMUeL", true), action);

        Action setupAction = ActionTools.createAction(command::showSetupWizard, "Setup Wizard");
        MenuTools.addMenuItems(quPathGUI.getMenu("Extensions>SAMUeL", true), setupAction);
    }

    private void showSetupWizard() {
        SetupWizard wizard = new SetupWizard();
        wizard.showAndWait();
    }

    private void run() {
        try {
            ImageData<BufferedImage> imageData = quPathGUI.getImageData();
            if (imageData == null) {
                logger.warn("No image is open");
                return;
            }

            PathObjectHierarchy hierarchy = imageData.getHierarchy();
            Collection<PathObject> annotationCollection = hierarchy.getAnnotationObjects();
            List<PathObject> annotations = new ArrayList<>(annotationCollection);
            SAMUELDialog dialog = new SAMUELDialog();
            var configOpt = dialog.showAndWait(() -> installBackendDependenciesInteractive());
            if (configOpt.isEmpty()) {
                return;
            }
            var config = configOpt.get();

            // Ask user what is being segmented
            TextInputDialog classificationDialog = new TextInputDialog("cell");
            classificationDialog.setTitle("SAMUeL Classification");
            classificationDialog.setHeaderText("What are we segmenting?");
            classificationDialog.setContentText("Enter the classification name for detected objects:");
            var classificationOpt = classificationDialog.showAndWait();
            if (classificationOpt.isEmpty()) {
                return;
            }
            String classificationName = classificationOpt.get().trim();
            if (classificationName.isEmpty()) {
                classificationName = "cell";
            }
            var pathClass = PathClass.getInstance(classificationName);

            List<PathObject> prompts = resolvePrompts(config, annotations, hierarchy);
            List<PathObject> targets = resolveTargets(config, annotations, imageData, hierarchy);
            if (targets.isEmpty()) {
                logger.warn("No target regions resolved for mode {}", config.targetMode());
                return;
            }

            AnnotationPromptConverter converter = new AnnotationPromptConverter();
            PromptBuilder promptBuilder = converter.fromAnnotations(prompts);

            Path resultsDir = createResultsDir();
            Path tilesDir = Files.createDirectories(resultsDir.resolve("tiles"));
            Path masksDir = Files.createDirectories(resultsDir.resolve("masks"));
            Files.createDirectories(resultsDir.resolve("logs"));
            Path manuscriptDir = Files.createDirectories(resultsDir.resolve("manuscript"));

            ImageServer<BufferedImage> server = imageData.getServer();
            SAMClient client = new SAMClient(config.backendUrl().isBlank() ? "http://127.0.0.1:8000" : config.backendUrl());
            ensureBackendAvailable(client, config);
            TileManager tileManager = new TileManager();
            MaskDecoder decoder = new MaskDecoder();
            MaskToPathObject converterToObject = new MaskToPathObject();
            int objectCount = 0;
            int tileCount = 0;

            for (PathObject target : targets) {
                if (target.getROI() == null) {
                    continue;
                }
                int x = (int) Math.floor(target.getROI().getBoundsX());
                int y = (int) Math.floor(target.getROI().getBoundsY());
                int w = (int) Math.ceil(target.getROI().getBoundsWidth());
                int h = (int) Math.ceil(target.getROI().getBoundsHeight());
                TileGrid grid = tileManager.generateGrid(x, y, w, h, config.tileSize(), config.overlap());
                for (Tile tile : grid.getTiles()) {
                    tileCount++;
                    BufferedImage tileImage = server.readRegion(
                            RegionRequest.createInstance(server.getPath(), 1.0, tile.x(), tile.y(), tile.width(), tile.height())
                    );
                    logger.debug("Processing tile {}: position ({}, {}), size {}x{}", tile.id(), tile.x(), tile.y(), tile.width(), tile.height());
                    if (config.saveMasks()) {
                        ImageIO.write(tileImage, "png", tilesDir.resolve(tile.id() + ".png").toFile());
                    }
                    SAMRequest request = buildRequest(tile, tileImage, promptBuilder, config);
                    SAMResponse response = client.segment(request);
                    for (SAMResponse.MaskPayload payload : response.getMasks()) {
                        logger.debug("Received mask: {}x{}, score: {}", payload.getWidth(), payload.getHeight(), payload.getScore());
                        boolean[][] mask = decoder.decode(payload.getData(), payload.getWidth(), payload.getHeight());
                        for (PathObject pathObject : converterToObject.convert(mask, tile.x(), tile.y(), config.outputType(), config.minMaskArea())) {
                            pathObject.setPathClass(pathClass);
                            hierarchy.addObjectBelowParent(target, pathObject, true);
                            objectCount++;
                        }
                        if (config.saveMasks()) {
                            Files.writeString(masksDir.resolve(tile.id() + "-" + objectCount + ".txt"), payload.getData());
                        }
                    }
                }
            }

            logger.info("SAMUeL finished with {} tiles and {} objects", tileCount, objectCount);
        } catch (Exception e) {
            logger.error("SAMUeL failed", e);
            showAlert(Alert.AlertType.ERROR, "SAMUeL error", e.getMessage() == null ? "Unexpected failure" : e.getMessage());
        }
    }

    private SAMRequest buildRequest(Tile tile, BufferedImage image, PromptBuilder promptBuilder, SAMUELDialog.Config config) throws IOException {
        SAMRequest request = new SAMRequest();
        request.setTileId(tile.id());
        request.setImage(toBase64Png(image));
        request.setModelType(config.modelType());
        request.setUseGpu(config.useGpu());

        for (List<Double> point : promptBuilder.getPoints()) {
            double gx = point.get(0);
            double gy = point.get(1);
            if (gx >= tile.x() && gx < tile.x() + tile.width() && gy >= tile.y() && gy < tile.y() + tile.height()) {
                request.getPoints().add(List.of(gx - tile.x(), gy - tile.y(), point.get(2)));
            }
        }
        for (List<Double> box : promptBuilder.getBoxes()) {
            double bx1 = box.get(0);
            double by1 = box.get(1);
            double bx2 = box.get(2);
            double by2 = box.get(3);
            if (bx2 >= tile.x() && by2 >= tile.y() && bx1 <= tile.x() + tile.width() && by1 <= tile.y() + tile.height()) {
                request.getBoxes().add(List.of(
                        Math.max(0, bx1 - tile.x()),
                        Math.max(0, by1 - tile.y()),
                        Math.min(tile.width(), bx2 - tile.x()),
                        Math.min(tile.height(), by2 - tile.y())
                ));
            }
        }
        return request;
    }

    private List<PathObject> resolvePrompts(SAMUELDialog.Config config, List<PathObject> annotations, PathObjectHierarchy hierarchy) {
        if (config.usePromptClass()) {
            return annotations.stream()
                    .filter(o -> o.getPathClass() != null
                            && o.getPathClass().getName() != null
                            && "prompt".equalsIgnoreCase(o.getPathClass().getName()))
                    .toList();
        }
        return new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
    }

    private List<PathObject> resolveTargets(SAMUELDialog.Config config, List<PathObject> annotations, ImageData<BufferedImage> imageData, PathObjectHierarchy hierarchy) {
        return switch (config.targetMode()) {
            case "all_annotations" -> annotations;
            case "current_annotation" -> {
                PathObject current = hierarchy.getSelectionModel().getSelectedObject();
                yield current == null ? List.of() : List.of(current);
            }
            case "whole_image" -> {
                double width = imageData.getServer().getWidth();
                double height = imageData.getServer().getHeight();
                yield List.of(PathObjects.createAnnotationObject(ROIs.createRectangleROI(0, 0, width, height, null)));
            }
            case "selected" -> new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
            default -> new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());
        };
    }

    private void ensureBackendAvailable(SAMClient client, SAMUELDialog.Config config) throws IOException, InterruptedException {
        if (client.isHealthy()) {
            return;
        }
        if (config.autoStartBackend()) {
            showAlert(Alert.AlertType.INFORMATION, "SAMUeL backend", "Attempting to start Python backend...");
            startBackendProcess(config);
            for (int i = 0; i < 30; i++) {  // Increased timeout to 30 seconds
                Thread.sleep(1000);
                if (client.isHealthy()) {
                    showAlert(Alert.AlertType.INFORMATION, "SAMUeL backend", "Python backend started successfully.");
                    return;
                }
                if (lastBackendProcess != null && !lastBackendProcess.isAlive()) {
                    break;
                }
            }
        }
        String diagnostics = readBackendProcessOutput();
        String errorMsg = "Cannot reach SAM backend at " + config.backendUrl() + ".\n\n";

        // Check backend directory
        try {
            Path backendDir = resolveBackendDirectory(config.backendDir());
            boolean hasServerPy = Files.exists(backendDir.resolve("server.py"));
            boolean hasRequirements = Files.exists(backendDir.resolve("requirements.txt"));
            errorMsg += "Backend directory: " + backendDir + "\n";
            errorMsg += "Has server.py: " + hasServerPy + "\n";
            errorMsg += "Has requirements.txt: " + hasRequirements + "\n\n";
        } catch (Exception e) {
            errorMsg += "Error resolving backend directory: " + e.getMessage() + "\n\n";
        }

        errorMsg += "Possible causes:\n";
        errorMsg += "1. Python backend is not running\n";
        errorMsg += "2. Wrong URL or port\n";
        errorMsg += "3. Python dependencies not installed\n";
        errorMsg += "4. Backend directory doesn't contain required files\n";
        errorMsg += "5. Python executable path is incorrect\n\n";
        errorMsg += "Solutions:\n";
        errorMsg += "- Use the Setup Wizard to configure paths\n";
        errorMsg += "- Copy python-backend files to your configured directory\n";
        errorMsg += "- Install dependencies: pip install -r requirements.txt\n";
        errorMsg += "- Start manually: python -m uvicorn server:app --host 127.0.0.1 --port 8000\n";
        if (!diagnostics.isBlank()) {
            errorMsg += "\nBackend startup log:\n" + diagnostics;
        }
        throw new IOException(errorMsg);
    }

    private void startBackendProcess(SAMUELDialog.Config config) throws IOException {
        String pythonExe = resolvePythonExecutable(config.pythonExe());
        Path backendDir = resolveBackendDirectory(config.backendDir());
        String[] hostPort = parseHostPort(config.backendUrl());
        List<String> command = new ArrayList<>();
        command.add(pythonExe);
        if ("py".equalsIgnoreCase(pythonExe)) {
            command.add("-3");
        }
        command.add("-m");
        command.add("uvicorn");
        command.add("server:app");
        command.add("--host");
        command.add(hostPort[0]);
        command.add("--port");
        command.add(hostPort[1]);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(backendDir.toFile());
        pb.redirectErrorStream(true);

        logger.info("Starting backend with command: {} in directory: {}", String.join(" ", command), backendDir);
        lastBackendProcess = pb.start();
    }

    private String resolvePythonExecutable(String configured) throws IOException {
        List<String> candidates = new ArrayList<>();
        String persistedVenv = PREFS.get(PREF_VENV_PYTHON, "").trim();
        if (!persistedVenv.isBlank()) {
            candidates.add(persistedVenv);
        }
        if (configured != null && !configured.isBlank()) {
            candidates.add(configured.trim());
        } else {
            candidates.add("python");
        }
        String userHome = System.getProperty("user.home");
        candidates.add(userHome + "\\AppData\\Local\\Programs\\Python\\Python313\\python.exe");
        candidates.add(userHome + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe");
        candidates.add(userHome + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe");
        candidates.add(userHome + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe");
        candidates.add("py");

        for (String candidate : candidates) {
            if (isPythonRunnable(candidate)) {
                return candidate;
            }
        }
        throw new IOException("No working Python executable found. Please set an absolute path to python.exe in SAMUeL settings.");
    }

    private boolean isPythonRunnable(String candidate) {
        try {
            ProcessBuilder pb;
            if ("py".equalsIgnoreCase(candidate)) {
                pb = new ProcessBuilder(candidate, "-3", "--version");
            } else {
                pb = new ProcessBuilder(candidate, "--version");
            }
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String readBackendProcessOutput() {
        if (lastBackendProcess == null) {
            return "";
        }
        if (lastBackendProcess.isAlive()) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(lastBackendProcess.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null && lines < 25) {
                sb.append(line).append(System.lineSeparator());
                lines++;
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void installBackendDependenciesInteractive() {
        Thread installer = new Thread(() -> {
            try {
                Path backendDir = resolveBackendDirectory("python-backend");
                Path requirements = backendDir.resolve("requirements.txt");
                if (!Files.exists(requirements)) {
                    throw new IOException("requirements.txt not found in backend directory: " + backendDir);
                }

                String basePython = resolvePythonExecutable("python");
                Path venvDir = getDefaultVenvDir();
                String venvPython = getVenvPythonPath(venvDir);

                if (!Files.exists(Path.of(venvPython))) {
                    List<String> createVenvCommand = new ArrayList<>();
                    createVenvCommand.add(basePython);
                    if ("py".equalsIgnoreCase(basePython)) {
                        createVenvCommand.add("-3");
                    }
                    createVenvCommand.add("-m");
                    createVenvCommand.add("venv");
                    createVenvCommand.add(venvDir.toAbsolutePath().toString());
                    runCommand(createVenvCommand, backendDir);
                }

                runCommand(List.of(venvPython, "-m", "pip", "install", "--upgrade", "pip"), backendDir);
                String output = runCommand(
                        List.of(venvPython, "-m", "pip", "install", "-r", requirements.toAbsolutePath().toString()),
                        backendDir
                );
                PREFS.put(PREF_VENV_PYTHON, venvPython);

                String message = "Backend dependencies installed in SAMUeL venv:\n" + venvPython;
                if (!output.isBlank()) {
                    String preview = output.lines().skip(Math.max(0, output.lines().count() - 10)).collect(Collectors.joining(System.lineSeparator()));
                    message += System.lineSeparator() + System.lineSeparator() + preview;
                }
                final String finalMessage = message;
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "SAMUeL setup", finalMessage));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(
                        Alert.AlertType.ERROR,
                        "Install dependencies failed",
                        e.getMessage() == null ? "Unknown error" : e.getMessage()
                ));
            }
        }, "samuel-install-dependencies");
        installer.setDaemon(true);
        installer.start();
    }

    private String runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            int code = p.waitFor();
            String out = sb.toString();
            if (code != 0) {
                throw new IOException("Command failed: " + String.join(" ", command) + System.lineSeparator() + out);
            }
            return out;
        }
    }

    private Path getDefaultVenvDir() {
        return Paths.get(System.getProperty("user.home"), ".samuel", "venv");
    }

    private String getVenvPythonPath(Path venvDir) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return venvDir.resolve("Scripts").resolve("python.exe").toString();
        }
        return venvDir.resolve("bin").resolve("python").toString();
    }

    private Path resolveBackendDirectory(String configuredDir) throws IOException {
        List<Path> candidates = new ArrayList<>();
        if (configuredDir != null && !configuredDir.isBlank()) {
            Path direct = Paths.get(configuredDir);
            candidates.add(direct);
            candidates.add(Paths.get(System.getProperty("user.dir")).resolve(configuredDir));
        } else {
            candidates.add(Paths.get("python-backend"));
            candidates.add(Paths.get(System.getProperty("user.dir")).resolve("python-backend"));
        }
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("server.py"))) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return extractBundledBackend();
    }

    private Path extractBundledBackend() throws IOException {
        String[] files = {"server.py", "sam_model.py", "tile_inference.py", "manuscript_generator.py", "requirements.txt"};
        Path backendDir = Paths.get(System.getProperty("user.home"), ".samuel", "python-backend");
        Files.createDirectories(backendDir);

        boolean needsExtraction = false;
        for (String file : files) {
            if (!Files.exists(backendDir.resolve(file))) {
                needsExtraction = true;
                break;
            }
        }

        if (needsExtraction) {
            for (String file : files) {
                try (InputStream inputStream = SAMUELCommand.class.getResourceAsStream("/python-backend/" + file)) {
                    if (inputStream == null) {
                        throw new IOException("Missing bundled backend resource: " + file);
                    }
                    Files.copy(inputStream, backendDir.resolve(file));
                }
            }
        }
        return backendDir;
    }

    private String[] parseHostPort(String backendUrl) {
        String fallbackHost = "127.0.0.1";
        String fallbackPort = "8000";
        if (backendUrl == null || backendUrl.isBlank()) {
            return new String[]{fallbackHost, fallbackPort};
        }
        try {
            var uri = java.net.URI.create(backendUrl);
            String host = uri.getHost() == null || uri.getHost().isBlank() ? fallbackHost : uri.getHost();
            int port = uri.getPort() <= 0 ? Integer.parseInt(fallbackPort) : uri.getPort();
            return new String[]{host, Integer.toString(port)};
        } catch (Exception e) {
            return new String[]{fallbackHost, fallbackPort};
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String toBase64Png(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private Path createResultsDir() throws IOException {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path dir = Path.of(System.getProperty("user.home"), "SAMUeL-results", stamp);
        return Files.createDirectories(dir);
    }
}
