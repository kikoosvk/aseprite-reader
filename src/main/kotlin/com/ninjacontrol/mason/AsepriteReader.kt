package com.ninjacontrol.mason

import java.io.File
import java.util.*
import kotlin.collections.ArrayList


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
@ExperimentalUnsignedTypes
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

@ExperimentalUnsignedTypes
data class AsepriteFile(
        private val header: AsepriteHeader
)

@ExperimentalUnsignedTypes
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

        return AsepriteHeader(size, frames, width, height, colorDepth, flags, speed, transparentIndex, numColors, pixelWidth, pixelHeight, gridXPosition, gridYPosition, gridWith, gridHeight)
    }

    fun read(file: File): AsepriteFile {
        unless(file.exists() && file.canRead()) { throw ReaderException("Could not read file ${file.name}") }
        val data = Data(file.readBytes())
        return AsepriteFile(header = getHeader(data))
    }


}

@ExperimentalUnsignedTypes
enum class ChunkType(val value: aseWord) {
    OldPalette(0x0004U),
    OldPalette2(0x0011U),
    Layer(0x02004U),
    Cel(0x02005U),
    CelExtra(0x02006U),
    ColorProfile(0x02007U),
    Mask(0x02016U), // Deprecated
    Path(0x02017U),
    Tags(0x02018U),
    Palette(0x02019U),
    UserData(0x02020U),
    Slice(0x02022U);

    companion object {
        fun fromWord(value: aseWord) = ChunkType.values().first { it.value == value }
    }
}


sealed class Chunk {
    abstract val size: aseDword
    val id: String = UUID.randomUUID().toString()
    abstract val index: Int
}

@ExperimentalUnsignedTypes
private class XOldPaletteChunk(override val index: Int, override val size: aseDword, private val data: Data) : Chunk() {
    val packets: List<Packet>

    init {
        val numPackets = data.getWord().toInt()
        packets = ArrayList(numPackets)
        for (i in 0 until numPackets) {
            val skipEntries = data.getByte()
            val numColors = data.getByte()
            val adjustedNumColors: Int = if (numColors.toInt() == 0) 256 else numColors.toInt()
            val colors = ArrayList<Color>(adjustedNumColors)
            for (j in 0 until adjustedNumColors) {
                colors[j] = Color(red = data.getByte(), green = data.getByte(), blue = data.getByte())
            }
            packets[i] = Packet(skipEntries, colors)
        }
    }
}

// Palette Chunks

@ExperimentalUnsignedTypes
data class Color(val red: aseByte, val green: aseByte, val blue: aseByte)

@ExperimentalUnsignedTypes
data class Packet(val skipEntries: aseByte, val colors: List<Color>)

data class OldPaletteChunk(override val index: Int,
                           override val size: aseDword,
                           val packets: List<Packet>) : Chunk()

fun getOldPaletteChunk(index: Int, size: aseDword, data: Data): OldPaletteChunk {
    val numPackets = data.getWord().toInt()
    val packets = ArrayList<Packet>(numPackets)
    for (i in 0 until numPackets) {
        val skipEntries = data.getByte()
        val numColors = data.getByte()
        val adjustedNumColors: Int = if (numColors.toInt() == 0) 256 else numColors.toInt()
        val colors = ArrayList<Color>(adjustedNumColors)
        for (j in 0 until adjustedNumColors) {
            colors[j] = Color(red = data.getByte(), green = data.getByte(), blue = data.getByte())
        }
        packets[i] = Packet(skipEntries, colors)
    }
    return OldPaletteChunk(index, size, packets)
}

enum class LayerFlags(val value: aseWord) {
    Visible(1U),
    Editable(2U),
    LockMovement(4U),
    Background(8U),
    PreferLinkedCels(16U),
    DisplayLayerGroupCollapsed(32U),
    ReferenceLayer(64U)
}

enum class BlendMode(val value: aseWord) {
    Normal(0U),
    Multiply(1U),
    Screen(2U),
    Overlay(3U),
    Darken(4U),
    Lighten(5U),
    ColorDodge(6U),
    ColorBurn(7U),
    HardLight(8U),
    SoftLight(9U),
    Difference(10U),
    Exclusion(11U),
    Hue(12U),
    Saturation(13U),
    Color(14U),
    Luminosity(15U),
    Addition(16U),
    Subtract(17U),
    Divide(18U);

