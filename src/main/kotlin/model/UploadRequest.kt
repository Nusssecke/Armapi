package model

import http.Net
import kotlinx.serialization.Serializable

@Serializable
class UploadRequest(
    val filename: String,
    val file_size: Int,
    val http_method: String,
    val relative_path: String
): ApiRequest() {

    fun upload(userToken: String): UploadResponse {
        val response = Net.post(Net.UPLOAD, userToken, this)
        return UploadResponse.of(response)
    }

}