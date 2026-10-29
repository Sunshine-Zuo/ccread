# 静读 Android 本地阅读器 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**状态：已实施完成（2026-10-29）。** 15 个任务与全部手工验收项均已完成并勾选；实施过程中与原计划的差异记录在文末"实施结果与偏差"，如与任务正文冲突，以文末记录和当前代码为准。

**Goal:** 从零构建一个纯离线、无广告的安卓阅读器，支持本地 TXT 与 EPUB 的导入、分章目录、两种翻页模式和基础排版设置。

**Architecture:** 单模块 Jetpack Compose 应用，单 Activity（`FragmentActivity`，为承载 Readium 的 Fragment 留出能力）。书架与设置由 Room + DataStore 持久化；TXT 走自研的流式分章器与字节偏移随机读取；EPUB 交给 Readium Kotlin Toolkit 渲染，Compose 只负责阅读页的覆盖层与控制面板。原文件不被复制，书架保存 SAF URI 与索引。

**Tech Stack:** Kotlin 2.4.20、Jetpack Compose（BOM 2025.05.00）、Room 2.8.5、DataStore 1.3.0、kotlinx.serialization 1.12.0、Readium Kotlin Toolkit 3.1.2（core library desugaring 2.1.5）、JUnit4 + Robolectric 4.17。构建在 Docker（Linux 容器，JDK 21 + Android SDK 36 + build-tools 36.0.0 + Gradle 8.13 + AGP 8.13.2）内完成，本机不安装 Android Studio、JDK 或 SDK。

**Spec:** `docs/superpowers/specs/2026-10-29-android-ereader-design.md`

## Global Constraints

- `minSdk 26`，`targetSdk 35`，`compileSdk 36`；锁定竖屏；界面仅简体中文。
- AndroidManifest **不得声明 `INTERNET` 或 `ACCESS_NETWORK_STATE` 权限**（用 `tools:node="remove"` 剔除依赖库合并的权限）；不集成广告、统计、崩溃上报 SDK。
- 导入采用**引用原文件**模式：不复制书籍文件；使用 `ACTION_OPEN_DOCUMENT` + `takePersistableUriPermission`；索引与进度存应用私有目录。外部 `ACTION_VIEW` 打开沿用同一套导入管线。
- 文件大小上限：TXT 200MB、EPUB 500MB；超限拒绝导入。
- EPUB 渲染"照书"，只覆盖背景色与按比例缩放的字号，不覆盖文字颜色；左右留白跟随全局边距设置。
- 原文件不可随机定位时拒绝导入；文件失效时提供"重新选择文件"与"从书架移除"。
- 应用名"静读"，包名 `com.ccread.reader`。
- **不执行任何 git 操作**（用户明确要求）。每个任务以测试与构建通过收尾，不包含 commit 步骤。
- 构建与测试全部在 Docker 容器内完成，本机不安装 JDK、Android SDK 或 Android Studio。
- 常驻开发容器名为 `jingdu-dev`（Task 1 创建）。测试命令：`docker exec jingdu-dev ./gradlew :app:testDebugUnitTest`；构建命令：`docker exec jingdu-dev ./gradlew :app:assembleDebug`；APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## Review Focus

以下是最容易伤到真实使用者的输入/条件，对应测试放在所列任务中：

1. 原文件被移动、删除或授权失效 → 打开书时给出"重新选择/移出书架"，不得崩溃或白屏（Task 5）。
2. 无 BOM 的 GBK/GB18030 TXT → 不乱码；随机二进制前缀不得让识别器崩溃（Task 3）。
3. 无章节标题、章节极短、单章超长、只有卷标题的 TXT → 目录非空、章节边界合法、正文不丢（Task 3）。
4. 接近 200MB 的 TXT 与超限文件 → 流式扫描不一次性读入内存，超限明确提示（Task 3、Task 4）。
5. 通过 `content://` 打开的 EPUB 且依赖书籍自带 CSS/图片 → 能渲染、切换模式不丢位置（Task 12、Task 14）。

---

## 文件结构

```
Dockerfile
.dockerignore
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/...
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/colors.xml
app/src/main/res/values/themes.xml
app/src/main/res/values/dimens.xml                 （覆盖 Readium 的 40dp 垂直内边距）
app/src/main/java/com/ccread/reader/
  MainActivity.kt                                  （含 ACTION_VIEW 外部打开）
  JingduApp.kt
  ui/theme/Theme.kt
  ui/navigation/AppNavHost.kt
  ui/bookshelf/BookshelfScreen.kt
  ui/bookshelf/BookshelfViewModel.kt
  ui/bookshelf/BookshelfSorting.kt
  ui/bookshelf/BookCard.kt
  ui/bookshelf/DeleteBookDialog.kt
  ui/bookshelf/BookDetailsDialog.kt                （详情 + 缺失文件对话框）
  ui/reader/ReaderScreen.kt
  ui/reader/ReaderViewModel.kt
  ui/reader/ReaderTopBar.kt
  ui/reader/ReaderBottomBar.kt
  ui/reader/ReaderMenu.kt
  ui/reader/ReaderInteraction.kt
  ui/reader/TocDrawer.kt
  ui/reader/TocModels.kt
  ui/reader/ReadingSettingsSheet.kt                （设置面板 + 亮度面板）
  ui/reader/EpubProgressRules.kt
  ui/reader/ImageViewerDialog.kt                   （全屏图片查看 Dialog）
  ui/reader/txt/TxtReaderView.kt                   （翻页/滚动模式分发）
  ui/reader/txt/TxtPagedReader.kt
  ui/reader/txt/TxtScrollReader.kt
  ui/reader/epub/EpubReaderView.kt
  domain/model/Book.kt
  domain/model/Chapter.kt                          （TocNode 也在此文件）
  domain/model/ReadingProgress.kt
  domain/model/ReadingSettings.kt
  domain/book/ImportRules.kt
  domain/book/BookImporter.kt
  domain/book/ImportResult.kt
  domain/book/FileLocation.kt
  domain/book/TxtBookIndexer.kt
  domain/txt/SeekableFile.kt
  domain/txt/CharsetDetector.kt
  domain/txt/ChapterRule.kt
  domain/txt/TxtChapterSplitter.kt
  domain/txt/TxtBookSource.kt
  domain/txt/TextPageBreaker.kt                    （含 PageSanitizer / PageMapping / ScrollMapping）
  domain/progress/ProgressCalculator.kt
  domain/progress/TxtProgressRemap.kt
  domain/settings/BackgroundPalette.kt             （含 BrightnessController）
  data/db/AppDatabase.kt
  data/db/BookEntity.kt
  data/db/ChapterEntity.kt
  data/db/BookDao.kt
  data/repository/BookRepository.kt
  data/settings/SettingsRepository.kt
  data/file/ContentUriSeekableFile.kt
  data/epub/EpubSession.kt
  data/epub/EpubStyle.kt                           （对应计划中的 EpubCss）
```

测试位于 `app/src/test/java/com/ccread/reader/...`，与主代码同包结构。

---

### Task 1: 工具链与可构建的项目骨架

**Files:**
- Create: `Dockerfile`、`.dockerignore`、`settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`gradle/libs.versions.toml`、`app/build.gradle.kts`、`app/src/main/AndroidManifest.xml`、`app/src/main/res/values/strings.xml`、`app/src/main/java/com/ccread/reader/MainActivity.kt`、`app/src/main/java/com/ccread/reader/JingduApp.kt`、`app/src/main/java/com/ccread/reader/ui/theme/Theme.kt`

**Interfaces:**
- Consumes: 无
- Produces: 可安装的空 App；`MainActivity` 继承 `FragmentActivity` 并调用 `setContent { JingduTheme { ... } }`；`strings.xml` 中 `app_name = 静读`；本机不安装 JDK/SDK/Android Studio

- [x] **Step 1: 写构建镜像的 Dockerfile**

本机已有 Docker Desktop（Linux 容器）。`Dockerfile` 内容：

```dockerfile
FROM gradle:8.13-jdk21

