package dev.dergoogler.mmrl.compat.ext

import android.app.Activity
import android.app.ActivityManager
import android.app.Application.ACTIVITY_SERVICE
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.dergoogler.mmrl.BuildConfig
import com.dergoogler.mmrl.R
import com.topjohnwu.superuser.Shell
import kotlinx.datetime.LocalDateTime
import java.io.File


val Context.tmpDir
    get() = cacheDir.resolve("tmp")
        .apply {
            if (!exists()) mkdirs()
        }

fun Context.shareText(text: String, type: String = "text/plain") {
    ShareCompat.IntentBuilder(this)
        .setType(type)
        .setText(text)
        .startChooser()
}

fun Context.shareFile(file: File, type: String = "text/plain") {
    ShareCompat.IntentBuilder(this)
        .setType(type)
        .addStream(getUriForFile(file))
        .startChooser()
}

fun Context.getUriForFile(file: File): Uri {
    return FileProvider.getUriForFile(
        this,
        "${BuildConfig.APPLICATION_ID}.provider", file
    )
}

val Context.managerVersion
    get(): Pair<String, Long> {
        return try {
            val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val versionName = packageInfo.versionName ?: "Unknown"
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Pair("Unknown", 0)
        }
    }

val Context.seLinuxStatus
    @Composable get(): String {
        val shell = Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR).build("sh")

        val list = ArrayList<String>()
        val result = shell.use {
            it.newJob().add("getenforce").to(list, list).exec()
        }
        val output = result.out.joinToString("\n").trim()

        if (result.isSuccess) {
            return when (output) {
                "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
                "Permissive" -> stringResource(R.string.selinux_status_permissive)
                "Disabled" -> stringResource(R.string.selinux_status_disabled)
                else -> stringResource(R.string.selinux_status_unknown)
            }
        }

        return if (output.endsWith("Permission denied")) {
            stringResource(R.string.selinux_status_enforcing)
        } else {
            stringResource(R.string.selinux_status_unknown)
        }
    }

val Context.logDir get() = cacheDir.resolve("log")

fun Context.deleteLog(name: String) {
    logDir.listFiles().orEmpty()
        .forEach {
            if (it.name.startsWith(name) && it.extension == "log") {
                it.delete()
            }
        }
}

fun Context.createLog(name: String) = logDir
    .resolve("${name}_${LocalDateTime.now()}.log")
    .apply {
        parentFile?.apply { if (!exists()) mkdirs() }
        createNewFile()
    }

fun Context.getLogPath(name: String) = logDir
    .listFiles().orEmpty()
    .find {
        it.name.startsWith(name) && it.extension == "log"
    } ?: createLog(name)

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

val Context.isAppForeground get(): Boolean {
    val mActivityManager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
    val l = mActivityManager.runningAppProcesses

    for (info in l) {
        if (info.uid == this.applicationInfo.uid && info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            return true
        }
    }

    return false
}