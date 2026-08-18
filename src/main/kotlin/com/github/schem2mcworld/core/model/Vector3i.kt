package com.github.schem2mcworld.core.model

data class Vector3i(
    val x: Int,
    val y: Int,
    val z: Int
) {
    operator fun plus(other: Vector3i): Vector3i =
        Vector3i(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vector3i): Vector3i =
        Vector3i(x - other.x, y - other.y, z - other.z)

    companion object {
        val ZERO = Vector3i(0, 0, 0)
    }
}
