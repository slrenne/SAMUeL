# SAMUeL

Segment Anything for Microscopy Using Labels.

## Overview

SAMUeL is a QuPath extension that runs Segment Anything (SAM) inference from annotation prompts and supports large whole-slide images by tiled processing. It includes:

- A Java 17 QuPath extension with user-friendly GUI configuration.
- A FastAPI Python backend running SAM inference.
- Prompt conversion from QuPath annotations.
- Tile extraction, overlap-aware processing, and mask reconstruction.
- Automatic output export plus technical documentation generation.

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
4. Use menu: `Extensions -> SAMUeL -> Setup Wizard` to configure Python and download models.
5. Use menu: `Extensions -> SAMUeL -> Run SAM segmentation`.

## Setup Wizard

The setup wizard helps you:

- Locate your Python installation
- Set the backend directory (files are copied automatically)
- Install Python dependencies automatically
- Download SAM model weights

Run it from `Extensions -> SAMUeL -> Setup Wizard` before first use. The wizard will copy the required Python backend files to your chosen directory.

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

- QuPath objects (detections or annotations) with accurate polygon shapes.
- Optional mask images and data saved to disk.
- Technical documentation PDF generated with run statistics.

## Recent Improvements

- **Accurate segmentation shapes**: Masks are now converted to precise polygon ROIs instead of bounding boxes.
- **Enhanced GUI**: Organized settings with tooltips and collapsible sections.
- **Setup Wizard**: Automated Python environment setup and SAM model downloads.
- **Technical documentation**: Generates PDF reports describing the plugin usage and results.
- **Bug fixes**: Fixed deprecated QuPath API usage and improved error handling for backend connections.

## Troubleshooting

### Backend Connection Issues

If you see "Connection reset by peer" or "Cannot reach SAM backend":

1. **Use Setup Wizard first**: `Extensions > SAMUeL > Setup Wizard` to configure paths and copy backend files
2. **Check if backend is running**: Open a browser to `http://127.0.0.1:8000/health`
3. **Start backend manually**:
   ```bash
   cd python-backend  # or your configured backend directory
   python -m venv .venv
   .venv\Scripts\activate  # Windows
   pip install -r requirements.txt
   python -m uvicorn server:app --host 127.0.0.1 --port 8000
   ```
4. **Check Python version**: Ensure Python 3.10+ is used
5. **Check dependencies**: Run `pip list` to verify torch, segment-anything are installed

### Common Issues

- **"No Python executable found"**: Set full path to python.exe in settings
- **"Model weights not found"**: Models download automatically on first use (~2.5GB)
- **"CUDA out of memory"**: Reduce tile size or disable GPU in settings
- **"pdflatex not found"**: Install LaTeX for PDF generation (optional)

When enabled:

- Generates `introduction.tex`, `methods.tex`, `results.tex`, `discussion.tex`, `supplementary.tex`, `references.bib`, `manuscript.tex`.
- Populates `results.tex` with run statistics.
- Executes `pdflatex manuscript.tex`.

Template files are provided in `templates/manuscript/`.
