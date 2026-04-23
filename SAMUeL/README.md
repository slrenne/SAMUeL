# SAMUeL

Segment Anything for Microscopy Using Labels.

## Overview

SAMUeL is a QuPath extension that runs Segment Anything (SAM) inference from annotation prompts and supports large whole-slide images by tiled processing. It includes:

- A Java 17 QuPath extension with GUI configuration.
- A FastAPI Python backend running SAM inference.
- Prompt conversion from QuPath annotations.
- Tile extraction, overlap-aware processing, and mask reconstruction.
- Automatic output export plus manuscript-ready LaTeX report generation.

## Installation

### Prerequisites

- Java 17+
- Gradle 8+
- QuPath 0.5.x
- Python 3.10+
- `pdflatex` in PATH (optional but recommended for PDF generation)

### Build extension

```bash
./gradlew clean test build
```

On Windows PowerShell:

```powershell
gradlew.bat clean test build
```

## Running the Python backend

```bash
cd python-backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn server:app --host 127.0.0.1 --port 8000
```

On Windows PowerShell:

```powershell
cd python-backend
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn server:app --host 127.0.0.1 --port 8000
```

## Installing the QuPath extension

1. Build the project.
2. Copy the generated JAR from `build/libs/` into the QuPath extensions directory.
3. Restart QuPath.
4. Use menu: `Extensions -> SAMUeL -> Run SAM segmentation`.

## Usage guide

1. Draw prompt annotations and assign class name `prompt`.
2. Draw target annotations (regions to segment) or select existing annotations.
3. Launch `Run SAM segmentation`.
4. Configure SAM model, tile settings, thresholds, output type, and report generation.
5. Run segmentation and inspect generated QuPath objects.

## Architecture

- `org.samuel.SAMUELExtension`: QuPath extension entry point.
- `org.samuel.SAMUELCommand`: orchestration pipeline.
- `org.samuel.tiling`: tile generation and overlap handling.
- `org.samuel.prompts`: QuPath annotation to SAM prompt conversion.
- `org.samuel.inference`: REST request/response + HTTP client.
- `org.samuel.objects`: mask decoding and QuPath object creation.
- `python-backend/`: SAM inference server and manuscript tooling.

## Tiling strategy

- Default tile size: `1024`.
- Default overlap: `128`.
- Tiles are generated over selected target regions.
- Prompts are transformed to tile-local coordinates.
- Returned masks are shifted back to global coordinates.

## Prompt system

Prompt mapping:

- Point ROI -> positive point prompt.
- Rectangle-like ROI -> box prompt.
- Polygon ROI -> sampled points along polygon vertices.
- Multiple prompt annotations -> multi-prompt SAM inference payload.

## Results output

Each run creates `~/SAMUeL-results/<timestamp>/` with:

- `tiles/`: extracted tile images.
- `masks/`: serialized output masks.
- `logs/`: run artifacts.
- `manuscript/`: LaTeX files and compiled PDF.

## Manuscript generation

When enabled:

- Generates `introduction.tex`, `methods.tex`, `results.tex`, `discussion.tex`, `supplementary.tex`, `references.bib`, `manuscript.tex`.
- Populates `results.tex` with run statistics.
- Executes `pdflatex manuscript.tex`.

Template files are provided in `templates/manuscript/`.
