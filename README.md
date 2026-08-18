# schem2mcworld

<p align="center">
  <b>高性能、纯 JVM / Kotlin 实现的 Minecraft 投影文件转基岩版世界转换器与 SDK</b><br>
  <i>Convert Java Edition .schem / .schematic structures directly into Bedrock Edition .mcworld saves.</i>
</p>

<p align="center">
  <a href="https://jitpack.io/#Dicecan/schem2mcworld"><img src="https://jitpack.io/v/Dicecan/schem2mcworld.svg" alt="JitPack"></a>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/JDK-17%20%7C%2021-blue.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Windows%20%7C%20JVM-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Dependencies-Zero%20JNI-success.svg" alt="Zero JNI">
  <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License">
</p>

---

## 📖 简介 (Introduction)

`schem2mcworld` 是一个现代化的 Minecraft 结构转换库与命令行工具。它可以将 Java 版的 **`.schem`**（1.13+ Sponge / WorldEdit 规范）与 **`.schematic`**（1.12.2 及以前 Legacy MCEdit 规范）投影文件，直接编译生成带有 LevelDB 数据库与 SubChunk 调色板位打包编码的基岩版世界存档（**`.mcworld`**）。

双击生成的 `.mcworld` 文件即可在 Windows / Android / iOS《我的世界》基岩版中**一键自动导入并游玩**！

---

## ✨ 核心特性 (Key Features)

- 🚀 **纯 JVM / 零 JNI 本地库依赖（Android 完美兼容）**
  - 底层 LevelDB 采用纯 Java 实现（`org.iq80.leveldb`），彻底杜绝 `arm64-v8a`、`armeabi-v7a`、`x86_64` 等 Android ABI 架构下的 `.so` 缺失或 JNI 崩溃问题。
  - 零 AWT / Swing 依赖，原生支持 `InputStream` / `OutputStream` 全流式管道传输，无缝对接 Android `ContentResolver` 与 `Uri`。
- 🗺️ **丰富世界类型定制（World Generators）**
  - **纯虚空世界 (`VOID`)**：整个世界为纯空气，建筑悬浮高空，零多余方块。
  - **经典超平坦 (`SUPERFLAT`)**：1层基岩 + 2层泥土 + 1层草方块，地平线一马平川，无任何深坑悬崖。
  - **超平坦海洋 (`SUPERFLAT_OCEAN`)**：基岩 + 石头 + 沙子 + 20层海水。
  - **普通自然无限世界 (`INFINITE`)**：由基岩版游戏引擎自然生成山川湖海，建筑可无缝放置在海平面或指定地表。
  - **经典有限世界 (`OLD_LIMITED`)**：边界限制为 256×256，适合小游戏地图或低配设备。
  - **自定义分层世界 (`CUSTOM`)**：自由定制任意厚度与材质的基底（如地狱岩、黑曜石、玻璃等）。
- 📏 **跨版本高低高度与版本规范限制**
  - **1.18+ 扩展高度 (`V1_18`, `V1_20`, `V1_21`)**：世界建筑高度范围 **$-64 \sim 319$**（SubChunk 索引 $-4 \sim 19$），平坦世界基底始于 $Y=-64$。
  - **1.12 ~ 1.17 经典高度 (`LEGACY_1_12_TO_1_17`)**：世界高度限制为 **$0 \sim 255$**（SubChunk $0 \sim 15$），平坦基底始于 $Y=0$。
- 📐 **严谨的 Y 轴定位与对齐策略**
  - **`GROUND_ALIGNED`（地面对齐）**：自动计算地形顶层高度，将建筑底部严丝合缝贴合在地面上。
  - **`ABSOLUTE`（绝对坐标）**：精准按指定 $(X, Y, Z)$ 坐标插入世界。
  - **`CENTERED`（原点居中）**：水平居中于世界原点 $(0, 0)$。
- 🛡️ **模组（Mod）方块过滤与替换**
  - **`REMOVE_TO_AIR`（剔除为空气）**：自动识别所有 Mod 方块（如 `twilightforest:*`、`create:*` 等）并直接替换为空气，消除模组残留。
  - **`REPLACE_WITH_FALLBACK`（安全替代）**：遇到模组或未识别方块时，自动降级为指定的安全方块（默认石头），维持建筑轮廓。
  - **`STRICT_VANILLA_ONLY`（严格原版）**：非原版 `minecraft:*` 命名空间的方块一律剔除。
