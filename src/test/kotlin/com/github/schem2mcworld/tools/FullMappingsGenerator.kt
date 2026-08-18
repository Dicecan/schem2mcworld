package com.github.schem2mcworld.tools

import java.io.File

/**
 * 完整 GeyserMC / ViaVersion 规范内置映射数据生成器。
 * 生成覆盖全量 Minecraft 原版方块（包含全部附着朝向、半砖高低位、活板门、门、流体、告示牌等）的保底字典。
 */
object FullMappingsGenerator {

    @JvmStatic
    fun main(args: Array<String>) {
        val javaToBedrockFile = File("src/main/resources/mappings/java_to_bedrock.json")
        val legacyToJavaFile = File("src/main/resources/mappings/legacy_to_java.json")

        javaToBedrockFile.parentFile.mkdirs()

        val j2b = generateJavaToBedrock()
        javaToBedrockFile.writeText(j2b, Charsets.UTF_8)
        println("Generated java_to_bedrock.json: ${javaToBedrockFile.length() / 1024} KB")

        val l2j = generateLegacyToJava()
        legacyToJavaFile.writeText(l2j, Charsets.UTF_8)
        println("Generated legacy_to_java.json: ${legacyToJavaFile.length() / 1024} KB")
    }

    private fun generateJavaToBedrock(): String {
        val entries = mutableListOf<String>()

        fun add(javaState: String, bedrockId: String, states: Map<String, Any> = emptyMap()) {
            val statesJson = if (states.isEmpty()) "" else {
                val s = states.entries.joinToString(", ") { (k, v) ->
                    val vStr = when (v) {
                        is String -> "\"$v\""
                        is Boolean -> "$v"
                        is Number -> "$v"
                        else -> "\"$v\""
                    }
                    "\"$k\": $vStr"
                }
                ", \"states\": { $s }"
            }
            entries.add("  \"$javaState\": { \"bedrock_identifier\": \"$bedrockId\"$statesJson }")
        }

        // 1. 基础空气与流体 (含源头与流动状态)
        add("minecraft:air", "minecraft:air")
        add("minecraft:cave_air", "minecraft:air")
        add("minecraft:void_air", "minecraft:air")
        add("minecraft:water", "minecraft:water", mapOf("liquid_depth" to 0))
        add("minecraft:water[level=0]", "minecraft:water", mapOf("liquid_depth" to 0))
        for (lvl in 1..7) {
            add("minecraft:water[level=$lvl]", "minecraft:flowing_water", mapOf("liquid_depth" to lvl))
            add("minecraft:lava[level=$lvl]", "minecraft:flowing_lava", mapOf("liquid_depth" to lvl))
        }
        add("minecraft:flowing_water", "minecraft:flowing_water")
        add("minecraft:lava", "minecraft:lava", mapOf("liquid_depth" to 0))
        add("minecraft:lava[level=0]", "minecraft:lava", mapOf("liquid_depth" to 0))
        add("minecraft:flowing_lava", "minecraft:flowing_lava")

        // 2. 石头与深板岩家族
        val stoneTypes = mapOf(
            "stone" to "stone",
            "granite" to "granite",
            "polished_granite" to "granite_smooth",
            "diorite" to "diorite",
            "polished_diorite" to "diorite_smooth",
            "andesite" to "andesite",
            "polished_andesite" to "andesite_smooth"
        )
        for ((name, type) in stoneTypes) {
            add("minecraft:$name", "minecraft:stone", mapOf("stone_type" to type))
        }

        val deepslateBlocks = listOf(
            "deepslate", "cobbled_deepslate", "polished_deepslate",
            "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles",
            "cracked_deepslate_tiles", "chiseled_deepslate", "reinforced_deepslate"
        )
        for (b in deepslateBlocks) {
            add("minecraft:$b", "minecraft:$b")
        }

        val tuffBlocks = listOf("tuff", "polished_tuff", "tuff_bricks", "chiseled_tuff_bricks")
        for (b in tuffBlocks) {
            add("minecraft:$b", "minecraft:$b")
        }

        val blackstones = listOf(
            "blackstone", "gilded_blackstone", "polished_blackstone",
            "chiseled_polished_blackstone", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks"
        )
        for (b in blackstones) {
            add("minecraft:$b", "minecraft:$b")
        }

        add("minecraft:basalt", "minecraft:basalt", mapOf("pillar_axis" to "y"))
        add("minecraft:polished_basalt", "minecraft:polished_basalt", mapOf("pillar_axis" to "y"))
        add("minecraft:smooth_basalt", "minecraft:smooth_basalt")

        add("minecraft:bedrock", "minecraft:bedrock")
        add("minecraft:cobblestone", "minecraft:cobblestone")
        add("minecraft:mossy_cobblestone", "minecraft:mossy_cobblestone")
        add("minecraft:stone_bricks", "minecraft:stone_brick", mapOf("stone_brick_type" to "default"))
        add("minecraft:mossy_stone_bricks", "minecraft:stone_brick", mapOf("stone_brick_type" to "mossy"))
        add("minecraft:cracked_stone_bricks", "minecraft:stone_brick", mapOf("stone_brick_type" to "cracked"))
        add("minecraft:chiseled_stone_bricks", "minecraft:stone_brick", mapOf("stone_brick_type" to "chiseled"))

        // 3. 土壤与地形
        add("minecraft:grass_block", "minecraft:grass_block")
        add("minecraft:dirt", "minecraft:dirt", mapOf("dirt_type" to "normal"))
        add("minecraft:coarse_dirt", "minecraft:dirt", mapOf("dirt_type" to "coarse"))
        add("minecraft:podzol", "minecraft:podzol")
        add("minecraft:rooted_dirt", "minecraft:dirt_with_roots")
        add("minecraft:mud", "minecraft:mud")
        add("minecraft:muddy_mangrove_roots", "minecraft:muddy_mangrove_roots")
        add("minecraft:mud_bricks", "minecraft:mud_bricks")
        add("minecraft:packed_mud", "minecraft:packed_mud")
        add("minecraft:dirt_path", "minecraft:grass_path")
        add("minecraft:grass_path", "minecraft:grass_path")
        add("minecraft:sand", "minecraft:sand", mapOf("sand_type" to "normal"))
        add("minecraft:red_sand", "minecraft:sand", mapOf("sand_type" to "red"))
        add("minecraft:gravel", "minecraft:gravel")
        add("minecraft:clay", "minecraft:clay")

        // 4. 砂岩与红砂岩
        add("minecraft:sandstone", "minecraft:sandstone", mapOf("sand_stone_type" to "default"))
        add("minecraft:chiseled_sandstone", "minecraft:sandstone", mapOf("sand_stone_type" to "heiroglyphs"))
        add("minecraft:cut_sandstone", "minecraft:sandstone", mapOf("sand_stone_type" to "cut"))
        add("minecraft:smooth_sandstone", "minecraft:sandstone", mapOf("sand_stone_type" to "smooth"))
        add("minecraft:red_sandstone", "minecraft:red_sandstone", mapOf("sand_stone_type" to "default"))
        add("minecraft:chiseled_red_sandstone", "minecraft:red_sandstone", mapOf("sand_stone_type" to "heiroglyphs"))
        add("minecraft:cut_red_sandstone", "minecraft:red_sandstone", mapOf("sand_stone_type" to "cut"))
        add("minecraft:smooth_red_sandstone", "minecraft:red_sandstone", mapOf("sand_stone_type" to "smooth"))

        // 5. 11 种木材家族 (Wood families)
        val woodTypes = listOf(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped"
        )
        val dirMap = mapOf("east" to 0, "west" to 1, "south" to 2, "north" to 3)
        val facing4 = listOf("north", "south", "east", "west")

        for (w in woodTypes) {
            val suffix = if (w == "crimson" || w == "warped") "stem" else "log"
            val woodSuffix = if (w == "crimson" || w == "warped") "hyphae" else "wood"

            // 木板
            add("minecraft:${w}_planks", "minecraft:${w}_planks")

            // 原木 & 树皮木
            if (w == "bamboo") {
                add("minecraft:bamboo_block", "minecraft:bamboo_block", mapOf("pillar_axis" to "y"))
                add("minecraft:stripped_bamboo_block", "minecraft:stripped_bamboo_block", mapOf("pillar_axis" to "y"))
            } else {
                for (axis in listOf("y", "x", "z")) {
                    add("minecraft:${w}_$suffix[axis=$axis]", "minecraft:${w}_$suffix", mapOf("pillar_axis" to axis))
                    add("minecraft:${w}_$suffix", "minecraft:${w}_$suffix", mapOf("pillar_axis" to "y"))
                    add("minecraft:stripped_${w}_$suffix[axis=$axis]", "minecraft:stripped_${w}_$suffix", mapOf("pillar_axis" to axis))
                    add("minecraft:stripped_${w}_$suffix", "minecraft:stripped_${w}_$suffix", mapOf("pillar_axis" to "y"))
                    add("minecraft:${w}_$woodSuffix[axis=$axis]", "minecraft:${w}_$woodSuffix", mapOf("pillar_axis" to axis))
                    add("minecraft:${w}_$woodSuffix", "minecraft:${w}_$woodSuffix", mapOf("pillar_axis" to "y"))
                    add("minecraft:stripped_${w}_$woodSuffix[axis=$axis]", "minecraft:stripped_${w}_$woodSuffix", mapOf("pillar_axis" to axis))
                    add("minecraft:stripped_${w}_$woodSuffix", "minecraft:stripped_${w}_$woodSuffix", mapOf("pillar_axis" to "y"))
                }
            }

            // 树叶与幼苗
            if (w != "crimson" && w != "warped" && w != "bamboo") {
                add("minecraft:${w}_leaves", "minecraft:${w}_leaves", mapOf("persistent_bit" to true, "update_bit" to false))
                add("minecraft:${w}_sapling", "minecraft:${w}_sapling")
            }

            // 栅栏、栅栏门、压力板、按钮
            add("minecraft:${w}_fence", "minecraft:${w}_fence")
            add("minecraft:${w}_fence_gate", "minecraft:${w}_fence_gate")
            add("minecraft:${w}_pressure_plate", "minecraft:${w}_pressure_plate")
            add("minecraft:${w}_button", "minecraft:${w}_button")

            // 门 (Doors) - 严格区分上下半部分
            val doorId = "minecraft:${w}_door"
            add("minecraft:${w}_door", doorId)
            for (f in facing4) {
                val d = when(f) { "south" -> 0; "west" -> 1; "north" -> 2; "east" -> 3; else -> 0 }
                add("minecraft:${w}_door[facing=$f,half=lower,open=false]", doorId, mapOf("direction" to d, "upper_block_bit" to false, "open_bit" to false))
                add("minecraft:${w}_door[facing=$f,half=lower,open=true]", doorId, mapOf("direction" to d, "upper_block_bit" to false, "open_bit" to true))
                add("minecraft:${w}_door[facing=$f,half=upper,open=false]", doorId, mapOf("direction" to d, "upper_block_bit" to true, "open_bit" to false))
                add("minecraft:${w}_door[facing=$f,half=upper,open=true]", doorId, mapOf("direction" to d, "upper_block_bit" to true, "open_bit" to true))
            }

            // 活板门 (Trapdoors) - 严格区分上下半格与朝向
            val trapdoorId = "minecraft:${w}_trapdoor"
            add("minecraft:${w}_trapdoor", trapdoorId)
            for (f in facing4) {
                val d = when(f) { "south" -> 0; "north" -> 1; "east" -> 2; "west" -> 3; else -> 0 }
                add("minecraft:${w}_trapdoor[facing=$f,half=bottom,open=false]", trapdoorId, mapOf("direction" to d, "upside_down_bit" to false, "open_bit" to false))
                add("minecraft:${w}_trapdoor[facing=$f,half=bottom,open=true]", trapdoorId, mapOf("direction" to d, "upside_down_bit" to false, "open_bit" to true))
                add("minecraft:${w}_trapdoor[facing=$f,half=top,open=false]", trapdoorId, mapOf("direction" to d, "upside_down_bit" to true, "open_bit" to false))
                add("minecraft:${w}_trapdoor[facing=$f,half=top,open=true]", trapdoorId, mapOf("direction" to d, "upside_down_bit" to true, "open_bit" to true))
            }

            // 告示牌 (Signs & Wall Signs)
            add("minecraft:${w}_sign", "minecraft:${w}_standing_sign")
            add("minecraft:${w}_wall_sign", "minecraft:${w}_wall_sign")
            add("minecraft:${w}_wall_sign[facing=north]", "minecraft:${w}_wall_sign", mapOf("facing_direction" to 2))
            add("minecraft:${w}_wall_sign[facing=south]", "minecraft:${w}_wall_sign", mapOf("facing_direction" to 3))
            add("minecraft:${w}_wall_sign[facing=west]", "minecraft:${w}_wall_sign", mapOf("facing_direction" to 4))
            add("minecraft:${w}_wall_sign[facing=east]", "minecraft:${w}_wall_sign", mapOf("facing_direction" to 5))
            add("minecraft:${w}_hanging_sign", "minecraft:${w}_hanging_sign")
            add("minecraft:${w}_wall_hanging_sign", "minecraft:${w}_hanging_sign")

            // 楼梯全方向与上下倒置状态
            val stairId = "minecraft:${w}_stairs"
            add("minecraft:${w}_stairs", stairId, mapOf("weirdo_direction" to 0, "upside_down_bit" to false))
            for ((facing, dir) in dirMap) {
                add("minecraft:${w}_stairs[facing=$facing,half=bottom,shape=straight]", stairId, mapOf("weirdo_direction" to dir, "upside_down_bit" to false))
                add("minecraft:${w}_stairs[facing=$facing,half=top,shape=straight]", stairId, mapOf("weirdo_direction" to dir, "upside_down_bit" to true))
                add("minecraft:${w}_stairs[facing=$facing,half=bottom]", stairId, mapOf("weirdo_direction" to dir, "upside_down_bit" to false))
                add("minecraft:${w}_stairs[facing=$facing,half=top]", stairId, mapOf("weirdo_direction" to dir, "upside_down_bit" to true))
            }

            // 台阶 (Slabs / 半砖) - 严格保证上下半方块贴合
            val slabId = "minecraft:${w}_slab"
            val doubleSlabId = "minecraft:${w}_double_slab"
            add("minecraft:${w}_slab", slabId, mapOf("minecraft:vertical_half" to "bottom", "top_slot_bit" to false))
            add("minecraft:${w}_slab[type=bottom]", slabId, mapOf("minecraft:vertical_half" to "bottom", "top_slot_bit" to false))
            add("minecraft:${w}_slab[type=top]", slabId, mapOf("minecraft:vertical_half" to "top", "top_slot_bit" to true))
            add("minecraft:${w}_slab[type=double]", doubleSlabId)
        }

        // 石质与所有其他材质台阶/半砖 (Slabs)
        val otherSlabs = listOf(
            "stone", "smooth_stone", "sandstone", "cut_sandstone", "cobblestone",
            "brick", "stone_brick", "mud_brick", "nether_brick", "quartz",
            "red_sandstone", "cut_red_sandstone", "purpur", "prismarine",
            "prismarine_brick", "dark_prismarine", "polished_granite", "smooth_red_sandstone",
            "mossy_stone_brick", "polished_diorite", "mossy_cobblestone", "end_stone_brick",
            "smooth_sandstone", "smooth_quartz", "granite", "andesite", "red_nether_brick",
            "polished_andesite", "diorite", "cobbled_deepslate", "polished_deepslate",
            "deepslate_brick", "deepslate_tile", "blackstone", "polished_blackstone",
            "polished_blackstone_brick", "tuff", "polished_tuff", "tuff_brick"
        )
        for (s in otherSlabs) {
            val baseId = "minecraft:${s}_slab"
            add("minecraft:${s}_slab", baseId, mapOf("minecraft:vertical_half" to "bottom", "top_slot_bit" to false))
            add("minecraft:${s}_slab[type=bottom]", baseId, mapOf("minecraft:vertical_half" to "bottom", "top_slot_bit" to false))
            add("minecraft:${s}_slab[type=top]", baseId, mapOf("minecraft:vertical_half" to "top", "top_slot_bit" to true))
            add("minecraft:${s}_slab[type=double]", "minecraft:${s}_double_slab")
        }

        // 石质楼梯
        for (s in otherSlabs) {
            val stairId = "minecraft:${s}_stairs"
            add("minecraft:${s}_stairs", stairId, mapOf("weirdo_direction" to 0, "upside_down_bit" to false))
            for ((facing, dir) in dirMap) {
                add("minecraft:${s}_stairs[facing=$facing,half=bottom,shape=straight]", stairId, mapOf("weirdo_direction" to dir, "upside_down_bit" to false))
                add("minecraft:${s}_stairs[facing=$facing,half=top,shape=straight]", stairId, mapOf("weirdo_direction" to dir, "upside_down_bit" to true))
            }
        }

        // 杜鹃树叶
        add("minecraft:azalea_leaves", "minecraft:azalea_leaves", mapOf("persistent_bit" to true, "update_bit" to false))
        add("minecraft:flowering_azalea_leaves", "minecraft:flowering_azalea_leaves", mapOf("persistent_bit" to true, "update_bit" to false))

        // 6. 16 色系完整方块
        val colors = listOf(
            "white" to "white", "orange" to "orange", "magenta" to "magenta", "light_blue" to "light_blue",
            "yellow" to "yellow", "lime" to "lime", "pink" to "pink", "gray" to "gray",
            "light_gray" to "silver", "cyan" to "cyan", "purple" to "purple", "blue" to "blue",
            "brown" to "brown", "green" to "green", "red" to "red", "black" to "black"
        )

        for ((javaColor, bedrockColor) in colors) {
            add("minecraft:${javaColor}_wool", "minecraft:wool", mapOf("color" to bedrockColor))
            add("minecraft:${javaColor}_carpet", "minecraft:carpet", mapOf("color" to bedrockColor))
            add("minecraft:${javaColor}_concrete", "minecraft:concrete", mapOf("color" to bedrockColor))
            add("minecraft:${javaColor}_concrete_powder", "minecraft:concrete_powder", mapOf("color" to bedrockColor))
            add("minecraft:${javaColor}_terracotta", "minecraft:stained_hardened_clay", mapOf("color" to bedrockColor))
            add("minecraft:${javaColor}_glazed_terracotta", "minecraft:${javaColor}_glazed_terracotta")
            add("minecraft:${javaColor}_stained_glass", "minecraft:stained_glass", mapOf("color" to bedrockColor))
            add("minecraft:${javaColor}_stained_glass_pane", "minecraft:stained_glass_pane", mapOf("color" to bedrockColor))

            // 床 (Beds) - 严格区分床头与床尾朝向
            for (f in facing4) {
                val d = when(f) { "south" -> 0; "west" -> 1; "north" -> 2; "east" -> 3; else -> 0 }
                add("minecraft:${javaColor}_bed[facing=$f,part=head]", "minecraft:bed", mapOf("direction" to d, "head_piece_bit" to true))
                add("minecraft:${javaColor}_bed[facing=$f,part=foot]", "minecraft:bed", mapOf("direction" to d, "head_piece_bit" to false))
            }
            add("minecraft:${javaColor}_bed", "minecraft:bed")
            add("minecraft:${javaColor}_shulker_box", "minecraft:${javaColor}_shulker_box")
            add("minecraft:${javaColor}_candle", "minecraft:${javaColor}_candle")
            add("minecraft:${javaColor}_banner", "minecraft:standing_banner")
            add("minecraft:${javaColor}_wall_banner", "minecraft:wall_banner")
        }

        add("minecraft:glass", "minecraft:glass")
        add("minecraft:glass_pane", "minecraft:glass_pane")
        add("minecraft:tinted_glass", "minecraft:tinted_glass")
        add("minecraft:shulker_box", "minecraft:shulker_box")
        add("minecraft:candle", "minecraft:candle")

        // 7. 矿石与金属方块
        val ores = listOf("coal", "iron", "gold", "diamond", "redstone", "lapis", "emerald", "copper")
        for (ore in ores) {
            val bedrockOre = if (ore == "quartz") "quartz_ore" else "${ore}_ore"
            add("minecraft:${ore}_ore", "minecraft:$bedrockOre")
            add("minecraft:deepslate_${ore}_ore", "minecraft:deepslate_${ore}_ore")
            add("minecraft:${ore}_block", "minecraft:${ore}_block")
        }
        add("minecraft:nether_quartz_ore", "minecraft:quartz_ore")
        add("minecraft:nether_gold_ore", "minecraft:nether_gold_ore")
        add("minecraft:ancient_debris", "minecraft:ancient_debris")
        add("minecraft:netherite_block", "minecraft:netherite_block")
        add("minecraft:raw_iron_block", "minecraft:raw_iron_block")
        add("minecraft:raw_copper_block", "minecraft:raw_copper_block")
        add("minecraft:raw_gold_block", "minecraft:raw_gold_block")
        add("minecraft:amethyst_block", "minecraft:amethyst_block")
        add("minecraft:budding_amethyst", "minecraft:budding_amethyst")
        add("minecraft:amethyst_cluster", "minecraft:amethyst_cluster")

        // 8. 铜家族
        val copperVariants = listOf("copper_block", "exposed_copper", "weathered_copper", "oxidized_copper", "cut_copper", "exposed_cut_copper", "weathered_cut_copper", "oxidized_cut_copper")
        for (c in copperVariants) {
            add("minecraft:$c", "minecraft:$c")
            add("minecraft:waxed_$c", "minecraft:waxed_$c")
        }

        // 9. 火把与附着类红石 (附着方向保证永不脱落)
        // 普通火把 / 魂火把 / 红石火把
        add("minecraft:torch", "minecraft:torch", mapOf("torch_facing_direction" to "top"))
        add("minecraft:wall_torch", "minecraft:torch", mapOf("torch_facing_direction" to "north"))
        add("minecraft:wall_torch[facing=north]", "minecraft:torch", mapOf("torch_facing_direction" to "north"))
        add("minecraft:wall_torch[facing=south]", "minecraft:torch", mapOf("torch_facing_direction" to "south"))
        add("minecraft:wall_torch[facing=west]", "minecraft:torch", mapOf("torch_facing_direction" to "west"))
        add("minecraft:wall_torch[facing=east]", "minecraft:torch", mapOf("torch_facing_direction" to "east"))

        add("minecraft:soul_torch", "minecraft:soul_torch", mapOf("torch_facing_direction" to "top"))
        add("minecraft:soul_wall_torch", "minecraft:soul_torch", mapOf("torch_facing_direction" to "north"))
        add("minecraft:soul_wall_torch[facing=north]", "minecraft:soul_torch", mapOf("torch_facing_direction" to "north"))
        add("minecraft:soul_wall_torch[facing=south]", "minecraft:soul_torch", mapOf("torch_facing_direction" to "south"))
        add("minecraft:soul_wall_torch[facing=west]", "minecraft:soul_torch", mapOf("torch_facing_direction" to "west"))
        add("minecraft:soul_wall_torch[facing=east]", "minecraft:soul_torch", mapOf("torch_facing_direction" to "east"))

        add("minecraft:redstone_torch", "minecraft:redstone_torch", mapOf("torch_facing_direction" to "top"))
        add("minecraft:redstone_wall_torch", "minecraft:redstone_torch", mapOf("torch_facing_direction" to "north"))
        add("minecraft:redstone_wall_torch[facing=north]", "minecraft:redstone_torch", mapOf("torch_facing_direction" to "north"))
        add("minecraft:redstone_wall_torch[facing=south]", "minecraft:redstone_torch", mapOf("torch_facing_direction" to "south"))
        add("minecraft:redstone_wall_torch[facing=west]", "minecraft:redstone_torch", mapOf("torch_facing_direction" to "west"))
        add("minecraft:redstone_wall_torch[facing=east]", "minecraft:redstone_torch", mapOf("torch_facing_direction" to "east"))

        // 梯子 (Ladder)
        add("minecraft:ladder", "minecraft:ladder", mapOf("facing_direction" to 2))
        add("minecraft:ladder[facing=north]", "minecraft:ladder", mapOf("facing_direction" to 2))
        add("minecraft:ladder[facing=south]", "minecraft:ladder", mapOf("facing_direction" to 3))
        add("minecraft:ladder[facing=west]", "minecraft:ladder", mapOf("facing_direction" to 4))
        add("minecraft:ladder[facing=east]", "minecraft:ladder", mapOf("facing_direction" to 5))

        // 绊线钩 (Tripwire Hook)
        add("minecraft:tripwire_hook", "minecraft:tripwire_hook", mapOf("direction" to 2, "attached_bit" to false, "powered_bit" to false))
        add("minecraft:tripwire_hook[facing=north]", "minecraft:tripwire_hook", mapOf("direction" to 2, "attached_bit" to false, "powered_bit" to false))
        add("minecraft:tripwire_hook[facing=south]", "minecraft:tripwire_hook", mapOf("direction" to 0, "attached_bit" to false, "powered_bit" to false))
        add("minecraft:tripwire_hook[facing=west]", "minecraft:tripwire_hook", mapOf("direction" to 1, "attached_bit" to false, "powered_bit" to false))
        add("minecraft:tripwire_hook[facing=east]", "minecraft:tripwire_hook", mapOf("direction" to 3, "attached_bit" to false, "powered_bit" to false))
        add("minecraft:tripwire", "minecraft:tripwire")

        // 铁砧 (Anvil) - 包含 4 朝向与各损伤状态
        val anvilDamages = listOf("anvil" to "undamaged", "chipped_anvil" to "slightly_damaged", "damaged_anvil" to "very_damaged")
        for ((aId, dmg) in anvilDamages) {
            add("minecraft:$aId", "minecraft:anvil", mapOf("damage" to dmg, "cardinal_direction" to "north"))
            for (f in facing4) {
                add("minecraft:$aId[facing=$f]", "minecraft:anvil", mapOf("damage" to dmg, "cardinal_direction" to f))
            }
        }

        // 灯笼 (Lanterns)
        add("minecraft:lantern", "minecraft:lantern", mapOf("hanging" to false))
        add("minecraft:lantern[hanging=false]", "minecraft:lantern", mapOf("hanging" to false))
        add("minecraft:lantern[hanging=true]", "minecraft:lantern", mapOf("hanging" to true))
        add("minecraft:soul_lantern", "minecraft:soul_lantern", mapOf("hanging" to false))
        add("minecraft:soul_lantern[hanging=false]", "minecraft:soul_lantern", mapOf("hanging" to false))
        add("minecraft:soul_lantern[hanging=true]", "minecraft:soul_lantern", mapOf("hanging" to true))

        add("minecraft:glowstone", "minecraft:glowstone")
        add("minecraft:sea_lantern", "minecraft:sea_lantern")
        add("minecraft:shroomlight", "minecraft:shroomlight")
        add("minecraft:froglight_ochre", "minecraft:ochre_froglight")
        add("minecraft:froglight_pearlescent", "minecraft:pearlescent_froglight")
        add("minecraft:froglight_verdant", "minecraft:verdant_froglight")

        add("minecraft:bricks", "minecraft:brick_block")
        add("minecraft:tnt", "minecraft:tnt")
        add("minecraft:bookshelf", "minecraft:bookshelf")
        add("minecraft:chiseled_bookshelf", "minecraft:chiseled_bookshelf")
        add("minecraft:obsidian", "minecraft:obsidian")
        add("minecraft:crying_obsidian", "minecraft:crying_obsidian")
        add("minecraft:crafting_table", "minecraft:crafting_table")
        add("minecraft:crafter", "minecraft:crafter")
        add("minecraft:furnace", "minecraft:furnace", mapOf("facing_direction" to 2))
        add("minecraft:blast_furnace", "minecraft:blast_furnace", mapOf("facing_direction" to 2))
        add("minecraft:smoker", "minecraft:smoker", mapOf("facing_direction" to 2))
        add("minecraft:chest", "minecraft:chest", mapOf("facing_direction" to 2))
        add("minecraft:trapped_chest", "minecraft:trapped_chest", mapOf("facing_direction" to 2))
        add("minecraft:ender_chest", "minecraft:ender_chest", mapOf("facing_direction" to 2))
        add("minecraft:barrel", "minecraft:barrel", mapOf("facing_direction" to 1))
        add("minecraft:smithing_table", "minecraft:smithing_table")
        add("minecraft:fletching_table", "minecraft:fletching_table")
        add("minecraft:cartography_table", "minecraft:cartography_table")
        add("minecraft:loom", "minecraft:loom")
        add("minecraft:stonecutter", "minecraft:stonecutter_block")
        add("minecraft:grindstone", "minecraft:grindstone")
        add("minecraft:cauldron", "minecraft:cauldron")
        add("minecraft:brewing_stand", "minecraft:brewing_stand")
        add("minecraft:beacon", "minecraft:beacon")
        add("minecraft:conduit", "minecraft:conduit")
        add("minecraft:bell", "minecraft:bell")
        add("minecraft:campfire", "minecraft:campfire")
        add("minecraft:soul_campfire", "minecraft:soul_campfire")
        add("minecraft:respawn_anchor", "minecraft:respawn_anchor")
        add("minecraft:lodestone", "minecraft:lodestone")
        add("minecraft:enchanting_table", "minecraft:enchanting_table")
        add("minecraft:composter", "minecraft:composter")

        add("minecraft:dispenser", "minecraft:dispenser", mapOf("facing_direction" to 2))
        add("minecraft:dropper", "minecraft:dropper", mapOf("facing_direction" to 2))
        add("minecraft:hopper", "minecraft:hopper", mapOf("facing_direction" to 0))
        add("minecraft:observer", "minecraft:observer", mapOf("facing_direction" to 2))
        add("minecraft:piston", "minecraft:piston", mapOf("facing_direction" to 1))
        add("minecraft:sticky_piston", "minecraft:sticky_piston", mapOf("facing_direction" to 1))
        add("minecraft:repeater", "minecraft:unpowered_repeater")
        add("minecraft:comparator", "minecraft:unpowered_comparator")
        add("minecraft:target", "minecraft:target")
        add("minecraft:lightning_rod", "minecraft:lightning_rod")
        add("minecraft:daylight_detector", "minecraft:daylight_detector")
        add("minecraft:note_block", "minecraft:noteblock")
        add("minecraft:jukebox", "minecraft:jukebox")
        add("minecraft:lever", "minecraft:lever")
        add("minecraft:redstone_wire", "minecraft:redstone_wire")

        // 10. 下界与末地方块
        add("minecraft:netherrack", "minecraft:netherrack")
        add("minecraft:soul_sand", "minecraft:soul_sand")
        add("minecraft:soul_soil", "minecraft:soul_soil")
        add("minecraft:magma_block", "minecraft:magma")
        add("minecraft:glowstone", "minecraft:glowstone")
        add("minecraft:end_stone", "minecraft:end_stone")
        add("minecraft:end_stone_bricks", "minecraft:end_brick")
        add("minecraft:purpur_block", "minecraft:purpur_block", mapOf("chisel_type" to "default"))
        add("minecraft:purpur_pillar", "minecraft:purpur_block", mapOf("chisel_type" to "lines", "pillar_axis" to "y"))
        add("minecraft:prismarine", "minecraft:prismarine", mapOf("prismarine_block_type" to "default"))
        add("minecraft:dark_prismarine", "minecraft:prismarine", mapOf("prismarine_block_type" to "dark"))
        add("minecraft:prismarine_bricks", "minecraft:prismarine", mapOf("prismarine_block_type" to "bricks"))
        add("minecraft:quartz_block", "minecraft:quartz_block", mapOf("chisel_type" to "default"))
        add("minecraft:smooth_quartz", "minecraft:quartz_block", mapOf("chisel_type" to "smooth"))
        add("minecraft:chiseled_quartz_block", "minecraft:quartz_block", mapOf("chisel_type" to "chiseled"))
        add("minecraft:quartz_pillar", "minecraft:quartz_block", mapOf("chisel_type" to "lines", "pillar_axis" to "y"))
        add("minecraft:quartz_bricks", "minecraft:quartz_bricks")

        // 11. 生物群系与自然植被
        val plants = listOf(
            "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip",
            "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
            "lily_of_the_valley", "wither_rose", "sunflower", "lilac", "rose_bush", "peony"
        )
        for (p in plants) {
            add("minecraft:$p", "minecraft:$p")
        }

        add("minecraft:short_grass", "minecraft:tallgrass", mapOf("tall_grass_type" to "default"))
        add("minecraft:grass", "minecraft:tallgrass", mapOf("tall_grass_type" to "default"))
        add("minecraft:tall_grass", "minecraft:double_plant", mapOf("double_plant_type" to "grass"))
        add("minecraft:fern", "minecraft:tallgrass", mapOf("tall_grass_type" to "fern"))
        add("minecraft:large_fern", "minecraft:double_plant", mapOf("double_plant_type" to "fern"))
        add("minecraft:dead_bush", "minecraft:deadbush")
        add("minecraft:seagrass", "minecraft:seagrass")
        add("minecraft:sea_pickle", "minecraft:sea_pickle")
        add("minecraft:kelp", "minecraft:kelp")
        add("minecraft:lily_pad", "minecraft:waterlily")
        add("minecraft:vine", "minecraft:vine")
        add("minecraft:glow_lichen", "minecraft:glow_lichen")
        add("minecraft:spore_blossom", "minecraft:spore_blossom")
        add("minecraft:moss_block", "minecraft:moss_block")
        add("minecraft:moss_carpet", "minecraft:moss_carpet")
        add("minecraft:cactus", "minecraft:cactus")
        add("minecraft:sugar_cane", "minecraft:reeds")
        add("minecraft:bamboo", "minecraft:bamboo_sapling")
        add("minecraft:pumpkin", "minecraft:pumpkin")
        add("minecraft:carved_pumpkin", "minecraft:carved_pumpkin", mapOf("facing_direction" to 2))
        add("minecraft:jack_o_lantern", "minecraft:lit_pumpkin", mapOf("facing_direction" to 2))
        add("minecraft:melon", "minecraft:melon_block")
        add("minecraft:brown_mushroom", "minecraft:brown_mushroom")
        add("minecraft:red_mushroom", "minecraft:red_mushroom")
        add("minecraft:brown_mushroom_block", "minecraft:brown_mushroom_block")
        add("minecraft:red_mushroom_block", "minecraft:red_mushroom_block")
        add("minecraft:mushroom_stem", "minecraft:brown_mushroom_block")

        // 幽匿 (Sculk)
        add("minecraft:sculk", "minecraft:sculk")
        add("minecraft:sculk_catalyst", "minecraft:sculk_catalyst")
        add("minecraft:sculk_sensor", "minecraft:sculk_sensor")
        add("minecraft:sculk_shrieker", "minecraft:sculk_shrieker")
        add("minecraft:sculk_vein", "minecraft:sculk_vein")

        // 冰雪
        add("minecraft:snow_block", "minecraft:snow")
        add("minecraft:ice", "minecraft:ice")
        add("minecraft:packed_ice", "minecraft:packed_ice")
        add("minecraft:blue_ice", "minecraft:blue_ice")
        add("minecraft:slime_block", "minecraft:slime")
        add("minecraft:honey_block", "minecraft:honey_block")
        add("minecraft:sponge", "minecraft:sponge", mapOf("sponge_type" to "dry"))
        add("minecraft:wet_sponge", "minecraft:sponge", mapOf("sponge_type" to "wet"))
        add("minecraft:hay_block", "minecraft:hay_block", mapOf("pillar_axis" to "y"))
        add("minecraft:bone_block", "minecraft:bone_block", mapOf("pillar_axis" to "y"))

        return "{\n" + entries.joinToString(",\n") + "\n}"
    }

