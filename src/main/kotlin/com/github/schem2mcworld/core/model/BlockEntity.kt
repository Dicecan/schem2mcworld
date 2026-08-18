package com.github.schem2mcworld.core.model

import net.querz.nbt.tag.CompoundTag

data class BlockEntity(
    val position: Vector3i,
    val id: String,
    val data: CompoundTag
)
