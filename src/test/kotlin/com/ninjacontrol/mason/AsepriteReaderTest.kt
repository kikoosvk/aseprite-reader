package com.ninjacontrol.mason

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

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

    @Test
    fun `it should correctly read frames`() {
        val file: File = File(javaClass.classLoader.getResource("test1.aseprite").file)
        val data = Data(file.readBytes())
        val header = AsepriteReader().getHeader(data)
        val frames = AsepriteReader().getFrames(header.frames.toInt(), header.pixelType, data)
        assertEquals(1, frames.size)
        assertEquals(1799U, frames[0].numBytes)
        assertEquals(5U, frames[0].numChunks)
        assertEquals(100, frames[0].frameDurationMs.toInt())
        assertEquals(5, frames[0].chunks.size)
        assertTrue(frames[0].chunks[0] is ColorProfileChunk)
        assertTrue(frames[0].chunks[1] is PaletteChunk)
        assertTrue(frames[0].chunks[2] is OldPaletteChunk)
        assertTrue(frames[0].chunks[3] is LayerChunk)
        assertTrue(frames[0].chunks[4] is CompressedCelChunk)

    }

    @Test
    fun `it should correctly read a file`() {

        val file: File = File(javaClass.classLoader.getResource("test1.aseprite").file)
        val asepriteFile = AsepriteReader().read(file)
        assertNotNull(asepriteFile)
    }
}