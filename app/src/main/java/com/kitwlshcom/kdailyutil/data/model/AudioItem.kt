package com.kitwlshcom.kdailyutil.data.model

import java.io.File

data class AudioItem(
    val name: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long
) {
    val file: File get() = File(path)
}
