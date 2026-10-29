# 静读 — Android 本地电子书阅读器 设计文档

日期：2026-10-29  
状态：已实施（2026-10-29）。本版本已按最终代码校订，与实现不一致处一律以本文为准。

## 1. 意图与缘由

现有的手机阅读 App 普遍带开屏广告、账号体系、在线书城和推荐流。用户需要的只是一个"打开就能安静读书"的工具：不联网、不登录、无广告、无推荐，只读手机本地的 TXT 和 EPUB。

成功的标准：导入一本书 → 打开 → 左右翻页或上下滚动阅读 → 从目录跳转章节 → 调整字号/背景/边距 → 退出后再次打开，回到上次的位置。全程不产生任何网络请求。

目标设备：Android 14 手机。最低支持 Android 8.0（API 26）。

## 2. 目标 / 非目标 / 反目标

### 目标

- 导入本地 TXT、EPUB 文件并管理书架
- 两种阅读模式：左右滑动翻页、上下滚动
- TXT 自动分章与目录跳转；EPUB 使用书籍自带目录
- 字号、左右边距、行距、背景色、翻页模式设置
- 亮度调节，可跟随系统
- 记录并恢复每本书的阅读进度
- 纯净：无广告、无账号、无引导页、无联网、无统计

### 非目标

- 在线书城、图书推荐、书评社区
- 云同步、多设备进度
- 账号、登录、会员
- TTS 听书、笔记、划线、书签、搜索
- 换源、自定义书源
- PDF、MOBI、AZW3 等其它格式
- 自定义字体、自定义背景图、自定义主题
- 用户自定义分章正则
- TXT 的分卷层级目录（TXT 目录平铺；EPUB 保留书籍自带层级）
- 平板/横屏专门适配
- 多语言界面（仅简体中文）

### 反目标（即使功能"能用"，出现以下情况也算失败）

- 出现任何网络请求或联网权限
- 出现开屏页、引导页、评分弹窗、更新弹窗
- 阅读页常驻除"章节名、时间、正文、百分比"之外的控件
- 启动或打开书籍明显卡顿
- 覆盖升级后丢失书架或进度（卸载重装会清空本地数据，这是"不联网、不云备份"的直接取舍，见 5.2）

## 3. 约束

- 纯离线：AndroidManifest 不声明 `INTERNET`、`ACCESS_NETWORK_STATE` 权限（用 `tools:node="remove"` 剔除依赖库合并进来的权限）；不集成任何广告、统计、崩溃上报 SDK
- 技术栈：Kotlin 2.4.20 + Jetpack Compose（BOM 2025.05.00），单 Activity（`FragmentActivity`）
- `minSdk 26`，`targetSdk 35`，`compileSdk 36`（AndroidX 1.13 / Readium 3.1.2 要求）
- AGP 8.13.2 + Gradle 8.13 + JDK 21（容器内构建）；KSP 2.3.12；core library desugaring 2.1.5
- 锁定竖屏
- 界面语言：简体中文
- 文件访问只使用系统文件选择器（SAF）与外部 `ACTION_VIEW` 打开，不申请存储权限

## 4. 已确定的决策（用户拍板）

### 4.1 阅读页

- 常驻元素只有四样：顶部一行（章节名居中、当前时间靠右）、正文、底部百分比
- 沉浸式全屏，隐藏系统状态栏与导航栏
- 百分比保留两位小数，如 `12.34%`
- 两种模式可切换：左右滑动翻页、上下滚动
- **每次进入阅读页时，翻页模式一律重置为左右翻页**（全局设置同步改写为 `PAGED`）；滚动模式需要在阅读页内手动切换
- 左右翻页模式下：点屏幕左侧 1/3 上一页，点右侧 1/3 下一页；同时支持横向滑动手势翻页，页面跟手移动，滑动与点击都可用；点击位置贴近屏幕边缘同样有效
- 上下滚动模式下：左右点击不翻页
- 点击屏幕中间弹出"更多菜单"
- 允许系统返回手势正常使用（不做 `systemGestureExclusion` 全屏拦截）

### 4.2 菜单

