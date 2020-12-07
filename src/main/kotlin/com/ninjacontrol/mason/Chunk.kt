package com.ninjacontrol.mason

import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.Inflater

sealed class Chunk {
    abstract val size: aseDword
    val id: String = UUID.randomUUID().toString()
    abstract val index: Int
}

fun getChunk(index: Int, pixelType: PixelType, data: Data): Chunk {
    val size = data.getDword()
    val type = data.getWord()
    return when (val chunkType = ChunkType.fromWord(type)) {
        ChunkType.OldPalette2, ChunkType.OldPalette -> getOldPaletteChunk(index, size, data)
        ChunkType.Layer -> getLayerChunk(index, size, data)
        ChunkType.Cel -> getCelChunk(index, size, pixelType, data)
        ChunkType.CelExtra -> getCelExtraChunk(index, size, data)
        ChunkType.ColorProfile -> getColorProfileChunk(index, size, data)
        ChunkType.Mask -> getMaskChunk(index, size, data) // Deprecated
        ChunkType.Path -> getPathChunk(index, size) // Not used
        ChunkType.Tags -> getTagsChunk(index, size, data)
        ChunkType.Palette -> getPaletteChunk(index, size, data)
        ChunkType.UserData -> getUserDataChunk(index, size, data)
        ChunkType.Slice -> getSliceChunk(index, size, data)
        else -> throw ReaderException("Chunk type '$chunkType' not implemented")
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

// -- Palette ---

data class OldColor(val red: aseByte, val green: aseByte, val blue: aseByte)
data class Color(
    val red: aseByte,
    val green: aseByte,
    val blue: aseByte,
    val alpha: aseByte,
    val name: String?
)

data class Packet(val skipEntries: aseByte, val colors: List<OldColor>)
data class OldPaletteChunk(
    override val index: Int,
    override val size: aseDword,
    val packets: List<Packet>
) : Chunk()

data class PaletteChunk(
    override val size: aseDword,
    override val index: Int,
    val newPaletteSize: aseDword,
    val firstIndex: aseDword,
    val lastIndex: aseDword,
    val colors: List<Color>
) : Chunk()

fun getOldPaletteChunk(index: Int, size: aseDword, data: Data): OldPaletteChunk {
    val numPackets = data.getWord().toInt()
    val packets = ArrayList<Packet>(numPackets)
    for (i in 0 until numPackets) {
        val skipEntries = data.getByte()
        val numColors = data.getByte()
        val adjustedNumColors: Int = if (numColors.toInt() == 0) 256 else numColors.toInt()
        val colors = ArrayList<OldColor>(adjustedNumColors)
        for (j in 0 until adjustedNumColors) {
            colors.add(
                j,
                OldColor(red = data.getByte(), green = data.getByte(), blue = data.getByte())
            )
        }
        packets.add(i, Packet(skipEntries, colors))
    }
    return OldPaletteChunk(index, size, packets)
}

fun getPaletteChunk(index: Int, size: aseDword, data: Data): PaletteChunk {
    val newPaletteSize = data.getDword()
    val firstIndex = data.getDword()
    val lastIndex = data.getDword()
    data.skipBytes(8)
    val numColors = lastIndex.toInt() - firstIndex.toInt() + 1
    val colors = ArrayList<Color>(numColors)
    for (i in (0 until numColors)) {
        val flags = data.getWord()
        val hasName: aseWord = 1U
        val red = data.getByte()
        val green = data.getByte()
        val blue = data.getByte()
        val alpha = data.getByte()
        val name: String? = if (flags == hasName) {
            data.getString()
        } else null
        colors.add(i, Color(red, green, blue, alpha, name))
    }
    return PaletteChunk(size, index, newPaletteSize, firstIndex, lastIndex, colors)
}

// -- User Data ---

data class UserDataChunk(
    override val size: aseDword,
    override val index: Int,
    val text: String?,
    val color: Color?
) : Chunk()


fun getUserDataChunk(index: Int, size: aseDword, data: Data): UserDataChunk {
    var text: String? = null
    var color: Color? = null
    val flags = data.getDword()
    if (flags.and(1U) == 1U) {
        text = data.getString()
    }
    if (flags.and(2U) == 2U) {
        color = Color(
            red = data.getByte(),
            green = data.getByte(),
            blue = data.getByte(),
            alpha = data.getByte(),
            name = null
        )

    }
    return UserDataChunk(size, index, text, color)
}

// -- Slice ---

data class Pivot(val xPosition: aseLong, val yPosition: aseLong)
data class Center(
    val xPosition: aseLong,
    val yPosition: aseLong,
    val width: aseDword,
    val height: aseDword
)

data class SliceKey(
    val frameNumber: aseDword,
    val xOrigin: aseLong,
    val yOrigin: aseLong,
    val width: aseDword,
    val height: aseDword,
    val center: Center?,
    val pivot: Pivot?
)

data class SliceChunk(
    override val size: aseDword,
    override val index: Int,
    val name: String,
    val sliceKeys: List<SliceKey>
) : Chunk()

fun getSliceChunk(index: Int, size: aseDword, data: Data): SliceChunk {
    val numSliceKeys = data.getDword()
    val flags = data.getDword()
    data.skipDword()
    val name = data.getString()
    val sliceKeys = ArrayList<SliceKey>(numSliceKeys.toInt())
    for (i in 0 until numSliceKeys.toInt()) {
        val frameNumber = data.getDword()
        val xOrigin = data.getLong()
        val yOrigin = data.getLong()
        val width = data.getDword()
        val height = data.getDword()
        var center: Center? = null
        var pivot: Pivot? = null
        if (flags.and(1U) == 1U) {
            center = Center(
                xPosition = data.getLong(),
                yPosition = data.getLong(),
                width = data.getDword(),
                height = data.getDword()
            )
        }
        if (flags.and(2U) == 2U) {
            pivot = Pivot(xPosition = data.getLong(), yPosition = data.getLong())
        }
        sliceKeys.add(i, SliceKey(frameNumber, xOrigin, yOrigin, width, height, center, pivot))
    }
    return SliceChunk(size, index, name, sliceKeys)
}

// -- Layer ---

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

data class LayerChunk(
    override val index: Int,
    override val size: aseDword,
    val layerFlags: aseWord,
    private val layerTypeValue: aseWord,
    val childLevel: aseWord,
    val defaultLayerWidth: aseWord,
    val defaultLayerHeight: aseWord,
    private val blendModeValue: aseWord,
    val opacity: aseByte,
    val layerName: String
) : Chunk() {

    val layerType: LayerType get() = LayerType.fromWord(layerTypeValue)
    val blendMode: BlendMode get() = BlendMode.fromWord(blendModeValue)
    fun isFlagSet(flag: LayerFlags): Boolean {
        return (layerFlags.or(flag.value)) == flag.value
    }
}

fun getLayerChunk(index: Int, size: aseDword, data: Data): LayerChunk {
    val layerFlags = data.getWord()
    val layerTypeValue = data.getWord()
    val childLevel = data.getWord()
    val defaultLayerWidth = data.getWord()
    val defaultLayerHeight = data.getWord()
    val blendModeValue = data.getWord()
    val opacity = data.getByte()
    data.skipBytes(3)
    val layerName = data.getString()

    return LayerChunk(
        index,
        size,
        layerFlags,
        layerTypeValue,
        childLevel,
        defaultLayerWidth,
        defaultLayerHeight,
        blendModeValue,
        opacity,
        layerName
    )
}

// -- Cels ---

sealed class CelChunk : Chunk() {
    abstract val layerIndex: aseWord
    abstract val xPosition: aseShort
    abstract val yPosition: aseShort
    abstract val opacityLevel: aseByte
}

data class LinkedCelChunk(
    override val size: aseDword,
    override val index: Int,
    override val layerIndex: aseWord,
    override val xPosition: aseShort,
    override val yPosition: aseShort,
    override val opacityLevel: aseByte,
    val linkedFramePosition: aseWord,
) : CelChunk()

data class RawRgbaCelChunk(
    override val size: aseDword,
    override val index: Int,
    override val layerIndex: aseWord,
    override val xPosition: aseShort,
    override val yPosition: aseShort,
    override val opacityLevel: aseByte,
    val width: aseWord,
    val height: aseWord,
    val pixels: Array<RGBAPixel>
) : CelChunk() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawRgbaCelChunk

        if (layerIndex != other.layerIndex) return false
        if (xPosition != other.xPosition) return false
        if (yPosition != other.yPosition) return false
        if (opacityLevel != other.opacityLevel) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerIndex.hashCode()
        result = 31 * result + xPosition
        result = 31 * result + yPosition
        result = 31 * result + opacityLevel.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + pixels.contentHashCode()
        return result
    }

}

