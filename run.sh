#!/bin/bash

# Tên main class được định nghĩa trong dự án của bạn
MAIN_CLASS="com.vinhtt.PDFReader.app.App"

# Kiểm tra xem Java có được cài đặt chưa
if ! command -v java &> /dev/null; then
    echo "❌ Lỗi: Java chưa được cài đặt hoặc không tìm thấy trong PATH."
    exit 1
fi

# Kiểm tra xem Maven có được cài đặt chưa
if ! command -v mvn &> /dev/null; then
    echo "❌ Lỗi: Maven (mvn) chưa được cài đặt hoặc không tìm thấy trong PATH."
    echo "👉 Vui lòng cài đặt Maven trước khi chạy script này."
    exit 1
fi

# In thông tin môi trường
echo "========================================================"
echo "🚀 Đang khởi động Smart English PDF Reader..."
echo "🔧 Java Version:"
java -version | head -n 1
echo "📂 Main Class: $MAIN_CLASS"
echo "========================================================"

# Lệnh Maven để chạy ứng dụng JavaFX
# - clean: Dọn dẹp build cũ
# - javafx:run: Goal của plugin org.openjfx để chạy ứng dụng module hóa
mvn clean javafx:run

# Kiểm tra mã lỗi trả về
if [ $? -eq 0 ]; then
    echo "✅ Ứng dụng đã đóng thành công."
else
    echo "❌ Có lỗi xảy ra trong quá trình chạy."
fi