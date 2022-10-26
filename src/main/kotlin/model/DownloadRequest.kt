package model

import kotlinx.serialization.Serializable

@Serializable
class DownloadRequest(
    val http_method: String,
    val relative_path: String
): ApiRequest() {
}