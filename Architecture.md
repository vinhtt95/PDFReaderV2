# SOFTWARE ARCHITECTURE DOCUMENT (SAD)
## Project: Smart English PDF Reader
## Version: 1.0
## Date: 2025-11-18

---

## 1. ARCHITECTURAL OVERVIEW
Dự án được xây dựng dựa trên kiến trúc **MVVM (Model-View-ViewModel)** kết hợp với **Service-Oriented Design**. Mục tiêu là tách biệt hoàn toàn giao diện người dùng (UI) khỏi logic nghiệp vụ và xử lý dữ liệu, đảm bảo tính dễ bảo trì, mở rộng và tuân thủ nguyên lý **SOLID**.

### 1.1. High-Level Layers
1.  **Presentation Layer (View):** Các file FXML và Controller code-behind. Chịu trách nhiệm hiển thị dữ liệu và binding với ViewModel. Sử dụng thư viện **AtlantaFX** để apply theme "Deep Black".
2.  **Logic Layer (ViewModel):** Chứa trạng thái (State) của UI, xử lý logic hiển thị (Presentation Logic) và điều phối các lệnh từ View xuống Service.
3.  **Business Layer (Service):** Chứa các logic nghiệp vụ cốt lõi như xử lý PDF, gọi API Gemini, tính toán đồng bộ cuộn.
4.  **Data Access Layer (DAL):** Quản lý việc đọc/ghi dữ liệu vào SQLite và File System.

---

## 2. TECHNOLOGY STACK

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | Java 17+ | LTS Version, sử dụng Records, Text Blocks, var. |
| **UI Framework** | JavaFX 21 | OpenJFX. |
| **UI Theme** | AtlantaFX | Theme *Primer Dark* hoặc *Dracula* tùy chỉnh cho Deep Black mode. |
| **PDF Engine** | Apache PDFBox 3.0 | Trích xuất text và tọa độ (TextPosition). |
| **Database** | SQLite (JDBC) | Lưu trữ cache bản dịch và metadata. |
| **Networking** | OkHttp 4 | Client HTTP hiệu năng cao để gọi Gemini API. |
| **JSON Parser** | Gson / Jackson | Serialize/Deserialize dữ liệu JSON. |
| **DI Container** | Manual / Guice | Dependency Injection (Constructor Injection). |
| **Build Tool** | Maven | Quản lý dependency và build lifecycle. |

---

## 3. DETAILED DESIGN

### 3.1. Data Model (Entity)
Các POJO đại diện cho dữ liệu lõi, không chứa logic nghiệp vụ.
* `PdfDocument`: Chứa metadata file, đường dẫn, mã hash.
* `Paragraph`:
    * `id`: UUID/Int.
    * `content`: String (English).
    * `translation`: String (Vietnamese).
    * `yPosition`: float (Tọa độ trong PDF).
    * `pageIndex`: int.
* `Sentence`:
    * `original`: String.
    * `analysis`: String (JSON - S-V-O structure).

### 3.2. Database Schema (SQLite)
Mỗi file PDF sẽ đi kèm một file database `.meta.db`.

**Table: `file_info`**
Lưu metadata để verify file gốc chưa bị thay đổi.
```sql
CREATE TABLE file_info (
    file_hash TEXT PRIMARY KEY,
    file_path TEXT,
    last_opened TIMESTAMP
);
```

**Table: `paragraphs`** Lưu dữ liệu từng đoạn văn và trạng thái dịch.
```sql
CREATE TABLE paragraphs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    page_index INTEGER,
    y_position REAL, -- Tọa độ Y để sync scroll
    original_text TEXT,
    translated_text TEXT, -- Nullable
    is_translated BOOLEAN DEFAULT 0
);
```

