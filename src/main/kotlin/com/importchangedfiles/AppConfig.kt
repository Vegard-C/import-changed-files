package com.importchangedfiles

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File

@Configuration
class AppConfig {
    @Bean
    fun importSources(@Value("\${importchangedfiles.importdir}") importlist: String) =
        ImportSources { importlist.split(",").map { File(it) } }
    @Bean
    fun destination(@Value("\${importchangedfiles.destination}") dest: String) =
        Destination { File(dest) }
    @Bean
    fun memory(@Value("\${importchangedfiles.remember}") memory: String) = FileMemory(File(memory))
}

class FileMemory(val file: File) : Memory {
    val knownSet = mutableSetOf<String>()
    val newLines = mutableListOf<String>()
    init {
        if (file.exists()) {
            knownSet.addAll(file.readLines())
        }
    }
    override fun add(f: File): Boolean {
        val k = key(f)
        val new = knownSet.add(k)
        if (new) newLines.add(k)
        return new
    }

    override fun close() {
        if (newLines.isNotEmpty()) {
            file.appendText(newLines.sorted().joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
        }
    }

    private fun key(f: File): String {
        val name = f.name
        val modified = f.lastModified()
        val length = f.length()
        return "$name | $modified | $length"
    }

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
    fun add(f: File): Boolean
    fun close()
}

@Component
class TestRun (private val service: Service) {

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        service.run()
    }
}