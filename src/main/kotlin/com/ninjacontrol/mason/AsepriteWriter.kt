@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ninjacontrol.mason

import java.io.File

class AsepriteWriter {

    fun write(file: File, asepriteFile: AsepriteFile) {
        val header = asepriteFile.header
        val writer = DataWriter(asepriteFile.header.size.toInt())
        header.write(writer)
        asepriteFile.frames.forEach {
            it.write(writer)
        }

        file.writeBytes(writer.buffer)
    }

}
