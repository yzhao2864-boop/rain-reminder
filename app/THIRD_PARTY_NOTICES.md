# Offline OCR components

- Tesseract4Android 4.9.0: https://github.com/adaptech-cz/Tesseract4Android, Apache License 2.0; license text in licenses/Tesseract4Android-LICENSE.txt.
- Simplified Chinese model from tessdata_fast: https://github.com/tesseract-ocr/tessdata_fast, Apache License 2.0; license text in licenses/tessdata-fast-LICENSE.txt.
- Native OCR distribution includes Tesseract, Leptonica, libjpeg and libpng. Upstream source and licensing: https://github.com/tesseract-ocr/tesseract ; https://github.com/DanBloomberg/leptonica ; https://github.com/libjpeg-turbo/libjpeg-turbo ; http://www.libpng.org/pub/png/libpng.html .

The OCR library and model run within the app. Images are not sent to these projects or to a hosted OCR service. Android ARM64 and x86_64 binaries are included; the original AAR is published by the upstream project through JitPack.

## Version 0.8 additions

- PP-OCRv5 mobile Chinese recognition model, distributed by RapidAI/RapidOCR: https://www.modelscope.cn/models/RapidAI/RapidOCR/tree/v3.9.2/onnx/PP-OCRv5/rec ; model upstream https://github.com/PaddlePaddle/PaddleOCR . Apache-2.0 licenses in licenses/PaddleOCR-LICENSE.txt and licenses/RapidOCR-LICENSE.txt. The original model was quantized locally to QUInt8 MatMul/Gemm with ONNX Runtime; recognition runs entirely on device.
- ONNX Runtime Android 1.22.0: https://github.com/microsoft/onnxruntime , MIT license in licenses/ONNXRuntime-LICENSE.txt.
- Noto Sans SC: https://github.com/google/fonts/tree/main/ofl/notosanssc , SIL Open Font License 1.1 in licenses/NotoSansSC-OFL.txt. App font is a locally generated regular-weight subset including common Simplified Chinese characters, Latin and punctuation; system fallback covers characters outside the subset.

The phone APK includes ARM64 native binaries. Build with -Abi all for an APK that also includes x86_64 emulator libraries. Header/date recognition still uses Tesseract; course text uses PP-OCRv5. No screenshot is uploaded to any model provider.