USER root
RUN apt-get update && apt-get install -y --no-install-recommends unzip curl \
 && rm -rf /var/lib/apt/lists/*

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
RUN mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools" \
 && curl -fsSL -o /tmp/cmdline-tools.zip \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
 && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools" \
 && mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest" \
 && yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses \
 && "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" "platforms;android-36" "build-tools;36.0.0" \
 && rm /tmp/cmdline-tools.zip

ENV PATH="${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools"
ENV GRADLE_USER_HOME=/root/.gradle
WORKDIR /project
```

`.dockerignore` 至少包含 `.gradle`、`app/build`、`.idea`、`docs`。若 Google 的固定下载地址失效，从 <https://developer.android.com/studio#command-line-tools-only> 取最新 Linux 版地址替换。

- [x] **Step 2: 构建镜像并验证工具链**

```powershell
docker build -t jingdu-android:latest .
docker run --rm jingdu-android:latest bash -lc "java -version && sdkmanager --version && ls `$ANDROID_SDK_ROOT/platforms"
```

Expected: Java 21、sdkmanager 版本号、目录中出现 `android-36`。

- [x] **Step 3: 创建 Gradle 骨架**

`settings.gradle.kts` 声明 `rootProject.name = "jingdu"`、`:app` 模块、`google()`/`mavenCentral()` 仓库。`app/build.gradle.kts` 使用：

- `compileSdk = 36`、`minSdk = 26`、`targetSdk = 35`
- `kotlin("android")` 2.4.20、`org.jetbrains.kotlin.plugin.compose` 2.4.20
- 最终版本组合（已写入 `libs.versions.toml`）：AGP 8.13.2、Kotlin 2.4.20、KSP 2.3.12、Compose BOM 2025.05.00、activity-compose 1.10.1、Room 2.8.5、DataStore 1.3.0、kotlinx-serialization 1.12.0、kotlinx-coroutines 1.11.0、Robolectric 4.17、desugar_jdk_libs 2.1.5、Readium 3.1.2
- `androidx.activity:activity-compose`、`androidx.compose.material3`
- `buildFeatures { compose = true }`，Java/Kotlin target 17
- `namespace = "com.ccread.reader"`、`applicationId = "com.ccread.reader"`
- 测试依赖：JUnit4、Robolectric、`androidx.test:core`、`kotlinx-coroutines-test`

Kotlin 固定 2.4.20（与 Readium 3.1.2 的元数据版本对齐，不要降低）；`compileSdk` 必须为 36（AndroidX 1.13 与 Readium 3.1.2 的依赖链要求，35 会编译失败）；Compose BOM 不得高于 2025.05.00（更高版本要求 AGP 9.x / SDK 37）。若上述组合无法解析，查询 Maven Central 的最新稳定版并更新 `libs.versions.toml`，同时把最终版本写回本计划。

`AndroidManifest.xml` 只声明 `MainActivity`（`android:exported="true"`，LAUNCHER），**不写任何权限**，`android:screenOrientation="portrait"`，`android:allowBackup="false"`（不启用系统云备份）。

- [x] **Step 4: 生成 wrapper、启动开发容器并构建**

```powershell
docker run --rm -v "${PWD}:/project" -w /project jingdu-android:latest gradle wrapper --gradle-version 8.13
docker run -d --name jingdu-dev -v "${PWD}:/project" -v jingdu-gradle:/root/.gradle -w /project jingdu-android:latest sleep infinity
docker exec jingdu-dev ./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`，产物位于 `app/build/outputs/apk/debug/app-debug.apk`。后续所有 Gradle 命令都用这个常驻容器（`docker exec jingdu-dev ...`），避免每次冷启动。

- [x] **Step 5: 验证 APK 没有联网权限**

```powershell
docker exec jingdu-dev bash -lc '"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/debug/app-debug.apk'
```

Expected: 输出中不出现 `android.permission.INTERNET`。

- [x] **Step 6: 无线 adb 安装到设备确认启动**

手机开启开发者选项与"无线调试"（与电脑同一局域网），在手机上点"使用配对码配对设备"获取 IP、配对端口和 6 位码：

```powershell
docker exec jingdu-dev adb pair <手机IP>:<配对端口> <配对码>
docker exec jingdu-dev adb connect <手机IP>:<调试端口>
docker exec jingdu-dev adb devices
docker exec jingdu-dev adb install -r app/build/outputs/apk/debug/app-debug.apk
docker exec jingdu-dev adb shell am start -n com.ccread.reader/.MainActivity
```

Expected: `adb devices` 出现设备；出现空白界面，应用标题为"静读"，无崩溃。若容器无法直连手机，用 `docker cp jingdu-dev:/project/app/build/outputs/apk/debug/app-debug.apk .` 把 APK 拷回 Windows，再传到手机安装。日志用 `docker exec jingdu-dev adb logcat` 查看。

---

### Task 2: 领域模型与持久化

**Files:**
- Create: `domain/model/Book.kt`、`domain/model/Chapter.kt`、`domain/model/TocNode.kt`、`domain/model/ReadingProgress.kt`、`domain/model/ReadingSettings.kt`、`data/db/BookEntity.kt`、`data/db/ChapterEntity.kt`、`data/db/BookDao.kt`、`data/db/AppDatabase.kt`、`data/repository/BookRepository.kt`、`data/settings/SettingsRepository.kt`
- Test: `app/src/test/java/com/ccread/reader/data/BookDaoTest.kt`、`SettingsRepositoryTest.kt`、`ProgressCodecTest.kt`

**Interfaces:**
- Consumes: Task 1 的 Gradle 配置
- Produces:
  - `enum class BookFormat { TXT, EPUB }`
  - `data class Book(val id: Long, val title: String, val format: BookFormat, val uri: String, val sizeBytes: Long, val importedAt: Long, val lastReadAt: Long?, val progress: ReadingProgress?, val encodingName: String?, val totalChars: Long?)`
  - `data class Chapter(val bookId: Long, val index: Int, val title: String, val startByte: Long, val endByte: Long, val charStart: Long, val charCount: Long)`
  - `data class TocNode(val title: String, val href: String?, val children: List<TocNode>)`
  - `sealed interface ReadingProgress { val percent: Double }`，子类 `TxtProgress(percent, chapterIndex, charOffsetInChapter)` 与 `EpubProgress(percent, locatorJson)`
  - `enum class PageMode { PAGED, SCROLL }`
  - `enum class BackgroundId { WHITE, BEIGE, GREEN, DARK_GRAY, BLACK }`
  - `data class ReadingSettings(fontSize: Int = 18, marginHorizontal: Int = 24, lineSpacing: Float = 1.5f, backgroundId: BackgroundId = BackgroundId.BEIGE, pageMode: PageMode = PageMode.PAGED, followSystemBrightness: Boolean = true, brightness: Float = 0.5f)`
  - `object ProgressCodec { fun encode(progress: ReadingProgress): String; fun decode(json: String?): ReadingProgress? }`
  - `interface BookRepository { fun observeBooks(): Flow<List<Book>>; suspend fun getBook(id: Long): Book?; suspend fun addBook(book: Book, chapters: List<Chapter>): Long; suspend fun updateLastRead(id: Long, at: Long); suspend fun saveProgress(id: Long, progress: ReadingProgress); suspend fun deleteBook(book: Book, deleteOriginalFile: Boolean): Boolean; suspend fun getChapters(bookId: Long): List<Chapter>; suspend fun replaceChapters(bookId: Long, chapters: List<Chapter>); suspend fun findByUri(uri: String): Book? }`
  - `class SettingsRepository(context: Context) { val settings: Flow<ReadingSettings>; suspend fun update(transform: (ReadingSettings) -> ReadingSettings) }`

- [x] **Step 1: 写失败的 DAO 测试**

```kotlin
@RunWith(RobolectricTestRunner::class)
class BookDaoTest {
    @Test fun `books are ordered by last read then import time`() = runTest { /* 插入 A(imported=1,lastRead=null), B(imported=2,lastRead=5)，断言顺序 [B, A] */ }
    @Test fun `deleting a book removes its chapters`() = runTest { /* 插入书与 2 章，删除书，断言 getChapters 为空 */ }
    @Test fun `progress round trips through json`() {
        val p = TxtProgress(12.34, 3, 120)
        assertEquals(p, ProgressCodec.decode(ProgressCodec.encode(p)))
    }
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*BookDaoTest"`
Expected: FAIL，类不存在。

- [x] **Step 3: 实现实体、DAO、数据库、仓库与设置**

