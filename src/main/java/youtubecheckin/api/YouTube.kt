package youtubecheckin.api

import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.LaxRedirectStrategy
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.json.JSONObject
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import youtubecheckin.core.DroidConnectionSocketFactory
import youtubecheckin.core.com.akdeniz.googleplaycrawler.GooglePlayAPI

@RestController
class YouTube {

    @GetMapping("/ping", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun pongRequest(): ResponseEntity<String> {
        return ResponseEntity(JSONObject().put("ok", "200").toString(), HttpStatus.OK)
    }

    @PostMapping("/checkin", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun checkin(@RequestBody res: String): ResponseEntity<String> {
        val response = JSONObject()

        try {
            val request = JSONObject(res)
            val api = GooglePlayAPI(request.get("email").toString(), request.get("password").toString())

            api.client = createLoginClient()
            api.login()
            if (api.continueUrl.isNullOrEmpty()) {
                api.checkin()
                api.uploadDeviceConfig()
            }

            response.put("androidId", api.androidID)
            response.put("email", api.email)
            response.put("aas_et", api.aas_et)
            response.put("services", api.services)
            response.put("continueUrl", api.continueUrl)
        } catch (e: Exception) {
            response.put("exception", e.message)
            return ResponseEntity(response.toString(), HttpStatus.BAD_REQUEST)
        }

        return ResponseEntity(response.toString(), HttpStatus.OK)
    }

    @PostMapping("/login", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun login(@RequestBody res: String): ResponseEntity<String> {
        val response = JSONObject()

        try {
            val request = JSONObject(res)
            val api = GooglePlayAPI(request.get("email").toString(), request.get("password").toString())

            api.client = createLoginClient()
            api.login()

            response.put("androidId", api.androidID)
            response.put("email", api.email)
            response.put("aas_et", api.aas_et)
            response.put("services", api.services)
            response.put("continueUrl", api.continueUrl)
        } catch (e: Exception) {
            response.put("exception", e.message)
            return ResponseEntity(response.toString(), HttpStatus.BAD_REQUEST)
        }

        return ResponseEntity(response.toString(), HttpStatus.OK)
    }

    private fun createLoginClient(): HttpClient {
        val connManager = PoolingHttpClientConnectionManager(
            RegistryBuilder.create<ConnectionSocketFactory>().register("https", DroidConnectionSocketFactory()).build()
        )
        connManager.maxTotal = 100
        connManager.defaultMaxPerRoute = 30
        val config = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setConnectionRequestTimeout(10000)
            .setSocketTimeout(10000).build()
        return HttpClientBuilder.create().setDefaultRequestConfig(config).setConnectionManager(connManager).build()
    }
}