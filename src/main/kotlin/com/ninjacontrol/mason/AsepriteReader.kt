package com.ninjacontrol.mason

import java.io.File


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

typealias aseByte = UByte
typealias aseWord = UShort
typealias aseShort = Short
typealias aseDword = UInt
typealias aseLong = Int
typealias aseByteArray = UByteArray

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
data class AsepriteHeader(
    // Header
    val size: aseDword,
    val frames: aseWord,
    val width: aseWord,
    val height: aseWord,
    val colorDepth: aseWord,
    val flags: aseDword,
    val speed: aseWord,
    val transparentIndex: aseByte,
    val numColors: aseWord,
    val pixelWidth: aseByte,
    val pixelHeight: aseByte,
    val gridXPosition: aseShort,
    val gridYPosition: aseShort,
    val gridWith: aseWord,
    val gridHeight: aseWord
)

data class AsepriteFile(
    private val header: AsepriteHeader
)

class AsepriteReader {

    private val magicNumber: UShort = 42464u

    internal fun getHeader(data: Data): AsepriteHeader {
        val size: aseDword = data.getDword()
        val magic: aseWord = data.getWord()
        unless(magic == magicNumber) { throw ReaderException("Unrecognized file") }
        val frames: aseWord = data.getWord()
        val width: aseWord = data.getWord()
        val height: aseWord = data.getWord()
        val colorDepth: aseWord = data.getWord()
        val flags: aseDword = data.getDword()
        val speed: aseWord = data.getWord()
        data.skipDword()
        data.skipDword()
        val transparentIndex: aseByte = data.getByte()
        data.skipBytes(3)
        val numColors: aseWord = data.getWord()
        val pixelWidth: aseByte = data.getByte()
        val pixelHeight: aseByte = data.getByte()
        val gridXPosition: aseShort = data.getShort()
        val gridYPosition: aseShort = data.getShort()
        val gridWith: aseWord = data.getWord()
        val gridHeight: aseWord = data.getWord()
        data.skipBytes(84)

        return AsepriteHeader(
            size,
            frames,
            width,
            height,
            colorDepth,
            flags,
            speed,
            transparentIndex,
            numColors,
            pixelWidth,
            pixelHeight,
            gridXPosition,
            gridYPosition,
            gridWith,
            gridHeight
        )
    }

    fun read(file: File): AsepriteFile {
        unless(file.exists() && file.canRead()) { throw ReaderException("Could not read file ${file.name}") }
        val data = Data(file.readBytes())
        return AsepriteFile(header = getHeader(data))
    }


}


enum class PixelType {
    RGBA,
    Grayscale,
    Indexed
}

class ReaderException(message: String) : Throwable(message)

fun unless(condition: Boolean, block: () -> Unit) {
    if (!condition) block()
}