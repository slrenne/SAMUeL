package org.samuel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

/**
 * Entry point for the SAMUeL QuPath extension.
 */
public class SAMUELExtension implements QuPathExtension {

    private static final Logger logger = LoggerFactory.getLogger(SAMUELExtension.class);

    @Override
    public void installExtension(QuPathGUI quPathGUI) {
        logger.info("Installing SAMUeL extension");
        SAMUELCommand.install(quPathGUI);
    }

    @Override
    public String getName() {
        return "SAMUeL";
    }

    @Override
    public String getDescription() {
        return "Segment Anything for Microscopy Using Labels";
    }
}
