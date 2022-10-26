package model

import http.Net
import kotlinx.serialization.Serializable
import okhttp3.Response
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

@Serializable
class DocumentPagedata(
    val lines: List<String>
): ApiRequest() {

    constructor(string: String): this(string.split("\n"))

    val jsonLength: Int
        get() {
            return this.toJson().length
        }

    fun upload(gcsId: String, uuid: String, userToken: String): Response {
        val uploadResponse = UploadRequest("$uuid.pagedata", this.jsonLength, Net.PUT, gcsId).upload(userToken)
        val uploadUrl = uploadResponse.url
        return Net.put(uploadUrl, userToken, this)
    }

    companion object {

        fun of(gcsId: String, userToken: String): DocumentPagedata {
            val downloadUrl = DownloadResponse.of(gcsId, userToken).url
            val pagedataString = Net.get(downloadUrl, userToken).body!!.string()
            return DocumentPagedata(pagedataString)
        }

        fun makeDefaultPageData(pdf: File): DocumentPagedata {
            val document: PDDocument = PDDocument.load(pdf)

            val lines = IntRange(1, document.numberOfPages).map { "Blank" }
            document.close()
            return DocumentPagedata(lines)
        }

        fun makeDefaultPageData(pages: Int): DocumentPagedata {
            val lines = IntRange(1, pages).map { "Blank" }
            return DocumentPagedata(lines)
        }
    }

}