from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import torch
from segment_anything import SamPredictor, sam_model_registry


MODEL_URLS = {
    "vit_h": "https://dl.fbaipublicfiles.com/segment_anything/sam_vit_h_4b8939.pth",
    "vit_l": "https://dl.fbaipublicfiles.com/segment_anything/sam_vit_l_0b3195.pth",
    "vit_b": "https://dl.fbaipublicfiles.com/segment_anything/sam_vit_b_01ec64.pth",
}


class SAMModelManager:
    def __init__(self, model_type: str = "vit_h", use_gpu: bool = True) -> None:
        self.model_type = model_type
        self.use_gpu = use_gpu and torch.cuda.is_available()
        self.device = "cuda" if self.use_gpu else "cpu"
        self.checkpoint = self._ensure_checkpoint(model_type)
        self.predictor: SamPredictor | None = None

    def _ensure_checkpoint(self, model_type: str) -> str:
        cache_dir = Path.home() / ".samuel" / "weights"
        cache_dir.mkdir(parents=True, exist_ok=True)
        filename = f"sam_{model_type}.pth"
        checkpoint = cache_dir / filename
        if checkpoint.exists():
            return str(checkpoint)
        import urllib.request

        urllib.request.urlretrieve(MODEL_URLS[model_type], checkpoint)
        return str(checkpoint)

    def load(self) -> None:
        if self.predictor is not None:
            return
        sam = sam_model_registry[self.model_type](checkpoint=self.checkpoint)
        sam.to(device=self.device)
        self.predictor = SamPredictor(sam)

    def get_predictor(self) -> SamPredictor:
        self.load()
        assert self.predictor is not None
        return self.predictor
