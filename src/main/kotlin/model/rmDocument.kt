package model

import Armapi
import Utils
import http.Net
import http.Net.Companion.DOWNLOAD
import http.Net.Companion.JSON
import http.Net.Companion.UPLOAD
import http.Net.Companion.sendRequest
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.UnsupportedOperationException
import java.util.*

class rmDocument(
    val id: String // gcs id
    // val uuid
) {

    var indexFileContent: String? = null

    var metadata: DocumentMetadata? = null
    var content: DocumentContent? = null
    var pagedata: DocumentPagedata? = null
    var pdf: String? = null

    constructor(): this(UUID.randomUUID().toString()) // Not yet online

    constructor(id: String, subFiles: Int = -1) : this(id) {
        // From root file entry
    }

    fun getSubFiles(userToken: String){
        val indexFileResponse = Net.post(DOWNLOAD, userToken, DownloadRequest(Net.GET, id))
        val indexFileUrl = DownloadResponse.of(indexFileResponse).url

        indexFileContent = Net.get(indexFileUrl, userToken).body!!.string()
        println(indexFileContent)
        // Get download urls of all files

        // IDs of content, metadata, pagedata, pdf of the file
        val idList = indexFileContent!!.split("\n").subList(1, -1)
        print(idList)

        for (entry in idList){
            // downloadIndexFileEntry(entry, userToken)
        }

    }

    fun downloadIndexFileEntry(entry: String, userToken: String){
        // Example entry
        // {gcsId}:0:{uuid}.{fileEnding}:0:{size}
        // b81a7ad514253396d7f36361eb8073317f4f7939b6559fd2d92b5c910ea639ad:0:ceacda6b-424e-4c0e-afeb-ee0a3e8cb661.content:0:735
        val entryParts = entry.split(":")
        val fileEnding = entryParts[2].split(".")[1]

        val gcsId = entryParts[0]
        val uuid = entryParts[2].split(".")[0]
        val size = entryParts[4]

        when(fileEnding){
            "content" -> content = DocumentContent.of(gcsId, userToken)
            "metadata" -> metadata = DocumentMetadata.of(gcsId, userToken)
            "pagedata" -> pagedata = DocumentPagedata.of(gcsId, userToken)
            "pdf" -> {
                val pdfDownloadUrl = DownloadResponse.of(gcsId, userToken).url
                pdf = Net.get(pdfDownloadUrl, userToken).body!!.string()
            }
            else -> throw UnsupportedOperationException("$fileEnding is not yet supported")
        }
        // Additional file endings in document index file:
        // /$uuid-metadata.json
        // /$uuid.rm
    }

    fun upload(jrmapi: Armapi, parentId: String){
        val userToken = jrmapi.userToken
        // TODO Check if everything is set and not null

        val contentPath = Utils.sha265(content!!.toJson())
        val metadataPath = Utils.sha265(metadata!!.toJson())
        val pagedataPath = Utils.sha265(pagedata!!.toJson())
        val pdfPath = Utils.sha265(pdf!!)

        val pagedataJson = pagedata!!.toJson()
        val pdfDataJson = pdf!!

        println("Lengths: ${content!!.jsonLength}, ${metadata!!.jsonLength}, ${pagedataJson.length}, ${pdfDataJson.length}")

        val responseContent = content!!.upload(contentPath, id, userToken)
        val contentLength = responseContent.header("x-goog-stored-content-length")!!

        val responseMetadata = content!!.upload(metadataPath, id, userToken)
        val metadataLength = responseMetadata.header("x-goog-stored-content-length")!!

        val responsePagedata = content!!.upload(pagedataPath, id, userToken)
        val pagedataLength = responseMetadata.header("x-goog-stored-content-length")!!

        val uploadPdfResponse = UploadRequest("$id.pdf", pdfDataJson.length, Net.PUT, pdfPath).upload(userToken)
        val uploadUrlPdf = uploadPdfResponse.url
        val pdfLength = Net.put(uploadUrlPdf, userToken, pdf!!).header("x-goog-stored-content-length")!!

        println("Google Lengths: $contentLength, $metadataLength, $pagedataLength, $pdfLength")

        // Upload index file
        val newUUID = UUID.randomUUID()

        val indexFile: String =
            "3\n" +
                    "$contentPath:0:$id.content:$newUUID:${contentLength}\n" +
                    "$metadataPath:0:$id.metadata:$newUUID:${metadataLength}\n" +
                    "$pagedataPath:0:$id.pagedata:$newUUID:${pagedataLength}\n" +
                    "$pdfPath:0:$id.pdf:$newUUID:${pdfLength}\n"

        val indexFilePath = Utils.sha265(indexFile)
        val uploadIndexFileResponse = Net.post(Net.UPLOAD, userToken, UploadIndexFileRequest(Net.PUT, indexFilePath))
        val uploadUrlIndexFile = UploadResponse.of(uploadIndexFileResponse).url



        Net.put(uploadUrlIndexFile, userToken, indexFile)

        // Update root file
        // Get root file
        val rootFileResponse = jrmapi.getRootFileResponse(jrmapi.getRootDirectoryId())
        val sum: Int = contentLength.toInt() + metadataLength.toInt() + pagedataLength.toInt() + pdfLength.toInt()
        val rootFile = rootFileResponse.body!!.toString().trimEnd() + "\n" + "$indexFilePath:80000000:$newUUID:4:$sum\n"
        val rootFileGeneration = rootFileResponse.header("x-goog-generation")!!

        // Upload new root file
        val rootPath = Utils.sha265(rootFile)
        val uploadRootFileResponse = Net.post(Net.UPLOAD, userToken, UploadIndexFileRequest(Net.PUT, rootPath))
        val uploadUrlRootFile = UploadResponse.of(uploadRootFileResponse).url
        val generation = Net.put(uploadUrlRootFile, userToken, rootFile).header("x-goog-generation")!!

        // Uploading root file index
        println("rootPath: $rootPath")
        val uploadRootFileIndexRequestBody = UploadRootFileIndex(generation.toLong(), Net.PUT, "root", rootPath).toRequestBody()
        val uploadRootFileRequest: Request = Request.Builder()
            .url(UPLOAD)
            .post(uploadRootFileIndexRequestBody)
            .header("Authorization", "Bearer $userToken")
            .build()
        val uploadRootFileRequestResponse = Net.sendRequest(uploadRootFileRequest)
        println(uploadRootFileRequestResponse.headers)
        // println(uploadRootFileRequestResponse.body!!.string())

        val uploadUrlRootFileIndex = UploadResponse.of(uploadRootFileRequestResponse).url
        val requestRootFileIndex: Request = Request.Builder()
            .url(uploadUrlRootFileIndex)
            .put(rootPath.toRequestBody())
            .header("x-goog-if-generation-match", rootFileGeneration)
            .header("x-goog-content-length-range", "0,7000000000")
            .build()
        Net.sendRequest(requestRootFileIndex)

        println("sync")
        // Sync complete
        val request: Request = Request.Builder().url(Net.INTERNAL_CLOUD + "/sync/v2/sync-complete")
            .post("{\"generation\": ${generation.toLong()}}".toRequestBody(Net.JSON))
            .header("Authorization", "Bearer $userToken")
            .header("Content-Type", "application/json")
            .build()
        println(Net.client.newCall(request).execute().code)

    }

    companion object {
        fun of(rootFileEntry: String): rmDocument {
            // {GCS path identifier for index file}:8000000:{document/folder uuid on device}:{number of entries in index file}:0
            val values = rootFileEntry.split(":")

            return rmDocument(values[0], values[3].toInt())
        }
    }

}