- 底部一行三个按钮：目录、阅读设置、亮度；面板从屏幕最底部升起，盖住底部内容
- 点中间弹出的菜单带 25% 黑色遮罩，点遮罩关闭；菜单与遮罩带淡入/滑入动画
- 点目录：菜单收起，从左侧滑出侧边栏，占屏幕高度 100%、宽度 80%；顶部显示书名，下方为可上下滚动的目录；**打开时自动滚动到当前章节**；点击章节跳转并关闭侧边栏
- 点阅读设置：菜单收起，从底部弹出新菜单，包含字号、左右边距、行距、背景、翻页模式；面板一次性完整展开（`skipPartiallyExpanded = true`），不需要先上滑
- 点亮度：菜单收起，从底部弹出新菜单，包含亮度滑杆和"是否跟随系统亮度"的切换按钮

### 4.3 书架

- 顶部功能栏：左侧"书架"文字，右侧"+"按钮，点击选择本地文件导入
- 下方网格排列书方块，从左到右、从上到下，3 列
- 方块只显示书名和格式；书名一行放不下换行，仍放不下则最后一行末尾省略号
- 排序：默认导入时间倒序；最近阅读的书排到最前（左上角）
- 长按方块在原地弹出气泡菜单：删除、详情
- 删除：弹确认框，含"同时删除本地文件"勾选项；确认后移出书架，勾选则同时删除手机里的原始文件
- 详情：弹窗显示完整书名、文件大小、格式、本地文件位置；位置解析为真实的 `/storage/emulated/0/...` 路径（通过 `/proc/self/fd` 反查、文档提供方 path/_data 列、MediaStore 查 `_id` 等多种手段），并提供"在系统中打开"按钮跳转到系统文件管理器对应目录
- 外部"打开方式"：系统文件管理器里选择"静读"打开 TXT/EPUB 时直接导入并打开该书（见 5.2）

### 4.4 排版规则

- TXT：完全使用阅读设置（字号、边距、行距、背景）
- EPUB：尽量照书原样显示（图片、版式、字体、行距随书）；左右页面留白例外，跟随全局设置
- EPUB 中由用户设置强制覆盖的只有**字号**、**背景色**与**左右留白**（其余排版随书）
- EPUB 深色背景下**只覆盖背景色，不覆盖文字颜色**（用户确认）：书里若是黑色文字，深灰/纯黑背景可能不可读，这是明确接受的取舍，App 不做补救
- EPUB 的字号按**比例缩放全书**，标题与正文的相对大小保持不变；行距仍随书，行距滑杆在 EPUB 下置灰不可用
- **EPUB 的左右留白取自全局"左右边距"设置**：换算为 Readium 的 `pageMargins` 倍数（基准 20px），默认 24 → 左右各 24px，与 TXT 视觉一致。该滑杆在 EPUB 阅读时置灰，需在 TXT 阅读时调整全局值
- **EPUB 分页模式不保留任何上下空白**：覆盖 Readium 默认的 40dp 垂直内边距（`readium_navigator_epub_vertical_padding` → 0dp），并关闭刘海安全区内边距
- 图片在正文中居中显示（注入 `img/svg/figure` 居中样式），点击图片进入全屏查看（见 5.10）
- 字体不可切换，TXT 使用系统默认字体
- 背景只提供预置纯色，不支持自定义颜色和背景图
- 阅读设置为全局设置，所有书共用，不按书分别保存
- 背景预设 5 种：白、米黄、浅绿、深灰、纯黑；默认米黄

### 4.5 TXT 分章

- 使用内置分章规则，自动识别
- 识别不准时，用户在目录中触发"重新识别"（仍使用内置规则）即可，不提供自定义正则
- 识别失败时按固定字数分块，生成"第 N 部分"占位章节，不返回空目录

## 5. 技术方案

### 5.1 总体结构

单模块 Android 应用，MVVM：

- `ui/`：书架、阅读页、菜单/抽屉/弹窗（Compose）
- `domain/`：书籍元数据、阅读进度、设置、分章器接口
- `data/`：Room 数据库、DataStore、文件仓库、TXT 解析器、EPUB 适配器

依赖：

