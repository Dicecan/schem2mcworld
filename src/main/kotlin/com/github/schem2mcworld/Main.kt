package com.github.schem2mcworld

import com.github.schem2mcworld.api.AlignmentMode
import com.github.schem2mcworld.api.McworldConverter
import com.github.schem2mcworld.api.SchematicPlacement
import com.github.schem2mcworld.api.WorldLayer
import com.github.schem2mcworld.api.WorldTerrainType
import com.github.schem2mcworld.core.mapper.ModFilterMode
import com.github.schem2mcworld.core.model.BedrockBlockState
import com.github.schem2mcworld.core.model.BedrockVersion
import com.github.schem2mcworld.core.model.Vector3i
import java.io.File
import java.util.Scanner

fun main(args: Array<String>) {
    printBanner()

    if (args.isEmpty()) {
        runInteractiveMode()
        return
    }

    if (args[0] == "-h" || args[0] == "--help") {
        printUsage()
        return
    }

    runCliMode(args)
}

private fun printBanner() {
    println("""
========================================================================================
  ____   ____ _   _ _____ __  __ ____  __  __  ____ __        _____  ____  _     ____  
 / ___| / ___| | | | ____|  \/  |___ \|  \/  |/ ___|\ \      / / _ \|  _ \| |   |  _ \ 
 \___ \| |   | |_| |  _| | |\/| | __) | |\/| | |     \ \ /\ / / | | | |_) | |   | | | |
  ___) | |___|  _  | |___| |  | |/ __/| |  | | |___   \ V  V /| |_| |  _ <| |___| |_| |
 |____/ \____|_| |_|_____|_|  |_|_____|_|  |_|\____|   \_/\_/  \___/|_| \_\_____|____/ 
                                                                                
             SCHEMATIC TO MINECRAFT BEDROCK WORLD CONVERTER PRO
========================================================================================
 * Architecture : Pure JVM & Kotlin Core Engine (Zero Native JNI, 100% Android Safe)
 * Engine       : LevelDB SubChunk Bit-Packing v8 + LEB128 VarInt Sponge Parser
 * Worlds       : Superflat, Void, Infinite Natural, Limited (256x256), Ocean, Custom
 * Formats      : Litematica (.litematic), Sponge (.schem), Legacy (.schematic)
 * Multi-Import : Multi-Schematic Placement & Existing World (.mcworld) Injection
 * Mappings     : GeyserMC & ViaVersion Data Driven Mappings + Smart Fallback
 * License      : GNU General Public License v3.0 (GPLv3)
 * Author / Repo: https://github.com/Dicecan/schem2mcworld
========================================================================================
    """.trimIndent())
}

