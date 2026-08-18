package com.github.schem2mcworld.tools

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FullMappingsGeneratorTest {

    @Test
    fun `generate and verify full builtin mappings`() {
        FullMappingsGenerator.main(emptyArray())

        val j2b = File("src/main/resources/mappings/java_to_bedrock.json")
        val l2j = File("src/main/resources/mappings/legacy_to_java.json")

        assertTrue(j2b.exists())
        assertTrue(j2b.length() > 5000, "java_to_bedrock.json should contain extensive mappings")

        assertTrue(l2j.exists())
        assertTrue(l2j.length() > 1000, "legacy_to_java.json should contain legacy mappings")
    }
}
