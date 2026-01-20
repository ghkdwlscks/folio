package com.portfolio.manager.data.remote

import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class YahooFinanceApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: YahooFinanceApi

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YahooFinanceApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getChart - valid response - parses correctly`() = runTest {
        val responseJson = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "shortName": "Apple Inc.",
                                "regularMarketPrice": 178.50,
                                "chartPreviousClose": 175.00,
                                "currency": "USD"
                            }
                        }
                    ],
                    "error": null
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val response = api.getChart("AAPL")

        assertThat(response.chart.result).hasSize(1)
        val meta = response.chart.result!![0].meta
        assertThat(meta.symbol).isEqualTo("AAPL")
        assertThat(meta.shortName).isEqualTo("Apple Inc.")
        assertThat(meta.regularMarketPrice).isEqualTo(178.50)
        assertThat(meta.chartPreviousClose).isEqualTo(175.00)
        assertThat(meta.currency).isEqualTo("USD")
    }

    @Test
    fun `getChart - Korean stock - parses KRW currency`() = runTest {
        val responseJson = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "005930.KS",
                                "shortName": "Samsung Electronics",
                                "regularMarketPrice": 78500.0,
                                "chartPreviousClose": 78000.0,
                                "currency": "KRW"
                            }
                        }
                    ],
                    "error": null
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val response = api.getChart("005930.KS")

        assertThat(response.chart.result).hasSize(1)
        val meta = response.chart.result!![0].meta
        assertThat(meta.symbol).isEqualTo("005930.KS")
        assertThat(meta.currency).isEqualTo("KRW")
    }

    @Test
    fun `getChart - null result - returns null`() = runTest {
        val responseJson = """
            {
                "chart": {
                    "result": null,
                    "error": {
                        "code": "Not Found",
                        "description": "No data found"
                    }
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val response = api.getChart("INVALID")

        assertThat(response.chart.result).isNull()
        assertThat(response.chart.error).isNotNull()
    }

    @Test
    fun `getChart - sends correct request path`() = runTest {
        val responseJson = """
            {
                "chart": {
                    "result": [
                        {
                            "meta": {
                                "symbol": "AAPL",
                                "regularMarketPrice": 178.50,
                                "chartPreviousClose": 175.00,
                                "currency": "USD"
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        api.getChart("AAPL")

        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/v8/finance/chart/AAPL?interval=1d&range=1d")
        assertThat(request.method).isEqualTo("GET")
    }
}
