package com.skylineindustries.aeschryn.bitbucket

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.util.io.HttpRequests
import java.nio.charset.StandardCharsets
import java.util.Base64

@Service(Service.Level.APP)
class BitbucketApiClient {

    private val gson = Gson()

    fun fetchOpenPullRequests(
        workspace: String,
        repoSlug: String,
        email: String?,
        apiToken: String?,
    ): List<BitbucketPullRequest> {
        val result = mutableListOf<BitbucketPullRequest>()
        var url: String? =
            "https://api.bitbucket.org/2.0/repositories/$workspace/$repoSlug/pullrequests?state=OPEN&pagelen=50"

        while (url != null) {
            val json = HttpRequests.request(url)
                .tuner { connection ->
                    if (!email.isNullOrBlank() && !apiToken.isNullOrBlank()) {
                        val basicAuth = Base64.getEncoder()
                            .encodeToString("$email:$apiToken".toByteArray(StandardCharsets.UTF_8))
                        connection.setRequestProperty("Authorization", "Basic $basicAuth")
                    }
                }
                .readString()

            val page = gson.fromJson(json, BitbucketPullRequestPage::class.java)
            result += page.values
            url = page.next
        }

        return result
    }
}