- 📦 **数据驱动与成熟开源 Mappings（GeyserMC / ViaVersion 兼容）**
  - 内置 **110 KB** 全量原版方块映射保底库（覆盖全部 11 种木材家族、深板岩全系、16染色系、红石、功能方块与自然植被）。
  - 原生兼容 GeyserMC 官方 `blocks.json` 格式，支持通过本地文件或远程 GitHub URL 动态挂载与热插拔更新。
  - 智能安全 Fallback 拦截器，转换遇未知方块绝不崩溃。

---

## 🚀 快速启动 (Quick Start)

### 方式一：Windows 用户双击 `start.cmd`（最简单）
- **直接双击 `start.cmd`**：自动唤起带有 ASCII 艺术字符与交互式菜单的控制台，按提示将 `.schem` 或 `.schematic` 文件**拖入窗口**即可一键转换！
- **拖拽文件转换**：直接将 `.schem` 文件**拖放到 `start.cmd` 图标上**，系统会自动以超平坦地面对齐模式瞬间完成转换！

### 方式二：打包与运行独立单文件 JAR (Fat JAR)
- **编译打包**：执行 `.\gradlew.bat fatJar`，在 `build/libs/` 目录下生成包含全部依赖与 Mappings 的独立单文件 `schem2mcworld-1.0.0-all.jar`。
- **运行 JAR**：
  ```bash
  java -jar build/libs/schem2mcworld-1.0.0-all.jar my_house.schem my_world.mcworld -t superflat -a ground
  ```

在项目根目录下打开终端即可直接执行：

### 1. 查看帮助说明
```powershell
.\gradlew.bat run --args="--help"
```

### 2. 常用转换命令示例

```powershell
# 1. 转换为经典超平坦世界，并自动地面对齐
.\gradlew.bat run --args="my_house.schem my_world.mcworld -t superflat -a ground"

# 2. 转换为纯虚空世界，建筑放置在 Y=80 高空，并剔除所有 Mod 方块为空气
.\gradlew.bat run --args="mod_base.schem clean_base.mcworld -t void --y 80 -m remove"

# 3. 转换为 1.16 旧版本兼容模式（世界高度限制 0..255）
.\gradlew.bat run --args="castle.schematic old_castle.mcworld -t superflat -v legacy"

# 4. 转换为 256x256 有限世界（适合小游戏或低配手机）
.\gradlew.bat run --args="pvp_arena.schem arena.mcworld -t old -v 1.21"

# 5. 挂载外部或远程最新的 GeyserMC mappings 规则
.\gradlew.bat run --args="input.schem output.mcworld -t superflat --mappings https://raw.githubusercontent.com/GeyserMC/mappings/master/blocks.json"
```

### 3. CLI 参数列表

| 参数 | 简写 | 可选值 | 说明 |
| :--- | :--- | :--- | :--- |
| `--terrain` | `-t` | `void` / `superflat` / `ocean` / `infinite` / `old` | 基础世界类型（默认: `void` 虚空） |
| `--version` | `-v` | `1.21` / `1.20` / `1.18` / `legacy` | 目标基岩版版本（默认: `1.21` 最新） |
| `--mod-filter` | `-m` | `replace` / `remove` / `strict` | 模组方块过滤模式（默认: `replace`） |
| `--fallback` | `-f` | 任意基岩版方块ID（如 `minecraft:glass`） | 自定义降级替代方块（默认: `minecraft:stone`） |
| `--align` | `-a` | `abs` / `ground` / `center` | 对齐模式：绝对坐标 / 地面对齐 / 原点居中 |
| `--y` | | 整数数值（如 `64`, `100`, `-30`） | 目标放置的 Y 轴高度 |
| `--name` | `-n` | 任意字符串 | 基岩版世界列表中显示的游戏名称 |
| `--mappings` | | 本地文件路径 或 `http(s)://` URL | 动态载入外部 Geyser 映射文件 |

---

## 💻 SDK 集成指南 (SDK Integration)

### 0. 引入依赖 (JitPack)

**Gradle (Kotlin DSL - `build.gradle.kts`):**
```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Dicecan:schem2mcworld:1.0.1")
}
```

**Gradle (Groovy DSL - `build.gradle`):**
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Dicecan:schem2mcworld:1.0.1'
}
```

**Maven (`pom.xml`):**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Dicecan</groupId>
        <artifactId>schem2mcworld</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```

---

### 1. 基础 Kotlin 调用（极简一行）

