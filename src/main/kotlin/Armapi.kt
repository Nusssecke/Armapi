import http.Log
import http.Net
import http.Net.Companion.DOWNLOAD
import model.*
import okhttp3.Response
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class Armapi(deviceToken: String, workingDirectory: File) {

    val userToken: String

    private fun createWorkdir(workingDirectory: File) {
        val workdir = File(workingDirectory.absolutePath + "/.jrmapi/")
        // Utils.WORKDIR = workdir.getAbsolutePath();
        if (!workdir.exists()) {
            workdir.mkdir()
        }
    }

    init {
        userToken = Authentication().userToken(deviceToken)
        Log.d(JRMAPI_TAG, "User Token: $userToken") // TODO Remove
        createWorkdir(workingDirectory)
    }

    fun getRootDirectoryId(): String {
        val rootFileIndexResponse = Net.post(DOWNLOAD, userToken, DownloadRequest(Net.GET, "root"))
        val downloadResponse = DownloadResponse.of(rootFileIndexResponse)

        val rootFileIdUrl = downloadResponse.url // Extract the storage.googleapis url

        return Net.get(rootFileIdUrl, userToken).body!!.string()
    }

    fun getRootFileResponse(rootId: String): Response {
        val rootFileUrlResponse = Net.post(DOWNLOAD, userToken, DownloadRequest(Net.GET, rootId))
        val rootFileUrl = DownloadResponse.of(rootFileUrlResponse).url

        return Net.get(rootFileUrl, userToken)
    }

    fun getRootFile(rootId: String): String {
        return getRootFileResponse(rootId).body!!.string()
    }

    fun rootToDocument(rootId: String): List<rmDocument> {
        val rootFile = getRootFile(rootId).removePrefix("3\n").trimEnd()
        // Root file lines corresponds to documents
        // Line: {GCS path identifier for index file}:8000000:{document/folder uuid on device}:{number of entries in index file}:0
        return rootFile.lines().map { rmDocument.of(it) }
    }

    fun fetchZip(): File? {
        return null
    }

    fun exportPdf() {
        return
    }

    fun createDir(name: String?, parentID: String?) {}

    fun deleteEntry() {}
    fun moveEntry() {}

    companion object {
        private const val JRMAPI_TAG = "Jrmapi"
        // https://docs.google.com/document/u/0/d/1peZh79C2BThlp2AC3sITzinAQKJccQ1gn9ppdCIWLl8/mobilebasic
        @JvmStatic
        fun main(args: Array<String>) {
            // String deviceToken = new Authentication().registerDevice("", UUID.randomUUID())
            val deviceToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRoMC11c2VyaWQiOiJhdXRoMHw2MTJlNzk4OTA2OGVkNjAwNmE4ZTFlYWIiLCJkZXZpY2UtZGVzYyI6Im1vYmlsZS1hbmRyb2lkIiwiZGV2aWNlLWlkIjoiOTMwYTRhNDktMTBmNy00MzA1LWIzMzQtY2NmODBkZmY1NzcxIiwiaWF0IjoxNjY2NTIwNDk5LCJpc3MiOiJyTSBXZWJBcHAiLCJqdGkiOiJjazB0Z3BpOXRWcz0iLCJuYmYiOjE2NjY1MjA0OTksInN1YiI6InJNIERldmljZSBUb2tlbiJ9.pqJLtYvO4laR8FD5MndJJWZOg5wsk9C66HhxaEtrTAs"


            val jrmapi = Armapi(deviceToken, File(System.getProperty("user.home")))
            // rmDocument("934144be8c9166df08022552ce10665111ba4beb1a7d1ed34a7175590b86555d").getSubFiles(jrmapi.userToken)
            // println(jrmapi.getRootDirectoryId())
            // println(jrmapi.getRootFile(jrmapi.getRootDirectoryId()))
            // val documents = jrmapi.getRootFile(jrmapi.getRootDirectoryId())

            val d = rmDocument()
            d.downloadIndexFileEntry("b81a7ad514253396d7f36361eb8073317f4f7939b6559fd2d92b5c910ea639ad:0:ceacda6b-424e-4c0e-afeb-ee0a3e8cb661.content:0:735", jrmapi.userToken)
            println(d.content!!.toJson())

            val document = rmDocument()
            val pdf: File = File("/home/finn/Downloads/Test.pdf")
            document.content = DocumentContent.fromPdfFile(pdf)
            document.metadata = DocumentMetadata.makeDefaultMetadata()
            document.pagedata = DocumentPagedata.makeDefaultPageData(pdf)
            document.pdf = pdf.readText()
            document.upload(jrmapi, "")

            // print(document.id)
            // Check if single files are uploaded

        }


    }
}