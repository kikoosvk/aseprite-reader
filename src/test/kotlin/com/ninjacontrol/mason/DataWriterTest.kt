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
    fun getWord() {
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


}