- Jetpack Compose（BOM 2025.05.00）+ Material3；**不使用 Navigation Compose**，书架 ↔ 阅读页的切换由 `AppNavHost` 内的 Compose 状态完成
- Room 2.8.5：书籍、TXT 章节索引
- DataStore Preferences 1.3.0：阅读设置
- Readium Kotlin Toolkit 3.1.2：EPUB 解析与 WebView 渲染（配合 core library desugaring 2.1.5）
- kotlinx.serialization 1.12.0：EPUB 定位符（Locator）JSON 存取

### 5.2 导入与文件管理

- 通过 `ACTION_OPEN_DOCUMENT` 选择文件（一次一个；可重复导入），并调用 `takePersistableUriPermission` 持久化读权限
- **外部打开**：`MainActivity` 注册 `ACTION_VIEW`（`text/plain`、`application/epub+zip`，同时接受 `content://` 与 `file://`），`android:launchMode="singleTop"`；从系统文件管理器"打开方式"选静读时，退出正在阅读的书 → 回到书架导入 → 自动打开该书；书已在书架时直接打开，不重复导入
- 外部打开拿到的是临时读授权，同样尝试 `takePersistableUriPermission`；提供方不支持持久化时，重启后该书会走"找不到原文件"流程（见下一条）
- **不复制原文件**：书架只保存原文件 URI 以及索引数据（章节索引、进度、元数据），索引存于应用私有目录
- 原文件被移动、改名、删除或授权失效时书无法打开：阅读页弹提示，提供"重新选择文件"（替换 URI 后恢复）与"从书架移除"两个动作
- 删除：不勾选"同时删除本地文件"仅移出书架；勾选则通过 `DocumentsContract.deleteDocument` 删除原始文件；文件提供方不允许删除时提示失败
- 书架条目记录：书名（原文件名去扩展名）、格式、大小（原文件大小）、导入时间、最近阅读时间、进度定位符、文档 URI
- 不启用 Android Auto Backup，不做导出/导入备份；数据只存在本机，卸载即清空
- 大小上限：TXT 200MB、EPUB 500MB，超限拒绝导入并提示
- 导入时校验文件可随机定位读取；不可定位的流式文件拒绝导入，并提示先把文件保存到手机本地存储

### 5.3 TXT 处理管线

1. **编码识别**：带 BOM 的 UTF-8 / UTF-16 LE / UTF-16 BE 按 BOM 判定；无 BOM 时先严格 UTF-8 解码，失败回退 GB18030。不识别 Big5、Shift-JIS 等其它编码，也不提供手动切换编码（识别为乱码的文件不在支持范围内）。识别结果随书保存。
2. **读取方式**：不生成副本，直接通过原文件 URI 打开并随机定位读取
3. **分章扫描**：逐行扫描原文件（按识别出的编码解码），命中规则的行作为章节标题，记录每章的**字节偏移**（起止），连同标题写入 Room 的章节索引表。字节偏移不存正文，读取时按需定位
4. **规则集（按顺序匹配，内置；`ChapterRule.kt`）**：
   1. 第X章：`^\s{0,4}第[0-9零〇一二三四五六七八九十百千万两]+[章回节集篇部卷]\s{0,2}.{0,30}$`
   2. 特殊章节名：`^\s{0,4}(序章|楔子|前言|序言|引子|尾声|后记|番外)(\s+.{0,30})?$`
   3. Chapter：`^\s{0,4}(Chapter|CHAPTER)\s+\d{1,4}.{0,30}$`
   4. 数字序号：`^\d{1,4}[、.．].{0,20}$` —— **默认关闭**（误判率高）
5. **约束参数**：
   - 标题行去掉标记后的文字长度 ≤ 30 字，超过不算标题
   - 章节正文 < 200 字：并入上一章
   - 章节正文 > 200,000 字：强制拆分
   - 全书识别出的章节 < 2 章：按每 5,000 字切块，标题为"第 N 部分"
6. **重新识别**：重跑第 3–5 步，保留当前阅读位置（按字符比例映射到新章节）
7. **目录平铺**：不构建卷/章层级，"卷"标题行作为普通目录项

### 5.4 TXT 阅读与分页

