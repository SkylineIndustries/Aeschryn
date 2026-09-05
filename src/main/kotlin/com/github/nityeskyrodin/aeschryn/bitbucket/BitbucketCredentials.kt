package com.github.nityeskyrodin.aeschryn.bitbucket

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe

/**
 * Stores the Bitbucket API token in the IDE's secure credential store (not in project files).
 */
object BitbucketCredentials {

    private val attributes = CredentialAttributes(generateServiceName("Aeschryn", "Bitbucket API Token"))

    fun getToken(): String? = PasswordSafe.instance.getPassword(attributes)

    fun setToken(token: String?) {
        PasswordSafe.instance.set(attributes, token?.let { Credentials("bitbucket", it) })
    }
}
