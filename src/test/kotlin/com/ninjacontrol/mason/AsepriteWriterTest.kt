@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ninjacontrol.mason

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AsepriteWriterTest {

    @Test
    fun `it should correctly write a file header`() {
        val tempHeader = AsepriteHeader(
            1927U,
            1U,
            256U,
            256U,
            32U,
            1U,
            100U,
            0U,
            32U,
            1U,
            1U,
            0,
            0,
            16U,
            16U
        )
        val writer = DataWriter()
        tempHeader.write(writer)

        val header = AsepriteReader().getHeader(Data(writer.buffer))


        assertEquals(1927, header.size.toInt())
        assertEquals(1, header.frames.toInt())
        assertEquals(256, header.width.toInt())
        assertEquals(256, header.height.toInt())
        assertEquals(32, header.colorDepth.toInt())
        assertEquals(32, header.numColors.toInt())
        assertEquals(32, header.colorDepth.toInt())
        assertEquals(0, header.transparentIndex.toInt())
        assertEquals(1, header.pixelWidth.toInt())
        assertEquals(1, header.pixelHeight.toInt())
        assertEquals(1, header.flags.toInt())
        assertEquals(16, header.gridWidth.toInt())
        assertEquals(16, header.gridHeight.toInt())
        assertEquals(0, header.gridXPosition.toInt())
        assertEquals(0, header.gridYPosition.toInt())
        assertEquals(100, header.speed.toInt())
    }

    @Test
    fun `it should write mine file`() {
        val file = File(javaClass.classLoader.getResource("mine-test.aseprite").file)
        val asepriteFileOriginal = AsepriteReader().read(file)
        val frameOriginal = asepriteFileOriginal.frames[0]
        val fileToBeCreated = File("mine-test-out.aseprite")
        AsepriteWriter().write(fileToBeCreated, asepriteFileOriginal)
        val asepriteFileCreated = AsepriteReader().read(fileToBeCreated)
        val frameCreated = asepriteFileCreated.frames[0]

        assertNotNull(asepriteFileCreated)
        assertTrue(asepriteFileOriginal.header.equals(asepriteFileCreated.header))

        assertEquals(frameOriginal.numBytes, frameCreated.numBytes)
        assertEquals(frameOriginal.numChunks, frameCreated.numChunks)
        assertEquals(frameOriginal.frameDurationMs, frameCreated.frameDurationMs)
        assertEquals(frameOriginal.chunks.size, frameCreated.chunks.size)

        val colorProfileChunk = frameOriginal.chunks[0] as ColorProfileChunk
        val colorProfileChunkOut = frameCreated.chunks[0] as ColorProfileChunk
        assertEquals(colorProfileChunk, colorProfileChunkOut)

        val paletteChunk = frameOriginal.chunks[1] as PaletteChunk
        val paletteChunkOut = frameCreated.chunks[1] as PaletteChunk
        assertEquals(paletteChunk, paletteChunkOut)

        val oldPaletteChunk = frameOriginal.chunks[2] as OldPaletteChunk
        val oldPaletteChunkOut = frameCreated.chunks[2] as OldPaletteChunk
        assertEquals(oldPaletteChunk, oldPaletteChunkOut)

        val layerChunk = frameOriginal.chunks[3] as LayerChunk
        val layerChunkOut = frameCreated.chunks[3] as LayerChunk
        assertEquals(layerChunk, layerChunkOut)

        val compressedCelChunk = frameOriginal.chunks[4] as CompressedCelChunk
        val compressedCelChunkOut = frameCreated.chunks[4] as CompressedCelChunk
        assertEquals(compressedCelChunk.size, compressedCelChunkOut.size)
        assertEquals(compressedCelChunk.index, compressedCelChunkOut.index)
        assertEquals(compressedCelChunk.layerIndex, compressedCelChunkOut.layerIndex)
        assertEquals(compressedCelChunk.xPosition, compressedCelChunkOut.xPosition)
        assertEquals(compressedCelChunk.yPosition, compressedCelChunkOut.yPosition)
        assertEquals(compressedCelChunk.opacityLevel, compressedCelChunkOut.opacityLevel)
        assertEquals(compressedCelChunk.width, compressedCelChunkOut.width)
        assertEquals(compressedCelChunk.height, compressedCelChunkOut.height)
        assertTrue(compressedCelChunk.compressedPixels.toByteArray()
            .contentEquals(compressedCelChunkOut.compressedPixels.toByteArray()))



    }


    companion object {
        @AfterAll
        @JvmStatic
        fun clean() {
            val outFile = File("mine-test-out.aseprite")
            if(outFile.exists()) {
                outFile.delete()
            }
        }
    }

}

