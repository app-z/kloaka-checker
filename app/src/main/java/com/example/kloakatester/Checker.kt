package com.example.kloakatester

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.telephony.TelephonyManager
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.*
import kotlinx.serialization.json.*


@Keep
@Serializable
data class As(

    @SerializedName("asn") var asn: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("route") var route: String? = null,
    @SerializedName("domain") var domain: String? = null,
    @SerializedName("type") var type: String? = null

)

@Keep
@Serializable
data class Location(

    @SerializedName("country") var country: String? = null,
    @SerializedName("region") var region: String? = null,
    @SerializedName("timezone") var timezone: String? = null

)

@Keep
@Serializable
data class ClientNetworkInfo(

    @SerializedName("ip") var ip: String? = null,
    @SerializedName("location") var location: Location? = Location(),
    @SerializedName("domains") var domains: ArrayList<String> = arrayListOf(),
    @SerializedName("as") var as_: As? = As(),
    @SerializedName("isp") var isp: String? = null

)


object Checker {
    suspend fun isRejectVebWiew(context: Context): Flow<Boolean> {
        return flow {
            val result = merge(rejectByDeviceName("Pixel"), rejectByDomains())

            val ret = result.toList().filter{ it.isFailure || it.getOrNull() == true}
            if (ret.isNotEmpty()) emit(true)
            else emit(false)
        }
    }


    fun rejectByDeviceName(MASK_NAME: String): Flow<Result<Boolean>> {
        return flow {
            val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) emit(Result.success(true))
            val name: String? = adapter?.getName()
            emit(Result.success(name?.contains(MASK_NAME, ignoreCase = true) ?: true))
        }
    }


    fun rejectBySim(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (tm == null) return false
        val simID: String? = tm.simSerialNumber
        if (simID == null) return false
        if (simID.length <= 0) return false
        return true
    }

    // false is ok
    suspend fun rejectByDomains(): Flow<Result<Boolean>> {
        val key = ""

        val rejectedDomains = listOf("Google", "goo")
        val rejectRegion = "US"

        return flow {

            getRemoteIp().collect{
                if(it.isSuccess && it.getOrNull() != null) {
                    val ip = it.getOrNull()
//                val ip = "8.8.8.8"
                    val urlServAndReg =
                        "https://geo.ipify.org/api/v2/country?apiKey=$key&ipAddress=$ip"
                    println(urlServAndReg)
                    try {
                        val clientNetworkInfo: ClientNetworkInfo =
                            Client.getClient().get(urlServAndReg).body()

                        val intersectDomens = clientNetworkInfo.domains intersect rejectedDomains

                        if (intersectDomens.isNotEmpty()) {
                            emit(Result.success(true))
                        } else if (clientNetworkInfo.location?.country.equals(
                                    rejectRegion,
                                    ignoreCase = true
                                )
                            ) {
                                emit(Result.success(true))
                            }
                        else {
                            emit(Result.success(false))
                        }
                    } catch (ex: Exception) {
                        emit(Result.failure(ex))
                    }
                } else {
                    emit(Result.success(true))
                }
            }
        }
    }

    private suspend fun getRemoteIp(): Flow<Result<String>> {
        val urlForIp = "https://api.ipify.org"
        return flow {
            try {
                val responseIp: HttpResponse = Client.getClient().get(urlForIp)
                val ip = responseIp.bodyAsText()
                println("ip = $ip")
                emit(Result.success(ip))
            } catch (ex: Exception) {
                emit(Result.failure(ex))
            }
        }
    }


    object Client {
        private val client = HttpClient(CIO) {
            BrowserUserAgent()
//                    CurlUserAgent()
            install(ContentNegotiation) {
                engine {
                    requestTimeout = 10_000
                }
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
        }

        fun getClient(): HttpClient {
            return client
        }

        fun close() {
            client.close()
        }
    }

    private fun statusRequest(response: HttpResponse): Boolean {
        return (response.status.value in 200..299)
    }

}