Room 3 个必需依赖：`room-runtime`、`room-ktx`、KSP 编译器；`libs.versions.toml` 统一版本。`BookDao` 的查询用 `ORDER BY COALESCE(lastReadAt, importedAt) DESC`。`BookRepository` 用 `androidx.room.withTransaction` 保证书与章节一起写入。`SettingsRepository` 用 `DataStore<Preferences>`，字段逐项映射；读取时对数值做范围钳制（字号 12–36、边距 8–48、行距 1.0–2.0、亮度 0.0–1.0）。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*BookDaoTest" --tests "*SettingsRepositoryTest" --tests "*ProgressCodecTest"`
Expected: PASS。

---

### Task 3: 编码识别与流式分章器

**Files:**
- Create: `domain/txt/SeekableFile.kt`、`domain/txt/CharsetDetector.kt`、`domain/txt/ChapterRule.kt`、`domain/txt/TxtChapterSplitter.kt`
- Test: `app/src/test/java/com/ccread/reader/domain/txt/CharsetDetectorTest.kt`、`TxtChapterSplitterTest.kt`、`FakeSeekableFile.kt`

**Interfaces:**
- Consumes: 无（纯 Kotlin/JVM）
- Produces:
  - `interface SeekableFile { val sizeBytes: Long; fun readAt(offset: Long, length: Int): ByteArray; fun openStreamAt(offset: Long): InputStream; fun close() }`
  - `object CharsetDetector { fun detect(prefix: ByteArray): Charset }`
  - `data class ChapterRule(val name: String, val pattern: Regex, val titleGroup: Int = 0, val enabled: Boolean = true)`
  - `object BuiltInChapterRules { val all: List<ChapterRule> }`
  - `data class ChapterBoundary(val title: String, val startByte: Long, val endByte: Long, val charStart: Long, val charCount: Long)`
  - `data class TxtIndex(val charset: Charset, val totalChars: Long, val chapters: List<ChapterBoundary>)`
  - `data class SplitterParams(val minChapterChars: Long = 200, val maxChapterChars: Long = 200_000, val fallbackChunkChars: Long = 5_000, val maxTitleChars: Int = 30, val rules: List<ChapterRule> = BuiltInChapterRules.all.filter { it.enabled })`
  - `object TxtChapterSplitter { fun split(source: SeekableFile, charset: Charset, params: SplitterParams = SplitterParams()): TxtIndex }`

- [x] **Step 1: 写失败的识别测试**

```kotlin
@Test fun `UTF-8 BOM 被识别`() { assertEquals("UTF-8", CharsetDetector.detect(byteArrayOf(0xEF.toByte(),0xBB.toByte(),0xBF.toByte(),'a'.code.toByte())).name()) }
@Test fun `UTF-16LE BOM 被识别`() { assertEquals("UTF-16LE", CharsetDetector.detect(byteArrayOf(0xFF.toByte(),0xFE.toByte(),0x41,0x00)).name()) }
@Test fun `无 BOM 的 GBK 回退到 GB18030`() { val b = "第一章 起风了".toByteArray(charset("GBK")); assertEquals("GB18030", CharsetDetector.detect(b).name()) }
@Test fun `随机字节不抛异常`() { CharsetDetector.detect(ByteArray(4096) { it.toByte() }) }
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*CharsetDetectorTest"`
Expected: FAIL，类不存在。

- [x] **Step 3: 实现 CharsetDetector**

先看 BOM（EF BB BF → UTF-8；FF FE → UTF-16LE；FE FF → UTF-16BE）；无 BOM 时用严格解码器尝试 UTF-8（`CodingErrorAction.REPORT`），失败返回 `GB18030`。只检查前缀，不做整文件校验。

- [x] **Step 4: 写失败的分章测试**

```kotlin
private fun split(text: String, params: SplitterParams = SplitterParams()) =
    TxtChapterSplitter.split(FakeSeekableFile(text.toByteArray()), Charsets.UTF_8, params)

@Test fun `识别 第X章 并在标题前生成前言`() {
    val idx = split("引子正文\n第一章 起点\n正文一\n第二章 归途\n正文二")
    assertEquals(listOf("前言", "第一章 起点", "第二章 归途"), idx.chapters.map { it.title })
}
@Test fun `中文数字与 Chapter 都能识别`() {
    val idx = split("第十二章 风\n正文\nChapter 3 Home\n正文")
    assertEquals(listOf("第十二章 风", "Chapter 3 Home"), idx.chapters.map { it.title })
}
@Test fun `无章节时按 5000 字兜底且目录非空`() {
    val idx = split("字".repeat(12_000), SplitterParams(fallbackChunkChars = 5_000))
    assertEquals(3, idx.chapters.size)
    assertTrue(idx.chapters.all { it.title.startsWith("第") && it.title.endsWith("部分") })
}
@Test fun `短章合并 超长章拆分`() {
    val short = split("第一章 A\n短\n第二章 B\n" + "长".repeat(600), SplitterParams(minChapterChars = 200))
    assertEquals(2, short.chapters.size)
    val long = split("第一章 A\n" + "长".repeat(450_000) + "\n第二章 B\n正文", SplitterParams(maxChapterChars = 200_000))
    assertTrue(long.chapters.all { it.charCount <= 200_001 })
}
@Test fun `标题过长不当作章节`() { /* 构造 31 字的"第X章"行，断言只生成前言 */ }
@Test fun `字节偏移可精确定位原文章节`() { /* 用 FakeSeekableFile.readAt(startByte, endByte-startByte) 解码，断言以标题开头 */ }
@Test fun `扫描大文件时每次读取不超过 64KB`() { /* FakeSeekableFile 记录最大单次 read 长度，断言 <= 65536 */ }
@Test fun `超长单行不会无限累积`() {
    val idx = split("长".repeat(400_000)) // 约 1.2MB 无换行
    assertTrue(idx.chapters.isNotEmpty())
}
```

- [x] **Step 5: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*TxtChapterSplitterTest"`
Expected: FAIL。

- [x] **Step 6: 实现流式分章器**

按行扫描：分块读入（64KB），按编码的换行字节切行（UTF-8/GB18030 用 `0x0A`，UTF-16 按 BOM 用双字节换行），每行单独解码，避免整本进内存。单行累积超过 1MB 时强制在该处断开，防止无换行文件撑爆内存。对每个候选标题行依次尝试启用中的规则；匹配则关闭上一章并开新章。规则如下：

```kotlin
val all = listOf(
    ChapterRule("第X章", Regex("""^\s{0,4}第[0-9零〇一二三四五六七八九十百千万两]+[章回节集篇部卷]\s{0,2}.{0,30}$"""), titleGroup = 0),
    ChapterRule("特殊章节", Regex("""^\s{0,4}(序章|楔子|前言|序言|引子|尾声|后记|番外)(\s+.{0,30})?$"""), titleGroup = 0),
    ChapterRule("Chapter", Regex("""^\s{0,4}(Chapter|CHAPTER)\s+\d{1,4}.{0,30}$"""), titleGroup = 0),
    ChapterRule("数字序号", Regex("""^\d{1,4}[、.．].{0,20}$"""), titleGroup = 0, enabled = false),
)
```

标题取 `match.groupValues[titleGroup].trim()`（即整行）；去掉 `第…章`/`Chapter N` 等标记后的长度 > `maxTitleChars` 则不算标题。短章（`charCount < minChapterChars` 且不是最后一章）并入上一章（没有上一章时保留；"前言"不参与合并）；单章超过 `maxChapterChars` 时在行边界拆成"原题（续）"。全书识别不足 2 章时按 `fallbackChunkChars` 逐行累加切块，标题 `第 N 部分`。字符计数包含每行末尾的换行符（统一记 1 个字符）。

- [x] **Step 7: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*CharsetDetectorTest" --tests "*TxtChapterSplitterTest"`
Expected: PASS。

---

### Task 4: 导入管线（SAF、格式、上限、可定位校验）

**Files:**
- Create: `domain/book/BookFormat.kt`、`domain/book/ImportResult.kt`、`domain/book/BookImporter.kt`、`data/file/ContentUriSeekableFile.kt`
- Test: `app/src/test/java/com/ccread/reader/domain/book/ImportRulesTest.kt`

**Interfaces:**
- Consumes: `BookRepository`、`TxtChapterSplitter`、`CharsetDetector`、`SeekableFile`
- Produces:
  - `sealed interface ImportResult { data class Success(val bookId: Long): ImportResult; data class TooLarge(val limitBytes: Long): ImportResult; data object UnsupportedFormat: ImportResult; data object NotSeekable: ImportResult; data object Duplicate: ImportResult; data class Failed(val message: String): ImportResult }`
  - `object ImportRules { fun formatOf(fileName: String, mime: String?): BookFormat?; fun limitBytes(format: BookFormat): Long; fun titleOf(fileName: String): String }`
  - `class BookImporter(context: Context, repository: BookRepository) { suspend fun import(uri: Uri): ImportResult }`
  - `class ContentUriSeekableFile(context: Context, uri: Uri) : SeekableFile`

