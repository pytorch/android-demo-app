'''
USAGE:
python test.py --img A_test.jpg
'''
import torch
import joblib
import torch.nn as nn
import numpy as np
import cv2
import argparse
import albumentations
import torch.nn.functional as F
import time
import cnn_models
# construct the argument parser and parse the arguments
parser = argparse.ArgumentParser()
parser.add_argument('-i', '--img', default='../app/src/main/assets/C1.jpg', type=str,
    help='path for the image to test on')
args = vars(parser.parse_args())

aug = albumentations.Compose([
                albumentations.Resize(224, 224, always_apply=True),
])

# load label binarizer
lb = joblib.load('lb.pkl')
model = cnn_models.CustomCNN()
model.load_state_dict(torch.load('asl.pth'))
print(model)
print('Model loaded')

image = cv2.imread(f"{args['img']}")
image_copy = image.copy()

image = aug(image=np.array(image))['image']
image = np.transpose(image, (2, 0, 1)).astype(np.float32)
image = torch.tensor(image, dtype=torch.float)
image = image.unsqueeze(0)
print(image.shape)

start = time.time()
outputs = model(image)
_, preds = torch.max(outputs.data, 1)
print('PREDS', preds)
print(f"Predicted output: {lb.classes_[preds]}")
end = time.time()
print(f"{(end-start):.3f} seconds")
