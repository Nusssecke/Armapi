package http

import model.ApiRequest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

// TODO Copyright

class Net {

    companion object {
        const val INTERNAL_CLOUD = "https://internal.cloud.remarkable.com"
        const val DOWNLOAD = "$INTERNAL_CLOUD/sync/v2/signed-urls/downloads"
        const val UPLOAD = "$INTERNAL_CLOUD/sync/v2/signed-urls/uploads"

        val JSON: MediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!
        val PDF: MediaType = "application/pdf; charset=utf-8".toMediaTypeOrNull()!!

        private const val JRMAPI_NET_TAG = "Jrmapi-Net"

        const val GET = "GET"
        const val PUT = "PUT"
        const val POST = "POST"

        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()


        fun get(url: String, token: String): Response {
            val request: Request = Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
            return sendRequest(request)
        }

        fun getFile(url: String, token: String, file: File) {
            val request: Request = Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .build()
            sendRequest(request, file)
        }

        fun post(url: String, token: String, payload: ApiRequest): Response {
            return post(url, token, payload.toRequestBody())
        }


        fun post(url: String, token: String, requestBody: RequestBody = byteArrayOf().toRequestBody()): Response {
            val request: Request = Request.Builder().url(url)
                .post(requestBody)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json") //.header("Content-Length", "0")
                .build()
            return sendRequest(request)
        }

        fun put(url: String, token: String, payload: ApiRequest): Response {
            val request: Request = Request.Builder()
                .url(url)
                .put(payload.toRequestBody())
                .header("Authorization", "Bearer $token")
                .header("x-goog-content-length-range", "0,7000000000")
                .build()
            return sendRequest(request)
            // return sendRequest(request)
        }

        fun put(url: String, token: String, payload: String): Response {
            val request: Request = Request.Builder()
                .url(url)
                .put(payload.toRequestBody(JSON))
                .header("Authorization", "Bearer $token")
                .header("x-goog-content-length-range", "0,7000000000")
                .build()
            return sendRequest(request)
        }

        fun putFile(url: String, token: String, file: File): Response {
            val request: Request = Request.Builder()
                .url(url)
                // TODO .put(RequestBody.create(file, ))
                .header("Authorization", "Bearer $token")
                .build()
            return sendRequest(request)
        }

        fun sendRequest(request: Request): Response {
            var res: Response = Response.Builder().code(418)
                .request(Request.Builder().url("http://error.org").build())
                .protocol(Protocol.HTTP_1_0)
                .message("Error").build()
            try {
                Log.d(JRMAPI_NET_TAG, "Url: " + request.url.toString())
                // http.Log.d(JRMAPI_NET_TAG, request.headers().toString());
                val response = client.newCall(request).execute()
                Log.d(JRMAPI_NET_TAG, "Reponse code: " + response.code)
                if (response.code == 200) {
                    res = response
                } else {
                    Log.e(JRMAPI_NET_TAG, response.body!!.string())
                }
            } catch (e: IOException) {
                Log.e(JRMAPI_NET_TAG, "Error while launching request", e)
            }
            return res
        }

        private fun sendRequest(request: Request, file: File) {
            try {
                Log.d(JRMAPI_NET_TAG, request.url.toString())
                val response = client.newCall(request).execute()
                val fileData = response.body!!.source()
                // Save to file
                val sink = file.sink().buffer()
                sink.writeAll(fileData)
                sink.close()
                Log.d(JRMAPI_NET_TAG, response.code.toString())
            } catch (e: IOException) {
                Log.e(JRMAPI_NET_TAG, "Error while launching request", e)
            }
        }
    }

}