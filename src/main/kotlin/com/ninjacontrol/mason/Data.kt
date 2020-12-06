package com.ninjacontrol.mason

@ExperimentalUnsignedTypes
class Data(private val buffer: ByteArray) {
    private var index: Int = 0
    val size: Int get() = buffer.size
    private fun advance(amount: Int) {
        if (index + amount > size) {
            throw ArrayIndexOutOfBoundsException("Index would be out of bounds")
        }
        index += amount
    }

    fun getByte(): aseByte = buffer[index++].toUByte()
    fun skipByte() = skip()
    fun getBytes(n: Int): UByteArray {
        val value = buffer.sliceArray(index..index + (n - 1)).asUByteArray()
        advance(n)
        return value
    }

    fun skipBytes(n: Int) = skip(n)
    private fun skip(n: Int = 1) {
        index += n
    }

    fun getWord(): aseWord {
        val value: aseWord =
            ((buffer[index + 1].toUInt() shl 8) + buffer[index].toUByte()).toUShort()
        advance(2)
        return value
    }

    fun skipWord() = skip(2)

    fun getShort(): aseShort {
        val value: aseShort =
            ((buffer[index + 1].toUInt() shl 8) + buffer[index].toUByte()).toShort()
        advance(2)
        return value
    }

    fun getDword(): aseDword {
        val valueHigh = getWord()
        val valueLow = getWord()
        return ((valueLow.toUInt() shl 16) + valueHigh.toUInt())
    }

    fun skipDword() = skip(4)

    fun getLong(): aseLong = getDword().toInt()
    fun getString(): String {
        val length: aseWord = getWord()
        val chars = getBytes(length.toInt() - 1)
        return chars.toByteArray().decodeToString()
    }

    // Read 16.16 fixed point value
    fun getFixed(): Float {
        val fixedLong = getLong()
        return (fixedLong / 65536f)
    }

    fun getRGBAPixel(): RGBAPixel {
        val data = getBytes(4)
        return RGBAPixel(red = data[0], green = data[1], blue = data[2], alpha = data[3])
    }

    fun getGrayscalePixel(): GrayscalePixel {
        val data = getBytes(2)
        return GrayscalePixel(value = data[0], alpha = data[1])
    }
}

data class RGBAPixel(val red: aseByte, val green: aseByte, val blue: aseByte, val alpha: aseByte)
data class GrayscalePixel(val value: aseByte, val alpha: aseByte)