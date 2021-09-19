package com.importchangedfiles

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


@Service
class Service(private val sources: ImportSources, private val destination: Destination, private val memory: Memory) {
    private val zoneId = ZoneId.of("Europe/Berlin")
    private val timeZone = TimeZone.getTimeZone(zoneId)

    fun run() {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        var count = 0
        sources.sources().forEach { importDir ->
            println("Importing from $importDir")
            importDir.listFiles()?.forEach { file ->
                val new = memory.add(file)
                if (new) {
                    val createDate = file.createDate()
                    println("Copying ${file.name} with CreateDate $createDate")

                    val year = "${now.year}"
                    val importTimestamp = now.format(formatter)
                    val fileTimestamp = createDate.format(formatter)

                    val destFile =
                        File(File(File(destination.dir(), year), importTimestamp), "${fileTimestamp}_${file.name}")
                    file.copyTo(destFile)
                    count++
                }
            }
        }
        memory.store()
        println("$count files copied")
    }

    private fun File.createDate(): LocalDateTime {
        try {
            val metadata = ImageMetadataReader.readMetadata(this)
            metadata.getDirectoriesOfType(ExifSubIFDDirectory::class.java).forEach {
                val date: Date? = it.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, timeZone)
                if (date != null) {
                    return date.toInstant().atZone(zoneId).toLocalDateTime()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Date(this.lastModified()).toInstant().atZone(zoneId).toLocalDateTime()
    }
}