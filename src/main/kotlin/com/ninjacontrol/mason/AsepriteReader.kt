package com.ninjacontrol.mason

/*
## Types

BYTE: An 8-bit unsigned integer value
WORD: A 16-bit unsigned integer value
SHORT: A 16-bit signed integer value
DWORD: A 32-bit unsigned integer value
LONG: A 32-bit signed integer value
FIXED: A 32-bit fixed point (16.16) value
BYTE[n]: "n" bytes.
STRING:
WORD: string length (number of bytes)
BYTE[length]: characters (in UTF-8) The '\0' character is not included.
PIXEL: One pixel, depending on the image pixel format:
RGBA: BYTE[4], each pixel have 4 bytes in this order Red, Green, Blue, Alpha.
Grayscale: BYTE[2], each pixel have 2 bytes in the order Value, Alpha.
Indexed: BYTE, Each pixel uses 1 byte (the index).
 */

@ExperimentalUnsignedTypes
typealias aseByte = UByte
@ExperimentalUnsignedTypes
typealias aseWord = UShort
typealias aseShort = Short
@ExperimentalUnsignedTypes
typealias aseDword = UInt
typealias aseLong = Int

/*
## Header

DWORD       File size
WORD        Magic number (0xA5E0)
WORD        Frames
WORD        Width in pixels
WORD        Height in pixels
WORD        Color depth (bits per pixel)
              32 bpp = RGBA
              16 bpp = Grayscale
              8 bpp = Indexed
DWORD       Flags:
              1 = Layer opacity has valid value
WORD        Speed (milliseconds between frame, like in FLC files)
            DEPRECATED: You should use the frame duration field
            from each frame header
DWORD       Set be 0
DWORD       Set be 0
BYTE        Palette entry (index) which represent transparent color
            in all non-background layers (only for Indexed sprites).
BYTE[3]     Ignore these bytes
WORD        Number of colors (0 means 256 for old sprites)
BYTE        Pixel width (pixel ratio is "pixel width/pixel height").
            If this or pixel height field is zero, pixel ratio is 1:1
BYTE        Pixel height
SHORT       X position of the grid
SHORT       Y position of the grid
WORD        Grid width (zero if there is no grid, grid size
            is 16x16 on Aseprite by default)
WORD        Grid height (zero if there is no grid)
BYTE[84]    For future (set to zero)
*/
@ExperimentalUnsignedTypes
data class AsepriteHeader(
        // Header
        private val size: aseDword,
        private val frames: aseWord,
        private val width: aseWord,
        private val height: aseWord,
        private val colorDepth: aseWord,
        private val flags: aseDword,
        private val speed: aseWord,
        private val transparentIndex: aseByte,
        private val numColors: aseWord,
        private val pixelWidth: aseByte,
        private val pixelHeight: aseByte,
        private val gridXPosition: aseShort,
        private val gridYPosition: aseShort,
        private val gridWith: aseWord,
        private val gridHeight: aseWord
)

data class AsepriteFile(
        private val header: AsepriteHeader
)

class AsepriteReader {

    // fun read( file: File) : AsepriteFile {

    // file.read
    // val buffer : ByteBuffer =

    // }

    // private fun readHeader(file: File) : AsepriteHeader =
    // file.read
}

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
    fun getBytes(n: Int): UByteArray {
        val value = buffer.sliceArray(index..index + (n - 1)).asUByteArray()
        advance(n)
        return value
    }

    fun getWord(): aseWord {
        val value: aseWord = ((buffer[index].toUInt() shl 8) + buffer[index + 1].toUByte()).toUShort()
        advance(2)
        return value
    }

    fun getShort(): aseShort {
        val value: aseShort = ((buffer[index].toUInt() shl 8) + buffer[index + 1].toUByte()).toShort()
        advance(2)
        return value
    }

    fun getDword(): aseDword {
        val valueHigh = getWord()
        val valueLow = getWord()
        return ((valueHigh.toUInt() shl 16) + valueLow.toUInt())
    }

    fun getLong(): aseLong = getDword().toInt()
    fun getString(): String {
        val length: aseWord = getWord()
        val chars = getBytes(length.toInt() - 1)
        return chars.toByteArray().decodeToString()
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
