# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
## Project: Smart English PDF Reader
## Version: 1.0
## Date: 2025-11-18

---

## 1. INTRODUCTION
Tài liệu này mô tả các yêu cầu chức năng (Functional Requirements) và phi chức năng (Non-functional Requirements) cho ứng dụng **Smart English PDF Reader**. Ứng dụng hỗ trợ người dùng đọc tài liệu tiếng Anh chuyên ngành, tích hợp dịch thuật và phân tích ngữ pháp song song sử dụng AI (Gemini), với cơ chế lưu trữ cục bộ tối ưu (SQLite).

---

## 2. ACTORS (TÁC NHÂN)
* **User (Người dùng):** Người sử dụng ứng dụng để đọc, dịch và học tiếng Anh.
* **System (Hệ thống):** Ứng dụng Desktop (JavaFX).
* **Gemini API:** Dịch vụ AI cung cấp tính năng dịch và phân tích ngôn ngữ.
* **Local Database (SQLite):** Hệ thống lưu trữ tệp tin cục bộ (.meta.db).

---

## 3. FUNCTIONAL REQUIREMENTS (USE CASES)

### UC-01: Cấu hình API Key
* **Description:** Người dùng nhập và lưu trữ token key của Gemini để kích hoạt các tính năng AI.
* **Pre-condition:** Người dùng đã có tài khoản Google AI Studio và tạo API Key.
* **Main Flow:**
    1.  User mở màn hình "Settings".
    2.  User nhập chuỗi Gemini API Key vào trường input.
    3.  User nhấn nút "Save".
    4.  System mã hóa nhẹ (obfuscate) và lưu key vào `Java Preferences` hoặc file config bảo mật.
    5.  System hiển thị thông báo "Key saved successfully".
* **Alternative Flow:**
    * Nếu User nhập key rỗng hoặc sai định dạng, System hiển thị lỗi và yêu cầu nhập lại.

### UC-02: Mở tài liệu PDF (Lần đầu tiên - Parsing)
* **Description:** Mở một file PDF chưa từng được xử lý trước đó. Hệ thống phải phân tích cấu trúc và tạo file cache.
* **Pre-condition:** File PDF hợp lệ, có quyền đọc (read permission).
* **Main Flow:**
    1.  User chọn chức năng "Open File" và chọn một file PDF (ví dụ: `Book.pdf`).
    2.  System tính mã Hash (MD5/SHA-256) của file PDF.
    3.  System kiểm tra sự tồn tại của file database đi kèm (`Book.meta.db`) trong cùng thư mục.
    4.  **Condition:** Không tìm thấy file database.
    5.  System khởi tạo luồng xử lý nền (Background Task):
        * Sử dụng thư viện PDF Engine (PDFBox) để quét toàn bộ file.
        * Trích xuất văn bản và chia tách thành các đoạn văn (Paragraphs).
        * Lấy tọa độ Y (Y-axis coordinate) của từng đoạn văn để phục vụ tính năng Sync Scroll.
    6.  System tạo file SQLite mới (`Book.meta.db`) và tạo bảng dữ liệu.
    7.  System lưu trữ (Batch Insert) toàn bộ cấu trúc đoạn văn (Original Text, Page Index, Y-Coordinate) vào bảng `paragraphs`.
    8.  System hiển thị giao diện chính với dữ liệu vừa phân tích.
* **Post-condition:** File `.meta.db` được tạo ra, dữ liệu gốc được hiển thị lên Cột 1 và Cột 2.

### UC-03: Mở tài liệu PDF (Các lần sau - Loading from Cache)
* **Description:** Mở lại một file PDF đã từng được xử lý. Hệ thống load dữ liệu từ Cache để tối ưu hiệu năng.
* **Pre-condition:** File PDF và file `.meta.db` tương ứng tồn tại cùng thư mục.
* **Main Flow:**
    1.  User chọn file PDF.
    2.  System kiểm tra sự tồn tại của file database đi kèm.
    3.  **Condition:** Tìm thấy file database (`.meta.db`).
    4.  System kết nối tới SQLite.
    5.  System truy vấn bảng `paragraphs` để lấy danh sách đoạn văn và các bản dịch (nếu có).
    6.  System hiển thị ngay lập tức dữ liệu lên giao diện (Bỏ qua bước Parse PDF).
* **Post-condition:** Ứng dụng hiển thị trạng thái đọc trước đó, bao gồm các đoạn đã dịch.

