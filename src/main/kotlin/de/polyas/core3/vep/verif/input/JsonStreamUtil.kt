/*
 * Copyright 2026 Polyas GmbH
 *
 * Licensed under GNU Affero General Public License v3.0; you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at https://www.gnu.org/licenses/agpl-3.0.en.html.
 * This software is distributed WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 */

package de.polyas.core3.vep.verif.input

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.MappingJsonFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.polyas.core3.vep.verif.input.Input.WithPayload
import java.io.File
import java.nio.file.Paths

/**
 * A utility for reading elements from json files representing content of bulletin boards
 */
internal class JsonFileReader(private val baseDirectory: File) {

    /**
     * Returns a sequence of elements read from the json file representing the content of a bulletin board.
     */
    inline fun<reified T> sequence(filename: String): Sequence<T> {
        val file = Paths.get(baseDirectory.absolutePath, filename).toFile()
        return strSequenceFromJsonFile(file).map { streamUtilObjectMapper.readValue<T>(it) }
    }

    /**
     * Returns a sequence of elements read the json file representing the content of a bulletin board,
     * of `null` is the file does not exist.
     */
    inline fun<reified T> sequenceOrNull(filename: String): Sequence<T>? =
        if (fileExists(filename)) sequence(filename)
        else null

    /**
     * Returns the only element read from the json file representing the content of a bulletin board.
     * Throws there is no or more than one element in this file.
     */
    inline fun <reified T> singleton(filename: String): T {
        val l = sequence<WithPayload<T>>(filename).toList()
        check(l.size == 1) { "File $filename should have exactly one entry; it has ${l.size}" }
        return l[0].payload
    }

    private fun fileExists(filename: String): Boolean =
        baseDirectory.resolve(filename).exists()

    companion object {
        private val streamUtilObjectMapper = jacksonObjectMapper()

        fun strSequenceFromJsonFile(jsonFile: File): Sequence<String> = sequence {
            val jp = createJsonParser(jsonFile)
            val current = jp.nextToken()
            require(current == JsonToken.START_ARRAY) { jsonFile.path + " corrupted: expected [ at the beginning." }
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                val v: ImportRecord = jp.readValueAs(ImportRecord::class.java)
                if (v.r == ImportRecord.ExportRecordType.S) break
                yield(v.c!!)
            }
            check(jp.nextToken() == JsonToken.END_ARRAY) {
                jsonFile.path + " corrupted: After import of all hash regular chain records there should be the sealing entry."
            }
            check(jp.nextToken() == null) {
                jsonFile.path + " corrupted: After the sealing entry there are still tokens to parse."
            }
        }

        private fun createJsonParser(jsonFile: File) : JsonParser =
            MappingJsonFactory().createParser(jsonFile)
    }
}

private class ImportRecord {
    var h: ExportHashChainItem? = null
    var r: ExportRecordType? = null
    var s: ByteArray? = null
    var c: String? = null

    override fun toString(): String = "{h=$h, r=$r, s=$s, c=$c}"

    enum class ExportRecordType { R, S }

    class ExportHashChainItem {
        var n: Long? = null
        var p: ByteArray? = null
        var c: ByteArray? = null

        override fun toString(): String = "{$n, $p, $c}"
    }
}

