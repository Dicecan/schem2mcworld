package com.github.schem2mcworld.core.model

data class SchematicData(
    val width: Int,
    val height: Int,
    val length: Int,
    val blocks: Array<UniversalBlock>,
    val blockEntities: List<BlockEntity> = emptyList(),
    val offset: Vector3i = Vector3i.ZERO
) {
    val totalBlocks: Int get() = width * height * length

    init {
        require(width > 0 && height > 0 && length > 0) {
            "Schematic dimensions must be positive, got width=$width, height=$height, length=$length"
        }
        require(blocks.size == totalBlocks) {
            "Block array size (${blocks.size}) does not match dimensions ($width x $height x $length = $totalBlocks)"
        }
    }

    fun getIndex(x: Int, y: Int, z: Int): Int = (y * length + z) * width + x

    fun getBlock(x: Int, y: Int, z: Int): UniversalBlock {
        if (x !in 0 until width || y !in 0 until height || z !in 0 until length) {
            return UniversalBlock.AIR
        }
        return blocks[getIndex(x, y, z)]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SchematicData
        if (width != other.width) return false
        if (height != other.height) return false
        if (length != other.length) return false
        if (!blocks.contentEquals(other.blocks)) return false
        if (blockEntities != other.blockEntities) return false
        if (offset != other.offset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + length
        result = 31 * result + blocks.contentHashCode()
        result = 31 * result + blockEntities.hashCode()
        result = 31 * result + offset.hashCode()
        return result
    }
}