- [x] **Step 1: 写失败的导入规则测试**

```kotlin
@Test fun `扩展名与 mime 决定格式`() {
    assertEquals(BookFormat.TXT, ImportRules.formatOf("a.TXT", null))
    assertEquals(BookFormat.EPUB, ImportRules.formatOf("b.epub", "application/epub+zip"))
    assertNull(ImportRules.formatOf("c.pdf", "application/pdf"))
}
@Test fun `上限是 200MB 与 500MB`() {
    assertEquals(200L * 1024 * 1024, ImportRules.limitBytes(BookFormat.TXT))
    assertEquals(500L * 1024 * 1024, ImportRules.limitBytes(BookFormat.EPUB))
}
@Test fun `书名去掉扩展名并保留其余点号`() {
    assertEquals("三国演义.全本", ImportRules.titleOf("三国演义.全本.txt"))
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*ImportRulesTest"`
Expected: FAIL。

- [x] **Step 3: 实现导入器**

流程：`contentResolver.query(OpenableColumns)` 读文件名与大小 → `takePersistableUriPermission(FLAG_GRANT_READ_URI_PERMISSION)` → 校验格式与大小 → `findByUri` 去重 → `openFileDescriptor(uri, "r")` 校验可定位（`FileChannel.position()` 可用）→ TXT：取前 64KB 识别编码，用 `TxtChapterSplitter` 生成索引，`Book.encodingName = index.charset.name()`、`Book.totalChars = index.totalChars` → 事务写入 `Book` + `Chapter` → 返回 `Success`。EPUB 的 `totalChars` 与 `encodingName` 为 null。所有 IO 在 `Dispatchers.IO`。异常转 `Failed(message)`，不得崩溃。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*ImportRulesTest"`
Expected: PASS。

- [x] **Step 5: 真机手工验证**

导入一个 GBK 编码的 TXT 与一个 EPUB，确认书架出现条目；再用一个超限文件确认提示；用来自流式提供方（如某些网盘）的文件确认"不可定位"提示。

---

### Task 5: 书架界面、排序、删除、详情、文件失效

**Files:**
- Create: `ui/bookshelf/BookshelfScreen.kt`、`BookshelfViewModel.kt`、`BookCard.kt`、`DeleteBookDialog.kt`、`BookDetailsDialog.kt`、`ui/navigation/AppNavHost.kt`
- Test: `app/src/test/java/com/ccread/reader/ui/bookshelf/BookshelfViewModelTest.kt`

**Interfaces:**
- Consumes: `BookRepository`、`BookImporter`、`Book`、`BookFormat`
- Produces:
  - `data class BookshelfUiState(val books: List<Book>, val importing: Boolean, val message: String?)`
  - `class BookshelfViewModel(repository: BookRepository, importer: BookImporter) { val state: StateFlow<BookshelfUiState>; fun import(uri: Uri); fun delete(book: Book, deleteOriginal: Boolean); fun onBookOpened(book: Book) }`
  - `@Composable fun BookshelfScreen(onOpenBook: (Long) -> Unit, viewModel: BookshelfViewModel)`

- [x] **Step 1: 写失败的排序与删除测试**

```kotlin
@Test fun `最近阅读置顶 未读按导入时间`() = runTest {
    val books = listOf(
        book(id = 1, importedAt = 100, lastReadAt = null),
        book(id = 2, importedAt = 200, lastReadAt = null),
        book(id = 3, importedAt = 50, lastReadAt = 300))
    assertEquals(listOf(3L, 2L, 1L), repository.observeBooks().first().map { it.id })
}
@Test fun `删除未勾选原文件时只移除书架`() = runTest { /* fake repo 断言 deleteOriginalFile = false */ }
@Test fun `缺失文件时返回重新选择状态`() { /* open(book) 抛 FileNotFoundException，断言 state.message 包含"找不到原文件"，且 book 保留 */ }
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*BookshelfViewModelTest"`
Expected: FAIL。

- [x] **Step 3: 实现书架 UI 与 ViewModel**

- 顶部行：左"书架"，右"+"按钮；`+` 用 `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`，MIME `arrayOf("text/plain","application/epub+zip")`。
- 网格 `LazyVerticalGrid(columns = GridCells.Fixed(3))`；书方块圆角矩形 + 左侧书脊色带；书名 `maxLines = 3`、`overflow = TextOverflow.Ellipsis`；格式标签。
- 长按用 `combinedClickable`，弹出与方块锚定的 `Popup`，含"删除""详情"。
- 删除弹窗含 `Checkbox`"同时删除本地文件"；确认后调用 `repository.deleteBook(book, checked)`；返回 false 时提示"无法删除原文件"。
- 详情弹窗显示完整书名、大小（KB/MB 格式化）、格式、原始文件位置（解码后的 URI）。
- 打开书时更新 `lastReadAt`；文件缺失时显示恢复对话框，提供"重新选择文件"（`OpenDocument`，选中后替换 URI 与索引）与"从书架移除"。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*BookshelfViewModelTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

导入两本书，阅读其中一本后返回，确认它移到左上角；长按删除与详情行为符合规格；删除时分别勾选/不勾选，检查原文件。

---

### Task 6: 阅读页壳、点击区与菜单显隐

**Files:**
- Create: `ui/reader/ReaderScreen.kt`、`ReaderViewModel.kt`、`ReaderTopBar.kt`、`ReaderBottomBar.kt`、`ReaderMenu.kt`
- Test: `app/src/test/java/com/ccread/reader/ui/reader/ReaderInteractionTest.kt`

**Interfaces:**
- Consumes: `Book`、`ReadingSettings`、`SettingsRepository`
- Produces:
  - `enum class TapZone { LEFT, CENTER, RIGHT }`
  - `data class OverlayState(val menu: Boolean = false, val toc: Boolean = false, val settings: Boolean = false, val brightness: Boolean = false)`
  - `object ReaderInteraction { fun zoneFor(x: Float, width: Float): TapZone; fun onTap(zone: TapZone, mode: PageMode, overlays: OverlayState): OverlayState; fun onBack(overlays: OverlayState): OverlayState }`
  - `data class ReaderUiState(val book: Book?, val chapterTitle: String, val percentText: String, val overlays: OverlayState)`
  - `class ReaderViewModel(bookId: Long, repository: BookRepository, settingsRepository: SettingsRepository) { val state: StateFlow<ReaderUiState>; fun onTapZone(zone: TapZone); fun nextPage(); fun previousPage(); fun openToc(); fun openSettings(); fun openBrightness(); fun dismissOverlays() }`
  - `@Composable fun ReaderScreen(bookId: Long, onBack: () -> Unit, viewModel: ReaderViewModel)`

- [x] **Step 1: 写失败的交互测试**

```kotlin
@Test fun `三区划分符合 1-3 1-3 1-3`() {
    assertEquals(TapZone.LEFT, ReaderInteraction.zoneFor(10f, 300f))
    assertEquals(TapZone.CENTER, ReaderInteraction.zoneFor(150f, 300f))
    assertEquals(TapZone.RIGHT, ReaderInteraction.zoneFor(290f, 300f))
}
@Test fun `滚动模式下左右点击不翻页`() { /* onTap(LEFT, SCROLL) 不改变页码回调 */ }
@Test fun `中间点击打开菜单 再次点击关闭`() { /* overlays.menu 翻转 */ }
@Test fun `返回键先关面板`() {
    val closed = ReaderInteraction.onBack(OverlayState(menu = true, toc = true))
    assertFalse(closed.toc); assertTrue(closed.menu)
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*ReaderInteractionTest"`
Expected: FAIL。

- [x] **Step 3: 实现阅读页壳**

