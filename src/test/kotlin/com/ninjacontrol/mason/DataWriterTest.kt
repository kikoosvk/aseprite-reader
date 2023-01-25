package com.ninjacontrol.mason

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
open class DataWriterTest {

    /*
    * NB: The Aseprite file format uses LE byte ordering
    */

    @Test
    fun writeByte() {
        val b1: UByte = 1u
        val b2: UByte = 0x2eu
        val b3: UByte = 0x38u
        val b4: UByte = 0xd4u
        val dataWriter = DataWriter()

        dataWriter.writeByte(b1)
        dataWriter.writeByte(b2)
        dataWriter.writeByte(b3)
        dataWriter.writeByte(b4)

        val data = Data(dataWriter.buffer)
        assertEquals(b1, data.getByte())
        assertEquals(b2, data.getByte())
        assertEquals(b3, data.getByte())
        assertEquals(b4, data.getByte())
    }

    infix fun UByteArray.eq(that: UByteArray) =
        this.withIndex().all { v -> that[v.index] == v.value }

    @Test
    fun writeBytes() {
        val dataWriter = DataWriter()
        val b1: UByte = 1u
        dataWriter.writeByte(b1)
        val array = ubyteArrayOf(0x2EU, 0x38U, 0xD4U, 0x89U, 0xFFU)
        dataWriter.writeBytes(array)

        val data = Data(dataWriter.buffer)

        val b2 = ubyteArrayOf(0x2eu, 0x38U, 0xD4U, 0x89U, 0xFFU)
        assertEquals(b1, data.getByte())
        val ba = data.getBytes(5)
        assertTrue(b2 eq ba)
    }

    @Test
    fun writeWord() {
        val dataWriter = DataWriter()
        val word1: aseWord = 0x01235U
        val word2: aseWord = 0x059D4U
        val b5: aseByte = 0x89U
        val b6: aseByte = 0xFFU

        dataWriter.writeWord(word1)
        dataWriter.writeWord(word2)
        dataWriter.writeByte(b5)
        dataWriter.writeByte(b6)

        val data = Data(dataWriter.buffer)
        assertEquals(word1, data.getWord())
        assertEquals(word2, data.getWord())
        assertEquals(b5, data.getByte())
        assertEquals(b6, data.getByte())
    }

    @Test
    fun writeShort() {
        // 0x0FFFF (65535 unsigned) in 2's complement = 0x0001 -> -1 signed
        // 0x0FEFF (65279 unsigned) in 2's complement = 0x00101 -> -257 signed
        val dataWriter = DataWriter()
        val uint1: UInt = (0xFFU).toUInt()
        val sh1 = ((uint1 shl 8) + (0xFFU).toUInt()).toShort()
        val uini2: UInt = (0xFEU).toUInt()
        val sh2 = ((uini2 shl 8) + (0xFFU).toUInt()).toShort()

        dataWriter.writeShort(sh1)
        dataWriter.writeShort(sh2)

        val data = Data(dataWriter.buffer)
        val s1: aseShort = data.getShort()
        val s2: aseShort = data.getShort()
        assertEquals(-1, s1)
        assertEquals(-257, s2)
    }

    @Test
    fun writeDword() {
        val dataWriter = DataWriter()
        val s1: aseDword = 0x012345678U // (305419896u)
        dataWriter.writeDword(s1)

        val data = Data(dataWriter.buffer)

        assertEquals(s1, data.getDword())
    }

    @Test
    fun writeLong() {
        val dataWriter = DataWriter()
        val s1: aseLong = -231451016 // 0xDCBA988
        dataWriter.writeLong(s1)

        val data = Data(dataWriter.buffer)

        assertEquals(s1, data.getLong())
    }

    @Test
    fun writeFixed() {
        val dataWriter = DataWriter()
        val s1: Float = -3531.66223144f
        val s2: Float = 4.59999f
        val s3: Float = 0.98f

        dataWriter.writeFixed(s1)
        dataWriter.writeFixed(s2)
        dataWriter.writeFixed(s3)

        val data = Data(dataWriter.buffer)

        val returnedFixed1 = data.getFixed()
        val returnedFixed2 = data.getFixed()
        val returnedFixed3 = data.getFixed()
        assertEquals(s1, returnedFixed1, 0.00001f)
        assertEquals(s2, returnedFixed2, 0.0001f)
        assertEquals(s3, returnedFixed3, 0.00001f)
    }


    @Test
    fun writeString() {
        val dataWriter = DataWriter()
        val str = "detta är ett test!\"&&&&ÅÄÖ§"

        dataWriter.writeString(str)

        val data = Data(dataWriter.buffer)
        val returnedStr = data.getString()
        assertEquals(str, returnedStr)
    }

    @Test
    fun writeStringMine() {
        val dataWriter = DataWriter()
        val byte: UByte = 0xFFU
        val str = "This is a test of a write string"

        dataWriter.writeByte(byte)
        dataWriter.writeString(str)

        val data = Data(dataWriter.buffer)
        assertEquals(byte, data.getByte())
        val returnedStr = data.getString()
        assertEquals(str, returnedStr)
    }


    @Test
    fun writeRGBAPixel() {
        val dataWriter = DataWriter()
        val pixel = RGBAPixel(red = 0x0FFU, green = 0x0EEu, blue = 0x0DDu, alpha = 0x099u)

        dataWriter.writeRGBAPixel(pixel)

        val data = Data(dataWriter.buffer)
        assertEquals(pixel, data.getRGBAPixel())
    }

    @Test
    fun writeGrayscalePixel() {
        val dataWriter = DataWriter()
        val pixel = GrayscalePixel(value = 0x0DDu, alpha = 0x099u)

        dataWriter.writeGrayscalePixel(pixel)

        val data = Data(dataWriter.buffer)
        assertEquals(pixel, data.getGrayscalePixel())
    }

}
