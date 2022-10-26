package model

import http.Net
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response

/*
 * {
 *      "deleted": false,
 *      "lastModified": "1666525041078",
 *      "lastOpened": "0",
 *      "lastOpenedPage": 0,
 *      "metadatamodified": true,
 *      "modified": true,
 *      "parent": "trash",
 *      "pinned": false,
 *      "synced": false,
 *      "type": "DocumentType",
 *      "version": 0,
 *      "visibleName": "v52"
 * }
 */

@Serializable
@SerialName("pdf")
class DocumentMetadata(
    val deleted: Boolean,
    val lastModified: String,
    val lastOpened: String,
    val lastOpenedPage: Int,
    val metadatamodified: Boolean,
    val modified: Boolean,
    val parent: String,
    val pinned: Boolean,
    val synced: Boolean,
    val _type: String = "unset",
    val version: Int,
    val visibleName: String
): ApiRequest() {

    val jsonLength: Int
        get() {
            return this.toJson().length
        }

    // TODO Put into super class or interface
    fun upload(gcsId: String, uuid: String, userToken: String): Response {
        val uploadResponse = UploadRequest("$uuid.metadata", this.jsonLength, Net.PUT, gcsId).upload(userToken)
        val uploadUrl = uploadResponse.url
        return Net.put(uploadUrl, userToken, this)
    }

    companion object {

        private val jsonParser = Json { ignoreUnknownKeys = true }

        fun of(gcsId: String, userToken: String): DocumentMetadata {
            val downloadUrl = DownloadResponse.of(gcsId, userToken).url
            val json = Net.get(downloadUrl, userToken).body!!.string()
            return jsonParser.decodeFromString(json)
        }

        fun makeDefaultMetadata(): DocumentMetadata {
            return DocumentMetadata(
                deleted = false,
                lastModified = System.currentTimeMillis().toString(),
                lastOpened = "0",
                lastOpenedPage = 0,
                metadatamodified = true,
                modified = true,
                parent = "trash",
                pinned = false,
                synced = false,
                _type = "DocumentType",
                version = 0,
                visibleName = ""
            )
        }
    }

}