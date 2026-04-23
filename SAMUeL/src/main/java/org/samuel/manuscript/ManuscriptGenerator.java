package org.samuel.manuscript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Writes a manuscript scaffold and compiles it via pdflatex.
 */
public class ManuscriptGenerator {

    public void generate(Path manuscriptDir, Map<String, Object> stats) throws IOException, InterruptedException {
        Files.createDirectories(manuscriptDir);
        write(manuscriptDir.resolve("introduction.tex"), """
                \\section{Introduction}
                SAMUeL integrates Segment Anything in QuPath for annotation-prompted microscopy segmentation.
                """);
        write(manuscriptDir.resolve("methods.tex"), """
                \\section{Methods}
                The workflow used annotation prompts, tile-based extraction, FastAPI model inference, and mask fusion.
                """);
        write(manuscriptDir.resolve("results.tex"), """
                \\section{Results}
                Processed tiles: %d\\\\
                Generated objects: %d\\\\
                """.formatted(stats.getOrDefault("tiles", 0), stats.getOrDefault("objects", 0)));
        write(manuscriptDir.resolve("discussion.tex"), """
                \\section{Discussion}
                Tile overlap reduces border artifacts while preserving throughput.
                """);
        write(manuscriptDir.resolve("supplementary.tex"), """
                \\section*{Supplementary}
                Additional qualitative overlays are available in the results directory.
                """);
        write(manuscriptDir.resolve("references.bib"), """
                @article{kirillov2023segment,
                  title={Segment Anything},
                  author={Kirillov, Alexander and others},
                  journal={arXiv preprint arXiv:2304.02643},
                  year={2023}
                }
                """);
        write(manuscriptDir.resolve("manuscript.tex"), """
                \\documentclass{article}
                \\usepackage[margin=1in]{geometry}
                \\usepackage{graphicx}
                \\begin{document}
                \\title{SAMUeL: Segment Anything for Microscopy Using Labels}
                \\author{Automated report}
                \\maketitle
                \\input{introduction.tex}
                \\input{methods.tex}
                \\input{results.tex}
                \\input{discussion.tex}
                \\input{supplementary.tex}
                \\bibliographystyle{plain}
                \\bibliography{references}
                \\end{document}
                """);
        ProcessBuilder pb = new ProcessBuilder("pdflatex", "manuscript.tex");
        pb.directory(manuscriptDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
    }

    private void write(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }
}