- 全屏沉浸：`WindowCompat.setDecorFitsSystemWindows(window, false)` + `WindowInsetsControllerCompat.hide(statusBars|navigationBars)`；离开阅读页时恢复。
- 顶部行：章节名居中（`weight(1f)` + `textAlign = Center` + 左侧占位），时间靠右，`HH:mm`，用 `LaunchedEffect` 每 15 秒刷新；字号约为正文 0.75 倍，次要颜色。
- 底部：百分比居中，`PercentFormatter.format` 输出两位小数。
- 点击区覆盖整个正文区域，`pointerInput` 计算 `TapZone`；PAGED 模式左右区触发翻页回调，SCROLL 模式忽略；CENTER 切换菜单。
- 底部菜单三项：目录、阅读设置、亮度；点任一项先收起菜单再打开对应面板（由后续任务填充）。
- 返回键：`BackHandler` 依次关闭 brightness → settings → toc → menu，全部关闭后 `onBack()`。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*ReaderInteractionTest"`
Expected: PASS。

---

### Task 7: TXT 章节读取、百分比、位置恢复

**Files:**
- Create: `domain/txt/TxtBookSource.kt`、`domain/progress/ProgressCalculator.kt`（`PercentFormatter` 也在此文件）
- Test: `app/src/test/java/com/ccread/reader/domain/txt/TxtBookSourceTest.kt`、`ProgressCalculatorTest.kt`

**Interfaces:**
- Consumes: `SeekableFile`、`Book`、`Chapter`、`ReadingProgress.TxtProgress`
- Produces:
  - `class TxtBookSource(source: SeekableFile, chapters: List<Chapter>, charset: Charset, totalChars: Long) { fun chapterText(chapterIndex: Int): String; fun totalChars(): Long }`
  - `object ProgressCalculator { fun txtPercent(totalChars: Long, chapterCharStart: Long, offsetInChapter: Long): Double; fun locate(chapters: List<Chapter>, charPosition: Long): Pair<Int, Long> }`
  - `object PercentFormatter { fun format(percent: Double): String }`

- [x] **Step 1: 写失败的测试**

```kotlin
@Test fun `按字节偏移读取指定章节`() { /* 两章 UTF-8 文本，断言 chapterText(1) 以 "第二章" 开头且不含第一章正文 */ }
@Test fun `百分比按字符偏移计算并钳制`() {
    assertEquals(50.0, ProgressCalculator.txtPercent(1000, 400, 100), 0.001)
    assertEquals(0.0, ProgressCalculator.txtPercent(1000, 0, -5), 0.001)
    assertEquals(0.0, ProgressCalculator.txtPercent(0, 0, 0), 0.001)
}
@Test fun `格式化保留两位小数`() {
    assertEquals("12.34%", PercentFormatter.format(12.34))
    assertEquals("0.00%", PercentFormatter.format(0.0))
    assertEquals("100.00%", PercentFormatter.format(100.0))
}
@Test fun `位置映射到所在章节与章内偏移`() { /* chapters 起点 0/300/700，charPosition=500 → (1, 200) */ }
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*TxtBookSourceTest" --tests "*ProgressCalculatorTest"`
Expected: FAIL。

- [x] **Step 3: 实现**

`chapterText` 用 `source.readAt(startByte, (endByte - startByte).toInt())` 解码；章节超过 `Int.MAX_VALUE` 不现实（已由 200MB 上限约束）。阅读页用 `Book.encodingName`、`Book.totalChars` 与 Room 中该书章节列表重建 `TxtBookSource`（`charset = charset(encodingName)`），不依赖导入期的内存对象。`txtPercent` = `(chapterCharStart + offsetInChapter) / totalChars * 100`，钳制 0–100；`totalChars == 0` 返回 0.0。`PercentFormatter` 用 `String.format(Locale.US, "%.2f%%", percent)`。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*TxtBookSourceTest" --tests "*ProgressCalculatorTest"`
Expected: PASS。

---

### Task 8: TXT 左右翻页（分页 + 横向滑动 + 点击）

**Files:**
- Create: `domain/txt/TextPageBreaker.kt`、`ui/reader/txt/TxtPagedReader.kt`
- Test: `app/src/test/java/com/ccread/reader/domain/txt/PageMappingTest.kt`

**Interfaces:**
- Consumes: `TxtBookSource`、`ReadingSettings`、`PageMode`
- Produces:
  - `data class Page(val text: String, val startChar: Int, val endChar: Int)`
  - `interface TextPageBreaker { fun paginate(text: String): List<Page> }`
  - `class ComposePageBreaker(measurer: TextMeasurer, widthPx: Int, heightPx: Int, style: TextStyle) : TextPageBreaker`
  - `object PageMapping { fun shouldFlip(dragPx: Float, widthPx: Float): Boolean; fun pageForOffset(pages: List<Page>, offset: Int): Int }`
  - `@Composable fun TxtPagedReader(source: TxtBookSource, chapters: List<Chapter>, settings: ReadingSettings, startChapterIndex: Int, startOffsetInChapter: Int, onPositionChanged: (chapterIndex: Int, offsetInChapter: Int, percent: Double) -> Unit)`

- [x] **Step 1: 写失败的映射测试**

```kotlin
@Test fun `滑动超过屏宽 20% 才翻页`() {
    assertTrue(PageMapping.shouldFlip(dragPx = 61f, widthPx = 300f))
    assertFalse(PageMapping.shouldFlip(dragPx = 59f, widthPx = 300f))
}
@Test fun `按字符偏移找到所在页`() {
    val pages = listOf(Page("a", 0, 10), Page("b", 10, 20), Page("c", 20, 30))
    assertEquals(1, PageMapping.pageForOffset(pages, 15))
    assertEquals(2, PageMapping.pageForOffset(pages, 999))
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*PageMappingTest"`
Expected: FAIL。

- [x] **Step 3: 实现分页与阅读组件**

`ComposePageBreaker`：用 `TextMeasurer.measure` 对文本做二分查找，找出在给定宽高内可容纳的最长前缀；按段落优先断行，记录每页起止字符索引。分页结果按 `(chapterIndex, fontSize, margins, lineSpacing, width, height)` 缓存。

`TxtPagedReader`：当前章节用 `Box` 渲染当前页文本；`pointerInput` 处理横向拖动，位移实时偏移页面，松手按 `shouldFlip` 翻页，动画使用 `animateFloatAsState` 或 `Animatable`；左/中/右点击由外层 `ReaderScreen` 通过回调驱动（`nextPage` / `previousPage` / `toggleMenu`）。到章末自动进入下一章第 1 页，章首向前回到上一章最后一页。每次翻页通过 `onPositionChanged` 上报 `chapter.charStart + page.startChar` 对应的章内偏移与百分比。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*PageMappingTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

打开一本多章 TXT：点击左/右翻页、横向滑动跟手、章末进入下一章、字号改变后重排仍停在原位置附近。

---

### Task 9: TXT 上下滚动模式与模式切换位置保持

**Files:**
- Create: `ui/reader/txt/TxtScrollReader.kt`
- Test: `app/src/test/java/com/ccread/reader/domain/txt/ScrollMappingTest.kt`

**Interfaces:**
- Consumes: `TxtBookSource`、`ReadingSettings`、`ProgressCalculator`
- Produces:
  - `data class ScrollItem(val chapterIndex: Int, val charStart: Int)`
  - `object ScrollMapping { fun positionFor(items: List<ScrollItem>, firstVisibleItem: Int): Pair<Int, Int> }`（返回 `chapterIndex to offsetInChapter`）
  - `@Composable fun TxtScrollReader(source: TxtBookSource, chapters: List<Chapter>, settings: ReadingSettings, startChapter: Int, startOffset: Int, onPositionChanged: (chapterIndex: Int, offsetInChapter: Int, percent: Double) -> Unit)`

- [x] **Step 1: 写失败的滚动映射测试**

```kotlin
@Test fun `首个可见段落映射到章节与偏移`() {
    val items = listOf(ScrollItem(0, 0), ScrollItem(0, 100), ScrollItem(0, 250), ScrollItem(1, 0))
    assertEquals(0 to 250, ScrollMapping.positionFor(items, firstVisibleItem = 2))
    assertEquals(1 to 0, ScrollMapping.positionFor(items, firstVisibleItem = 3))
}
@Test fun `空列表返回 0 到 0`() {
    assertEquals(0 to 0, ScrollMapping.positionFor(emptyList(), 0))
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*ScrollMappingTest"`
Expected: FAIL。

- [x] **Step 3: 实现滚动阅读**

当前章节按段落（`\n` 切分）放入 `LazyColumn`；`LaunchedEffect` 观察 `listState.firstVisibleItemIndex` 上报偏移；滚动到末尾附近时追加下一章段落（保留最近 2 章、丢弃更早章节）。进入时用 `startOffset` 估算段落索引并 `scrollToItem`。从翻页模式切到滚动模式、或反向切换时，由 `ReaderViewModel` 把当前 `(chapterIndex, offset)` 传给对应组件，保证位置不跳回章首。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*ScrollMappingTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

