package com.ninjacontrol.mason

@ExperimentalUnsignedTypes
class DataWriter(bufferSize: Int = 9999) {
    private var index: Int = 0
    val buffer = ByteArray(bufferSize)
    val size: Int get() = buffer.size

    private fun advance(amount: Int) {
        if (index + amount > size) {
            throw ArrayIndexOutOfBoundsException("Index would be out of bounds")
        }
        index += amount
    }

    fun writeByte(byte: aseByte) {
        this.buffer[index++] = byte.toByte()
    }

    fun skipByte() = skip()

    fun writeBytes(array: UByteArray) {
        writeBytes(array.toByteArray())
    }

    fun writeBytes(array: ByteArray) {
        array.copyInto(buffer, index, 0, array.size)
        index += array.size
    }

    fun skipBytes(n: Int) = skip(n)
    private fun skip(n: Int = 1) {
        index += n
    }

    fun writeWord(word: aseWord) {
        buffer[index] = word.toByte()
        buffer[index + 1] = (word.toInt() shr 8).toByte()
        advance(2)
    }

    fun skipWord() = skip(2)

    fun writeShort(sh1: aseShort) {
        buffer[index] = sh1.toByte()
        buffer[index + 1] = (sh1.toInt() shr 8).toByte()
        advance(2)
    }

    fun writeDword(dword: aseDword) {
        val valueLow = (dword shr 16).toUShort()
        val valueHigh = dword.toUShort()
        writeWord(valueHigh)
        writeWord(valueLow)
    }

    fun skipDword() = skip(4)

    fun writeLong(long: aseLong) {
        writeDword(long.toUInt())
    }


    fun writeString(str: String) {
        val array = str.toByteArray()
        writeWord(array.size.toUShort())
        writeBytes(array)
    }

    // Write 16.16 fixed point value
    fun writeFixed(fixed: Float) {
        val fixedLong = fixed * 65536f

        writeLong(fixedLong.toInt())
    }


    fun writeRGBAPixel(pixel: RGBAPixel) {
        val (red, green, blue, alpha) = pixel
        writeByte(red)
        writeByte(green)
        writeByte(blue)
        writeByte(alpha)
    }

    fun writeColor(color: Color) {
        val (red, green, blue, alpha) = color
        writeByte(red)
        writeByte(green)
        writeByte(blue)
        writeByte(alpha)
    }


    fun writeGrayscalePixel(pixel: GrayscalePixel) {
        val (value, alpha) = pixel
        writeByte(value)
        writeByte(alpha)
    }

    fun writeOldColor(oldColor: OldColor) {
        val (red, green, blue) = oldColor
        writeByte(red)
        writeByte(green)
        writeByte(blue)
    }

}
