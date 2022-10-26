import http.Net
import model.DeviceRegistration
import java.util.*

// TODO Copyright

private const val DEVICE_AUTH_URL = "https://webapp-production-dot-remarkable-production.appspot.com/token/json/2/device/new"
private const val USER_AUTH_URL = "https://webapp-production-dot-remarkable-production.appspot.com/token/json/2/user/new"

class Authentication {

    /**
     * Register new "device" (or app,or service).
     * See https://github.com/splitbrain/ReMarkableAPI/wiki/Authentication#registering-a-device.
     *
     * @param oneTimeCodeValid5Min the one time code that's valid for 5 minutes
     * which was obtained by the end-user on
     * https://my.remarkable.com/connect/mobile
     * @param deviceUUID           a UUID-4 to identify the client
     *
     * @return new token in plain text
     */
    fun registerDevice(oneTimeCodeValid5Min: String, deviceUUID: UUID): String {
        return Net.post(
            DEVICE_AUTH_URL, "", DeviceRegistration(
                code = oneTimeCodeValid5Min,
                deviceDesc = "mobile-android",
                deviceID = deviceUUID.toString()
            )
        ).body!!.string()
    }

    /**
     * Refresh Bearer Token.
     * See https://github.com/splitbrain/ReMarkableAPI/wiki/Authentication#refreshing-a-token.
     */
    fun userToken(deviceToken: String): String {
        return Net.post(USER_AUTH_URL, deviceToken).body!!.string()
    }

    companion object {

        val userToken: String? = null

        fun main(args: Array<String>) {
            val otp = "ydnyltnh"
            println("OTP: $otp")
            val auth = Authentication()
            val deviceToken = auth.registerDevice(otp, UUID.randomUUID())
            println("New Device Token: $deviceToken")
        }
    }


}