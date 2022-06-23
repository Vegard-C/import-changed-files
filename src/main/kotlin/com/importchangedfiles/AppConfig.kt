package com.importchangedfiles

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Configuration
class AppConfig {
    @Bean
    fun importSources(@Value("\${importchangedfiles.importdir}") importlist: String) =
        ImportSources { importlist.split(",").map { File(it) } }
    @Bean
    fun destination(@Value("\${importchangedfiles.destination}") dest: String) =
        Destination { File(dest) }
    @Bean
    fun memory(@Value("\${importchangedfiles.remember}") memory: String,
               @Value("\${importchangedfiles.maxrememberdays:365}") maxRememberDays: Long) =
        FileMemory(File(memory), maxRememberDays)
}

class FileMemory(private val file: File, private val maxRememberDays: Long) : Memory {
    private val bufferSize = 1024 * 8
    private val known: MutableMap<String, MemoryItem> = mutableMapOf()
    private val objectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())
    init {
        val lastTsRemember = Instant.now().minus(maxRememberDays, ChronoUnit.DAYS)
        if (file.exists()) {
            file.readLines().forEach {
                if (it.isNotEmpty()) {
                    val i: MemoryItem = objectMapper.readValue(it)
                    if (i.rememberTs.isAfter(lastTsRemember)) {
                        known[key(i)] = i
                    }
                }
            }
        }
    }
    override fun add(f: File, createDate: Instant): Boolean =
        item(f, createDate)
            .let { known.put(key(it), it) == null }

    override fun store() {
        val newFile = File(file.parent, file.name + ".tmp")
        newFile.appendText(known.values
            .map { objectMapper.writeValueAsString(it) }
            .joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        newFile.copyTo(file, overwrite = true)
        newFile.delete()
    }

    private fun item(f: File, createDate: Instant): MemoryItem =
        MemoryItem(
            originalFileName = f.name,
            createTs = createDate,
            rememberTs = Instant.now(),
            sha = sha(f),
            length = f.length(),
        )

    private fun key(i: MemoryItem): String =
        "${i.sha}|${i.originalFileName}|${i.length}"

    private fun sha(f: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buffer = ByteArray(bufferSize)
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                digest.update(buffer, 0, bytes)
                bytes = input.read(buffer)
            }
        }
        return HexFormat.of().formatHex(digest.digest())
    }
    private data class MemoryItem (
        val originalFileName: String,
        val createTs: Instant,
        val rememberTs: Instant,
        val sha: String,
        val length: Long,
    )
}

fun interface ImportSources {
    fun sources() : List<File>
}

fun interface Destination {
    fun dir() : File
}

interface Memory {
    /**
     *  if it was not known
     */
    fun add(f: File, createDate: Instant): Boolean
    fun store()
}

@Component
class TestRun (private val service: Service) {

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        service.run()
    }
}