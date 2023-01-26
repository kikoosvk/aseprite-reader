@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ninjacontrol.mason

import kotlin.reflect.KClass
val magicNumber: UShort = 0x0F1FAU

data class Frame(
    val numBytes: aseDword,
    val numChunks: aseDword,
    val frameDurationMs: aseWord,
    val chunks: List<Chunk>
) {
    fun write(writer: DataWriter) {
        writer.writeDword(numBytes)
        writer.writeWord(magicNumber)
        writer.writeWord(numChunks.toUShort())
        writer.writeWord(frameDurationMs)
        writer.skipBytes(2)
        writer.writeDword(numChunks)
        chunks.forEach {
            it.write(writer)
        }
    }
}

inline fun <reified T : Chunk> Frame.getChunksBy(type: KClass<T>): List<T> =
    chunks.filterIsInstance<T>()

fun getFrame(pixelType: PixelType, data: Data): Frame {

    val numChunks: aseDword

    val numBytes = data.getDword()
    val magic = data.getWord()
    unless(magic == magicNumber) { throw ReaderException("Invalid chunk type") }
    val oldNumChunks = data.getWord()
    val frameDurationMs = data.getWord()
    data.skipBytes(2)
    val newNumChunks = data.getDword()
    numChunks = if (newNumChunks.toInt() == 0) oldNumChunks.toUInt() else newNumChunks
    val chunks = ArrayList<Chunk>(numChunks.toInt())

    for (i in 0 until numChunks.toInt()) {
        chunks.add(i, getChunk(i, pixelType, data))
    }
    return Frame(numBytes, numChunks, frameDurationMs, chunks)
}