滚动模式上下滚动流畅、跨章连续；切换模式后停在原位置；左右点击无反应。

---

### Task 10: 阅读设置、背景与亮度

**Files:**
- Create: `ui/reader/ReadingSettingsSheet.kt`（含亮度面板）、`domain/settings/BackgroundPalette.kt`（含 `BrightnessController`）
- Modify: `ui/reader/ReaderScreen.kt`
- Test: `app/src/test/java/com/ccread/reader/domain/settings/SettingsRulesTest.kt`

**Interfaces:**
- Consumes: `ReadingSettings`、`SettingsRepository`、`PageMode`、`BackgroundId`
- Produces:
  - `data class BackgroundStyle(val background: Color, val text: Color)`
  - `object BackgroundPalette { fun styleOf(id: BackgroundId): BackgroundStyle; val all: List<BackgroundId> }`
  - `object BrightnessController { fun windowBrightness(followSystem: Boolean, value: Float): Float }`
  - `@Composable fun ReadingSettingsSheet(settings: ReadingSettings, format: BookFormat, onChange: (ReadingSettings) -> Unit)`
  - `@Composable fun BrightnessSheet(settings: ReadingSettings, onChange: (ReadingSettings) -> Unit)`

- [x] **Step 1: 写失败的设置规则测试**

```kotlin
@Test fun `五种背景的文字颜色`() {
    val beige = BackgroundPalette.styleOf(BackgroundId.BEIGE)
    assertEquals(Color(0xFFF5ECD8), beige.background)
    assertEquals(Color(0xFF3B2F2F), beige.text)
    val black = BackgroundPalette.styleOf(BackgroundId.BLACK)
    assertEquals(Color(0xFF000000), black.background)
    assertEquals(Color(0xFFE8E8E8), black.text)
}
@Test fun `跟随系统时窗口亮度为 -1`() {
    assertEquals(-1f, BrightnessController.windowBrightness(true, 0.7f))
    assertEquals(0.7f, BrightnessController.windowBrightness(false, 0.7f))
}
@Test fun `设置值被钳制在范围内`() { /* SettingsRepository 的 clamp 函数：字号 5→12，99→36；行距 3→2.0 */ }
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*SettingsRulesTest"`
Expected: FAIL。

- [x] **Step 3: 实现设置面板与亮度**

- 背景与文字色常量（TXT 使用；EPUB 只用背景色）：WHITE `#FFFFFF`/`#1A1A1A`、BEIGE `#F5ECD8`/`#3B2F2F`、GREEN `#CCE8CF`/`#1F2A1F`、DARK_GRAY `#303030`/`#D0D0D0`、BLACK `#000000`/`#E8E8E8`。
- 设置面板：字号滑杆 12–36、边距 8–48dp、行距 1.0–2.0；5 个背景色块；翻页模式二选一。EPUB 时字号滑杆仍可用（按比例），边距与行距滑杆 `enabled = false` 并置灰。改动立即调用 `SettingsRepository.update`。
- 背景应用到阅读页 `Surface` / 正文文字；TXT 深色背景用浅色文字；EPUB 只把背景色交给 Task 14 的 CSS。
- 亮度面板：滑杆 0–1 + "跟随系统亮度"开关；非跟随时通过窗口 `WindowManager.LayoutParams.screenBrightness` 设置，跟随时恢复 `-1`（BRIGHTNESS_OVERRIDE_NONE）。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*SettingsRulesTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

调字号/边距/行距即时生效；5 种背景切换；亮度滑杆与跟随开关生效；退出重进设置保持。

---

### Task 11: TXT 目录抽屉与重新识别

**Files:**
- Create: `ui/reader/TocDrawer.kt`
- Modify: `ui/reader/ReaderViewModel.kt`、`ReaderScreen.kt`
- Test: `app/src/test/java/com/ccread/reader/ui/reader/TocViewModelTest.kt`

**Interfaces:**
- Consumes: `Chapter`、`TxtChapterSplitter`、`BookRepository`
- Produces:
  - `object ResplitMapping { fun map(oldChapters: List<Chapter>, newChapters: List<Chapter>, oldCharPosition: Long): Pair<Int, Long> }`
  - `data class TocRow(val key: String, val title: String, val depth: Int, val expandable: Boolean = false, val expanded: Boolean = true)`
  - `@Composable fun TocDrawer(bookTitle: String, rows: List<TocRow>, currentKey: String?, onSelect: (TocRow) -> Unit, onToggle: (TocRow) -> Unit, onResplit: (() -> Unit)?, onDismiss: () -> Unit)`

- [x] **Step 1: 写失败的重新识别映射测试**

```kotlin
@Test fun `重新识别后按字符比例落到新章节`() {
    val old = listOf(chapter(index = 0, charStart = 0, charCount = 500), chapter(index = 1, charStart = 500, charCount = 500))
    val new = listOf(chapter(index = 0, charStart = 0, charCount = 200), chapter(index = 1, charStart = 200, charCount = 800))
    val (idx, offset) = ResplitMapping.map(old, new, oldCharPosition = 600)
    assertEquals(1, idx); assertEquals(400, offset)
}
@Test fun `空目录不崩溃`() { /* 返回 (0, 0) */ }
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*TocViewModelTest"`
Expected: FAIL。

- [x] **Step 3: 实现目录抽屉**

从左侧滑入，宽 80%、全高；顶部书名；列表可滚动；当前章节高亮并自动滚动到可见位置。TXT 用章节列表构造 `TocRow`（`key = chapter.index.toString()`、`depth = 0`、不可展开）；点击一行跳转并关闭（阅读页接收目标 `chapterIndex` 与偏移 0）。TXT 目录顶部提供"重新识别"操作：重跑分章器，`replaceChapters` 后用 `ResplitMapping` 恢复位置，并提示"已按内置规则重新识别"。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*TocViewModelTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

打开目录跳章正确；重新识别后仍在原阅读位置附近；侧边栏宽度与滚动符合规格。

---

### Task 12: EPUB 打开与渲染（Readium 3.1.2）

**Files:**
- Modify: `app/build.gradle.kts`、`MainActivity.kt`、`ui/reader/ReaderScreen.kt`
- Create: `data/epub/EpubSession.kt`、`ui/reader/epub/EpubReaderView.kt`
- Test: `app/src/test/java/com/ccread/reader/data/epub/EpubTocTest.kt`

**Interfaces:**
- Consumes: `Book`、`TapZone`、`PageMode`
- Produces:
  - `class EpubSession(context: Context, uri: Uri) { suspend fun open(): Boolean; fun tocTree(): List<TocNode>; fun chapterTitleFor(href: String): String?; fun progression(locatorJson: String): Double; companion object { fun titleIn(tree: List<TocNode>, href: String): String? } }`
  - `interface EpubViewAdapter { fun goForward(); fun goBackward(); fun currentLocatorJson(): String?; fun goToLocator(json: String); fun setScrollMode(scroll: Boolean); fun applyCss(css: String) }`
  - `class ReadiumEpubAdapter(...) : EpubViewAdapter`（封装 Readium navigator 的 Fragment 与 API）
  - `@Composable fun EpubReaderView(adapter: EpubViewAdapter, onTap: (TapZone) -> Unit)`

- [x] **Step 1: 加入依赖并确认编译**

```kotlin
implementation("org.readium.kotlin-toolkit:readium-shared:3.1.2")
implementation("org.readium.kotlin-toolkit:readium-streamer:3.1.2")
implementation("org.readium.kotlin-toolkit:readium-navigator:3.1.2")
implementation("org.readium.kotlin-toolkit:readium-opds:3.1.2")
```