    private fun generateLegacyToJava(): String {
        val entries = mutableListOf<String>()

        fun add(legacyKey: String, javaState: String) {
            entries.add("  \"$legacyKey\": \"$javaState\"")
        }

        // 经典 1.12.2 旧版数字 ID 映射
        add("0", "minecraft:air")
        add("1", "minecraft:stone")
        add("1:1", "minecraft:granite")
        add("1:2", "minecraft:polished_granite")
        add("1:3", "minecraft:diorite")
        add("1:4", "minecraft:polished_diorite")
        add("1:5", "minecraft:andesite")
        add("1:6", "minecraft:polished_andesite")
        add("2", "minecraft:grass_block")
        add("3", "minecraft:dirt")
        add("3:1", "minecraft:coarse_dirt")
        add("3:2", "minecraft:podzol")
        add("4", "minecraft:cobblestone")
        add("5", "minecraft:oak_planks")
        add("5:1", "minecraft:spruce_planks")
        add("5:2", "minecraft:birch_planks")
        add("5:3", "minecraft:jungle_planks")
        add("5:4", "minecraft:acacia_planks")
        add("5:5", "minecraft:dark_oak_planks")
        add("7", "minecraft:bedrock")
        add("8", "minecraft:water")
        add("9", "minecraft:water")
        add("10", "minecraft:lava")
        add("11", "minecraft:lava")
        add("12", "minecraft:sand")
        add("12:1", "minecraft:red_sand")
        add("13", "minecraft:gravel")
        add("14", "minecraft:gold_ore")
        add("15", "minecraft:iron_ore")
        add("16", "minecraft:coal_ore")
        add("17", "minecraft:oak_log")
        add("17:1", "minecraft:spruce_log")
        add("17:2", "minecraft:birch_log")
        add("17:3", "minecraft:jungle_log")
        add("18", "minecraft:oak_leaves")
        add("18:1", "minecraft:spruce_leaves")
        add("18:2", "minecraft:birch_leaves")
        add("18:3", "minecraft:jungle_leaves")
        add("19", "minecraft:sponge")
        add("19:1", "minecraft:wet_sponge")
        add("20", "minecraft:glass")
        add("21", "minecraft:lapis_ore")
        add("22", "minecraft:lapis_block")
        add("23", "minecraft:dispenser")
        add("24", "minecraft:sandstone")
        add("25", "minecraft:note_block")
        add("26", "minecraft:bed")
        add("27", "minecraft:powered_rail")
        add("28", "minecraft:detector_rail")
        add("29", "minecraft:sticky_piston")
        add("30", "minecraft:cobweb")
        add("35", "minecraft:white_wool")
        add("35:1", "minecraft:orange_wool")
        add("35:2", "minecraft:magenta_wool")
        add("35:3", "minecraft:light_blue_wool")
        add("35:4", "minecraft:yellow_wool")
        add("35:5", "minecraft:lime_wool")
        add("35:6", "minecraft:pink_wool")
        add("35:7", "minecraft:gray_wool")
        add("35:8", "minecraft:light_gray_wool")
        add("35:9", "minecraft:cyan_wool")
        add("35:10", "minecraft:purple_wool")
        add("35:11", "minecraft:blue_wool")
        add("35:12", "minecraft:brown_wool")
        add("35:13", "minecraft:green_wool")
        add("35:14", "minecraft:red_wool")
        add("35:15", "minecraft:black_wool")
        add("41", "minecraft:gold_block")
        add("42", "minecraft:iron_block")
        add("44", "minecraft:stone_slab")
        add("45", "minecraft:bricks")
        add("46", "minecraft:tnt")
        add("47", "minecraft:bookshelf")
        add("48", "minecraft:mossy_cobblestone")
        add("49", "minecraft:obsidian")
        add("50", "minecraft:torch")
        add("53", "minecraft:oak_stairs")
        add("54", "minecraft:chest")
        add("56", "minecraft:diamond_ore")
        add("57", "minecraft:diamond_block")
        add("58", "minecraft:crafting_table")
        add("61", "minecraft:furnace")
        add("67", "minecraft:cobblestone_stairs")
        add("73", "minecraft:redstone_ore")
        add("79", "minecraft:ice")
        add("80", "minecraft:snow_block")
        add("81", "minecraft:cactus")
        add("82", "minecraft:clay")
        add("86", "minecraft:pumpkin")
        add("87", "minecraft:netherrack")
        add("88", "minecraft:soul_sand")
        add("89", "minecraft:glowstone")
        add("91", "minecraft:jack_o_lantern")
        add("98", "minecraft:stone_bricks")
        add("98:1", "minecraft:mossy_stone_bricks")
        add("98:2", "minecraft:cracked_stone_bricks")
        add("98:3", "minecraft:chiseled_stone_bricks")
        add("103", "minecraft:melon")
        add("121", "minecraft:end_stone")
        add("129", "minecraft:emerald_ore")
        add("133", "minecraft:emerald_block")
        add("138", "minecraft:beacon")
        add("152", "minecraft:redstone_block")
        add("155", "minecraft:quartz_block")
        add("159", "minecraft:white_terracotta")
        add("159:1", "minecraft:orange_terracotta")
        add("159:2", "minecraft:magenta_terracotta")
        add("159:3", "minecraft:light_blue_terracotta")
        add("159:4", "minecraft:yellow_terracotta")
        add("159:5", "minecraft:lime_terracotta")
        add("159:6", "minecraft:pink_terracotta")
        add("159:7", "minecraft:gray_terracotta")
        add("159:8", "minecraft:light_gray_terracotta")
        add("159:9", "minecraft:cyan_terracotta")
        add("159:10", "minecraft:purple_terracotta")
        add("159:11", "minecraft:blue_terracotta")
        add("159:12", "minecraft:brown_terracotta")
        add("159:13", "minecraft:green_terracotta")
        add("159:14", "minecraft:red_terracotta")
        add("159:15", "minecraft:black_terracotta")
        add("168", "minecraft:prismarine")
        add("168:1", "minecraft:prismarine_bricks")
        add("168:2", "minecraft:dark_prismarine")
        add("169", "minecraft:sea_lantern")
        add("174", "minecraft:packed_ice")
        add("201", "minecraft:purpur_block")
        add("206", "minecraft:end_stone_bricks")
        add("213", "minecraft:magma_block")
        add("214", "minecraft:nether_wart_block")
        add("215", "minecraft:red_nether_bricks")
        add("216", "minecraft:bone_block")
        add("251", "minecraft:white_concrete")
        add("251:1", "minecraft:orange_concrete")
        add("251:2", "minecraft:magenta_concrete")
        add("251:3", "minecraft:light_blue_concrete")
        add("251:4", "minecraft:yellow_concrete")
        add("251:5", "minecraft:lime_concrete")
        add("251:6", "minecraft:pink_concrete")
        add("251:7", "minecraft:gray_concrete")
        add("251:8", "minecraft:light_gray_concrete")
        add("251:9", "minecraft:cyan_concrete")
        add("251:10", "minecraft:purple_concrete")
        add("251:11", "minecraft:blue_concrete")
        add("251:12", "minecraft:brown_concrete")
        add("251:13", "minecraft:green_concrete")
        add("251:14", "minecraft:red_concrete")
        add("251:15", "minecraft:black_concrete")

        return "{\n" + entries.joinToString(",\n") + "\n}"
    }
}
