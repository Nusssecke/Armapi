package model

import http.Net
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
class UploadRootFileIndex(
    val generation: Long,
    val http_method: String,
    val relative_path: String,
    val root_schema: String
) {

    fun toRequestBody(): RequestBody {
        return toJson().toRequestBody(Net.JSON)
    }

     fun toJson(): String {
        return "{" +
                "\"generation\":$generation," +
                "\"http_method\":\"$http_method\"," +
                "\"relative_path\":\"$relative_path\"," +
                "\"root_schema\":\"$root_schema\"" +
                "}"
    }

}