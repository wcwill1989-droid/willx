package com.willx.ai

import android.content.ContentResolver
import android.net.Uri
import java.nio.charset.Charset

object ContentResolverUtils {
    fun readText(
        resolver: ContentResolver,
        uri: Uri,
        maxBytes: Int = 200_000,
        charset: Charset = Charsets.UTF_8,
    ): String {
        resolver.openInputStream(uri).use { input ->
            if (input == null) return ""
            val buf = ByteArray(maxBytes)
            val read = input.read(buf)
            if (read <= 0) return ""
            return buf.copyOf(read).toString(charset)
        }
    }
}
