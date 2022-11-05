package model

import http.Net
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
sealed class ApiRequest {
    val jsonLength: Int
        get() {
            return this.toJson().length
        }

    fun toJson(): String {
        return Json{classDiscriminator = "#class"}.encodeToString(this)
    }

    fun toRequestBody(): RequestBody {
        return toJson().toRequestBody(Net.JSON)
    }
}