private fun runInteractiveMode() {
    val scanner = Scanner(System.`in`)
    while (true) {
        println("\n[+] 交互模式就绪。")
        println("请选择操作模式 (默认 1):")
        println("  1. 生成全新基岩版世界")
        println("  2. 追加/导入到已有 .mcworld 世界存档 (多投影无缝合并)")
        print("选择 [1-2] (默认 1, 输入 q 退出): ")
        val modeInput = scanner.nextLine().trim()
        if (modeInput.equals("q", ignoreCase = true)) {
            println("感谢使用，再见！")
            break
        }

        var baseWorldFile: File? = null
        if (modeInput == "2") {
            print("[?] 请将【已有的 .mcworld 存档文件】拖入此窗口并回车: ")
            val bwStr = scanner.nextLine().trim().replace("\"", "").replace("'", "")
            val bwFile = File(bwStr)
            if (!bwFile.exists() || !bwFile.isFile) {
                println("❌ 错误：找不到文件 '$bwStr'，请重新操作！")
                continue
            }
            baseWorldFile = bwFile
        }

        val placements = mutableListOf<SchematicPlacement>()
        var schemIndex = 1

        while (true) {
            val prompt = if (placements.isEmpty()) {
                "[?] 请将第 $schemIndex 个投影文件 (.litematic / .schem / .schematic) 拖入此窗口并回车: "
            } else {
                "[?] 继续拖入下一个投影文件 (或直接按回车/输入 done 开始转换): "
            }
            print(prompt)
            val rawInput = scanner.nextLine().trim().replace("\"", "").replace("'", "")
            if (rawInput.isEmpty() || rawInput.equals("done", ignoreCase = true)) {
                if (placements.isNotEmpty()) break
                println("❌ 请至少拖入一个投影文件！")
                continue
            }
            if (rawInput.equals("q", ignoreCase = true)) {
                break
            }

            val inputFile = File(rawInput)
            if (!inputFile.exists() || !inputFile.isFile) {
                println("❌ 错误：找不到文件 '$rawInput'，请重新拖入！")
                continue
            }

            print("  设置该投影的放置坐标 X Y Z (空格隔开, 默认 0 64 0): ")
            val posStr = scanner.nextLine().trim()
            val posParts = posStr.split(" ", ",").filter { it.isNotBlank() }
            val px = if (posParts.size >= 1) posParts[0].toIntOrNull() ?: 0 else 0
            val py = if (posParts.size >= 2) posParts[1].toIntOrNull() ?: 64 else 64
            val pz = if (posParts.size >= 3) posParts[2].toIntOrNull() ?: 0 else 0

            print("  选择对齐方式 (1: 地面对齐, 2: 绝对坐标, 3: 原点居中, 默认 1): ")
            val alignChoice = scanner.nextLine().trim()
            val align = when (alignChoice) {
                "2" -> AlignmentMode.ABSOLUTE
                "3" -> AlignmentMode.CENTERED
                else -> AlignmentMode.GROUND_ALIGNED
            }

            placements.add(SchematicPlacement(inputFile, Vector3i(px, py, pz), align))
            println("  [✔] 已添加投影: ${inputFile.name} -> 放置坐标: ($px, $py, $pz), 对齐: $align\n")
            schemIndex++
        }

        if (placements.isEmpty()) continue

        var terrain = WorldTerrainType.SUPERFLAT
        var customLayers: List<WorldLayer> = emptyList()

        if (baseWorldFile == null) {
            println("\n[1/3] 选择生成的世界类型 (输入序号, 默认 1):")
            println("  1. 经典超平坦世界 (Superflat, 自动地面对齐 - 推荐)")
            println("  2. 纯虚空世界 (Void, 悬浮高空 Y=64, 无多余方块)")
            println("  3. 超平坦海洋世界 (Superflat Ocean)")
            println("  4. 经典 256x256 有限世界 (Old Limited, 适合小游戏/低配手机)")
            println("  5. 普通自然无限世界 (Infinite, 游戏自然生成地形)")
            println("  6. 用户自定义分层世界 (Custom Layers, 自定义多层方块与厚度)")
            print("选择 [1-6] (默认 1): ")
            val tChoice = scanner.nextLine().trim()

            terrain = when (tChoice) {
                "2" -> WorldTerrainType.VOID
                "3" -> WorldTerrainType.SUPERFLAT_OCEAN
                "4" -> WorldTerrainType.OLD_LIMITED
                "5" -> WorldTerrainType.INFINITE
                "6" -> {
                    println("\n  请配置自定义分层 (支持快捷预设 或 手动输入):")
                    println("    A. 下界地狱风格: 1*bedrock, 30*netherrack, 5*soul_sand")
                    println("    B. 纯玻璃展示台: 1*bedrock, 40*glass")
                    println("    C. 矿物平原风格: 1*bedrock, 20*stone, 5*iron_block, 1*diamond_block")
                    println("    D. 自定义手动输入 (格式: 数量*方块ID, 例如: 1*bedrock,20*stone,5*sand)")
                    print("  选择预设 [A/B/C/D] 或直接输入分层: ")
                    val lChoice = scanner.nextLine().trim()
                    customLayers = when (lChoice.uppercase()) {
                        "A" -> parseCustomLayers("1*bedrock,30*netherrack,5*soul_sand")
                        "B" -> parseCustomLayers("1*bedrock,40*glass")
                        "C" -> parseCustomLayers("1*bedrock,20*stone,5*iron_block,1*diamond_block")
                        "D" -> {
                            print("  请输入分层 (如: 1*bedrock,10*stone,3*dirt,1*grass_block): ")
                            val manual = scanner.nextLine().trim()
                            parseCustomLayers(manual)
                        }
                        else -> if (lChoice.contains("*") || lChoice.contains(",")) parseCustomLayers(lChoice) else parseCustomLayers("1*bedrock,30*netherrack,5*soul_sand")
                    }
                    println("  [✔] 已配置 ${customLayers.size} 个自定义分层 (总厚度: ${customLayers.sumOf { it.count }} 格)")
                    WorldTerrainType.CUSTOM
                }
                else -> WorldTerrainType.SUPERFLAT
            }
        }

        println("\n[2/3] 模组 (Mod) 方块处理策略 (输入序号, 默认 1):")
        println("  1. 剔除所有模组方块为空气 (移除 Mod 残留 - 推荐)")
        println("  2. 安全降级替代 (将未识别 Mod 方块转为石头)")
        print("选择 [1-2] (默认 1): ")
        val mChoice = scanner.nextLine().trim()
        val modFilter = if (mChoice == "2") ModFilterMode.REPLACE_WITH_FALLBACK else ModFilterMode.REMOVE_TO_AIR

        println("\n[3/3] 目标基岩版版本 (输入序号, 默认 1):")
        println("  1. 基岩版 1.21+ / 1.18+ (扩展建筑高度 -64..319 - 推荐)")
        println("  2. 基岩版 1.12 ~ 1.17 经典旧版本 (建筑高度限制 0..255)")
        print("选择 [1-2] (默认 1): ")
        val vChoice = scanner.nextLine().trim()
        val version = if (vChoice == "2") BedrockVersion.LEGACY_1_12_TO_1_17 else BedrockVersion.V1_21

        val firstFile = placements[0].source as File
        val outputFile = if (baseWorldFile != null) {
            File(baseWorldFile.parentFile, baseWorldFile.nameWithoutExtension + "_updated.mcworld")
        } else {
            File(firstFile.parentFile, firstFile.nameWithoutExtension + ".mcworld")
        }

        println("\n[>] 正在执行转换与合并 -> ${outputFile.name} ...")

        val builder = McworldConverter.builder()
            .worldName(if (baseWorldFile != null) baseWorldFile.nameWithoutExtension else firstFile.nameWithoutExtension)
            .terrain(terrain)
            .customLayers(customLayers)
            .targetVersion(version)
            .modFilterMode(modFilter)

        if (baseWorldFile != null) {
            builder.baseWorld(baseWorldFile)
        }

        for (p in placements) {
            builder.addSchematic(p)
        }

        val result = builder.convert(outputFile)

        if (result.success) {
            println("==================================================")
            println("🎉 转换成功！已合并 ${placements.size} 个投影")
            println("==================================================")
            println("生成文件: ${outputFile.absolutePath}")
            println("转换方块: ${result.totalBlocksConverted} 个")
            println("写入区块: ${result.chunksGenerated} Chunks (${result.subChunksWritten} SubChunks)")
            println("转换耗时: ${result.durationMs} ms")
            println("文件大小: ${outputFile.length() / 1024} KB")
            println("\n👉 提示：直接双击 '${outputFile.name}' 即可在《我的世界》基岩版中导入游玩！\n")
        } else {
            System.err.println("❌ 转换失败！")
        }
    }
}