data class RawGrayscaleCelChunk(
    override val size: aseDword,
    override val index: Int,
    override val layerIndex: aseWord,
    override val xPosition: aseShort,
    override val yPosition: aseShort,
    override val opacityLevel: aseByte,
    val width: aseWord,
    val height: aseWord,
    val pixels: Array<GrayscalePixel>
) : CelChunk() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawGrayscaleCelChunk

        if (layerIndex != other.layerIndex) return false
        if (xPosition != other.xPosition) return false
        if (yPosition != other.yPosition) return false
        if (opacityLevel != other.opacityLevel) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layerIndex.hashCode()
        result = 31 * result + xPosition
        result = 31 * result + yPosition
        result = 31 * result + opacityLevel.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + pixels.contentHashCode()
        return result
    }

}

data class CompressedCelChunk(
    override val size: aseDword,
    override val index: Int,
    override val layerIndex: aseWord,
    override val xPosition: aseShort,
    override val yPosition: aseShort,
    override val opacityLevel: aseByte,
    val width: aseWord,
    val height: aseWord,
    val pixels: UByteArray
) : CelChunk()

data class RawIndexedCelChunk(
    override val size: aseDword,
    override val index: Int,
    override val layerIndex: aseWord,
    override val xPosition: aseShort,
    override val yPosition: aseShort,
    override val opacityLevel: aseByte,
    val width: aseWord,
    val height: aseWord,
    val pixels: UByteArray
) : CelChunk()

