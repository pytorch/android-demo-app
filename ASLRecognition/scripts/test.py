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
import torchvision.transforms as transforms
import torch.nn.functional as F
import time
import cnn_models
from PIL import Image

# construct the argument parser and parse the arguments
parser = argparse.ArgumentParser()
parser.add_argument('-i', '--img', default='../app/src/main/assets/C1.jpg', type=str,
    help='path for the image to test on')
args = vars(parser.parse_args())

aug = transforms.Compose([
                transforms.Resize((224, 224)),
])

# load label binarizer
lb = joblib.load('lb.pkl')
model = cnn_models.CustomCNN()
model.load_state_dict(torch.load('asl.pth'))
print(model)
print('Model loaded')

image = Image.open(f"{args['img']}")
image = aug(image)
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
