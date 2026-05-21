package com.yanaa.app.ml

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

object FileUtil {
    fun loadMappedFile(context: Context, fileName: String): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(fileName)
        val inputStream = assetManager.open(fileName)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return ByteBuffer.wrap(bytes)
    }

    fun loadLabels(context: Context, fileName: String): List<String> {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val labels = reader.readLines()
        reader.close()
        return labels
    }
}
