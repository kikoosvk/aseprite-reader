@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ninjacontrol.mason

data class AsepriteHeader(
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
    val gridWidth: aseWord,
    val gridHeight: aseWord
) {
    private val magicNumber: UShort = 42464u

    fun write(writer: DataWriter) {
        writer.writeDword(size)
        writer.writeWord(magicNumber)
        writer.writeWord(frames)
        writer.writeWord(width)
        writer.writeWord(height)
        writer.writeWord(colorDepth)
        writer.writeDword(flags)
        writer.writeWord(speed)
        writer.skipDword()
        writer.skipDword()
        writer.writeByte(transparentIndex)
        writer.skipBytes(3)
        writer.writeWord(numColors)
        writer.writeByte(pixelWidth)
        writer.writeByte(pixelHeight)
        writer.writeShort(gridXPosition)
        writer.writeShort(gridYPosition)
        writer.writeWord(gridWidth)
        writer.writeWord(gridHeight)
        writer.skipBytes(84)
    }
}