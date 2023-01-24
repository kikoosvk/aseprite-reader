package com.ninjacontrol.mason

import java.io.File

class AsepriteWriter {

    fun write(file: File, asepriteFile: AsepriteFile) {
        val bytes = ByteArray(99999)
        val header = asepriteFile.header

        file.writeBytes(bytes)
    }

}