data class CelExtraChunk(
    override val size: aseDword,
    override val index: Int,
    val flags: aseDword,
    val preciseXPosition: Float,
    val preciseYPosition: Float,
    val celWidth: Float,
    val celHeight: Float
) : Chunk()

enum class CelType(val value: aseWord) {
    Raw(0U),
    Linked(1U),
    Compressed(2U);

    companion object {
        fun fromWord(value: aseWord) = values().first { it.value == value }
    }
}

fun getRawCel(
    size: aseDword,
    chunkIndex: Int,
    pixelType: PixelType,
    layerIndex: aseWord,
    xPosition: aseShort,
    yPosition: aseShort,
    opacityLevel: aseByte,
    data: Data
): CelChunk {
    return when (pixelType) {
        PixelType.RGBA -> {
            val width = data.getWord()
            val height = data.getWord()
            val pixels = Array((width.toInt() * height.toInt())) {
                RGBAPixel(
                    red = data.getByte(),
                    blue = data.getByte(),
                    green = data.getByte(),
                    alpha = data.getByte()
                )
            }
            RawRgbaCelChunk(
                size = size,
                index = chunkIndex,
                layerIndex = layerIndex,
                xPosition = xPosition,
                yPosition = yPosition,
                opacityLevel = opacityLevel,
                width = width,
                height = height,
                pixels = pixels
            )
        }
        PixelType.Grayscale -> {
            val width = data.getWord()
            val height = data.getWord()
            val pixels = Array((width.toInt() * height.toInt())) {
                GrayscalePixel(value = data.getByte(), alpha = data.getByte())
            }
            RawGrayscaleCelChunk(
                size = size,
                index = chunkIndex,
                layerIndex = layerIndex,
                xPosition = xPosition,
                yPosition = yPosition,
                opacityLevel = opacityLevel,
                width = width,
                height = height,
                pixels = pixels
            )
        }
        PixelType.Indexed -> {
            val width = data.getWord()
            val height = data.getWord()
            val pixels = data.getBytes((width.toInt() * height.toInt()))
            RawIndexedCelChunk(
                size = size,
                index = chunkIndex,
                layerIndex = layerIndex,
                xPosition = xPosition,
                yPosition = yPosition,
                opacityLevel = opacityLevel,
                width = width,
                height = height,
                pixels = pixels
            )
        }
    }
}