Run: `docker exec jingdu-dev ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL。Readium 3.1.2 的 minSdk 为 24，满足本项目 26；它需要 core library desugaring（`coreLibraryDesugaring(libs.desugar.jdk.libs)`）。**不要升到 3.4.x**：3.4.0 要求 compileSdk 37 / AGP 9.1。

- [x] **Step 2: 写失败的目录辅助测试**

```kotlin
@Test fun `按 href 查找章节标题 支持嵌套`() {
    val tree = listOf(TocNode("第一卷", "a.xhtml", listOf(TocNode("第一章", "b.xhtml", emptyList()))))
    assertEquals("第一卷", EpubSession.titleIn(tree, "a.xhtml"))
    assertEquals("第一章", EpubSession.titleIn(tree, "b.xhtml#id2"))
}
@Test fun `未知 href 返回 null`() { assertNull(EpubSession.titleIn(emptyList(), "x")) }
```

- [x] **Step 3: 实现 EPUB 会话与视图**

`EpubSession` 用 Readium 的 `AssetRetriever` + `PublicationOpener(EpubParser())` 打开 `content://` URI（3.1.2 原生支持，无需缓存整本）。`MainActivity` 改为 `FragmentActivity`，并在 `super.onCreate` 之前装 `SafeFragmentFactory`（Readium 的 Fragment 没有无参构造函数，进程重建时会崩溃）；`EpubReaderView` 用 `AndroidView` 承载 `FragmentContainerView`，通过 `supportFragmentManager` 添加 Readium 的 `EpubNavigatorFragment`。点击处理不再用 Compose 覆盖层，而是 Readium 的 `InputListener.onTap` + `elementFromPoint` 探测（详见实施结果）。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*EpubTocTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

导入一本带图片和多层目录的 EPUB，打开能正常渲染图片与书内排版；横向滑动/点击翻页；返回键退出正常。

---

### Task 13: EPUB 目录、进度与位置恢复

**Files:**
- Modify: `ui/reader/TocDrawer.kt`、`ui/reader/ReaderViewModel.kt`、`ui/reader/ReaderScreen.kt`
- Test: `app/src/test/java/com/ccread/reader/ui/reader/EpubProgressTest.kt`

**Interfaces:**
- Consumes: `EpubSession`、`EpubProgress`
- Produces:
  - `object EpubProgressRules { fun clampProgression(value: Double): Double; fun percentText(progression: Double): String }`
  - 目录抽屉支持嵌套：父节点点击跳转，箭头展开/收起；当前章节自动展开祖先链并高亮

- [x] **Step 1: 写失败的进度测试**

```kotlin
@Test fun `progression 钳制并保留两位小数`() {
    assertEquals(0.0, EpubProgressRules.clampProgression(-0.1), 0.001)
    assertEquals(1.0, EpubProgressRules.clampProgression(1.5), 0.001)
    assertEquals("12.34%", EpubProgressRules.percentText(0.1234))
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*EpubProgressTest"`
Expected: FAIL。

- [x] **Step 3: 实现**

每页变化时保存 `EpubProgress(percent = totalProgression * 100, locatorJson)`；恢复时 `goToLocator`。目录抽屉把 `TocNode` 树扁平化为 `TocRow`（`depth` 递增、父节点 `expandable = true`、`key = href`），`onToggle` 切换展开状态，`onSelect` 跳转，当前项按 href 前缀匹配并自动展开祖先链。顶部章节名通过 `EpubSession.chapterTitleFor` 反查，取最深匹配。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*EpubProgressTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

跳章、退出重进恢复位置、百分比两位小数、目录层级展开收起。

---

### Task 14: EPUB 字号/背景覆盖与模式切换

**Files:**
- Create: `data/epub/EpubStyle.kt`（原计划名 `EpubCss.kt`）
- Modify: `ui/reader/ReadingSettingsSheet.kt`、`ui/reader/epub/EpubReaderView.kt`、`ReaderViewModel.kt`、`app/src/main/res/values/dimens.xml`
- Test: `app/src/test/java/com/ccread/reader/data/epub/EpubStyleTest.kt`

**Interfaces:**
- Consumes: `ReadingSettings`、`BackgroundPalette`、`EpubPreferences`
- Produces:
  - `object EpubStyle { fun fontScale(fontSize: Int): Double; fun pageMarginFactor(marginHorizontal: Int): Double; fun preferences(settings: ReadingSettings, mode: PageMode): EpubPreferences }`

**实施偏差：** 最终没有手写 CSS，而是用 Readium 的 `EpubPreferences`（`fontSize`、`backgroundColor`、`scroll`、`pageMargins`）表达覆盖；只有图片居中样式与图片点击监听通过 JS 注入。左右留白由 `pageMargins`（= 边距 ÷ 20）实现，上下留白通过覆盖 `readium_navigator_epub_vertical_padding = 0dp` 去除。

- [x] **Step 1: 写失败的样式测试**

```kotlin
@Test fun `背景色覆盖且不设置文字颜色`() {
    val preferences = EpubStyle.preferences(ReadingSettings(backgroundId = BackgroundId.GREEN), PageMode.PAGED)
    assertEquals(0xFFCCE8CF.toInt(), preferences.backgroundColor?.int)
    assertNull(preferences.textColor)
}
@Test fun `字号按比例缩放 基准 18sp`() {
    assertEquals(1.0, EpubStyle.fontScale(18), 0.001)
    assertEquals(1.5, EpubStyle.fontScale(27), 0.001)
}
@Test fun `左右边距换算为 pageMargins 倍数`() {
    assertEquals(1.2, EpubStyle.pageMarginFactor(24), 0.001)
}
```

- [x] **Step 2: 运行确认失败**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*EpubStyleTest"`
Expected: FAIL。

- [x] **Step 3: 实现**

`EpubStyle.preferences` 每次设置变化时重新 `submitPreferences`，Readium 会保持当前 Locator；切换翻页模式即切换 `scroll` 偏好。`pageMargins` 为 `marginHorizontal / 20.0`（Readium 的 `--RS__pageGutter` 在手机宽度下为 20px）。

- [x] **Step 4: 运行确认通过**

Run: `docker exec jingdu-dev ./gradlew :app:testDebugUnitTest --tests "*EpubStyleTest"`
Expected: PASS。

- [x] **Step 5: 真机确认**

EPUB 调字号时标题与正文同比例变化；背景切换只改背景（深色下黑字看不清是预期）；切换翻页模式位置不丢；左右留白与 TXT 一致；正文上下无额外留白。

---

### Task 15: 收尾与整体验收

**Files:**
- Modify: `app/src/main/res/values/strings.xml`、`AndroidManifest.xml`、`ui/theme/Theme.kt`
- Test: 全量测试

**Interfaces:**
- Consumes: 全部
- Produces: 可交付的 debug APK

- [x] **Step 1: 补齐应用信息与图标**

`app_name = 静读`；应用图标替换为简单的书本图形（自适应图标）；主题使用无动态色、无多余动效；确认所有界面文案为简体中文。

- [x] **Step 2: 跑全量单元测试**

```powershell
docker exec jingdu-dev ./gradlew :app:testDebugUnitTest
```

Expected: 全部 PASS，无跳过。

- [x] **Step 3: 构建并检查权限**

```powershell
docker exec jingdu-dev ./gradlew :app:assembleDebug
docker exec jingdu-dev bash -lc '"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/debug/app-debug.apk'
```

Expected: 构建成功；权限列表不含 `INTERNET`。

- [x] **Step 4: 按规格做手工验收**

逐项确认设计文档第 6 节"手动验收"：

- [x] TXT 与 EPUB 各导入一本，书架显示正确
- [x] 最近阅读置顶；详情显示完整书名、大小、格式、位置
- [x] 两种模式切换，点击与滑动翻页正确，滚动模式左右点击无动作
- [x] TXT 目录跳转、重新识别、进度恢复
- [x] EPUB 层级目录展开/收起、跳转、进度恢复
- [x] 5 种背景；EPUB 深色下文字照书（可能不可读，符合预期）
- [x] 字号/边距/行距改动即时生效；EPUB 边距/行距置灰
- [x] 亮度跟随与手动切换
- [x] 删除（勾选/不勾选原文件）、缺失文件提示与重新选择
- [x] 断网下完整使用

- [x] **Step 5: 性能检查**

导入并打开一个 5MB 的 TXT，记录从点击到正文出现的时间，目标 ≤ 2 秒；连续翻页 50 次无明显卡顿；检查 Logcat 无 OOM。

---

## 实施顺序与依赖

- Task 1 → Task 2 → Task 3 → Task 4 → Task 5 完成"打开 App 能看到书架"。
- Task 6 → Task 7 → Task 8 → Task 9 → Task 10 → Task 11 完成"TXT 端到端可读"。
- Task 12 → Task 13 → Task 14 完成"EPUB 端到端可读"。
- Task 15 收尾。

Task 3 与 Task 4 的纯逻辑（编码、分章、导入规则）可以在没有设备时用 JVM 测试先行完成；Task 12 起需要真机验证 Readium 的 Fragment 与手势集成。

