#!/bin/bash

IMAGE=beehivelab/tornado-gpu:latest
exec docker run --gpus=all --rm -it --user="$(id -u):$(id -g)" --net=none -v "$PWD":/data "$IMAGE" "$@"