fun getLinkedCelChunk(
    index: Int,
    size: aseDword,
    layerIndex: aseWord,
    xPosition: aseShort,
    yPosition: aseShort,
    opacityLevel: aseByte,
    data: Data
): LinkedCelChunk =
    LinkedCelChunk(
        size,
        index,
        layerIndex,
        xPosition,
        yPosition,
        opacityLevel,
        linkedFramePosition = data.getWord()
    )

fun getCompressedCelChunk(
    index: Int,
    size: aseDword,
    layerIndex: aseWord, // 2 bytes
    xPosition: aseShort, // 2 bytes
    yPosition: aseShort, // 2 bytes
    opacityLevel: aseByte, // 1 byte
    // also, 2 + 7 bytes (cel type & skipped data) + 6 bytes chunk header
    data: Data
): CompressedCelChunk {
    val width = data.getWord() // 2 bytes
    val height = data.getWord() // 2 bytes  -> offset = 26 bytes
    val offset = 26U
    val pixels = data.getBytes((size - offset).toInt()) // Read to end of chunk
    return CompressedCelChunk(
        size,
        index,
        layerIndex,
        xPosition,
        yPosition,
        opacityLevel,
        width,
        height,
        pixels
    )
}

fun getCelChunk(index: Int, size: aseDword, pixelType: PixelType, data: Data): CelChunk {
    val layerIndex: aseWord = data.getWord()
    val xPosition: aseShort = data.getShort()
    val yPosition: aseShort = data.getShort()
    val opacityLevel: aseByte = data.getByte()
    val celTypeValue: aseWord = data.getWord()
    data.skipBytes(7)
    return when (CelType.fromWord(celTypeValue)) {
        CelType.Raw -> getRawCel(
            size,
            index,
            pixelType,
            layerIndex,
            xPosition,
            yPosition,
            opacityLevel,
            data
        )
        CelType.Linked -> getLinkedCelChunk(
            index,
            size,
            layerIndex,
            xPosition,
            yPosition,
            opacityLevel,
            data
        )
        CelType.Compressed -> getCompressedCelChunk(
            index,
            size,
            layerIndex,
            xPosition,
            yPosition,
            opacityLevel,
            data
        )
    }
}

fun getCelExtraChunk(index: Int, size: aseDword, data: Data): Chunk {
    val flags = data.getDword()
    val preciseXPosition = data.getFixed()
    val preciseYPosition = data.getFixed()
    val celWidth = data.getFixed()
    val celHeight = data.getFixed()
    data.skipBytes(16)
    return CelExtraChunk(
        size,
        index,
        flags,
        preciseXPosition,
        preciseYPosition,
        celWidth,
        celHeight
    )
}

// -- Color profile --

data class ColorProfileChunk(
    override val size: aseDword,
    override val index: Int,
    val typeValue: aseWord,
    val flags: aseWord,
    val gamma: Float,
    val iccProfileData: aseByteArray?

) : Chunk()

