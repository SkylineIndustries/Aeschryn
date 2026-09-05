package com.skylineindustries.aeschryn.bitbucket

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

/**
 * Stores the user's Atlassian account e-mail + personal API token in the IDE's secure
 * credential store (not in project files). Bitbucket Cloud's API expects these as
 * HTTP Basic auth (email:token), unlike repository/workspace access tokens which use Bearer.
 */
object BitbucketCredentials {

    private val attributes = CredentialAttributes(generateServiceName("Aeschryn", "Bitbucket API Token"))

    fun get(): Credentials? = PasswordSafe.instance.get(attributes)

    fun set(email: String?, apiToken: String?) {
        PasswordSafe.instance.set(
            attributes,
            if (email.isNullOrBlank() || apiToken.isNullOrBlank()) null else Credentials(email, apiToken)
        )
    }
}