private fun runCliMode(args: Array<String>) {
    var baseWorldFile: File? = null
    val placements = mutableListOf<SchematicPlacement>()

    var terrainType = WorldTerrainType.VOID
    var customLayers: List<WorldLayer> = emptyList()
    var worldName: String? = null
    var targetY = 64
    var alignment = AlignmentMode.ABSOLUTE
    var targetVersion = BedrockVersion.LATEST
    var modFilterMode = ModFilterMode.REPLACE_WITH_FALLBACK
    var fallbackBlock = BedrockBlockState.STONE
    var outputFile: File? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--base-world", "-w" -> {
                if (i + 1 < args.size) {
                    baseWorldFile = File(args[i + 1])
                    i++
                }
            }
            "--input", "-i" -> {
                if (i + 1 < args.size) {
                    val inputSpec = args[i + 1]
                    placements.add(parsePlacementSpec(inputSpec))
                    i++
                }
            }
            "--terrain", "-t" -> {
                if (i + 1 < args.size) {
                    terrainType = WorldTerrainType.fromString(args[i + 1])
                    i++
                }
            }
            "--layers", "-l" -> {
                if (i + 1 < args.size) {
                    customLayers = parseCustomLayers(args[i + 1])
                    terrainType = WorldTerrainType.CUSTOM
                    i++
                }
            }
            "--name", "-n" -> {
                if (i + 1 < args.size) {
                    worldName = args[i + 1]
                    i++
                }
            }
            "--y" -> {
                if (i + 1 < args.size) {
                    targetY = args[i + 1].toIntOrNull() ?: 64
                    i++
                }
            }
            "--align", "-a" -> {
                if (i + 1 < args.size) {
                    alignment = when (args[i + 1].lowercase()) {
                        "abs", "absolute" -> AlignmentMode.ABSOLUTE
                        "ground", "ground_aligned" -> AlignmentMode.GROUND_ALIGNED
                        "center", "centered" -> AlignmentMode.CENTERED
                        else -> AlignmentMode.ABSOLUTE
                    }
                    i++
                }
            }
            "--version", "-v" -> {
                if (i + 1 < args.size) {
                    targetVersion = BedrockVersion.fromString(args[i + 1])
                    i++
                }
            }
            "--mod-filter", "-m" -> {
                if (i + 1 < args.size) {
                    modFilterMode = when (args[i + 1].lowercase()) {
                        "remove", "air" -> ModFilterMode.REMOVE_TO_AIR
                        "strict" -> ModFilterMode.STRICT_VANILLA_ONLY
                        else -> ModFilterMode.REPLACE_WITH_FALLBACK
                    }
                    i++
                }
            }
            "--fallback", "-f" -> {
                if (i + 1 < args.size) {
                    fallbackBlock = BedrockBlockState(args[i + 1])
                    i++
                }
            }
            else -> {
                if (!args[i].startsWith("-")) {
                    if (placements.isEmpty()) {
                        val file = File(args[i])
                        if (file.exists()) {
                            placements.add(SchematicPlacement(file, Vector3i(0, targetY, 0), alignment))
                        }
                    } else if (outputFile == null) {
                        outputFile = File(args[i])
                    }
                }
            }
        }
        i++
    }

    if (placements.isEmpty()) {
        System.err.println("错误：未指定任何输入投影文件！")
        printUsage()
        return
    }

    val firstFile = placements[0].source as? File
    if (outputFile == null) {
        val outName = if (baseWorldFile != null) {
            baseWorldFile.nameWithoutExtension + "_updated.mcworld"
        } else if (firstFile != null) {
            firstFile.nameWithoutExtension + ".mcworld"
        } else "output.mcworld"
        outputFile = File(outName)
    }

    val finalWorldName = worldName ?: baseWorldFile?.nameWithoutExtension ?: firstFile?.nameWithoutExtension ?: "Imported World"

    println("导入投影数: ${placements.size}")
    if (baseWorldFile != null) {
        println("基底世界存档: ${baseWorldFile.absolutePath}")
    }
    println("输出文件: ${outputFile.absolutePath}")
    println("世界名称: $finalWorldName")
    println("世界类型: $terrainType (Generator=${terrainType.generatorId})")
    if (customLayers.isNotEmpty()) {
        println("自定义分层: ${customLayers.joinToString(", ") { "${it.count}*${it.block.name}" }}")
    }
    println("目标版本: ${targetVersion.displayName}")
    println("模组过滤: $modFilterMode")
    println("开始转换与合并...")

    val builder = McworldConverter.builder()
        .worldName(finalWorldName)
        .terrain(terrainType)
        .customLayers(customLayers)
        .targetVersion(targetVersion)
        .modFilterMode(modFilterMode)
        .fallbackBlock(fallbackBlock)

    if (baseWorldFile != null) {
        builder.baseWorld(baseWorldFile)
    }

    for (p in placements) {
        builder.addSchematic(p)
    }

    val result = builder.convert(outputFile)

    if (result.success) {
        println("==================================================")
        println("🎉 转换成功！已合并 ${placements.size} 个投影")
        println("==================================================")
        println("总方块数: ${result.totalBlocksConverted}")
        println("生成 Chunk 数: ${result.chunksGenerated}")
        println("写入 SubChunk 数: ${result.subChunksWritten}")
        println("转换耗时: ${result.durationMs} ms")
        println("输出文件大小: ${outputFile.length() / 1024} KB")
        if (result.unmappedBlocks.isNotEmpty()) {
            println("提示：共有 ${result.unmappedBlocks.size} 个未识别或 Mod 方块已处理：")
            result.unmappedBlocks.take(5).forEach { println("  - $it") }
            if (result.unmappedBlocks.size > 5) {
                println("  ...等共 ${result.unmappedBlocks.size} 个")
            }
        }
        println("\n你可以双击 '${outputFile.name}' 直接在 Minecraft 基岩版中导入并游玩！")
    } else {
        System.err.println("转换失败！")
    }
}