**Table: `sentences`** Lưu cache phân tích sâu (Grammar/Vocabulary).
```sql
CREATE TABLE sentences (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    paragraph_id INTEGER,
    original_sentence TEXT,
    grammar_json TEXT, -- Lưu JSON cấu trúc câu
    vocab_json TEXT, -- Lưu JSON từ vựng khó
    FOREIGN KEY(paragraph_id) REFERENCES paragraphs(id)
);
```
### 3.3. Service Layer (Business Logic)
Tất cả Service đều được định nghĩa qua **Interface** để đảm bảo **DIP (Dependency Inversion Principle)**.

* `IPdfService`:
    * `parsePdf(File file)`: Trả về `List<Paragraph>`. Sử dụng `PDFTextStripper` custom để nhóm các dòng text (TextLine) thành đoạn văn dựa trên khoảng cách dòng (`line spacing`).
    * `extractMetadata(File file)`: Lấy thông tin file để tạo hash.
* `ITranslationService`:
    * `translate(String text)`: Gửi HTTP request tới Gemini API.
    * `analyzeSentence(String sentence)`: Gọi Gemini API với prompt chuyên biệt để phân tích ngữ pháp (S-V-O).
* `IStorageService`:
    * `initDatabase(String dbPath)`: Tạo file `.db` và các bảng (Tables) nếu chưa tồn tại.
    * `saveParagraphs(List<Paragraph> paragraphs)`: Thực hiện Batch Insert ban đầu (dùng Transaction).
    * `updateTranslation(int id, String translatedText)`: Cập nhật cột `translated_text` và `is_translated`.
    * `getParagraphs()`: Load dữ liệu cache lên ViewModel.

### 3.4. ViewModel Layer
* `MainViewModel`:
    * **Properties:**
        * `ObjectProperty<List<Paragraph>> paragraphList`: Danh sách hiển thị trên UI.
        * `DoubleProperty currentPdfScrollY`: Binding 2 chiều hoặc Listener với ScrollBar của PDF View.
        * `ObjectProperty<Paragraph> selectedParagraph`: Quản lý state khi user click vào Tile.
    * **Logic Sync Scroll:**
        * Lắng nghe sự thay đổi của `currentPdfScrollY` từ View (Cột 1).
        * Tính toán tỷ lệ hoặc tìm đoạn văn có `yPosition` gần nhất trong list.
        * Bắn sự kiện hoặc cập nhật property `scrollToIndex` để ListView (Cột 2) tự cuộn.

---

## 4. CONCURRENCY MODEL (XỬ LÝ ĐA LUỒNG)
JavaFX là đơn luồng (Single Threaded UI). Các tác vụ nặng phải chạy trên Worker Thread để tránh treo giao diện.

1.  **PDF Parsing:**
    * Sử dụng `javafx.concurrent.Task`.
    * Hiển thị `ProgressIndicator` đè lên màn hình chính trong lúc đang parse file và insert vào DB.
2.  **API Calls (Gemini):**
    * Sử dụng `CompletableFuture` hoặc `Service<String>`.
    * *Flow:* UI click Translate -> ViewModel gọi Service (Async) -> Service trả kết quả -> ViewModel dùng `Platform.runLater()` cập nhật Property -> UI tự động thay đổi nhờ Data Binding.
    * Cần xử lý Exception (Mất mạng, Key lỗi) và hiển thị Alert.

---

## 5. PROJECT STRUCTURE (MAVEN)

```text
com.vinhtt.PDFReader
├── app
│   ├── App.java                # Main Entry Point (extends Application)
│   └── AppModule.java          # Dependency Injection Configuration (nếu dùng Guice)
├── model                       # POJOs (Data Transfer Objects)
│   ├── Paragraph.java
│   └── Sentence.java
├── view                        # UI Layer (FXML + Code-behind)
│   ├── components              # Custom Controls
│   │   └── ParagraphTile.java  # Custom ListCell renderer
│   ├── MainView.fxml
│   └── MainController.java
├── viewmodel                   # State & Logic
│   └── MainViewModel.java
├── service                     # Business Logic Interfaces & Implementations
│   ├── api
│   │   ├── GeminiService.java
│   │   └── ITranslationService.java
│   ├── pdf
│   │   ├── PdfBoxService.java
│   │   └── IPdfService.java
│   └── storage
│       ├── SqliteStorageService.java
│       └── IStorageService.java
└── util                        # Helpers
    ├── ConfigLoader.java       # Load API Key/Preferences
    └── StyleManager.java       # Apply AtlantaFX Theme & CSS classes
```
## 6. DESIGN PATTERNS APPLIED

