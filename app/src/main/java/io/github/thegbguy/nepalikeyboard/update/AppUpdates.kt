package io.github.thegbguy.nepalikeyboard.update

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import io.github.thegbguy.nepalikeyboard.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the UI shows about a Play update. */
sealed interface UpdateState {
    data object None : UpdateState
    /** A flexible update is downloading; [progress] is 0..1, or null before Play reports a size. */
    data class Downloading(val progress: Float?) : UpdateState
    /** Downloaded; installing restarts the app. */
    data object Ready : UpdateState
}

/**
 * Play in-app updates, ported from GPS Simulator. A newer build downloads in the background
 * (flexible) once Play's consent dialog is accepted; releases marked priority 4–5 use the
 * full-screen immediate flow. Checked when the app's Settings opens, never from the keyboard.
 * The flexible prompt is offered once per process so declining isn't nagged on every resume;
 * the immediate flow is offered on every resume, since those releases are required.
 */
class AppUpdates(private val manager: AppUpdateManager, private val fake: FakeAppUpdateManager? = null) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.None)
    val state: StateFlow<UpdateState> = _state
    private var offeredFlexible = false

    private val listener = InstallStateUpdatedListener { install ->
        _state.value = when (install.installStatus()) {
            InstallStatus.PENDING -> UpdateState.Downloading(null)
            InstallStatus.DOWNLOADING -> UpdateState.Downloading(install.totalBytesToDownload().takeIf { it > 0 }?.let { install.bytesDownloaded().toFloat() / it })
            InstallStatus.DOWNLOADED -> UpdateState.Ready
            InstallStatus.INSTALLING -> _state.value
            else -> UpdateState.None // failed, canceled, installed
        }
    }

    fun start() = manager.registerListener(listener)
    fun stop() = manager.unregisterListener(listener)

    /** Call from onResume. Picks up a finished download and offers an update when Play has one. */
    suspend fun check(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = runCatching { manager.requestAppUpdateInfo() }.onFailure { Log.d(TAG, "Update check failed: ${it.message}") }.getOrNull() ?: return
        if (info.installStatus() == InstallStatus.DOWNLOADED) { _state.value = UpdateState.Ready; return }
        val immediate = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        when (info.updateAvailability()) {
            // An immediate update the user left half way; Play expects the app to resume it.
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> if (info.isUpdateTypeAllowed(immediate)) manager.startUpdateFlowForResult(info, launcher, immediate)
            UpdateAvailability.UPDATE_AVAILABLE -> {
                val flexible = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                when {
                    info.updatePriority() >= IMMEDIATE_PRIORITY && info.isUpdateTypeAllowed(immediate) -> manager.startUpdateFlowForResult(info, launcher, immediate)
                    !offeredFlexible && info.isUpdateTypeAllowed(flexible) -> { offeredFlexible = true; manager.startUpdateFlowForResult(info, launcher, flexible) }
                }
            }
        }
    }

    /** Installs the downloaded update; Play restarts the app. */
    fun install() { manager.completeUpdate(); fake?.installCompletes() }

    /** Debug builds only: plays a fake flexible update through every state so the UI can be checked. */
    fun simulateFlexibleUpdate(scope: CoroutineScope, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val fake = fake ?: return
        scope.launch {
            fake.setUpdateAvailable(BuildConfig.VERSION_CODE + 1, AppUpdateType.FLEXIBLE)
            check(launcher)
            delay(500)
            if (fake.isConfirmationDialogVisible) fake.userAcceptsUpdate()
            fake.setTotalBytesToDownload(100)
            fake.downloadStarts()
            // The fake's listener events carry no byte counts (real Play's do), so report progress directly.
            for (bytes in 10L..100L step 10) { delay(400); fake.setBytesDownloaded(bytes); _state.value = UpdateState.Downloading(bytes / 100f) }
            fake.downloadCompletes()
        }
    }

    companion object {
        private const val TAG = "AppUpdates"
        private const val IMMEDIATE_PRIORITY = 4

        /** Real Play updates, or a scripted fake in debug builds when [fakeUpdate] is set. */
        fun create(context: Context, fakeUpdate: Boolean): AppUpdates =
            if (BuildConfig.DEBUG && fakeUpdate) FakeAppUpdateManager(context).let { AppUpdates(it, it) }
            else AppUpdates(AppUpdateManagerFactory.create(context))
    }
}
