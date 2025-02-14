package org.odk.collect.android.formmanagement

import android.content.Context
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.analytics.AnalyticsUtils
import org.odk.collect.android.external.FormsContract
import org.odk.collect.android.formmanagement.matchexactly.ServerFormsSynchronizer
import org.odk.collect.android.formmanagement.matchexactly.SyncStatusAppState
import org.odk.collect.android.notifications.Notifier
import org.odk.collect.android.projects.ProjectDependencyProvider
import org.odk.collect.android.projects.ProjectDependencyProviderFactory
import org.odk.collect.android.utilities.FormsDirDiskFormsSynchronizer
import org.odk.collect.forms.FormSourceException
import org.odk.collect.settings.keys.ProjectKeys
import java.io.File
import java.util.stream.Collectors

class FormsUpdater(
    private val context: Context,
    private val notifier: Notifier,
    private val analytics: Analytics,
    private val syncStatusAppState: SyncStatusAppState,
    private val projectDependencyProviderFactory: ProjectDependencyProviderFactory
) {

    /**
     * Downloads updates for the project's already downloaded forms. If Automatic download is
     * disabled the user will just be notified that there are updates available.
     */
    fun downloadUpdates(projectId: String) {
        val sandbox = projectDependencyProviderFactory.create(projectId)

        val diskFormsSynchronizer = diskFormsSynchronizer(sandbox)
        val serverFormsDetailsFetcher = serverFormsDetailsFetcher(sandbox, diskFormsSynchronizer)
        val formDownloader = formDownloader(sandbox, analytics)

        try {
            val serverForms: List<ServerFormDetails> = serverFormsDetailsFetcher.fetchFormDetails()
            val updatedForms =
                serverForms.stream().filter { obj: ServerFormDetails -> obj.isUpdated }
                    .collect(Collectors.toList())
            if (updatedForms.isNotEmpty()) {
                if (sandbox.generalSettings.getBoolean(ProjectKeys.KEY_AUTOMATIC_UPDATE)) {
                    val formUpdateDownloader = FormUpdateDownloader()
                    val results = formUpdateDownloader.downloadUpdates(
                        updatedForms,
                        sandbox.formsLock,
                        formDownloader
                    )

                    notifier.onUpdatesDownloaded(results, projectId)
                } else {
                    notifier.onUpdatesAvailable(updatedForms, projectId)
                }
            }

            context.contentResolver.notifyChange(FormsContract.getUri(projectId), null)
        } catch (_: FormSourceException) {
            // Ignored
        }
    }

    /**
     * Downloads new forms, updates existing forms and deletes forms that are no longer part of
     * the project's form list.
     */
    @JvmOverloads
    fun matchFormsWithServer(projectId: String, notify: Boolean = true): Boolean {
        val sandbox = projectDependencyProviderFactory.create(projectId)

        val diskFormsSynchronizer = diskFormsSynchronizer(sandbox)
        val serverFormsDetailsFetcher = serverFormsDetailsFetcher(sandbox, diskFormsSynchronizer)
        val formDownloader = formDownloader(sandbox, analytics)

        val serverFormsSynchronizer = ServerFormsSynchronizer(
            serverFormsDetailsFetcher,
            sandbox.formsRepository,
            sandbox.instancesRepository,
            formDownloader
        )

        return sandbox.formsLock.withLock { acquiredLock ->
            if (acquiredLock) {
                syncStatusAppState.startSync(projectId)

                val exception = try {
                    serverFormsSynchronizer.synchronize()
                    syncStatusAppState.finishSync(projectId, null)
                    if (notify) {
                        notifier.onSync(null, projectId)
                        AnalyticsUtils.logMatchExactlyCompleted(analytics, null)
                    }
                    null
                } catch (e: FormSourceException) {
                    syncStatusAppState.finishSync(projectId, e)
                    if (notify) {
                        notifier.onSync(e, projectId)
                        AnalyticsUtils.logMatchExactlyCompleted(analytics, e)
                    }
                    e
                }

                exception == null
            } else {
                false
            }
        }
    }
}

private fun formDownloader(
    projectDependencyProvider: ProjectDependencyProvider,
    analytics: Analytics
): ServerFormDownloader {
    return ServerFormDownloader(
        projectDependencyProvider.formSource,
        projectDependencyProvider.formsRepository,
        File(projectDependencyProvider.cacheDir),
        projectDependencyProvider.formsDir,
        FormMetadataParser(),
        analytics
    )
}

private fun serverFormsDetailsFetcher(
    projectDependencyProvider: ProjectDependencyProvider,
    diskFormsSynchronizer: FormsDirDiskFormsSynchronizer
): ServerFormsDetailsFetcher {
    return ServerFormsDetailsFetcher(
        projectDependencyProvider.formsRepository,
        projectDependencyProvider.formSource,
        diskFormsSynchronizer
    )
}

private fun diskFormsSynchronizer(projectDependencyProvider: ProjectDependencyProvider): FormsDirDiskFormsSynchronizer {
    return FormsDirDiskFormsSynchronizer(
        projectDependencyProvider.formsRepository,
        projectDependencyProvider.formsDir
    )
}