### UC-04: Đồng bộ cuộn trang (Synchronized Scrolling)
* **Description:** Đảm bảo nội dung giữa Cột 1 (PDF View) và Cột 2 (Paragraph List) luôn tương ứng nhau khi người dùng cuộn chuột.
* **Main Flow:**
    1.  User thực hiện cuộn (scroll) tại Cột 1 (PDF View).
    2.  System bắt sự kiện scroll, lấy chỉ số trang (Page Index) và tọa độ Y hiện tại của Viewport.
    3.  System đối chiếu tọa độ này với dữ liệu tọa độ các đoạn văn đã lưu trong Memory/DB.
    4.  System tính toán vị trí index của List View ở Cột 2 tương ứng.
    5.  System tự động cuộn Cột 2 đến vị trí index vừa tính toán (`listView.scrollTo(index)`).
* **Alternative Flow:**
    * Nếu User cuộn ở Cột 2, System tính toán ngược lại vị trí trang và tọa độ Y để cuộn Cột 1.

### UC-05: Dịch đoạn văn (Paragraph Translation)
* **Description:** Dịch toàn văn một đoạn từ tiếng Anh sang tiếng Việt và lưu lại kết quả.
* **Pre-condition:** Đã cấu hình API Key (UC-01).
* **Main Flow:**
    1.  User nhấn nút "Translate" tại một Tile (đoạn văn) ở Cột 2.
    2.  System hiển thị trạng thái "Loading..." tại Tile đó.
    3.  System gửi nội dung gốc (`original_text`) của đoạn văn tới Gemini API với prompt yêu cầu dịch.
    4.  Gemini trả về kết quả dịch.
    5.  System cập nhật giao diện Tile: thay thế text tiếng Anh bằng text tiếng Việt.
    6.  System lưu kết quả dịch vào trường `translated_text` trong bảng `paragraphs` của file SQLite (`UPDATE paragraphs SET translated_text = ? WHERE id = ?`).
* **Post-condition:** Đoạn văn được hiển thị tiếng Việt và dữ liệu được lưu vĩnh viễn.

### UC-06: Xem chi tiết và Phân tích câu (Detail & Analysis)
* **Description:** Xem chi tiết từng câu trong đoạn văn, phân tích ngữ pháp và từ vựng.
* **Pre-condition:** User đã chọn một đoạn văn ở Cột 2.
* **Main Flow:**
    1.  User click chọn (focus) một Tile ở Cột 2.
    2.  System hiển thị Cột 3 (Detail Pane).
    3.  System tách đoạn văn thành các câu riêng biệt (Sentence Splitting).
    4.  User chọn một câu cụ thể trong Cột 3 và chọn "Analyze" hoặc "Vocabulary".
    5.  System kiểm tra trong bảng `sentences` xem câu này đã có dữ liệu phân tích chưa.
        * **Nếu có:** Hiển thị dữ liệu từ DB.
        * **Nếu chưa:**
            1.  Gửi request tới Gemini API yêu cầu phân tích ngữ pháp (S-V-O, Tense) hoặc giải nghĩa từ vựng.
            2.  Nhận kết quả JSON từ API.
            3.  Hiển thị kết quả dưới dạng trực quan (Tree View hoặc Card).
            4.  Lưu kết quả vào bảng `sentences` trong SQLite.

---

## 4. NON-FUNCTIONAL REQUIREMENTS (YÊU CẦU PHI CHỨC NĂNG)

### 4.1. Giao diện người dùng (UI/UX)
* **Theme:** Ứng dụng mặc định sử dụng chế độ **Dark Mode (Deep Black)**.
* **Color Palette:**
    * Background: `#0d1117` (Rich Black).
    * Surface/Containers: `#161b22`.
    * Text: `#c9d1d9` (Light Gray - chống mỏi mắt).
    * Accent: `#58a6ff` (Blue).
* **Layout:** Sử dụng thiết kế phẳng (Flat Design), bố cục 3 cột có thể thay đổi kích thước (Resizable SplitPane).

### 4.2. Hiệu năng (Performance)
* **Concurrency:** Các tác vụ nặng (Parse PDF, Call API, Connect DB) bắt buộc phải chạy trên Background Thread (sử dụng `javafx.concurrent.Task` hoặc `Service`). Không được chặn (block) UI Thread.
* **Lazy Loading:** Chỉ load chi tiết phân tích câu khi người dùng thực sự click vào đoạn văn đó.

### 4.3. Lưu trữ dữ liệu (Data Persistence)
* **Database:** SQLite.
* **Naming Convention:** File DB phải trùng tên với file PDF và có hậu tố `.meta.db`.
* **Optimization:** Sử dụng Transaction khi insert dữ liệu số lượng lớn (Batch Insert) lúc parse file PDF lần đầu để giảm thời gian chờ.

### 4.4. Công nghệ (Technology Constraints)
* **Language:** Java 17+.
* **Framework:** JavaFX (với mô hình MVVM).
* **Build Tool:** Maven.
* **Libraries:**
    * PDF Processing: Apache PDFBox.
    * Database: SQLite JDBC.
    * UI Theme: AtlantaFX.
    * JSON Processing: Gson/Jackson.