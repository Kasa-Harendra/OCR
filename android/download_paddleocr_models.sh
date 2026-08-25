#!/bin/bash
# Script to automatically download PaddleOCR ONNX models and dictionary files
# into app/src/main/assets/paddle_ocr/

set -e

ASSETS_DIR="app/src/main/assets/paddle_ocr"
mkdir -p "$ASSETS_DIR"

echo "===================================================="
echo " Downloading PaddleOCR (PP-OCRv4) Models & Dictionary"
echo " Target Directory: $ASSETS_DIR"
echo "===================================================="

# 1. Detection Model (PP-OCRv4 Det ONNX)
DET_URL="https://github.com/dandelin/PaddleOCR-onnx/raw/main/ch_PP-OCRv4_det_infer.onnx"
# Alternative mirror / release download
if [ ! -f "$ASSETS_DIR/ch_PP-OCRv4_det_infer.onnx" ]; then
    echo "[1/3] Downloading Detection Model (ch_PP-OCRv4_det_infer.onnx)..."
    curl -L -o "$ASSETS_DIR/ch_PP-OCRv4_det_infer.onnx" "$DET_URL" || {
        echo "Primary download failed, fetching from fallback mirror..."
        curl -L -o "$ASSETS_DIR/ch_PP-OCRv4_det_infer.onnx" "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese_lite/ch_PP-OCRv4_det_infer.tar"
    }
else
    echo "[1/3] Detection model already exists."
fi

# 2. Recognition Model (PP-OCRv4 Rec ONNX)
REC_URL="https://github.com/dandelin/PaddleOCR-onnx/raw/main/ch_PP-OCRv4_rec_infer.onnx"
if [ ! -f "$ASSETS_DIR/ch_PP-OCRv4_rec_infer.onnx" ]; then
    echo "[2/3] Downloading Recognition Model (ch_PP-OCRv4_rec_infer.onnx)..."
    curl -L -o "$ASSETS_DIR/ch_PP-OCRv4_rec_infer.onnx" "$REC_URL" || {
        echo "Primary download failed, fetching from fallback mirror..."
        curl -L -o "$ASSETS_DIR/ch_PP-OCRv4_rec_infer.onnx" "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese_lite/ch_PP-OCRv4_rec_infer.tar"
    }
else
    echo "[2/3] Recognition model already exists."
fi

# 3. PPOCR Character Keys Dictionary
KEYS_URL="https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.7/ppocr/utils/ppocr_keys_v1.txt"
if [ ! -f "$ASSETS_DIR/ppocr_keys_v1.txt" ]; then
    echo "[3/3] Downloading Character Dictionary (ppocr_keys_v1.txt)..."
    curl -L -o "$ASSETS_DIR/ppocr_keys_v1.txt" "$KEYS_URL"
else
    echo "[3/3] Character dictionary already exists."
fi

echo "===================================================="
echo " PaddleOCR Assets Successfully Prepared in:"
echo " $(pwd)/$ASSETS_DIR"
echo "===================================================="
ls -lh "$ASSETS_DIR"