- 以**字节偏移随机访问**为唯一读取模型（5.3）：通过原文件 URI 拿到可定位的文件描述符，阅读时不把全书解码进内存，只加载当前章节及相邻章节用于预取
- 全书字符总数在导入扫描时一并算出并随书保存，供百分比计算使用
- 左右翻页：用 Compose `TextMeasurer` 按当前字号、边距、行距、屏幕尺寸计算行高，对当前章节文本做二分测量分页；分页结果缓存，设置变化或章节切换时失效
- 左右翻页同时支持横向滑动手势：页面跟手移动，松手按位移阈值翻页，位移不足则回弹
- 分页结果会过滤纯空白页（避免章末出现需要再点一次的空白页）；**不按段落边界强制换页**，尽可能填满整页
- 上下滚动：`LazyColumn` 按段落渲染当前章节，支持跨章连续滚动（滚动到章节末尾时追加下一章，滚到顶部时向前补上上一章并保持可视位置）；进入时定位到上次位置只执行一次
- 阅读页正文上下内边距为 8dp，左右内边距等于"左右边距"设置
- 百分比：`(当前章节起始字符偏移 + 章节内已读字符偏移) / 全书字符总数`，结果 ×100 保留两位小数。按字符而非页数计算，因此切换字号或翻页模式时百分比稳定
- 章节内已读偏移：左右模式下按页首字符索引，上下模式下按首个可见段落的字符索引（近似）
- **位置保持**：切换翻页模式、改字号/边距/行距导致重排后，仍停在当前阅读位置（按字符偏移映射到新的页码或滚动位置）

### 5.5 EPUB 处理

- 使用 Readium Kotlin Toolkit **3.1.2**：`AssetRetriever` + `PublicationOpener(EpubParser())` 打开 `content://` URI，`EpubNavigatorFactory` 创建 `EpubNavigatorFragment`，用 `FragmentContainerView`（`AndroidView` 承载）挂到 Compose 阅读页
- `EpubNavigatorFragment.Configuration(shouldApplyInsetsPadding = false)`，避免刘海安全区额外内边距；同时在 app 资源中把 `readium_navigator_epub_vertical_padding` 覆盖为 `0dp`（Readium 分页模式默认上下各加 40dp）
- 目录来自 EPUB 自带的 nav/ncx，**保留书籍层级**：可按卷/章展开收起；点父节点跳到该节点对应位置；当前章节高亮，打开目录时自动滚动到当前项，并自动展开当前项的祖先链
- 渲染：Readium 的 WebView 导航器，保留书籍图片、CSS、字体、行距
- 用户覆盖：通过 `EpubPreferences` 覆盖**背景色**、按比例缩放的**字号**、翻页/滚动模式、`pageMargins`（= 左右边距 ÷ 20，Readium 以此为 `--RS__pageGutter` 的倍数）；不设置文字颜色
- 图片与样式：每次当前资源（href）变化时，用 `EpubNavigatorFragment.evaluateJavascript` 注入脚本——给 `img/svg` 加居中样式，并给每张图片绑定点击事件（canvas 生成 2048px WebP data URL，经 Readium 官方 `registerJavascriptInterface("CCReadBridge")` 回传原生层）
- 翻页模式映射：左右翻页 ↔ Readium 分页布局；上下滚动 ↔ Readium 滚动布局
- 位置保持：切换布局后按当前 Locator 重新定位，不停留在章节开头
- 进度：使用 Readium Locator 的 `totalProgression` 作为百分比，持久化 Locator JSON
- 顶部章节名：由当前 Locator 的 href 在目录中反查标题

### 5.6 阅读页交互

- 三个点击区：左 1/3、中 1/3、右 1/3。上下滚动模式下仅中区响应（开菜单），左右区无动作
- TXT 左右翻页同时响应横向滑动手势，与点击并存；点击区用不消费事件的 `pointerInput` 实现，贴近屏幕边缘也能命中
- EPUB 不使用 Compose 覆盖层：通过 Readium 的 `InputListener.onTap` 接收点击，再用 `evaluateJavascript(elementFromPoint)` 探测落点——
  - 落在图片/`figure` 上：不做任何翻页或菜单动作（图片由页面内脚本通过 `CCReadBridge` 打开全屏查看）
  - 文本区域：按横向位置调用 `goBackward` / `goForward` 或打开菜单
  - EPUB 的横向滑动交给 Readium 原生分页处理
