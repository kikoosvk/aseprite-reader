package com.ninjacontrol.mason

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
open class DataTest {

    @Test
    fun getByte() {
        val data = Data(ubyteArrayOf(0x1U, 0x2EU, 0x38U, 0xD4U, 0x89U, 0xC3U).toByteArray())
        val b1: UByte = 1u
        val b2: UByte = 0x2eu
        val b3: UByte = 0x38u
        val b4: UByte = 0xd4u
        assertEquals(b1, data.getByte())
        assertEquals(b2, data.getByte())
        assertEquals(b3, data.getByte())
        assertEquals(b4, data.getByte())
    }

    private infix fun UByteArray.eq(that: UByteArray) = this.withIndex().all { v -> that[v.index] == v.value }

    @Test
    fun getBytes() {
        val data = Data(ubyteArrayOf(0x1U, 0x2EU, 0x38U, 0xD4U, 0x89U, 0xFFU).toByteArray())
        val b1: UByte = 1u
        val b2 = ubyteArrayOf(0x2eu, 0x38U, 0xD4U, 0x89U, 0xFFU)
        assertEquals(b1, data.getByte())
        val ba = data.getBytes(5)
        assertTrue(b2 eq ba)
    }

    @Test
    fun getWord() {
        val data = Data(ubyteArrayOf(0x12U, 0x35U, 0x59U, 0xD4U, 0x89U, 0xFFU).toByteArray())
        val word1: aseWord = 0x01235U
        val word2: aseWord = 0x059D4U
        val b5: aseByte = 0x89U
        val b6: aseByte = 0xFFU
        assertEquals(word1, data.getWord())
        assertEquals(word2, data.getWord())
        assertEquals(b5, data.getByte())
        assertEquals(b6, data.getByte())
    }

    @Test
    fun getShort() {
        // 0x0FFFF (65535 unsigned) in 2's complement = 0x0001 -> -1 signed
        // 0x0FEFF (65279 unsigned) in 2's complement = 0x00101 -> -257 signed
        val data = Data(ubyteArrayOf(0xFFU, 0xFFU, 0xFEU, 0xFFU).toByteArray())
        val s1: aseShort = data.getShort()
        val s2: aseShort = data.getShort()
        assertEquals(-1, s1)
        assertEquals(-257, s2)
    }

    @Test
    fun getDword() {
        val data = Data(ubyteArrayOf(0x12U, 0x34U, 0x56U, 0x78U).toByteArray())
        val s1: aseDword = 0x012345678U // (305419896u)
        assertEquals(s1, data.getDword())
    }

    @Test
    fun getLong() {
        val data = Data(ubyteArrayOf(0xF2U, 0x34U, 0x56U, 0x78U).toByteArray())
        // 2's complement of 0x0F2345678 = 0xFFFFFFFF0DCBA988 -> -DCBA988 = -231451016
        val s1: Int = -231451016 // 0xDCBA988
        assertEquals(s1, data.getLong())
    }

    @Test
    fun getString() {
        val data = Data(ubyteArrayOf(0x000u, 0x021u /* length */, 0x064u, 0x065u, 0x074u, 0x074u, 0x061u, 0x020u, 0x0c3u, 0x0a4u, 0x072u, 0x020u, 0x065u, 0x074u, 0x074u, 0x020u, 0x074u, 0x065u, 0x073u, 0x074u, 0x021u, 0x022u, 0x026u, 0x026u, 0x026u, 0x026u, 0x0c3u, 0x085u, 0x0c3u, 0x084u, 0x0c3u, 0x096u, 0x0c2u, 0x0a7u).toByteArray())
        val str = "detta är ett test!\"&&&&ÅÄÖ§"
        assertEquals(str, data.getString())
    }

    @Test
    fun getRGBAPixel() {
        val data = Data(ubyteArrayOf(0x0FFU, 0x0EEu, 0x0DDu, 0x099u).toByteArray())
        val pixel = RGBAPixel(red = 0x0FFU, green = 0x0EEu, blue = 0x0DDu, alpha = 0x099u)
        assertEquals(pixel, data.getRGBAPixel())
    }

    @Test
    fun getGrayscalePixel() {
        val data = Data(ubyteArrayOf(0x0DDu, 0x099u).toByteArray())
        val pixel = GrayscalePixel(value = 0x0DDu, alpha = 0x099u)
        assertEquals(pixel, data.getGrayscalePixel())
    }
}
