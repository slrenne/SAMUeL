package org.samuel;

import org.controlsfx.control.action.Action;
import org.samuel.inference.SAMClient;
import org.samuel.inference.SAMRequest;
import org.samuel.inference.SAMResponse;
import org.samuel.manuscript.ManuscriptGenerator;
import org.samuel.objects.MaskDecoder;
import org.samuel.objects.MaskToPathObject;
import org.samuel.prompts.AnnotationPromptConverter;
import org.samuel.prompts.PromptBuilder;
import org.samuel.tiling.Tile;
import org.samuel.tiling.TileGrid;
import org.samuel.tiling.TileManager;
import org.samuel.ui.SAMUELDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Main command handler exposed in QuPath menu.
 */
public class SAMUELCommand {

    private static final Logger logger = LoggerFactory.getLogger(SAMUELCommand.class);
    private final QuPathGUI quPathGUI;

    public SAMUELCommand(QuPathGUI quPathGUI) {
        this.quPathGUI = quPathGUI;
    }

    public static void install(QuPathGUI quPathGUI) {
        SAMUELCommand command = new SAMUELCommand(quPathGUI);
        Action action = ActionTools.createAction(command::run, "Run SAM segmentation");
        MenuTools.addMenuItems(quPathGUI.getMenu("Extensions>SAMUeL", true), action);
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
            var configOpt = dialog.showAndWait();
            if (configOpt.isEmpty()) {
                return;
            }
            var config = configOpt.get();
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
                    BufferedImage tileImage = server.readBufferedImage(
                            RegionRequest.createInstance(server.getPath(), 1.0, tile.x(), tile.y(), tile.width(), tile.height())
                    );
                    if (config.saveMasks()) {
                        ImageIO.write(tileImage, "png", tilesDir.resolve(tile.id() + ".png").toFile());
                    }
                    SAMRequest request = buildRequest(tile, tileImage, promptBuilder, config);
                    SAMResponse response = client.segment(request);
                    for (SAMResponse.MaskPayload payload : response.getMasks()) {
                        boolean[][] mask = decoder.decode(payload.getData(), payload.getWidth(), payload.getHeight());
                        for (PathObject pathObject : converterToObject.convert(mask, tile.x(), tile.y(), config.outputType(), config.minMaskArea())) {
                            hierarchy.addObject(pathObject);
                            objectCount++;
                        }
                        if (config.saveMasks()) {
                            Files.writeString(masksDir.resolve(tile.id() + "-" + objectCount + ".txt"), payload.getData());
                        }
                    }
                }
            }

            if (config.generateManuscript()) {
                new ManuscriptGenerator().generate(manuscriptDir, Map.of("tiles", tileCount, "objects", objectCount));
            }

            logger.info("SAMUeL finished with {} tiles and {} objects", tileCount, objectCount);
        } catch (Exception e) {
            logger.error("SAMUeL failed", e);
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
                    .filter(o -> o.getPathClass() != null && "prompt".equalsIgnoreCase(o.getPathClass().toString()))
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
