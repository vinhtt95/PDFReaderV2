#!/bin/bash

# --- CẤU HÌNH ---
APP_NAME="PDFReader"
APP_VERSION="1.0.0"
MAIN_JAR="PDFReader-1.0-SNAPSHOT.jar"
MAIN_CLASS="com.vinhtt.PDFReader.app.App"
ICON_PNG="src/main/resources/app_icon.png"
OUTPUT_DIR="target/installer"

echo "=========================================="
echo "📦 BẮT ĐẦU ĐÓNG GÓI CHO MACOS..."
echo "=========================================="

# 1. Clean & Build & Copy Dependencies
echo "⚙️  Đang build và gom thư viện..."
mvn clean package
if [ $? -ne 0 ]; then
    echo "❌ Build thất bại!"
    exit 1
fi

# 2. Copy Main Jar vào cùng thư mục libs
echo "📂 Chuẩn bị thư mục đầu vào..."
cp "target/$MAIN_JAR" "target/libs/"

# 3. Tự động tạo file .icns từ .png (MacOS yêu cầu .icns)
echo "🎨 Đang tạo icon chuẩn macOS (.icns)..."
ICON_ICNS="target/icon.icns"
ICONSET_DIR="target/icons.iconset"
mkdir -p "$ICONSET_DIR"

# Tạo các kích thước icon khác nhau từ file gốc 512x512
sips -z 16 16     "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16.png" > /dev/null
sips -z 32 32     "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png" > /dev/null
sips -z 32 32     "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32.png" > /dev/null
sips -z 64 64     "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png" > /dev/null
sips -z 128 128   "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128.png" > /dev/null
sips -z 256 256   "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png" > /dev/null
sips -z 256 256   "$ICON_PNG" --out "$ICONSET_DIR/icon_256x256.png" > /dev/null
sips -z 512 512   "$ICON_PNG" --out "$ICONSET_DIR/icon_256x256@2x.png" > /dev/null
sips -z 512 512   "$ICON_PNG" --out "$ICONSET_DIR/icon_512x512.png" > /dev/null

# Gom thành file .icns
iconutil -c icns "$ICONSET_DIR" -o "$ICON_ICNS"
echo "✅ Đã tạo icon: $ICON_ICNS"

# 4. Chạy jpackage
echo "🚀 Đang chạy jpackage..."
rm -rf "$OUTPUT_DIR"

# Lưu ý: --input trỏ vào target/libs (nơi chứa CẢ main jar và các lib dependency)
jpackage \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input target/libs \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --type app-image \
  --icon "$ICON_ICNS" \
  --java-options "--enable-preview" \
  --dest "$OUTPUT_DIR"

if [ $? -eq 0 ]; then
    echo "=========================================="
    echo "✅ THÀNH CÔNG!"
    echo "👉 File app của bạn nằm tại: $OUTPUT_DIR/$APP_NAME.app"
    echo "👉 Bạn có thể kéo nó vào thư mục Applications để chạy."
    echo "=========================================="

    # Mở thư mục chứa kết quả
    open "$OUTPUT_DIR"
else
    echo "❌ Lỗi khi chạy jpackage."
fi