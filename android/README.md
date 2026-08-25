# On-Device Handwritten Document Extraction & PaddleOCR Application

A complete offline, on-device handwritten text extraction and document ingestion application built in **Kotlin** for Android.

This application is specialized for **Handwritten Text OCR** (handwritten notes, manuscripts, forms, camera captures, and scanned PDFs), automatically downloads OCR models directly on the Android device, extracts page content using **PaddleOCR**, splits text into semantic chunks, and saves the extracted data persistently in an **ObjectBox** database.

---

## 🌟 Key Features

1. **Automatic On-Device Model Downloader**:
   - On first app launch, `OcrModelDownloader` automatically downloads specialized Handwritten OCR models (`handwriting_det.onnx`, `handwriting_rec.onnx`, `ppocr_keys_v1.txt`) over HTTPS directly into internal app storage (`context.filesDir/paddle_ocr/`).
   - Zero manual setup required by the user on the developer machine or target device.
2. **Handwritten Text OCR & Adaptive Pre-processing**:
   - Optimized for handwritten pen/pencil strokes, forms, and camera captures.
   - Adaptive local binarization, stroke contrast enhancement, and background whitening.
3. **Document Ingestion & Camera Scanner**:
   - Ingest PDF files, plain text documents, and images (PNG/JPG).
   - Capture multi-page handwritten notes directly using CameraX live preview.
   - Separation of multi-page documents into individual page frames.
4. **Persistent ObjectBox Database**:
   - Extracted text, page numbers, word counts, and metadata (`docId`, `docTitle`, `pageNumber`) are saved persistently in **ObjectBox DB**.
   - Browse and explore all saved documents and extracted text chunks in the app.

---

## 📱 How to Build & Run

1. Open project in **Android Studio** (`Jellyfish` or newer).
2. Connect an Android device or emulator with internet connectivity (for initial model download on first app launch).
3. Click **Run** (`Control + R`).
4. When launched, the application automatically downloads the Handwritten OCR models in the background and displays status progress on the home dashboard.
