package com.github.nityeskyrodin.aeschryn.toolWindow

import com.github.nityeskyrodin.aeschryn.bitbucket.BitbucketApiClient
import com.github.nityeskyrodin.aeschryn.bitbucket.BitbucketCredentials
import com.github.nityeskyrodin.aeschryn.bitbucket.BitbucketPullRequest
import com.github.nityeskyrodin.aeschryn.bitbucket.BitbucketRepoDetector
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.io.HttpRequests
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel

class PullRequestToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PullRequestPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}

private class PullRequestPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val listModel = CollectionListModel<BitbucketPullRequest>()
    private val list = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = PullRequestCellRenderer()
    }
    private val statusLabel = JBLabel().apply { border = JBUI.Borders.empty(4, 8) }

    init {
        val toolbar = ActionManager.getInstance().createActionToolbar(
            "Aeschryn.PullRequests.Toolbar",
            createActionGroup(),
            true
        )
        toolbar.targetComponent = this

        add(toolbar.component, BorderLayout.NORTH)
        add(JBScrollPane(list), BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)

        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) openSelectedPullRequest()
            }
        })
        list.registerKeyboardAction(
            { openSelectedPullRequest() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        )

        reload()
    }

    private fun createActionGroup(): DefaultActionGroup {
        val group = DefaultActionGroup()
        group.add(object : AnAction("Refresh", "Reload pull requests", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) = reload()
        })
        group.add(object : AnAction("Set API Token…", "Store your Bitbucket API token", AllIcons.General.GearPlain) {
            override fun actionPerformed(e: AnActionEvent) = promptForToken()
        })
        return group
    }

    private fun promptForToken() {
        val token = Messages.showPasswordDialog(
            project,
            "Bitbucket API token (Repository settings > API tokens):",
            "Bitbucket API Token",
            null
        ) ?: return
        BitbucketCredentials.setToken(token.ifBlank { null })
        reload()
    }

    private fun openSelectedPullRequest() {
        val pr = list.selectedValue ?: return
        pr.links?.html?.href?.let { BrowserUtil.browse(it) }
    }

    private fun reload() {
        val repo = BitbucketRepoDetector.detect(project)
        if (repo == null) {
            statusLabel.text = "No Bitbucket remote (bitbucket.org) found for this project."
            updateList(emptyList())
            return
        }

        statusLabel.text = "Loading pull requests for ${repo.workspace}/${repo.repoSlug}…"
        val token = BitbucketCredentials.getToken()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading Bitbucket pull requests", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val prs = service<BitbucketApiClient>().fetchOpenPullRequests(repo.workspace, repo.repoSlug, token)
                    ApplicationManager.getApplication().invokeLater {
                        updateList(prs)
                        statusLabel.text = "${prs.size} open pull request(s) for ${repo.workspace}/${repo.repoSlug}"
                    }
                } catch (ex: HttpRequests.HttpStatusException) {
                    val message = if (ex.statusCode == 401 || ex.statusCode == 403)
                        "Not authorized — check your API token (gear icon above)."
                    else
                        "Bitbucket API returned HTTP ${ex.statusCode}."
                    ApplicationManager.getApplication().invokeLater { statusLabel.text = message }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        statusLabel.text = "Failed to load pull requests: ${ex.message}"
                    }
                }
            }
        })
    }

    private fun updateList(items: List<BitbucketPullRequest>) {
        listModel.removeAll()
        listModel.addAll(0, items)
    }
}

private class PullRequestCellRenderer : ColoredListCellRenderer<BitbucketPullRequest>() {
    override fun customizeCellRenderer(
        list: JList<out BitbucketPullRequest>,
        value: BitbucketPullRequest,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        append("#${value.id} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(value.title, SimpleTextAttributes.REGULAR_ATTRIBUTES)

        val author = value.author?.displayName
        val branches = listOfNotNull(value.source?.branch?.name, value.destination?.branch?.name)
            .takeIf { it.size == 2 }
            ?.let { "${it[0]} → ${it[1]}" }
        val details = listOfNotNull(author, branches).joinToString("  ·  ")
        if (details.isNotEmpty()) {
            append("   $details", SimpleTextAttributes.GRAY_ATTRIBUTES)
        }
    }
}
