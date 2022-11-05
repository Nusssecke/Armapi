import model.DocumentContent
import model.DocumentMetadata
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.random.Random

class Utils {

    companion object {
        val WORKDIR: String = ""

        fun sha265(json: String): String {
            val byteArray = json.toByteArray(StandardCharsets.UTF_8)
            val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
            val hash: ByteArray = digest.digest(byteArray)
            val hex = hash.toHex()
            return hex
        }

        fun sortRootFile(rootFile: String): String {
            val lineList = rootFile.split("\n")
            lineList.sortedBy { it.split(":")[2] }
            return lineList.joinToString("\n" )
        }
    }

}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }