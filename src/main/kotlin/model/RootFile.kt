package model

import okhttp3.Response

class RootFile(response: Response) {
    val generation: Long
    val body: String

    init {
        generation = response.header("x-goog-generation")!!.toLong()
        body = response.body!!.string()
    }

    fun toDocuments(): List<RMDocument> {
        val rootFileEntries = body.removePrefix("3\n").trimEnd() // Clean up leading 3 and ending linebreak
        // Root file lines corresponds to documents index files
        // Line: {GCS path identifier for index file}:8000000:{document/folder uuid on device}:{number of entries in index file}:0
        return rootFileEntries.lines().map { RMDocument.of(it) }
    }

}