package com.android.example.mathalarm.utils

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

fun copyStream(`in`: InputStream, out: OutputStream) {
    try {
        var bytesRead: Int
        val buffer = ByteArray(4096)
        while (`in`.read(buffer).also { bytesRead = it } != -1) {
            out.write(buffer, 0, bytesRead)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            `in`.close()
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}