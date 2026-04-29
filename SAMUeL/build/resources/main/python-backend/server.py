from __future__ import annotations

from pathlib import Path
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field

from sam_model import SAMModelManager
from tile_inference import TileRequest, run_tile_inference


class SegmentRequest(BaseModel):
    tile_id: str
    image: str
    points: list[list[float]] = Field(default_factory=list)
    boxes: list[list[float]] = Field(default_factory=list)
    model_type: str = "vit_h"
    use_gpu: bool = True


class SegmentResponse(BaseModel):
    tile_id: str
    masks: list[dict[str, Any]]


app = FastAPI(title="SAMUeL SAM Backend", version="0.1.0")
MODEL_MANAGERS: dict[tuple[str, bool], SAMModelManager] = {}


def get_manager(model_type: str, use_gpu: bool) -> SAMModelManager:
    key = (model_type, use_gpu)
    if key not in MODEL_MANAGERS:
        MODEL_MANAGERS[key] = SAMModelManager(model_type=model_type, use_gpu=use_gpu)
    return MODEL_MANAGERS[key]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/segment", response_model=SegmentResponse)
def segment(req: SegmentRequest) -> SegmentResponse:
    manager = get_manager(req.model_type, req.use_gpu)
    payload = TileRequest(
        tile_id=req.tile_id,
        image_b64=req.image,
        points=req.points,
        boxes=req.boxes,
    )
    result = run_tile_inference(payload, manager)
    return SegmentResponse(**result)
