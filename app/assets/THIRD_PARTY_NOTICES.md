# Offline OCR components

- Tesseract4Android 4.9.0: https://github.com/adaptech-cz/Tesseract4Android, Apache License 2.0; license text in licenses/Tesseract4Android-LICENSE.txt.
- Simplified Chinese model from tessdata_fast: https://github.com/tesseract-ocr/tessdata_fast, Apache License 2.0; license text in licenses/tessdata-fast-LICENSE.txt.
- Native OCR distribution includes Tesseract, Leptonica, libjpeg and libpng. Upstream source and licensing: https://github.com/tesseract-ocr/tesseract ; https://github.com/DanBloomberg/leptonica ; https://github.com/libjpeg-turbo/libjpeg-turbo ; http://www.libpng.org/pub/png/libpng.html .

The OCR library and model run within the app. Images are not sent to these projects or to a hosted OCR service. Android ARM64 and x86_64 binaries are included; the original AAR is published by the upstream project through JitPack.
