package model

import kotlinx.serialization.Serializable

@Serializable
class UploadIndexFileRequest(
    val http_method: String,
    val relative_path: String
): ApiRequest() {

}