package com.skylineindustries.aeschryn.bitbucket

import com.google.gson.annotations.SerializedName

data class BitbucketPullRequestPage(
    val values: List<BitbucketPullRequest> = emptyList(),
    val next: String? = null,
)

data class BitbucketPullRequest(
    val id: Int,
    val title: String,
    val state: String,
    val author: BitbucketUser?,
    val source: BitbucketBranchRef?,
    val destination: BitbucketBranchRef?,
    val links: BitbucketLinks?,
)

data class BitbucketUser(
    @SerializedName("display_name") val displayName: String?,
)

data class BitbucketBranchRef(
    val branch: BitbucketBranch?,
)

data class BitbucketBranch(
    val name: String?,
)

data class BitbucketLinks(
    val html: BitbucketHref?,
)

data class BitbucketHref(
    val href: String?,
)