    companion object {
        fun fromWord(value: aseWord) = BlendMode.values().first { it.value == value }
    }
}

enum class LayerType(val value: aseWord) {
    Normal(0U),
    Group(1U);

    companion object {
        fun fromWord(value: aseWord) = LayerType.values().first { it.value == value }
    }
}

private class LayerChunk(override val index: Int, override val size: aseDword, private val data: Data) : Chunk() {


    val layerFlags: aseWord
    private val layerTypeValue: aseWord

    /**
     *
     *  The child level is used to show the relationship of this layer
     *  with the last one read, for example:
     *
     *  Layer name and hierarchy    Child Level
     *  -----------------------------------------------
     *  - Background                0
     *   `- Layer1                  1
     *  - Foreground                0
     *   |- My set1                 1
     *   |  `- Layer                2
     *   `- Layer3                  1
     *
     */
    val childLevel: aseWord
    val defaultLayerWidth: aseWord
    val defaultLayerHeight: aseWord
    private val blendModeValue: aseWord
    val opacity: aseByte
    val layerName: String

    init {
        layerFlags = data.getWord()
        layerTypeValue = data.getWord()
        childLevel = data.getWord()
        defaultLayerWidth = data.getWord()
        defaultLayerHeight = data.getWord()
        blendModeValue = data.getWord()
        opacity = data.getByte()
        data.skipBytes(3)
        layerName = data.getString()
    }

    val layerType: LayerType get() = LayerType.fromWord(layerTypeValue)
    val blendMode: BlendMode get() = BlendMode.fromWord(blendModeValue)
    fun isFlagSet(flag: LayerFlags): Boolean {
        return (layerFlags.or(flag.value)) == flag.value
    }

}


class CelChunk(override val index: Int, override val size: aseDword, private val data: Data) : Chunk() {
    val layerIndex: aseWord
    val xPosition: aseShort
    val yPosition: aseShort
    val opacityLevel: aseByte
    val celTypeValue: aseWord
    val width: aseWord?
    val height: aseWord?
    val linkedFrame: aseWord?
    val rgbaPixels: Array<RGBAPixel>?
    val grayscalePixels: Array<GrayscalePixel>?
    val rawPixels: aseByteArray?

}


class Frame(private val data: Data) {
    private val magicNumber: UShort = 0x0F1FAU

    internal val numBytes: aseDword
    val numChunks: aseDword
    val frameDurationMs: aseWord
    val chunks: List<Chunk>

    init {

        numBytes = data.getDword()
        val magic = data.getWord()
        unless(magic == magicNumber) { throw ReaderException("Invalid chunk type") }
        val oldNumChunks = data.getWord()
        frameDurationMs = data.getWord()
        data.skipBytes(2)
        val newNumChunks = data.getDword()
        numChunks = if (newNumChunks.toInt() == 0) oldNumChunks.toUInt() else newNumChunks
        chunks = ArrayList(numChunks.toInt())
        for (i in 0 until numChunks.toInt()) {
            val current = getChunk(i, data)
            chunks[i] = current
        }
    }

    internal fun getChunk(index: Int, data: Data): Chunk {
        val size = data.getDword()
        val type = data.getWord()
        return when (val chunkType = ChunkType.fromWord(type)) {
            ChunkType.OldPalette2, ChunkType.OldPalette -> OldPaletteChunk(index, size, data)
            ChunkType.Layer -> LayerChunk(index, size, data)
            else -> throw ReaderException("Chunk type '$chunkType' not implemented")
        }
    }

}

class ReaderException(message: String) : Throwable(message)

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
        val value: aseWord = ((buffer[index + 1].toUInt() shl 8) + buffer[index].toUByte()).toUShort()
        advance(2)
        return value
    }

    fun skipWord() = skip(2)

    fun getShort(): aseShort {
        val value: aseShort = ((buffer[index + 1].toUInt() shl 8) + buffer[index].toUByte()).toShort()
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

fun unless(condition: Boolean, block: () -> Unit) {
    if (!condition) block()
}