- 顶部行：章节名居中，时间 HH:mm 靠右，每 15 秒刷新一次
- 底部：百分比居中
- 返回键：依次关闭弹出的面板/抽屉/弹窗，全部关闭时才退出阅读页；图片查看层打开时，返回键只关闭图片查看

### 5.7 菜单与面板

- 底部菜单：一行三项，背景与阅读背景一致，顶部圆角，`navigationBarsPadding` 贴合屏幕底部；上方一层 25% 黑色遮罩，点遮罩关闭
- 菜单、遮罩、目录抽屉、图片查看层都带简短的淡入/滑入动画
- 目录抽屉：左滑入，宽度 80%，全高；顶部书名，列表可滚动，当前章节高亮并自动滚动到可视区域
- 阅读设置面板：`ModalBottomSheet(skipPartiallyExpanded = true)`，字号滑杆、左右边距滑杆、行距滑杆、5 个背景色块、翻页模式二选一；改动即时生效。EPUB 下字号滑杆按比例缩放全书；左右边距与行距滑杆置灰（左右边距值仍然生效）
- 亮度面板：滑杆 + "跟随系统亮度"开关；非跟随模式通过 `WindowManager.LayoutParams.screenBrightness` 设置窗口亮度，退出阅读页恢复为 `-1`

### 5.8 数据模型

Room（表 `books` / `chapters`）：

- `Book(id, title, format, uri, sizeBytes, importedAt, lastReadAt, progress, encodingName, totalChars)`
- `Chapter(bookId, index, title, startByte, endByte, charStart, charCount)`，主键 `(bookId, index)`

DataStore：

- `fontSize`、`marginHorizontal`、`lineSpacing`、`backgroundId`、`pageMode`（PAGED/SCROLL）、`followSystemBrightness`、`brightness`；读取时统一做范围钳制

### 5.9 视觉默认（我的决定，可改）

- 背景预设：`#FFFFFF`、`#F5ECD8`（米黄，默认）、`#CCE8CF`（浅绿）、`#303030`（深灰）、`#000000`（纯黑）；**TXT** 在深色背景下自动改用浅色文字；EPUB 不覆盖文字颜色（见 4.4）
- 顶部行字号约为正文字号的 0.75 倍，使用次要文字颜色
- 正文使用系统默认字体；字号范围 12–36，边距 8–48dp，行距 1.0–2.0 倍
- 书架书方块为圆角矩形 + 左侧一条书脊色带，标题最多 3 行，超出末尾省略号
- 应用名"静读"，包名 `com.ccread.reader`，图标为简单的书本图形
- 窗口背景与启动画面背景统一为米黄 `app_background`；启动画面只显示透明底的图标，不出现白色方块；状态栏/导航栏透明并使用深色图标

### 5.10 EPUB 图片全屏查看

- 点击正文图片：页面脚本把图片绘制到 canvas，缩放到最长边 2048px，导出为 WebP data URL，经 `CCReadBridge.openImage` 回传到原生层
- 原生层用**独立窗口的 Compose Dialog** 显示：窗口 `MATCH_PARENT`，`dimAmount = 0.25`（与菜单遮罩同浓度），隐藏系统栏——因此遮罩必然铺满整屏（包括阅读页顶栏/底栏区域），且触摸事件根本不会进入 WebView，不会触发翻页
- 手势：双指缩放 1–5 倍、单指平移（限制在图片范围内）、单击关闭、返回键关闭
- 图片解码在 IO 线程完成；关闭即释放 Bitmap

## 6. 验收与测试

单元测试（`app/src/test`，共 73 个，`./gradlew :app:testDebugUnitTest`）：

