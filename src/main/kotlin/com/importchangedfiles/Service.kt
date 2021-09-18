package com.importchangedfiles

import org.springframework.stereotype.Service
import java.io.File

@Service
class Service(private val sources: ImportSources, private val destination: Destination, private val memory: Memory) {
    fun run() {
        var count = 0
        sources.sources().forEach { importDir ->
            println("Importing from $importDir")
            importDir.listFiles().forEach { file ->
                val new = memory.add(file)
                if (new) {
                    println("Copying ${file.name}")
                    val destFile = File(destination.dir(), file.name)
                    file.copyTo(destFile)
                    count++
                } else {
                    println("Ignoring already known file ${file.name}")
                }
            }
        }
        memory.close()
        println("$count files copied")
    }
}