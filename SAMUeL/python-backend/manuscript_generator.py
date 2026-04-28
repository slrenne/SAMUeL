from __future__ import annotations

import subprocess
from pathlib import Path
from typing import Any


def write_file(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def generate_manuscript(manuscript_dir: Path, stats: dict[str, Any]) -> None:
    manuscript_dir.mkdir(parents=True, exist_ok=True)
    write_file(
        manuscript_dir / "introduction.tex",
        "\\section{Introduction}\nSAMUeL enables annotation-guided SAM segmentation for microscopy.\n",
    )
    write_file(
        manuscript_dir / "methods.tex",
        "\\section{Architecture}\n"
        "The system includes:\n"
        "\\begin{itemize}\n"
        "\\item QuPath Java extension\n"
        "\\item FastAPI Python backend\n"
        "\\item Tile-based inference\n"
        "\\item Mask reconstruction\n"
        "\\end{itemize}\n",
    )
    write_file(
        manuscript_dir / "results.tex",
        "\\section{Usage Statistics}\n"
        f"Tiles processed: {stats.get('tiles', 0)}\\\\\n"
        f"Objects generated: {stats.get('objects', 0)}\\\\\n",
    )
    write_file(
        manuscript_dir / "discussion.tex",
        "\\section{Technical Details}\n"
        "SAM models supported: vit_h, vit_l, vit_b\\\\\n"
        "Processing: Overlapping tile strategy\\\\\n"
        "Output: QuPath-compatible objects\n",
    )
    write_file(
        manuscript_dir / "supplementary.tex",
        "\\section*{Configuration}\n"
        "Backend: FastAPI on port 8000\\\\\n"
        "Dependencies: PyTorch, segment-anything, OpenCV\\\\\n"
        "Integration: HTTP REST API\n",
    )
    write_file(
        manuscript_dir / "references.bib",
        "@article{sam2023,\n  title={Segment Anything},\n  author={Kirillov, A. and others},\n  year={2023}\n}\n"
        "@software{qupath,\n  title={QuPath},\n  author={Bankhead, P. and others},\n  year={2017}\n}\n",
    )
    write_file(
        manuscript_dir / "manuscript.tex",
        "\\documentclass{article}\n"
        "\\usepackage[margin=1in]{geometry}\n"
        "\\begin{document}\n"
        "\\title{SAMUeL Technical Documentation}\n\\maketitle\n"
        "\\input{introduction.tex}\n"
        "\\input{methods.tex}\n"
        "\\input{results.tex}\n"
        "\\input{discussion.tex}\n"
        "\\input{supplementary.tex}\n"
        "\\bibliographystyle{plain}\n\\bibliography{references}\n"
        "\\end{document}\n",
    )
    subprocess.run(
        ["pdflatex", "manuscript.tex"],
        cwd=str(manuscript_dir),
        check=False,
        capture_output=True,
        text=True,
    )
