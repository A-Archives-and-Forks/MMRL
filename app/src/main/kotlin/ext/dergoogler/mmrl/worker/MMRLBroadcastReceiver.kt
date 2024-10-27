package ext.dergoogler.mmrl.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dergoogler.mmrl.datastore.UserPreferencesCompat
import com.dergoogler.mmrl.repository.LocalRepository
import com.dergoogler.mmrl.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
open class MMRLBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var localRepository: LocalRepository

    override fun onReceive(context: Context, intent: Intent) {
        synchronized(lock) {
            CoroutineScope(Dispatchers.Main).launch {
                if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
                    onBooted(context, intent)
                }
            }
        }
    }

    /**
     * Called when the device is booted. Called inside of a `CoroutineScope`
     */
    open suspend fun onBooted(context: Context, intent: Intent) {}


    companion object {
        val lock = Any()

        inline fun <reified W : MMRLCoroutineWorker> startWorkTask(
            context: Context,
            enabled: Boolean,
            repeatInterval: Int,
            repeatIntervalUnit: TimeUnit = TimeUnit.HOURS,
            existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
            workName: String,
        ) {
            W::class.java

            val workManager = WorkManager.getInstance(context)
            if (enabled) {

                Timber.d("Starting work task: $workName")

                val updateRequest = PeriodicWorkRequestBuilder<W>(
                    repeatInterval.toLong(),
                    repeatIntervalUnit
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .setRequiresDeviceIdle(false)
                            .setRequiresCharging(false)
                            .build()
                    )
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    workName,
                    existingPeriodicWorkPolicy,
                    updateRequest
                )
            } else {
                workManager.cancelUniqueWork(workName)
            }
        }

        fun cancelWorkTask(context: Context, workName: String) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(workName)
        }
    }
}