package model

import kotlinx.serialization.Serializable

@Serializable
class DeviceRegistration(
    val code: String,
    val deviceDesc: String,
    val deviceID: String
): ApiRequest() {

}