enum class ColorProfileType(val value: aseWord) {
    NoProfile(0U),
    sRGB(1U),
    embeddedICC(2U);

    companion object {
        fun fromWord(value: aseWord) = values().first { it.value == value }
    }
}

val ColorProfileChunk.type: ColorProfileType get() = ColorProfileType.fromWord(typeValue)

fun getColorProfileChunk(index: Int, size: aseDword, data: Data): ColorProfileChunk {
    val typeValue = data.getWord()
    val flags = data.getWord()
    val gamma = data.getFixed()
    var iccProfileData: aseByteArray? = null
    data.skipBytes(8)
    if (ColorProfileType.fromWord(typeValue) == ColorProfileType.embeddedICC) {
        val length = data.getDword()
        iccProfileData = data.getBytes(length.toInt())
    }
    return ColorProfileChunk(size, index, typeValue, flags, gamma, iccProfileData)
}

// -- Mask Chunk (DEPRECATED) ---

@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Mask Chunk is deprecated according to ase-file-specs.md"
)
data class MaskChunk(
    override val size: aseDword,
    override val index: Int,
    val xPosition: aseShort,
    val yPosition: aseShort,
    val width: aseWord,
    val height: aseWord,
    val name: String,
    val bitMap: aseByteArray
) : Chunk()

fun getMaskChunk(index: Int, size: aseDword, data: Data): MaskChunk {
    val xPosition = data.getShort()
    val yPosition = data.getShort()
    val width = data.getWord()
    val height = data.getWord()
    val name = data.getString()
    val bitMapLength = height.toInt() * ((width.toInt() + 7) / 8)
    val bitMap = data.getBytes(bitMapLength)
    return MaskChunk(size, index, xPosition, yPosition, width, height, name, bitMap)
}

// -- Path Chunk (Not used) ---

data class PatchChunk(override val size: aseDword, override val index: Int) : Chunk()

fun getPathChunk(index: Int, size: aseDword) = PatchChunk(size, index)

// -- Tags Chunk ---

data class TagsChunk(
    override val size: aseDword,
    override val index: Int,
    val tags: List<Tag>
) : Chunk()

data class Tag(
    val fromFrame: aseWord,
    val toFrame: aseWord,
    val loopAnimationDirectionValue: aseByte,
    val tagColor: TagColor,
    val tagName: String
)

val Tag.loopAnimationDirection: LoopAnimationDirection
    get() = LoopAnimationDirection.fromByte(
        loopAnimationDirectionValue
    )

data class TagColor(val red: aseByte, val green: aseByte, val blue: aseByte)
enum class LoopAnimationDirection(val value: aseByte) {
    Forward(0U),
    Reverse(1U),
    PingPong(2U);

    companion object {
        fun fromByte(value: aseByte) = values().first { it.value == value }
    }
}

fun getTagsChunk(index: Int, size: aseDword, data: Data): TagsChunk {
    val numTags = data.getWord()
    val tags = ArrayList<Tag>(numTags.toInt())
    data.skipBytes(8)
    for (i in (0 until numTags.toInt())) {
        val fromFrame = data.getWord()
        val toFrame = data.getWord()
        val loopAnimationDirectionValue = data.getByte()
        data.skipBytes(8)
        val tagColor = TagColor(red = data.getByte(), green = data.getByte(), blue = data.getByte())
        data.skipByte()
        val tagName = data.getString()
        tags.add(i, Tag(fromFrame, toFrame, loopAnimationDirectionValue, tagColor, tagName))
    }
    return TagsChunk(size, index, tags)
}

// -- Util ---

fun CompressedCelChunk.getInflatedPixels(): UByteArray {
    val inflater = Inflater()
    inflater.setInput(pixels.asByteArray())
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (!inflater.finished()) {
        val count = inflater.inflate(buffer)
        outputStream.write(buffer, 0, count)
    }
    outputStream.close()
    return outputStream.toByteArray().toUByteArray()
}