1.  **Observer Pattern:**
    * Sử dụng triệt để JavaFX Properties (`StringProperty`, `BooleanProperty`, `ObjectProperty`) để binding dữ liệu tự động giữa **ViewModel** và **View**.
    * Khi ViewModel cập nhật trạng thái (ví dụ: `translationStatus` từ `LOADING` sang `DONE`), UI sẽ tự động thay đổi (ẩn ProgressBar, hiện Text) mà không cần Controller gọi hàm update thủ công.

2.  **Strategy Pattern:**
    * Áp dụng cho `ITranslationService`.
    * *Mục đích:* Cho phép dễ dàng thay đổi thuật toán hoặc nhà cung cấp dịch thuật (Gemini, ChatGPT, DeepL, Offline Dictionary) trong tương lai mà không ảnh hưởng đến code của ViewModel.
    * *Implementation:* ViewModel chỉ giữ tham chiếu đến interface `ITranslationService`, class cụ thể (`GeminiService`) được inject vào lúc Runtime.

3.  **Data Access Object (DAO):**
    * `SqliteStorageService` đóng vai trò là DAO.
    * *Mục đích:* Ẩn giấu các câu lệnh SQL phức tạp và logic kết nối JDBC khỏi tầng Business Logic. ViewModel chỉ gọi các phương thức trừu tượng như `saveParagraph()` chứ không quan tâm data được lưu vào file JSON hay SQLite.

4.  **Lazy Loading (Proxy Pattern variant):**
    * Dữ liệu phân tích câu (`grammar_json`, `vocab_json` trong bảng `sentences`) **KHÔNG** được load ngay khi mở file PDF để tiết kiệm RAM.
    * Chỉ khi người dùng click vào một đoạn văn (Event Trigger), hệ thống mới truy vấn DB hoặc gọi API để lấy dữ liệu chi tiết này cho Cột 3.

5.  **Singleton Pattern:**
    * Áp dụng cho `GeminiClient` (Wrapper của OkHttpClient).
    * *Mục đích:* Đảm bảo chỉ có một instance của `OkHttpClient` được khởi tạo trong suốt vòng đời ứng dụng để tối ưu hóa connection pool và tài nguyên mạng.

---

## 7. UI/UX SPECIFICATIONS (DARK MODE)

