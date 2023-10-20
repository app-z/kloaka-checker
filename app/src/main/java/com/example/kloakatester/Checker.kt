package com.example.kloakatester

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.telephony.TelephonyManager
import com.google.gson.annotations.SerializedName

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import io.ktor.serialization.kotlinx.json.*


data class As(

    @SerializedName("asn") var asn: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("route") var route: String? = null,
    @SerializedName("domain") var domain: String? = null,
    @SerializedName("type") var type: String? = null

)

data class Location(

    @SerializedName("country") var country: String? = null,
    @SerializedName("region") var region: String? = null,
    @SerializedName("timezone") var timezone: String? = null

)

data class ClientNetworkInfo(

    @SerializedName("ip") var ip: String? = null,
    @SerializedName("location") var location: Location? = Location(),
    @SerializedName("domains") var domains: ArrayList<String> = arrayListOf(),
    @SerializedName("as") var as_: As? = As(),
    @SerializedName("isp") var isp: String? = null

)


object Checker {
    suspend fun isOpenVebView(context: Context): Boolean {
        return checkDeviceName("Pixel")
                // || checkSim(context)
                || checkDomens()
    }


    fun checkDeviceName(MASK_NAME: String): Boolean {
        val name = BluetoothAdapter.getDefaultAdapter().getName()
        return name.contains(MASK_NAME, ignoreCase = true)
    }


    fun checkSim(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm == null) return false
        val simID: String? = tm.simSerialNumber
        if (simID == null) return false
        if (simID.length <= 0) return false
        return true
    }

    suspend fun checkDomens(): Boolean {

        val rejectedDomains = listOf<String>("Google", "goo")
        val rejectRegion = "US"

        CoroutineScope(Dispatchers.IO).launch {

            val res = async {

                val urlForIp = "https://api.ipify.org"
                val client = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                        })
                    }
                }
                val responseIp: HttpResponse = client.get(urlForIp)

                if (!statusRequest(responseIp)) return@async false

                var ip = responseIp.bodyAsText()
                println("ip = $ip")
//                ip = "8.8.8.8"

                val urlServAndReg =
                    "https://geo.ipify.org/api/v2/country?apiKey=$key&ipAddress=$ip"
                val clientNetworkInfo: ClientNetworkInfo = client.get(urlServAndReg).body()

                val intersectDomens = clientNetworkInfo.domains intersect rejectedDomains

                if (intersectDomens.isNotEmpty())
                    return@async false

                if (clientNetworkInfo.location?.country.equals(rejectRegion, ignoreCase = true))
                    return@async false

                client.close()

                return@async true
            }
            res.await()

        }

        return false
    }

    fun statusRequest(response: HttpResponse): Boolean {
        return (response.status.value in 200..299)
    }

}