- 编码识别 `CharsetDetectorTest`：UTF-8、UTF-8 BOM、GBK/GB18030 回退、UTF-16 LE/BE、随机字节
- 分章器 `TxtChapterSplitterTest`：第X章、中文数字、Chapter、序章、无章节兜底、超长章拆分、短章合并、标题过长、64KB 读取上限
- 导入规则 `ImportRulesTest`：扩展名/MIME、200MB/500MB 上限、书名去扩展名
- 文件位置 `FileLocationTest`（Robolectric）：document id 解码、`primary:`/`home:`/`raw:` 路径
- 分页与滚动映射 `PageMappingTest`、`PageSanitizerTest`、`ScrollMappingTest`
- 百分比与位置 `ProgressCalculatorTest`、`TxtProgressRemapTest`、`ProgressCodecTest`
- 设置 `SettingsRepositoryTest`、`SettingsRulesTest`（范围钳制）
- 阅读交互与目录 `ReaderInteractionTest`、`TocViewModelTest`、`TocAncestorsTest`
- EPUB `EpubTocTest`、`EpubProgressTest`、`EpubStyleTest`（字号比例、背景覆盖、`pageMargins` 换算）
- 书架 `BookDaoTest`、`BookshelfViewModelTest`（排序、删除、外部打开时选择要打开的书）
- 图片 `ImageDataTest`（data URL 解析）

手动验收：

- 导入 TXT 与 EPUB 各一本，书架显示正确
- 两种模式切换，左右点击与横向滑动翻页行为正确，滚动模式左右点击无动作；进入阅读页时模式被重置为左右翻页
- 点击位置贴近左右边缘也能翻页
- EPUB 目录层级可展开/收起，点击父节点与章节均能正确跳转，打开目录自动滚到当前项
- EPUB 在 5 种背景下的表现符合"只改背景、文字照书"，深色背景下的可读性问题与预期一致
- EPUB 正文上下无额外留白，左右留白与 TXT 一致（同一"左右边距"值）
- 点击 EPUB 图片进入全屏查看：遮罩铺满整屏、双指缩放、单击/返回关闭，查看时左右滑动不翻页
- 目录跳转、重新识别、进度恢复
- 5 种背景、字号/边距/行距改动即时生效
- 亮度跟随与手动切换
- 长按删除（勾选/不勾选"同时删除本地文件"）、详情信息正确、路径为 `/storage/...` 并可跳转系统文件管理器
- 从系统文件管理器"打开方式"选静读：TXT/EPUB 均出现在候选列表，打开后自动进入阅读页；重复打开同一文件不重复入架
- 断网状态下完整使用；APK 的 Manifest 中无 `INTERNET` / `ACCESS_NETWORK_STATE` 权限

性能参考：5MB TXT 导入与首次打开 ≤ 2 秒；翻页无可感知卡顿。

## 7. 实施结果与遗留取舍

原"留给实施者的空间"已在实现中定案：

- Readium 用 **3.1.2**（3.4.x 需要 compileSdk 37 / AGP 9.1）；`content://` 由 `AssetRetriever` 直接支持；CSS 偏好走 `EpubPreferences`
- 原文件不可定位读取 → 直接拒绝导入并提示"请先保存到手机本地存储"
- TXT 分页按 `(章节, 字号, 行距, 边距, 宽, 高)` 缓存，并预取当前章与前后各一章
- SAF 删除失败 → 提示"无法删除原文件"，书架条目仍移除
- 重复导入 → 返回 `Duplicate`；从"打开方式"进来时直接打开已有书，从"+"按钮导入时提示"这本书已经在书架上了"
- 图标使用自适应书本图形；启动页只显示透明底图标

仍存在、且明确接受的取舍：

- 外部"打开方式"的 `content://` 授权不一定可持久化，重启后可能需要重新关联文件
- 不复制原文件，因此原文件被移动/删除后必须重新选择
- EPUB 分页模式下覆盖了 Readium 的 40dp 上下内边距与刘海 insets，正文可能贴近屏幕物理边缘
- 阅读页每次进入都强制左右翻页，滚动模式不会跨次保留

## 8. 构建与打包

### 8.1 构建环境（Docker）

本机不安装 JDK、Android SDK 或 Android Studio，全部在 Linux 容器内完成。

`Dockerfile`：

