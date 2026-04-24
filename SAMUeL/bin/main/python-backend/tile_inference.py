from __future__ import annotations

import base64
from dataclasses import dataclass
from io import BytesIO
from typing import Any

import numpy as np
from PIL import Image

from sam_model import SAMModelManager


@dataclass
class TileRequest:
    tile_id: str
    image_b64: str
    points: list[list[float]]
    boxes: list[list[float]]


def decode_image(image_b64: str) -> np.ndarray:
    raw = base64.b64decode(image_b64)
    img = Image.open(BytesIO(raw)).convert("RGB")
    return np.array(img)


def encode_mask(mask: np.ndarray) -> str:
    return base64.b64encode(mask.astype(np.uint8).flatten().tobytes()).decode("utf-8")


def run_tile_inference(request: TileRequest, model_manager: SAMModelManager) -> dict[str, Any]:
    predictor = model_manager.get_predictor()
    image = decode_image(request.image_b64)
    predictor.set_image(image)

    point_coords = None
    point_labels = None
    box = None

    if request.points:
        arr = np.array(request.points, dtype=np.float32)
        point_coords = arr[:, :2]
        point_labels = arr[:, 2].astype(np.int32)

    if request.boxes:
        box = np.array(request.boxes[0], dtype=np.float32)

    masks, scores, _ = predictor.predict(
        point_coords=point_coords,
        point_labels=point_labels,
        box=box,
        multimask_output=True,
    )

    serialized = []
    for i in range(masks.shape[0]):
        m = masks[i]
        serialized.append(
            {
                "width": int(m.shape[1]),
                "height": int(m.shape[0]),
                "data": encode_mask(m),
                "score": float(scores[i]),
            }
        )

    return {"tile_id": request.tile_id, "masks": serialized}
