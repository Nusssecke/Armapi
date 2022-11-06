package model

import okhttp3.Response

class RootFile(response: Response) {
    val generation: Long
    val string: String

    // TODO Add constructor from string
    // TODO Add static method that returns a new rootFile with an added line or smth.

    init {
        generation = response.header("x-goog-generation")!!.toLong()
        string = response.body!!.string()
    }

    fun toDocuments(): List<RMDocument> {
        val rootFileEntries = string.removePrefix("3\n").trimEnd() // Clean up leading 3 and ending linebreak
        // Root file lines corresponds to documents index files
        // Line: {GCS path identifier for index file}:8000000:{document/folder uuid on device}:{number of entries in index file}:0
        return rootFileEntries.lines().map { RMDocument.of(it) }
    }

}