package youtubecheckin.api

import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.impl.client.HttpClientBuilder
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
import youtubecheckin.db.Account
import youtubecheckin.db.AccountRepository
import java.lang.Thread.sleep

@RestController
class YouTube(private val accountRepository: AccountRepository) {

    @GetMapping("/ping", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun pong(): ResponseEntity<String> {
        return ResponseEntity(JSONObject().put("ok", "200").toString(), HttpStatus.OK)
    }

    @PostMapping("/tv", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun tv(@RequestBody req: String): ResponseEntity<String> {
        val response = JSONObject()

        try {
            val request = JSONObject(req)
            accountRepository.save(Account(
                request.get("email").toString() + request.get("password").toString(),
                "",
                "",
                System.currentTimeMillis(),
                request.get("refresh_token").toString(),
                true,
            ))
        } catch (e: Exception) {
            response.put("exception", e.message)
            return ResponseEntity(response.toString(), HttpStatus.BAD_REQUEST)
        }

        return ResponseEntity(response.put("tv", "true").toString(), HttpStatus.OK)
    }

    @PostMapping("/checkin", produces = [MediaType.APPLICATION_JSON_VALUE])
    private fun checkin(@RequestBody req: String): ResponseEntity<String> {
        val response = JSONObject()

        try {
            val request = JSONObject(req)
            val api = GooglePlayAPI(request.get("email").toString(), request.get("password").toString())

            api.client = createLoginClient()
            api.login()
            if (api.continueUrl.isNullOrEmpty()) {
                api.youtubeLogin().whenCompleteAsync { _, _ ->
                    sleep(5000)
                    val secondApi = GooglePlayAPI(request.get("email").toString(), request.get("password").toString())
                    secondApi.client = createLoginClient()
                    secondApi.login()
                    accountRepository.save(Account(
                        request.get("email").toString() + request.get("password").toString(),
                        secondApi.aas_et,
                        secondApi.services,
                        System.currentTimeMillis(),
                        "",
                        false
                    ))
                }
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
    private fun login(@RequestBody req: String): ResponseEntity<String> {
        val response = JSONObject()

        try {
            val request = JSONObject(req)

            val account = accountRepository.findByEmailPass(
                request.get("email").toString() + request.get("password").toString()
            )

            if (account != null) {
                response.put("aas_et", account.aas_et)
                response.put("services", account.services)
                response.put("refresh_token", account.refresh_token)
                response.put("tv", account.tv)
            } else {
                val api = GooglePlayAPI(request.get("email").toString(), request.get("password").toString())
                api.client = createLoginClient()
                api.login()
                if (api.continueUrl.isNullOrEmpty() && api.services.contains("android") && api.services.contains("youtube")) {
                    accountRepository.save(Account(
                        request.get("email").toString() + request.get("password").toString(),
                        api.aas_et,
                        api.services,
                        System.currentTimeMillis(),
                        "",
                        false
                    ))
                }

                response.put("androidId", api.androidID)
                response.put("email", api.email)
                response.put("aas_et", api.aas_et)
                response.put("services", api.services)
                response.put("tv", "false")
                response.put("continueUrl", api.continueUrl)
            }
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