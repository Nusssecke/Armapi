import http.*
import http.Net.Companion.DOWNLOAD
import model.*
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.util.stream.Collectors

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

    var rootFile
        get() {
            if(field is null){
                return field
            } else {
                return getRootFileGCSId()
            }
        }
        set(value) {
            // TODO
        }

    fun getRootFileGCSId(): String {
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

    // Fixes the error when there are files in the root index which cannot be downloaded
    fun fixApiError() {
        val documents = listOf<RMDocument>() // TODO rootToDocument(getRootFileGCSId())
        for (document in documents){
            val entryIds = document.loadIndexFile(userToken)
            for (entry in entryIds){
                if (!document.downloadIndexFileEntry(entry, userToken)) {

                }
            }
            println("${document.gcsId}: ${document.uuid}: ${document.metadata!!.visibleName}")

        }
    }

    // Returns the newRootFileGeneration
    fun uploadRootFileIndex(oldRootFileGeneration: String, rootFileGCSId: String): String {
        // Uploading root file index
        println("rootPath: $rootFileGCSId")
        val uploadRootFileIndexRequestBody = UploadRootFileIndex(oldRootFileGeneration.toLong(), Net.PUT, "root", rootFileGCSId).toRequestBody()
        val uploadRootFileRequest: Request = Request.Builder()
            .url(Net.UPLOAD)
            .post(uploadRootFileIndexRequestBody)
            .header("Authorization", "Bearer $userToken")
            //.header("Content-MD5", rootPath.encodeUtf8().md5().base64())
            .build()
        val uploadRootFileRequestResponse = Net.sendRequest(uploadRootFileRequest)
        // println(uploadRootFileRequestResponse.body!!.string())


        // Looks good for md5-content header testing needed
        // println(rootPath.encodeUtf8().md5().base64())
        // check content type and User-Agent

        val uploadUrlRootFileIndex = UploadResponse.of(uploadRootFileRequestResponse).url
        val requestRootFileIndex: Request = Request.Builder()
            .url(uploadUrlRootFileIndex)
            .put(rootFileGCSId.toRequestBody(null))
            .header("x-goog-if-generation-match", oldRootFileGeneration)
            .header("x-goog-content-length-range", "0,7000000000")
            //.header("Content-MD5", rootPath.encodeUtf8().md5().base64())
            .build()
        val newGeneration = Net.sendRequest(requestRootFileIndex).header("x-goog-generation")!!
        return newGeneration
    }

    fun tree(){
        // Tree call
        // val uploadTreeFileResponse = Net.post(Net.UPLOAD, userToken, UploadIndexFileRequest(Net.PUT, "tree"))
        // val uploadUrlTree = UploadResponse.of(uploadTreeFileResponse).url

        // 13ac7003 : dbe84ce1 : 13ac7003 : cd5f10e8 :: 80000000 ::  ${rootFile.split("\n").size - 2} :: ForUpload, Rehash :: .
        /* val treeBody = """generation: $newGeneration
         server   : local    : previous : final    :: filemode :: chld :: markers

                  : ${indexFileGcsId.substring(0, 8)} :          : ${indexFileGcsId.substring(0, 8)} :: 80000000 ::    4 :: ForUpload ::   $newUUID
                  : ${contentGcsId.substring(0, 8)} :          : ${contentGcsId.substring(0, 8)} :: 00000000 ::    0 :: ForUpload ::     $newUUID.content
                  : ${metadataGcsId.substring(0, 8)} :          : ${metadataGcsId.substring(0, 8)} :: 00000000 ::    0 :: ForUpload ::     $newUUID.metadata
                  : ${pagedataGcsId.substring(0, 8)} :          : ${pagedataGcsId.substring(0, 8)} :: 00000000 ::    0 :: ForUpload ::     $newUUID.pagedata
                  : ${pdfGcsId.substring(0, 8)} :          : ${pdfGcsId.substring(0, 8)} :: 00000000 ::    0 :: ForUpload ::     $newUUID.pdf

        """
        println(treeBody)
        // Net.put(uploadUrlTree, userToken, treeBody, null) */
    }

    fun syncComplete(newRootGeneration: String){
        println("sync")
        // Sync complete
        val request: Request = Request.Builder().url(Net.INTERNAL_CLOUD + "/sync/v2/sync-complete")
            .post("{\"generation\": ${newRootGeneration.toLong()}}".toRequestBody(Net.JSON))
            .header("Authorization", "Bearer $userToken")
            .header("Content-Type", "application/json")
            .build()
        println(Net.client.newCall(request).execute().code)
    }

    fun removeGCSIdFromRootFile(gcsId: String){
        // Update root file
        // Get root file
        val rootFileResponse = getRootFileResponse(getRootFileGCSId())

        // REMOVE bad gcsId
        val rootFile = rootFileResponse.body!!.string().split("\n").stream().filter { !it.contains(gcsId) }.collect(Collectors.joining("\n"))
        val rootFileGeneration = 0 // TODO getRootGeneration()


        // Upload new root file
        val rootPath = Utils.sha265(rootFile)
        val uploadRootFileResponse = Net.post(Net.UPLOAD, userToken, UploadIndexFileRequest(Net.PUT, rootPath))
        val uploadUrlRootFile = UploadResponse.of(uploadRootFileResponse).url
        val generation = Net.put(uploadUrlRootFile, userToken, rootFile, null).header("x-goog-generation")!!

        // println("Generation 1 and 2: $rootFileGeneration, $generation ")

        // Uploading root file index
        println("rootPath: $rootPath")
        val uploadRootFileIndexRequestBody = UploadRootFileIndex(rootFileGeneration.toLong(), Net.PUT, "root", rootPath).toRequestBody()
        val uploadRootFileRequest: Request = Request.Builder()
            .url(Net.UPLOAD)
            .post(uploadRootFileIndexRequestBody)
            .header("Authorization", "Bearer $userToken")
            //.header("Content-MD5", rootPath.encodeUtf8().md5().base64())
            .build()
        val uploadRootFileRequestResponse = Net.sendRequest(uploadRootFileRequest)
        // println(uploadRootFileRequestResponse.body!!.string())


        // Looks good for md5-content header testing needed
        // println(rootPath.encodeUtf8().md5().base64())
        // check content type and User-Agent

        val uploadUrlRootFileIndex = UploadResponse.of(uploadRootFileRequestResponse).url
        val requestRootFileIndex: Request = Request.Builder()
            .url(uploadUrlRootFileIndex)
            .put(rootPath.toRequestBody(null))
            .header("x-goog-if-generation-match", rootFileGeneration.toString())
            .header("x-goog-content-length-range", "0,7000000000")
            //.header("Content-MD5", rootPath.encodeUtf8().md5().base64())
            .build()
        val newGeneration = Net.sendRequest(requestRootFileIndex).header("x-goog-generation")!!

        println("sync")
        // Sync complete
        val request: Request = Request.Builder().url(Net.INTERNAL_CLOUD + "/sync/v2/sync-complete")
            .post("{\"generation\": ${newGeneration.toLong()}}".toRequestBody(Net.JSON))
            .header("Authorization", "Bearer $userToken")
            .header("Content-Type", "application/json")
            .build()
        println(Net.client.newCall(request).execute().code)
    }

    companion object {
        private const val JRMAPI_TAG = "Jrmapi"
        // https://docs.google.com/document/u/0/d/1peZh79C2BThlp2AC3sITzinAQKJccQ1gn9ppdCIWLl8/mobilebasic
        @JvmStatic
        fun main(args: Array<String>) {
            // String deviceToken = new Authentication().registerDevice("", UUID.randomUUID())
            val deviceToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRoMC11c2VyaWQiOiJhdXRoMHw2MTJlNzk4OTA2OGVkNjAwNmE4ZTFlYWIiLCJkZXZpY2UtZGVzYyI6Im1vYmlsZS1hbmRyb2lkIiwiZGV2aWNlLWlkIjoiOTMwYTRhNDktMTBmNy00MzA1LWIzMzQtY2NmODBkZmY1NzcxIiwiaWF0IjoxNjY2NTIwNDk5LCJpc3MiOiJyTSBXZWJBcHAiLCJqdGkiOiJjazB0Z3BpOXRWcz0iLCJuYmYiOjE2NjY1MjA0OTksInN1YiI6InJNIERldmljZSBUb2tlbiJ9.pqJLtYvO4laR8FD5MndJJWZOg5wsk9C66HhxaEtrTAs"


            val jrmapi = Armapi(deviceToken, File(System.getProperty("user.home")))
            println(jrmapi.getRootFileGCSId())
            // jrmapi.fixApiError()

            // SUS
            // 92afd78e58ee90640d6ef5ffad97d900437e35688f6baf82771397b84d50dab5: 664915b3-8c75-4115-bdeb-bfd3123e983b:
            // aa8f60f8b671ba91678ee0681a0eb345b0926899dbb04bed05f7e9c6d5112553: 810c5b62-d1b6-4925-a9a9-ec2d48ec85c3:
            // 10d0a63faba7bc7b27a33c2838df76254e3076d1729edf67f90a7f8a237bd374: df055fac-ab6f-41b7-b166-268ab0ab1abd:

            // println(jrmapi.getRootFile(jrmapi.getRootDirectoryId()))
            // println("Without")
            // jrmapi.removeGCSIdFromRootFile("04cfc795f1a81d219fac59efa19722d5796ea74b080599bb3556cc91cc544559")

            // rmDocument("934144be8c9166df08022552ce10665111ba4beb1a7d1ed34a7175590b86555d").getSubFiles(jrmapi.userToken)
            // println(jrmapi.getRootDirectoryId())
            // println(jrmapi.getRootFile(jrmapi.getRootDirectoryId()))
            // val documents = jrmapi.getRootFile(jrmapi.getRootDirectoryId())

            // val d = rmDocument()
            // d.downloadIndexFileEntry("780211a82ce841005a435f44419e93610e0e03fa15b002c4e6bf57c21293216d:0:01de551a-5f3a-4d0d-88e9-ad34986ec023.metadata:0:255", jrmapi.userToken)
            // println(d.content!!.toJson())

            // val document = RMDocument()
            // val pdf: File = File("C:\\Users\\finn-\\Downloads\\test.pdf")
            // document.content = DocumentContent.fromPdfFile(pdf)
            // document.metadata = DocumentMetadata.makeDefaultMetadata()
            // document.pagedata = DocumentPagedata.makeDefaultPageData(pdf)
            // document.pdf = pdf.readText()
            // document.upload(jrmapi, "")

            // print(document.id)
            // Check if single files are uploaded

        }


    }
}