private fun parsePlacementSpec(spec: String): SchematicPlacement {
    // 格式: path:x=100,y=64,z=200,align=ground
    val colonIdx = spec.indexOf(":")
    if (colonIdx == -1 || File(spec).exists()) {
        return SchematicPlacement(File(spec), Vector3i.ZERO, AlignmentMode.ABSOLUTE)
    }

    val filePath = spec.substring(0, colonIdx).trim()
    val paramPart = spec.substring(colonIdx + 1).trim()
    var x = 0
    var y = 64
    var z = 0
    var align = AlignmentMode.ABSOLUTE

    for (item in paramPart.split(",")) {
        val kv = item.split("=", limit = 2)
        if (kv.size == 2) {
            when (kv[0].trim().lowercase()) {
                "x" -> x = kv[1].trim().toIntOrNull() ?: 0
                "y" -> y = kv[1].trim().toIntOrNull() ?: 64
                "z" -> z = kv[1].trim().toIntOrNull() ?: 0
                "align", "a" -> align = when (kv[1].trim().lowercase()) {
                    "ground", "ground_aligned" -> AlignmentMode.GROUND_ALIGNED
                    "center", "centered" -> AlignmentMode.CENTERED
                    else -> AlignmentMode.ABSOLUTE
                }
            }
        }
    }

    return SchematicPlacement(File(filePath), Vector3i(x, y, z), align)
}

