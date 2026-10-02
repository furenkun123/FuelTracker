package com.fueltracker.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object ShowApiClient {

    private const val BASE_URL =
        "https://route.showapi.com/1467-1"

    private val client =
        HttpClient(OkHttp)

    suspend fun testConnection(
        apiKey: String
    ): TestResult {

        return try {

            val key = apiKey.trim()

            if (key.isEmpty()) {
                return TestResult(
                    success = false,
                    message = "API Key 不能为空"
                )
            }

            val response =
                client.get(BASE_URL) {
                    parameter(
                        "appKey",
                        key
                    )
                }

            val text =
                response.bodyAsText()

            if (text.isBlank()) {
                return TestResult(
                    success = false,
                    message = "服务器返回为空"
                )
            }

            val root =
                JSONObject(text)

            val showApiBody =
                root.optJSONObject(
                    "showapi_res_body"
                )

            if (showApiBody == null) {

                return TestResult(
                    success = false,
                    message = "返回数据格式异常"
                )
            }

            val retCode =
                showApiBody.optString(
                    "ret_code"
                )

            val message =
                showApiBody.optString(
                    "msg",
                    "未知错误"
                )

            if (retCode == "0") {

                val data =
                    showApiBody.optJSONArray(
                        "data"
                    )

                val count =
                    data?.length() ?: 0

                TestResult(
                    success = true,
                    message =
                        "连接成功，共获取 $count 个品牌"
                )

            } else {

                TestResult(
                    success = false,
                    message =
                        "API 返回错误：$message"
                )
            }

        } catch (e: Exception) {

            TestResult(
                success = false,
                message =
                    "连接失败：${e.message ?: "未知错误"}"
            )
        }
    }
}

data class TestResult(
    val success: Boolean,
    val message: String
)