#!/usr/bin/env python3

import contextlib
import copy
import os
import unittest
from PIL import Image

import torch
from d2go.export.api import convert_and_export_predictor
from d2go.export.d2_meta_arch import patch_d2_meta_arch
from d2go.runner import create_runner, GeneralizedRCNNRunner
from d2go.model_zoo import model_zoo

from mobile_cv.common.misc.file_utils import make_temp_directory
from d2go.tests.data_loader_helper import LocalImageGenerator, register_toy_dataset


patch_d2_meta_arch()


@contextlib.contextmanager
def create_fake_detection_data_loader(height, width, is_train):
    with make_temp_directory("detectron2go_tmp_dataset") as dataset_dir:
        runner = create_runner("d2go.runner.GeneralizedRCNNRunner")
        cfg = runner.get_default_cfg()
        cfg.DATASETS.TRAIN = ["default_dataset_train"]
        cfg.DATASETS.TEST = ["default_dataset_test"]

        with make_temp_directory("detectron2go_tmp_dataset") as dataset_dir:
            image_dir = os.path.join(dataset_dir, "images")
            os.makedirs(image_dir)
            image_generator = LocalImageGenerator(image_dir, width=width, height=height)

            if is_train:
                with register_toy_dataset(
                    "default_dataset_train", image_generator, num_images=3
                ):
                    train_loader = runner.build_detection_train_loader(cfg)
                    yield train_loader
            else:
                with register_toy_dataset(
                    "default_dataset_test", image_generator, num_images=3
                ):
                    test_loader = runner.build_detection_test_loader(
                        cfg, dataset_name="default_dataset_test"
                    )
                    yield test_loader

def test_export_torchvision_format():
    cfg_name = 'faster_rcnn_fbnetv3a_dsmask_C4.yaml'
    pytorch_model = model_zoo.get(cfg_name, trained=True)

    from typing import List, Dict
    class Wrapper(torch.nn.Module):
        def __init__(self, model):
            super().__init__()
            self.model = model
            coco_idx_list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                             27, 28, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50, 51,
                             52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 67, 70, 72, 73, 74, 75, 76, 77,
                             78, 79, 80, 81, 82, 84, 85, 86, 87, 88, 89, 90, 91]

            self.coco_idx = torch.tensor(coco_idx_list)

        def forward(self, inputs: List[torch.Tensor]):
            x = inputs[0].unsqueeze(0) * 255
            scale = 320.0 / min(x.shape[-2], x.shape[-1])
            x = torch.nn.functional.interpolate(x, scale_factor=scale, mode="bilinear", align_corners=True, recompute_scale_factor=True)
            out = self.model(x[0])
            res : Dict[str, torch.Tensor] = {}
            res["boxes"] = out[0] / scale
            res["labels"] = torch.index_select(self.coco_idx, 0, out[1])
            res["scores"] = out[2]
            return inputs, [res]

    size_divisibility = max(pytorch_model.backbone.size_divisibility, 10)
    h, w = size_divisibility, size_divisibility * 2
    with create_fake_detection_data_loader(h, w, is_train=False) as data_loader:
        predictor_path = convert_and_export_predictor(
            model_zoo.get_config(cfg_name),
            copy.deepcopy(pytorch_model),
            "torchscript_int8@tracing",
            './',
            data_loader,
        )

        orig_model = torch.jit.load(os.path.join(predictor_path, "model.jit"))
        wrapped_model = Wrapper(orig_model)
        # optionally do a forward
        wrapped_model([torch.rand(3, 600, 600)])
        scripted_model = torch.jit.script(wrapped_model)
        scripted_model.save("ObjectDetection/app/src/main/assets/d2go.pt")

if __name__ == '__main__':
    test_export_torchvision_format()
