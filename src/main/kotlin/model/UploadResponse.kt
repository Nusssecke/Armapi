package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response

@Serializable
class UploadResponse(
    val relative_path: String,
    val url: String,
    val expires: String,
    val method: String,
    val maxuploadsize_bytes: Long
) {

    companion object{
        fun of(response: Response): UploadResponse {
            val json = response.body!!.string()
            return Json.decodeFromString(json)
        }

        fun of(json: String): UploadResponse {
            return Json.decodeFromString(json)
        }
    }

}