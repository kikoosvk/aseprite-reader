package com.ninjacontrol.mason

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalUnsignedTypes
internal class AsepriteReaderTest {


    @Test
    fun `it should correctly read a file header`() {

        val file: File = File(javaClass.classLoader.getResource("test1.aseprite").file)
        val data = Data(file.readBytes())
        val header = AsepriteReader().getHeader(data)
        assertEquals(256, header.width.toInt())
        assertEquals(256, header.height.toInt())
        assertEquals(32, header.numColors.toInt())
        assertEquals(1, header.frames.toInt())
        assertEquals(32, header.colorDepth.toInt())
        assertEquals(0, header.transparentIndex.toInt())
        assertEquals(1, header.pixelWidth.toInt())
        assertEquals(1, header.pixelHeight.toInt())
        assertEquals(1, header.flags.toInt())
        assertEquals(16, header.gridWith.toInt())
        assertEquals(16, header.gridHeight.toInt())
        assertEquals(0, header.gridXPosition.toInt())
        assertEquals(0, header.gridYPosition.toInt())
        assertEquals(1927, header.size.toInt())
        assertEquals(100, header.speed.toInt())
    }
}