fun parseCustomLayers(input: String): List<WorldLayer> {
    val layers = mutableListOf<WorldLayer>()
    val parts = input.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
    for (part in parts) {
        val count: Int
        val blockName: String
        when {
            part.contains("*") -> {
                val sub = part.split("*")
                if (sub[0].toIntOrNull() != null) {
                    count = sub[0].toInt()
                    blockName = sub[1].trim()
                } else {
                    count = sub[1].toIntOrNull() ?: 1
                    blockName = sub[0].trim()
                }
            }
            part.contains(":") && part.substringAfterLast(":").toIntOrNull() != null -> {
                count = part.substringAfterLast(":").toInt()
                blockName = part.substringBeforeLast(":").trim()
            }
            else -> {
                count = 1
                blockName = part.trim()
            }
        }
        val fullBlockId = if (blockName.contains(":")) blockName else "minecraft:$blockName"
        layers.add(WorldLayer(BedrockBlockState(fullBlockId), count.coerceAtLeast(1)))
    }
    return layers
}

private fun printUsage() {
    println("""
        用法：
          schem2mcworld.jar <输入文件.litematic|.schem|.schematic> [输出文件.mcworld] [选项]
          schem2mcworld.jar -w <已有世界.mcworld> -i "house.schem:x=100,y=64,z=100" -i "tower.litematic:x=-200,y=70,z=50"

        已有世界存档注入 (--base-world, -w)：
          -w, --base-world <文件.mcworld>  将投影注入到现有基岩版存档中，保留原有地形与建筑

        多投影批量导入 (--input, -i)：
          -i, --input <路径[:x=X,y=Y,z=Z,align=ground]>  指定单个投影文件与独立坐标

        世界类型选项 (--terrain, -t)：
          void             纯虚空世界（默认）
          superflat / flat 经典超平坦世界
          ocean            超平坦海洋世界
          infinite         普通自然无限地形世界
          old / limited    经典 256x256 有限世界
          custom           自定义分层世界 (需配合 --layers)

        自定义世界分层选项 (--layers, -l)：
          --layers <分层定义>   例如: "1*bedrock,20*stone,5*dirt,1*grass_block"
                               例如: "1*bedrock,30*netherrack,5*soul_sand"

        目标版本选项 (--version, -v)：
          1.21 / latest    基岩版 1.21+ 最新版（默认，扩展高度 -64..319）
          1.20             基岩版 1.20
          1.18             基岩版 1.18~1.19
          legacy / 1.16    基岩版 1.12~1.17 经典旧版本（世界高度 0..255）

        模组过滤选项 (--mod-filter, -m)：
          replace          使用安全降级方块替代 Mod 方块（默认）
          remove / air     直接剔除所有 Mod 方块为空气
          strict           严格原版模式（非原版方块全部剔除）

        其他选项：
          --name, -n <世界名>         设置基岩版世界显示名称
          --y <Y坐标>                 设置放置的目标 Y 坐标 (默认: 64)
          --align, -a <abs|ground|center> 对齐方式: abs(绝对), ground(地面对齐), center(原点居中)
          --fallback, -f <方块ID>     自定义降级方块 (默认: minecraft:stone)

        示例：
          java -jar schem2mcworld.jar -w survival.mcworld -i "castle.schem:x=100,y=64,z=200" -i "farm.litematic:x=-150,y=64,z=50"
          java -jar schem2mcworld.jar base.schem nether_base.mcworld -l "1*bedrock,30*netherrack,5*soul_sand" -a ground
    """.trimIndent())
}
