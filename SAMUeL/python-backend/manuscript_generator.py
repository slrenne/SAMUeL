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
        "\\section{Methods}\nThe pipeline performs prompt conversion, tile-based inference, and mask reconstruction.\n",
    )
    write_file(
        manuscript_dir / "results.tex",
        "\\section{Results}\n"
        f"Processed tiles: {stats.get('tiles', 0)}\\\\\n"
        f"Generated masks: {stats.get('masks', 0)}\\\\\n",
    )
    write_file(
        manuscript_dir / "discussion.tex",
        "\\section{Discussion}\nTile overlap mitigated edge artifacts in large WSI regions.\n",
    )
    write_file(
        manuscript_dir / "supplementary.tex",
        "\\section*{Supplementary}\nExample masks and overlays are exported with this run.\n",
    )
    write_file(
        manuscript_dir / "references.bib",
        "@article{sam2023,\n  title={Segment Anything},\n  author={Kirillov, A. and others},\n  year={2023}\n}\n",
    )
    write_file(
        manuscript_dir / "manuscript.tex",
        "\\documentclass{article}\n"
        "\\usepackage[margin=1in]{geometry}\n"
        "\\begin{document}\n"
        "\\title{SAMUeL Manuscript}\n\\maketitle\n"
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
