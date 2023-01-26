@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ninjacontrol.mason

import java.io.File

typealias aseByte = UByte
typealias aseWord = UShort
typealias aseShort = Short
typealias aseDword = UInt
typealias aseLong = Int
typealias aseByteArray = UByteArray

val AsepriteHeader.pixelType: PixelType
    get() = when (colorDepth.toInt()) {
        8 -> PixelType.Indexed
        16 -> PixelType.Grayscale
        32 -> PixelType.RGBA
        else -> throw ReaderException("Invalid color depth")
    }

data class AsepriteFile(
    val header: AsepriteHeader,
    val frames: List<Frame>
)

class AsepriteReader {

    private val magicNumber: UShort = 42464u

    internal fun getHeader(data: Data): AsepriteHeader {
        val size = data.getDword()
        val magic = data.getWord()
        unless(magic == magicNumber) { throw ReaderException("Unrecognized file") }
        val frames = data.getWord()
        val width = data.getWord()
        val height = data.getWord()
        val colorDepth = data.getWord()
        val flags = data.getDword()
        val speed = data.getWord()
        data.skipDword()
        data.skipDword()
        val transparentIndex = data.getByte()
        data.skipBytes(3)
        val numColors = data.getWord()
        val pixelWidth = data.getByte()
        val pixelHeight = data.getByte()
        val gridXPosition = data.getShort()
        val gridYPosition = data.getShort()
        val gridWidth = data.getWord()
        val gridHeight = data.getWord()
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
            gridWidth,
            gridHeight
        )
    }

    internal fun getFrames(numFrames: Int, pixelType: PixelType, data: Data): List<Frame> {
        val frames = ArrayList<Frame>(numFrames)
        for (i in 0 until numFrames) {
            frames.add(getFrame(pixelType, data))
        }
        return frames
    }

    fun read(file: File): AsepriteFile {
        unless(file.exists() && file.canRead()) { throw ReaderException("Could not read file ${file.name}") }
        val data = Data(file.readBytes())
        val header = getHeader(data)
        val frames = getFrames(header.frames.toInt(), header.pixelType, data)
        return AsepriteFile(header, frames)
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
