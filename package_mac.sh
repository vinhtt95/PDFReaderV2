#!/bin/bash

# --- CẤU HÌNH ---
APP_NAME="PDFReader"
APP_VERSION="1.0.0"
MAIN_JAR="PDFReader-1.0-SNAPSHOT.jar"
# Class khởi chạy (Đã sửa ở bước trước)
MAIN_CLASS="com.vinhtt.PDFReader.app.Launcher"
ICON_PNG="src/main/resources/app_icon.png"

# Thư mục chứa file JAR sạch để đóng gói (Tránh lỗi đệ quy)
STAGING_DIR="target/staging"
# Thư mục chứa kết quả đầu ra
OUTPUT_DIR="target/installer"

echo "=========================================="
echo "📦 ĐÓNG GÓI (FAT JAR MODE) - FIX RECURSION..."
echo "=========================================="

# 1. Build (Maven Shade sẽ tự gộp libs)
echo "⚙️  Đang Clean & Build..."
mvn clean package
if [ $? -ne 0 ]; then
    echo "❌ Build thất bại!"
    exit 1
fi

# 2. Chuẩn bị thư mục Staging (Quan trọng để sửa lỗi)
echo "📂 Đang chuẩn bị thư mục staging..."
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
# Chỉ copy file JAR cần thiết vào đây
cp "target/$MAIN_JAR" "$STAGING_DIR/"

# 3. Tạo Icon
echo "🎨 Đang tạo icon chuẩn macOS..."
ICON_ICNS="target/icon.icns"
ICONSET_DIR="target/icons.iconset"
mkdir -p "$ICONSET_DIR"

sips -z 16 16     "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16.png" > /dev/null
sips -z 32 32     "$ICON_PNG" --out "$ICONSET_DIR/icon_16x16@2x.png" > /dev/null
sips -z 32 32     "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32.png" > /dev/null
sips -z 64 64     "$ICON_PNG" --out "$ICONSET_DIR/icon_32x32@2x.png" > /dev/null
sips -z 128 128   "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128.png" > /dev/null
sips -z 256 256   "$ICON_PNG" --out "$ICONSET_DIR/icon_128x128@2x.png" > /dev/null
sips -z 512 512   "$ICON_PNG" --out "$ICONSET_DIR/icon_512x512.png" > /dev/null

iconutil -c icns "$ICONSET_DIR" -o "$ICON_ICNS"

# 4. Chạy jpackage
echo "🚀 Đang chạy jpackage..."
rm -rf "$OUTPUT_DIR"

# SỬA LỖI: --input trỏ vào STAGING_DIR thay vì target
jpackage \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$STAGING_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --type app-image \
  --icon "$ICON_ICNS" \
  --java-options "--enable-preview" \
  --dest "$OUTPUT_DIR"

if [ $? -eq 0 ]; then
    echo "✅ Xong! Kiểm tra tại: $OUTPUT_DIR/$APP_NAME.app"
    echo "👉 Mở thư mục chứa app..."
    open "$OUTPUT_DIR"
else
    echo "❌ Lỗi khi chạy jpackage."
fi