### 7.1. Theme & Libraries
* **Library:** [AtlantaFX](https://github.com/mkpaz/atlantafx)
* **Base Theme:** `PrimerDark` (GitHub Dark style) hoặc `Dracula`.

### 7.2. CSS Customization (`styles.css`)
Để đạt được giao diện "Deep Black" và phẳng theo yêu cầu:

```css
/* Global Background */
.root {
    -fx-background-color: #0d1117; /* Deep Black/Grey */
    -fx-font-family: 'Inter', 'Segoe UI', sans-serif;
}

/* SplitPane styling - Remove default dividers/borders */
.split-pane {
    -fx-background-color: transparent;
}
.split-pane > .split-pane-divider {
    -fx-background-color: transparent;
    -fx-padding: 0 2px 0 2px; /* Thin gap */
}

/* Paragraph Tile (ListCell Customization) */
.list-cell {
    -fx-background-color: transparent;
    -fx-padding: 5px;
}
.paragraph-card {
    -fx-background-color: #161b22; /* Surface color */
    -fx-background-radius: 8px;
    -fx-border-color: #30363d;     /* Border subtle */
    -fx-border-radius: 8px;
    -fx-padding: 15px;
}

.paragraph-card:hover {
    -fx-background-color: #1f2428;
    -fx-cursor: hand;
}

/* Selected State */
.list-cell:selected .paragraph-card {
    -fx-border-color: #58a6ff; /* Accent Blue */
    -fx-border-width: 1px;
    -fx-effect: dropshadow(three-pass-box, rgba(88, 166, 255, 0.2), 10, 0, 0, 0);
}

/* Text Styling */
.text-english {
    -fx-fill: #c9d1d9; /* Primary Text (Off-white) */
    -fx-font-size: 15px;
    -fx-line-spacing: 4px;
}

.text-vietnamese {
    -fx-fill: #8b949e; /* Secondary Text (Grey) */
    -fx-font-style: normal;
    -fx-font-size: 14px;
    -fx-font-weight: lighter;
}
```
## 8. ERROR HANDLING & SECURITY

### 8.1. Exception Handling Strategy
* **Network Errors (Gemini API):**
    * Nếu mất kết nối hoặc Timeout: Hiển thị thông báo dạng `Toast` hoặc `Snackbar` (nhẹ nhàng, không dùng Popup chặn màn hình).
    * Logic Fallback: Cho phép user nhấn nút "Retry" icon ngay tại Tile bị lỗi.
* **API Key Errors:**
    * Nếu API trả về `401 Unauthorized` hoặc `403 Forbidden`: Hệ thống tự động điều hướng user về màn hình Settings và hiển thị thông báo yêu cầu kiểm tra lại Key.
* **File Corrupt:**
    * Nếu file `.meta.db` bị lỗi không đọc được (do tắt đột ngột): Hệ thống tự động đổi tên file lỗi thành `.bak`, thông báo cho người dùng và tiến hành tạo file DB mới (Re-parse PDF).

### 8.2. Security & Privacy
* **API Key Storage:**
    * Sử dụng `java.util.prefs.Preferences` để lưu API Key.
    * **Tuyệt đối không** lưu plaintext vào file config JSON thông thường để tránh lộ key khi user chia sẻ thư mục project.
* **Data Privacy:**
    * Toàn bộ lịch sử dịch và file PDF chỉ nằm trên máy cục bộ của người dùng (Localhost).
    * Ứng dụng **không** upload file PDF lên bất kỳ server trung gian nào. Chỉ gửi text string (đoạn văn bản) lên Google Gemini API để xử lý.

---

## 9. TESTING STRATEGY

### 9.1. Unit Testing
* **Framework:** JUnit 5 + Mockito.
* **Scope:**
    * **Services:** Test logic parse PDF, logic lưu trữ SQLite (sử dụng in-memory DB cho test), logic xử lý response JSON từ Gemini.
    * **ViewModels:** Test các state `TranslationStatus`, logic tính toán sync scroll, và data binding. Mock các Service để không gọi API thật khi chạy test.

### 9.2. Integration/UI Testing
* **Framework:** TestFX (Thư viện chuyên dụng để test JavaFX).
* **Scope:**
    * Test dòng chảy người dùng (User Flow): Mở file -> Click Translate -> Hiển thị kết quả.
    * Test giao diện: Đảm bảo các control hiển thị đúng style, layout không bị vỡ khi resize cửa sổ.

---

## 10. DEPLOYMENT & PACKAGING

Do sử dụng Java 17+ và JavaFX (không còn nằm trong JDK mặc định), ứng dụng cần được đóng gói thành file thực thi độc lập (Self-contained application).

* **Tool:** `jpackage` (tích hợp sẵn trong JDK 17).
* **Output:**
    * **Windows:** File `.msi` hoặc `.exe` (kèm theo JRE minimal).
    * **macOS:** File `.dmg` hoặc `.app`.
* **Optimization:** Sử dụng `jlink` để tạo custom runtime image, chỉ gói gọn các module Java cần thiết (như `java.base`, `java.desktop`, `java.sql`, `javafx.controls`...) giúp giảm dung lượng bộ cài (xuống dưới 60MB).