```kotlin
import com.github.schem2mcworld.api.*
import java.io.File

fun main() {
    // 方式 A：一行完成转换
    val result = Schem2Mcworld.convert(
        source = File("my_castle.schem"),
        destination = File("my_castle.mcworld"),
        terrain = WorldTerrainType.SUPERFLAT,
        alignment = AlignmentMode.GROUND_ALIGNED
    )

    // 方式 B：使用链式 Builder
    val result2 = McworldConverter.builder()
        .source(File("my_castle.schem"))
        .worldName("我的城堡世界")
        .terrain(WorldTerrainType.SUPERFLAT)
        .alignment(AlignmentMode.GROUND_ALIGNED)
        .convert(File("my_castle.mcworld"))
}
```

---

### 2. Android 端极简接入（URI / ContentResolver）

针对 Android 端 `Uri` 与 `ContentResolver`，SDK 提供了直接的 `InputStream` / `OutputStream` 流式接口，**无内存溢出风险**：

```kotlin
import android.content.Context
import android.net.Uri
import com.github.schem2mcworld.api.*

fun convertSchematic(context: Context, inputUri: Uri, outputUri: Uri) {
    val cr = context.contentResolver
    cr.openInputStream(inputUri)?.use { input ->
        cr.openOutputStream(outputUri)?.use { output ->
            Schem2Mcworld.convert(
                source = input,
                destination = output,
                worldName = "Android Map",
                terrain = WorldTerrainType.SUPERFLAT,
                alignment = AlignmentMode.GROUND_ALIGNED
            )
        }
    }
}
```

---

### 3. 自定义世界分层示例

```kotlin
import com.github.schem2mcworld.api.WorldLayer
import com.github.schem2mcworld.core.model.BedrockBlockState

val result = McworldConverter.builder()
    .source(File("fortress.schem"))
    .worldName("Nether Base")
    .customLayers(
        WorldLayer(BedrockBlockState.BEDROCK, 1),
        WorldLayer(BedrockBlockState("minecraft:netherrack"), 25),
        WorldLayer(BedrockBlockState("minecraft:soul_sand"), 5)
    )
    .alignment(AlignmentMode.GROUND_ALIGNED)
    .convert(File("nether_base.mcworld"))
```

---

## 🏗️ 架构设计 (Architecture)

```
[源文件 (.schem / .schematic / InputStream)]
                      │
                      ▼
        【解析层：Legacy / Sponge Parser】
         (自动感知 GZIP、NBT Tag 特征与 LEB128 VarInt 标量流)
                      │
                      ▼
        【中间方块模型：UniversalBlock IR】
         (解耦方块标识与属性：namespace, id, properties)
                      │
                      ▼
        【数据驱动映射层：BlockStateMapper】
         (GeyserMC / ViaVersion 规范 + 模组过滤 + Fallback 拦截器)
                      │
                      ▼
        【基岩版方块状态：BedrockBlockState】
         (name, states, Little-Endian NBT Version Tag)
                      │
                      ▼
        【地形与坐标转换器：Terrain & CoordinateTransformer】
         (虚空/平坦/无限/有限世界 + 1.18+ [-64, 320) 高度适配)
                      │
                      ▼
        【SubChunk v8 编码器 & LevelDB 写入器】
         (32-bit Word 位打包 + 纯 Java LevelDB 存储 + Zip 封包)
                      │
                      ▼
[.mcworld 压缩包 / OutputStream]
```

---

## 🧪 自动化测试验证 (Testing)

项目拥有覆盖全模块的测试套件，执行以下命令即可运行全部测试：

```powershell
.\gradlew.bat test
```

测试覆盖范围包括：
- `LegacySchematicParserTest`：1.12.2 旧版 BlockID + Data + AddBlocks nibble 数组解析测试
- `SpongeSchematicParserTest`：1.13+ Sponge V1/V2/V3 VarInt 调色板解析测试
- `BlockStateMapperTest`：全状态精确匹配、属性转换、智能 Fallback 拦截测试
- `ModFilterPolicyTest`：模组方块剔除与安全降级测试
- `SubChunkEncoderTest`：1, 2, 3, 4, 5, 6, 8, 16 bits/block 调色板位数组打包解包测试
- `TerrainAndCoordinateTest`：虚空、超平坦、海洋、自定义分层及跨版本 Y 轴计算测试
- `McworldConverterTest`：端到端 `.schem` / `.schematic` 到 `.mcworld` 真实打包解包校验测试

---

## 👤 作者 (Author)

- GitHub: [@Dicecan](https://github.com/Dicecan)
- Repository: [https://github.com/Dicecan/schem2mcworld](https://github.com/Dicecan/schem2mcworld)

---

## 📄 开源协议 (License)

本项目基于 [GNU General Public License v3.0 (GPLv3)](LICENSE) 开源。

