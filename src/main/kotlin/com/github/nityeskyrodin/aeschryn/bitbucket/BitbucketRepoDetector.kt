package com.github.nityeskyrodin.aeschryn.bitbucket

import com.intellij.openapi.project.Project
import java.io.File

/**
 * Reads .git/config directly instead of depending on the bundled Git plugin,
 * so this plugin keeps working in any IntelliJ Platform IDE without extra dependencies.
 */
object BitbucketRepoDetector {

    data class RepoRef(val workspace: String, val repoSlug: String)

    private val REMOTE_URL_PATTERN = Regex("""bitbucket\.org[:/]([^/]+)/([^/]+?)(\.git)?/?$""")

    fun detect(project: Project): RepoRef? {
        val basePath = project.basePath ?: return null
        val gitConfig = File(basePath, ".git/config")
        if (!gitConfig.isFile) return null

        val originUrl = gitConfig.readLines()
            .dropWhile { !it.trim().startsWith("[remote \"origin\"]") }
            .drop(1)
            .takeWhile { !it.trim().startsWith("[") }
            .firstOrNull { it.trim().startsWith("url") }
            ?.substringAfter("=")
            ?.trim()
            ?: return null

        val match = REMOTE_URL_PATTERN.find(originUrl) ?: return null
        return RepoRef(workspace = match.groupValues[1], repoSlug = match.groupValues[2])
    }
}
