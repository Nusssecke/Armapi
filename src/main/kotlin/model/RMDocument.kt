package model

import Armapi
import Utils
import http.*
import http.Net.Companion.DOWNLOAD
import java.lang.IndexOutOfBoundsException
import java.util.*

// Wrapper around an index File
class RMDocument(
    val gcsId: String, // gcs id
    val uuid: String
) {

    var indexFileContent: String? = null

    var metadata: DocumentMetadata? = null
    var content: DocumentContent? = null
    var pagedata: DocumentPagedata? = null
    var pdf: String? = null

    constructor(): this("", UUID.randomUUID().toString()) // Not yet online

    constructor(gcsId: String, uuid: String, subFiles: Int = -1) : this(gcsId, uuid) {
        // From root file entry
    }

    // Returns index file idList, to be passed to downloadIndexFileEntry
    fun loadIndexFile(userToken: String): List<String> {
        val indexFileResponse = Net.post(DOWNLOAD, userToken, DownloadRequest(Net.GET, gcsId))
        val indexFileUrl = DownloadResponse.of(indexFileResponse).url

        indexFileContent = Net.get(indexFileUrl, userToken).body!!.string()
        // println("Got file $gcsId, $uuid")
        // println(indexFileContent)
        // println(indexFileContent)
        // Get download urls of all files

        // IDs of content, metadata, pagedata, pdf of the file
        return indexFileContent!!.split("\n").subList(1, indexFileContent!!.split("\n").size-1)
        // print(idList)
    }

    fun downloadIndexFileEntries(entries: List<String>, userToken: String){
        for (entry in entries){
            downloadIndexFileEntry(entry, userToken)
        }
    }

    // Returns false if the entry was invalid, and true if everything was ok
    fun downloadIndexFileEntry(entry: String, userToken: String): Boolean {
        // Example entry
        // {gcsId}:0:{uuid}.{fileEnding}:0:{size}
        // b81a7ad514253396d7f36361eb8073317f4f7939b6559fd2d92b5c910ea639ad:0:ceacda6b-424e-4c0e-afeb-ee0a3e8cb661.content:0:735
        val entryParts = entry.split(":")
        var fileEnding: String = ""
        try {
            fileEnding = entryParts[2].split(".")[1]
        } catch (e: IndexOutOfBoundsException) {
            println("IndexFile has weird entries: $entry")
            println("Document: ${gcsId}: $uuid")
            return false
        }

        val gcsId = entryParts[0]
        // val uuid = entryParts[2].split(".")[0]
        // val size = entryParts[4]

        when(fileEnding){
            // "content" -> content = DocumentContent.of(gcsId, userToken)
            "metadata" -> metadata = DocumentMetadata.of(gcsId, userToken)
            /* "pagedata" -> pagedata = DocumentPagedata.of(gcsId, userToken)
            "pdf" -> {
                val pdfDownloadUrl = DownloadResponse.of(gcsId, userToken).url
                pdf = Net.get(pdfDownloadUrl, userToken).body!!.string()
            }
            "json" -> {
                // TODO
            }
            "rm" -> {
                // TODO
            }
            // TODO missing -metadata.json
            else -> throw UnsupportedOperationException("$fileEnding is not yet supported")*/
        }
        // Additional file endings in document index file:
        // /$uuid-metadata.json
        // /$uuid.rm
        return true
    }

    fun upload(jrmapi: Armapi, parentId: String){
        println("Document GCS Id: $gcsId")
        val userToken = jrmapi.userToken
        // TODO Check if everything is set and not null

        val contentGcsId = Utils.sha265(content!!.toJson())
        val metadataGcsId = Utils.sha265(metadata!!.toJson())
        val pagedataGcsId = Utils.sha265(pagedata!!.toJson())
        val pdfGcsId = Utils.sha265(pdf!!)

        val pagedataJson = pagedata!!.toJson()
        val pdfDataJson = pdf!!

        println("Lengths: ${content!!.jsonLength}, ${metadata!!.jsonLength}, ${pagedataJson.length}, ${pdfDataJson.length}")

        val responseContent = content!!.upload(contentGcsId, gcsId, userToken)
        val contentLength = responseContent.header("x-goog-stored-content-length")!!

        val responseMetadata = metadata!!.upload(metadataGcsId, gcsId, userToken)
        val metadataLength = responseMetadata.header("x-goog-stored-content-length")!!

        val responsePagedata = pagedata!!.upload(pagedataGcsId, gcsId, userToken)
        val pagedataLength = responsePagedata.header("x-goog-stored-content-length")!!

        val uploadPdfResponse = UploadRequest("$gcsId.pdf", pdfDataJson.length, Net.PUT, pdfGcsId).upload(userToken)
        val uploadUrlPdf = uploadPdfResponse.url
        val pdfLength = Net.put(uploadUrlPdf, userToken, pdf!!).header("x-goog-stored-content-length")!!

        println("Google Lengths: $contentLength, $metadataLength, $pagedataLength, $pdfLength")

        // Upload index file
        val newUUID = UUID.randomUUID()

        val indexFile: String =
            "3\n" +
                    "$contentGcsId:0:$gcsId.content:$newUUID:${contentLength}\n" +
                    "$metadataGcsId:0:$gcsId.metadata:$newUUID:${metadataLength}\n" +
                    "$pagedataGcsId:0:$gcsId.pagedata:$newUUID:${pagedataLength}\n" +
                    "$pdfGcsId:0:$gcsId.pdf:$newUUID:${pdfLength}\n"
        println(indexFile)
        val indexFileGcsId = Utils.sha265(indexFile)
        println("Index File GCS Id: $indexFileGcsId")
        val uploadIndexFileResponse = Net.post(Net.UPLOAD, userToken, UploadIndexFileRequest(Net.PUT, indexFileGcsId))
        val uploadUrlIndexFile = UploadResponse.of(uploadIndexFileResponse).url

        Net.put(uploadUrlIndexFile, userToken, indexFile, null)

        // Update root file
        // Get root file
        val rootFile = jrmapi.rootFile
        val sum: Int = contentLength.toInt() + metadataLength.toInt() + pagedataLength.toInt() + pdfLength.toInt()
        val rootFileString = rootFile.string.trimEnd() + "\n" + "$indexFileGcsId:80000000:$newUUID:4:$sum\n"
        val rootFileGeneration = rootFile.generation


        // Upload new root file
        val rootPath = Utils.sha265(rootFileString)
        val uploadRootFileResponse = Net.post(Net.UPLOAD, userToken, UploadIndexFileRequest(Net.PUT, rootPath))
        val uploadUrlRootFile = UploadResponse.of(uploadRootFileResponse).url
        Net.put(uploadUrlRootFile, userToken, rootFileString, null).header("x-goog-generation")!!

        val newGeneration = jrmapi.changeRootFileIndex(rootFileGeneration.toString(), rootPath)

        jrmapi.syncComplete(newGeneration)

    }

    companion object {
        fun of(rootFileEntry: String): RMDocument {
            // {GCS path identifier for index file}:8000000:{document/folder uuid on device}:{number of entries in index file}:0
            val values = rootFileEntry.split(":")

            return RMDocument(values[0], values[2], values[3].toInt())
        }
    }

}