---

## 实施结果与偏差（以代码为准）

实施完成后回看，各任务与原计划的主要差异：

- **Task 1**：最终版本为 AGP 8.13.2 / Kotlin 2.4.20 / KSP 2.3.12 / Compose BOM 2025.05.00 / Room 2.8.5 / DataStore 1.3.0 / serialization 1.12.0 / coroutines 1.11.0 / Robolectric 4.17 / Readium 3.1.2；`compileSdk = 36`，Dockerfile 安装 `platforms;android-36` 与 `build-tools;36.0.0`；debug APK 约 21MB。
- **Task 2**：`Book` 字段为 `id/title/format/uri/sizeBytes/importedAt/lastReadAt/progress/encodingName/totalChars`；`Chapter` 主键 `(bookId, index)`；`ProgressCodec` 用 kotlinx.serialization 的多态序列化；设置读取时统一钳制范围。
- **Task 4**：`ImportRules` 独立成文件；新增 `FileLocation`（解析真实 `/storage/...` 路径）与 `TxtBookIndexer`（重新识别时复用）；`persistPermission` 先尝试读写、失败再尝试只读；导入前按 URI 去重。
- **Task 5**：`BookshelfUiState` 增加 `missingBook` 与 `openBookId`；新增 `BookshelfSorting`；`MissingFileDialog` 与详情弹窗同在 `BookDetailsDialog.kt`；详情路径可点击"在系统中打开"；"+"按钮使用 `OpenDocument` 选择 `*/*` 后由 `ImportRules` 判定格式。
- **Task 6**：`ReaderUiState` 实际包含 `loaded/book/chapters/settings/mode/percentText/chapterTitle/startChapterIndex/startOffsetInChapter/currentChapterIndex/jumpToken/missingFile/epubToc/epubCurrentHref/epubJumpHref/expandedEpubKeys/overlays`；进入阅读页时强制 `PageMode.PAGED`；返回键顺序为图片查看 → brightness → settings → toc → menu。
- **Task 8**：`pointerInput` 必须放在 `offset/padding` 之前，否则贴近边缘的点击会被内边距吞掉；分页结果过滤纯空白页（`PageSanitizer`），且不按段落边界强制换页；只预取当前章与前后各一章。
- **Task 9**：滚动模式在章末追加下一章、在顶部向前补上一章并保持可视位置；进入时的定位只执行一次（`initialScrollDone`）；点击检测使用不消费事件的 `awaitEachGesture`。
- **Task 10**：亮度面板实现在 `ReadingSettingsSheet.kt` 内（无独立的 `BrightnessSheet.kt`）；`ModalBottomSheet(skipPartiallyExpanded = true)` 保证一屏显示全部设置；滑杆在拖动结束时才提交，避免每帧写盘。
- **Task 11**：目录模型在 `TocModels.kt`；新增 `TocAncestors` 计算当前项祖先链；`TocDrawer` 打开时用 `LazyListState` 自动滚到当前项；重新识别用 `TxtProgressRemap` 保留位置。
- **Task 12**：Readium 固定 3.1.2（3.4.x 需要 SDK 37/AGP 9.1）；`MainActivity` 在 `super.onCreate` 前替换 `FragmentFactory`，避免进程重建时 `EpubNavigatorFragment` 无无参构造函数而崩溃；`FragmentContainerView` 使用稳定 id；`EpubNavigatorFragment.Configuration(shouldApplyInsetsPadding = false)`。
- **Task 13**：目录跳转通过显式 `jumpHref` 状态 + `LaunchedEffect` 消费；TXT 跳转通过 `key(jumpToken)` 重建阅读组件；EPUB 当前项按 href 精确匹配、失败再按去 fragment 前缀匹配。
- **Task 14**：`EpubCss` 改为 `EpubStyle` + `EpubPreferences`；`pageMargins = marginHorizontal / 20.0`；`dimens.xml` 覆盖 `readium_navigator_epub_vertical_padding = 0dp`；图片居中与点击监听通过 `evaluateJavascript` 注入。
- **Task 15**：新增外部"打开方式"（`ACTION_VIEW` + `singleTop`，见下）；主题统一米黄窗口背景、透明系统栏、启动页透明图标；单元测试合计 73 个。

### 后续追加需求（计划外）

1. 进入阅读页强制左右翻页（原计划保留上次模式）
2. EPUB 图片点击全屏查看：canvas → 2048px WebP data URL → `registerJavascriptInterface("CCReadBridge")` → 原生全屏 Dialog（25% 遮罩、双指缩放、平移、单击/返回关闭）
3. EPUB 正文上下留白清零、左右留白与 TXT 一致
4. 目录打开时自动滚动到当前章节
5. 所有菜单/弹窗/抽屉/图片层加简短的淡入/滑入动画
6. 书架详情显示真实 `/storage/...` 路径并可跳转系统文件管理器
7. 外部"打开方式"：清单注册 `text/plain`、`application/epub+zip`（content/file 两种 scheme），`singleTop`，传入 URI 后回书架导入并自动打开，重复打开不重复入架

### 真机验证轮次

开发期间共进行 8 轮真机反馈修复，全部记录在 `.superpowers/sdd/jingdu-reader/progress.md`。主要结论：

- TXT：分页不再出现章末空白页，也不按段落边界断页；边缘点击有效；滚动模式可上滑并跨章。
- EPUB：图片查看不再触发翻页（改为独立 Dialog 窗口）；遮罩铺满全屏；上下留白来自 Readium 的 40dp 垂直内边距，已用资源覆盖清零；左右留白来自 `pageMargins`。
- 文件路径：通过 `/proc/self/fd`、提供商 path/_data 列、MediaStore `_id` 三级解析，尽量还原 `/storage/emulated/0/...`。
- 视觉：窗口背景与启动页统一；菜单/抽屉/图片层均有动画。

---

## 打包与分发

### 构建环境

- 镜像：`docker build -t jingdu-android:latest .`（`gradle:8.13-jdk21` + cmdline-tools + `platforms;android-36` + `build-tools;36.0.0`）
- 常驻容器：

```powershell
docker run -d --name jingdu-dev -v "${PWD}:/project" -v jingdu-gradle:/root/.gradle -w /project jingdu-android:latest sleep infinity
```

- 首次需要网络下载 SDK 与依赖；Gradle 缓存保存在 `jingdu-gradle` 卷。

### 构建与测试

```powershell
docker exec jingdu-dev ./gradlew :app:testDebugUnitTest
docker exec jingdu-dev ./gradlew :app:assembleDebug
```

- 产物：`app/build/outputs/apk/debug/app-debug.apk`（debug keystore 签名，可直接安装）
- 每次 `assembleDebug` 结束后，`:app:syncDebugApk` 会自动把产物复制为仓库根目录的 `apk/jingdu.apk`
- 测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`

### 安装

- adb：`docker exec jingdu-dev adb install -r app/build/outputs/apk/debug/app-debug.apk`
- 或本机起 HTTP 服务：

```powershell
python -m http.server 8000 --bind 0.0.0.0 --directory "apk"
```

手机访问 `http://<电脑局域网IP>:8000/jingdu.apk?v=<自增版本号>`。`apk/jingdu.apk` 每次构建后自动更新。

### 发布前检查

```powershell
docker exec jingdu-dev bash -lc '"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/debug/app-debug.apk'
docker exec jingdu-dev bash -lc '"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump xmltree app/build/outputs/apk/debug/app-debug.apk --file AndroidManifest.xml | grep -A4 "android.intent.action.VIEW"'
```

- 权限不得包含 `INTERNET`、`ACCESS_NETWORK_STATE`
- 必须存在 `text/plain` 与 `application/epub+zip` 的 `ACTION_VIEW` filter
- `readium_navigator_epub_vertical_padding` 为 `0dp`

### Release 包（当前未配置）

工程目前只有 debug 构建。发布需要：

1. 生成 keystore，在 `app/build.gradle.kts` 增加 `signingConfigs.create("release")`，凭据从 `local.properties`/环境变量读取
2. `buildTypes.release` 指定签名；启用 R8 前先验证 Readium 的 `@JavascriptInterface` 与反射不被裁剪
3. 递增 `versionCode` / `versionName`（当前 `1` / `"1.0"`）
4. `docker exec jingdu-dev ./gradlew :app:assembleRelease`，产物 `app/build/outputs/apk/release/app-release.apk`
5. 用上面的检查命令复核 release 清单
