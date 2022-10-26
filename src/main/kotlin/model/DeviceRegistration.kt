package model

import http.Net
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
class DeviceRegistration(
    val code: String,
    val deviceDesc: String,
    val deviceID: String
): ApiRequest() {

}