- 基础镜像 `gradle:8.13-jdk21`（JDK 21 + Gradle 8.13）
- 安装 Android command-line tools，并通过 `sdkmanager` 安装 `platform-tools`、`platforms;android-36`、`build-tools;36.0.0`
- `ANDROID_SDK_ROOT=/opt/android-sdk`、`GRADLE_USER_HOME=/root/.gradle`、`WORKDIR /project`
- 构建镜像：`docker build -t jingdu-android:latest .`
- 常驻开发容器（挂载源码与 Gradle 缓存，避免每次冷启动）：

```powershell
docker run -d --name jingdu-dev -v "${PWD}:/project" -v jingdu-gradle:/root/.gradle -w /project jingdu-android:latest sleep infinity
```

- 首次构建需要联网下载 SDK 与依赖；之后依赖缓存在 `jingdu-gradle` 卷中。镜像或依赖清理后重新构建即可。

### 8.2 Debug 构建与测试

```powershell
docker exec jingdu-dev ./gradlew :app:testDebugUnitTest   # 单元测试（73 个）
docker exec jingdu-dev ./gradlew :app:assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
```

- Debug APK 由 AGP 的默认 debug keystore 签名，可直接安装
- 产物路径固定为 `app/build/outputs/apk/debug/app-debug.apk`，约 21MB
- 每次 `assembleDebug` 结束后，Gradle 任务 `:app:syncDebugApk` 会自动把产物复制为仓库根目录的 `apk/jingdu.apk`，供局域网分发使用
- 测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`

### 8.3 安装到手机

方式一（adb，容器内）：

```powershell
docker exec jingdu-dev adb install -r app/build/outputs/apk/debug/app-debug.apk
docker exec jingdu-dev adb shell am start -n com.ccread.reader/.MainActivity
```

方式一需要容器能访问手机；无线调试不可用时用方式二。

方式二（本机 HTTP 分发）：

```powershell
python -m http.server 8000 --bind 0.0.0.0 --directory "apk"
```

手机浏览器访问 `http://<电脑局域网IP>:8000/jingdu.apk?v=<版本号>` 下载安装。`apk/jingdu.apk` 每次构建后自动更新，递增 `?v=` 可避免浏览器缓存旧包。

### 8.4 打包前检查

```powershell
docker exec jingdu-dev bash -lc '"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/debug/app-debug.apk'
docker exec jingdu-dev bash -lc '"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump xmltree app/build/outputs/apk/debug/app-debug.apk --file AndroidManifest.xml | grep -A4 "android.intent.action.VIEW"'
```

- 权限列表必须不含 `android.permission.INTERNET`、`android.permission.ACCESS_NETWORK_STATE`
- 清单中必须存在 `text/plain` 与 `application/epub+zip` 两组 `ACTION_VIEW` filter
- 资源覆盖 `readium_navigator_epub_vertical_padding` 必须为 `0dp`（可用 `aapt2 dump resources` 检查）

### 8.5 Release 包

当前工程只配置了 debug 构建，**没有 release 签名配置**。要发布需要补充：

1. 生成 keystore，并在 `app/build.gradle.kts` 中增加 `signingConfigs.create("release")`，凭据从 `local.properties` 或环境变量读取，不要把密码写进仓库
2. `buildTypes.release` 指定 `signingConfig`；是否启用 `isMinifyEnabled` 需先验证 Readium 的 JS 接口与反射（`@JavascriptInterface` 方法名、`registerJavascriptInterface`）在 R8 下不被裁剪
3. 递增 `versionCode` / `versionName`（当前 `1` / `"1.0"`）
4. 执行 `docker exec jingdu-dev ./gradlew :app:assembleRelease`，产物为 `app/build/outputs/apk/release/app-release.apk`
5. 发布前用 8.4 的两条命令复核权限与 intent filter（release 清单合并规则与 debug 相同）

### 8.6 已知构建约束

- `compileSdk` 必须为 36：AndroidX 1.13.x 与 Readium 3.1.2 的依赖链要求；降到 35 会编译失败
- Compose BOM 固定在 2025.05.00，更高版本要求 AGP 9.x / SDK 37
- Readium 版本锁定 3.1.2，并开启 core library desugaring（`desugar_jdk_libs` 2.1.5）
- `gradle.properties` 中的 `GRADLE_USER_HOME` 由容器环境变量覆盖；不要在 Windows 与容器间共用同一个 Gradle 缓存目录
