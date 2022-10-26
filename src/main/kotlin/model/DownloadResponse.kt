package model

import http.Net
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response

@Serializable
class DownloadResponse(
    val relative_path: String,
    val url: String,
    val expires: String,
    val method: String
) {

    companion object{
        fun of(gcsId: String, userToken: String): DownloadResponse {
            val downloadResponse = Net.post(Net.DOWNLOAD, userToken, DownloadRequest(Net.GET, gcsId))
            val json = downloadResponse.body!!.string()
            return Json.decodeFromString(json)
        }

        fun of(response: Response): DownloadResponse {
            val json = response.body!!.string()
            return Json.decodeFromString(json)
        }
    }

}