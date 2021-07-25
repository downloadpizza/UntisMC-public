package net.downloadpizza.untiskt

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.success
import java.lang.Exception


val PARSER = Parser.default()

val klaxon = Klaxon()

val FIELDS = arrayOf("name", "id")

class WebUntis(
    private val user: String,
    private val password: String,
    private val server: String,
    private val schoolName: String,
    private val clientName: String = "UntisMC"
) {
    var id = 0.toBigInteger()
    var personId: Int? = null
    var personType: Int? = null
    var sessionId: String? = null

    private val url get() = "$server/WebUntis/jsonrpc.do"

    fun authenticate(): Boolean {
        val obj = ReqObj(
            newId(), "authenticate", JsonObject(
                mapOf(
                    "user" to user,
                    "password" to password,
                    "client" to clientName
                )
            )
        )

        val request = "$url?school=$schoolName"
            .httpPost()
            .jsonBody(klaxon.toJsonString(obj))
            .responseString { _, _, httpResult ->
                httpResult.success {
                    val json: JsonObject = PARSER.parse(StringBuilder(it)) as JsonObject
                    val result = json.obj("result")
                    sessionId = result?.string("sessionId")
                    personId = result?.int("personId")
                    personType = result?.int("personType")
                }
            }
        request.join()
        return sessionId != null
    }

    fun logout() {
        val obj = ReqObj(newId(), "logout")

        val request = url
            .httpPost()
            .jsonBody(klaxon.toJsonString(obj))
            .header("Cookie", "JSESSIONID=${sessionId!!}")
            .responseString { _, _, result ->
                result.success {
                    val json = PARSER.parse(StringBuilder(it)) as JsonObject
                    val err = json.obj("error")?.toJsonString()
                    if (err != null) System.err.println(err)
                }
                sessionId = null
                personType = null
                personId = null
            }
        request.join()
    }

    fun getTimetable(startDate: Int, endDate: Int): Array<Period> {
        val obj = ReqObj(
            newId(), "getTimetable", JsonObject(
                mapOf(
                    "options" to mapOf(
                        "element" to mapOf(
                            "id" to personId!!,
                            "type" to personType!!
                        ),

                        "startDate" to startDate,
                        "endDate" to endDate,

                        "klasseFields" to FIELDS,
                        "roomFields" to FIELDS,
                        "subjectFields" to FIELDS,
                        "teacherFields" to FIELDS
                    )
                )
            )
        )

        var periods: Array<Period>? = null

        val request = url
            .httpPost()
            .jsonBody(klaxon.toJsonString(obj))
            .header("Cookie", "JSESSIONID=${sessionId!!}")
            .responseString { _, _, result ->
                result.success {
                    val json = PARSER.parse(StringBuilder(it)) as JsonObject
                    val err = json.obj("error")?.toJsonString()
                    if (err != null) {
                        System.err.println(err)
                        throw UntisException(err)
                    }
                    val array = json.array<JsonObject>("result")!!
                    periods = array.map(::jsonToPeriod).toTypedArray()
                }
            }
        request.join()
        return periods!!
    }

    private fun newId(): String {
        val ret = id.toString(36)
        id++
        return ret
    }

    inline fun <R> use (block: WebUntis.() -> R): R {
        this.authenticate()
        checkNotNull(this.sessionId) { "Login should work" }
        val res = this.block()
        this.logout()
        return res
    }
}

data class ReqObj(
    val id: String,
    val method: String,
    val params: JsonObject = JsonObject(),
    val jsonrpc: String = "2.0"
)

class UntisException(err: String) : Exception(err)