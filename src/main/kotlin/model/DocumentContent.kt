package model

import http.Net
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

/*
{
    "coverPageNumber": 0,
    "documentMetadata": {
        "title": "v52.dvi"
    },
    "dummyDocument": false,
    "extraMetadata": {
    },
    "fileType": "pdf",
    "fontName": "",
    "formatVersion": 1,
    "lineHeight" -1,
    "margins": 125,
    "orientation": "portrait",
    "originalPageCount": 8,
    "pageCount": 8,
    "pageTags": [
    ],
    "pages": [
        "id",
        "id",
        "id"
    ],
    "redirectionPageMap": [
        0,
        1
    ],
    "sizeInBytes": "131193",
    "tags": [
    ],
    "textAlignment": "justify",
    "textScale": 1
}
 */
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import okhttp3.Response

/*
{
    "coverPageNumber": 0,
    "documentMetadata": {
        "title": "v52.dvi"
    },
    "dummyDocument": false,
    "extraMetadata": {

    },
    "fileType": "pdf",
    "fontName": "",
    "formatVersion": 1,
    "lineHeight": -1,
    "margins": 125,
    "orientation": "portrait",
    "originalPageCount": 8,
    "pageCount": 8,
    "pageTags": [
    ],
    "pages": [
        "f473d391-7c8d-4e9d-b97b-f31ae4b26bb5",
        "601f0c2e-6276-4349-816d-b8437821d6de",
        "c59ce878-313e-4a70-b5ab-59b1e283fe44",
        "e7b7a664-3cf4-476d-a09e-21a63a2d1a0f",
        "fb710e1c-2d64-430a-84d4-3b62e11cdc4b",
        "325d8bf2-49a1-4761-86fb-82e82ebab290",
        "e83a2202-4741-4167-b8c4-05c182fb5d62",
        "1b581746-9f34-4007-b13f-0725cbdc6bd1"
    ],
    "redirectionPageMap": [
        0,
        1,
        2,
        3,
        4,
        5,
        6,
        7
    ],
    "sizeInBytes": "131194",
    "tags": [
    ],
    "textAlignment": "justify",
    "textScale": 1
}
 */

@Serializable
data class DocumentContent (
    val coverPageNumber: Int,
    val documentMetadata: DocumentMetadataForContent,
    val dummyDocument: Boolean,
    val extraMetadata: ExtraMetadata,
    val fileType: String,
    val fontName: String,
    val formatVersion: Int,
    val lineHeight: Int,
    val margins: Int,
    val orientation: String,
    val originalPageCount: Int,
    val pageCount: Int,
    val pageTags: List<String> = listOf(),
    val pages: List<String>?,
    val redirectionPageMap: List<Int>? = null,
    val sizeInBytes: String,
    val tags: List<String> = listOf(),
    val textAlignment: String,
    val textScale: Int
): ApiRequest() {

    fun upload(gcsId: String, uuid: String, userToken: String): Response {
        val uploadResponse = UploadRequest("$uuid.content", this.jsonLength, Net.PUT, gcsId).upload(userToken)
        val uploadUrl = uploadResponse.url
        return Net.put(uploadUrl, userToken, this)
    }

    companion object {
        private val jsonParser = Json { ignoreUnknownKeys = true }

        fun of(gcsId: String, userToken: String): DocumentContent{
            val downloadUrl = DownloadResponse.of(gcsId, userToken).url
            val json = Net.get(downloadUrl, userToken).body!!.string()
            return jsonParser.decodeFromString(json)
        }

        fun fromPdfFile(file: File): DocumentContent {
            val document: PDDocument = PDDocument.load(file)
            val info = document.documentInformation
            // TODO

            val documentContent = DocumentContent(
                coverPageNumber = 0,
                documentMetadata = DocumentMetadataForContent(file.name),
                dummyDocument = false,
                extraMetadata = ExtraMetadata(),
                fileType = "pdf",
                fontName = "",
                formatVersion = 1,
                lineHeight = -1,
                margins = 125,
                orientation = "portrait",
                originalPageCount = document.numberOfPages,
                pageCount = document.numberOfPages,
                pageTags = listOf(),
                pages = listOf(),
                redirectionPageMap = IntArray(document.numberOfPages) { it }.toList(),
                sizeInBytes = file.length().toString(),
                tags =  listOf(),
                textAlignment = "justify",
                textScale = 1
            )
            document.close()
            return documentContent
        }
    }
}

@Serializable
class DocumentMetadataForContent(val title: String = "Default")

@Serializable
class ExtraMetadata(
    val LastBallpointColor: String = "Black",
    val LastBallpointSize: Int = 1,
    val LastBallpointv2Color: String = "Black",
    val LastBallpointv2Size: Int = 1,
    val LastCalligraphyColor: String = "Black",
    val LastCalligraphySize: Int = 1,
    val LastClearPageColor: String = "Black",
    val LastClearPageSize: Int = 1,
    val LastEraseSectionColor: String = "Black",
    val LastEraseSectionSize: Int = 1,
    val LastEraserColor: String = "Black",
    val LastEraserSize: Int = 1,
    val LastEraserTool: String = "Eraser",
    val LastFinelinerColor: String = "Black",
    val LastFinelinerSize: Int = 1,
    val LastFinelinerv2Color: String = "Black",
    val LastFinelinerv2Size: Int = 1,
    val LastHighlighterColor: String = "Black",
    val LastHighlighterSize: Int = 1,
    val LastHighlighterv2Color: String = "HighlighterYellow",
    val LastHighlighterv2Size: Int = 1,
    val LastMarkerColor: String = "Black",
    val LastMarkerSize: Int = 2,
    val LastMarkerv2Color: String = "Black",
    val LastMarkerv2Size: Int = 1,
    val LastPaintbrushColor: String = "Black",
    val LastPaintbrushSize: Int = 2,
    val LastPaintbrushv2Color: String = "Black",
    val LastPaintbrushv2Size: Int = 2,
    val LastPen: String = "Pencilv2",
    val LastPencilColor: String = "Black",
    val LastPencilSize: Int = 2,
    val LastPencilv2Color: String = "Black",
    val LastPencilv2Size: Int = 2,
    val LastReservedPenColor: String = "Black",
    val LastReservedPenSize: Int = 2,
    val LastSelectionToolColor: String = "Black",
    val LastSelectionToolSize: Int = 2,
    val LastSharpPencilColor: String = "Black",
    val LastSharpPencilSize: Int = 2,
    val LastSharpPencilv2Color: String = "Black",
    val LastSharpPencilv2Size: Int = 2,
    val LastSolidPenColor: String = "Black",
    val LastSolidPenSize: Int = 2,
    val LastTool: String = "Pencilv2",
    val LastUndefinedColor: String = "Black",
    val LastUndefinedSize: Int = 2,
    val LastZoomToolColor: String = "Black",
    val LastZoomToolSize: Int = 1
)

