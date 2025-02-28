package com.dergoogler.mmrl.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.dergoogler.mmrl.Compat
import com.dergoogler.mmrl.Platform
import com.dergoogler.mmrl.app.Const
import com.dergoogler.mmrl.app.moshi
import com.dergoogler.mmrl.datastore.developerMode
import com.dergoogler.mmrl.repository.LocalRepository
import com.dergoogler.mmrl.repository.ModulesRepository
import com.dergoogler.mmrl.repository.UserPreferencesRepository
import com.topjohnwu.superuser.Shell
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dalvik.system.InMemoryDexClassLoader
import dev.dergoogler.mmrl.compat.ext.isLocalWifiUrl
import dev.dergoogler.mmrl.compat.stub.IFileManager
import dev.dergoogler.mmrl.compat.viewmodel.MMRLViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer

@HiltViewModel(assistedFactory = WebUIViewModel.Factory::class)
class WebUIViewModel @AssistedInject constructor(
    @Assisted val modId: String,
    application: Application,
    localRepository: LocalRepository,
    modulesRepository: ModulesRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : MMRLViewModel(
    application,
    localRepository,
    modulesRepository,
    userPreferencesRepository
) {
    private val userPrefs = runBlocking { userPreferencesRepository.data.first() }

    val isProviderAlive get() = Compat.isAlive

    val versionName: String
        get() = Compat.get("") {
            with(moduleManager) { version }
        }

    val versionCode: Int
        get() = Compat.get(-1) {
            with(moduleManager) { versionCode }
        }

    val fs: IFileManager? = Compat.get(null) {
        fileManager
    }

    val platform: Platform
        get() = Compat.get(Platform.EMPTY) {
            platform
        }

    val moduleDir = "/data/adb/modules/$modId"
    val webRoot = File("$moduleDir/webroot")

    val sanitizedModId: String
        get() {
            return modId.replace(Regex("[^a-zA-Z0-9._]"), "_")
        }

    val sanitizedModIdWithFile
        get(): String {
            return "$${
                when {
                    sanitizedModId.length >= 2 -> sanitizedModId[0].uppercase() + sanitizedModId[1]
                    sanitizedModId.isNotEmpty() -> sanitizedModId[0].uppercase()
                    else -> ""
                }
            }File"
        }

    var dialogRequestAdvancedKernelSUAPI by mutableStateOf(false)
    var dialogRequestFileSystemAPI by mutableStateOf(false)

    fun isDomainSafe(domain: String): Boolean {
        val default = Const.WEBUI_DOMAIN_SAFE_REGEX.matches(domain)
        return userPrefs.developerMode({ useWebUiDevUrl }, default) {
            webUiDevUrl.isLocalWifiUrl()
        }
    }

    val domainUrl
        get(): String {
            val default = "https://mui.kernelsu.org/index.html"
            return userPrefs.developerMode({ useWebUiDevUrl }, default) {
                webUiDevUrl
            }
        }

    val rootShell
        get(): Shell {
            return Compat.createRootShell(
                globalMnt = true,
                devMode = userPrefs.developerMode
            )
        }

    var recomposeCount by mutableIntStateOf(0)
    var hasRequestedAdvancedKernelSUAPI by mutableStateOf(false)
    var hasRequestFileSystemAPI by mutableStateOf(false)

    var topInset by mutableStateOf<Int?>(null)
        private set
    var bottomInset by mutableStateOf<Int?>(null)
        private set
    var leftInset by mutableStateOf<Int?>(null)
        private set
    var rightInset by mutableStateOf<Int?>(null)
        private set

    fun initInsets(density: Density, layoutDirection: LayoutDirection, insets: WindowInsets) {
        topInset = (insets.getTop(density) / density.density).toInt()
        bottomInset = (insets.getBottom(density) / density.density).toInt()
        leftInset = (insets.getLeft(density, layoutDirection) / density.density).toInt()
        rightInset = (insets.getRight(density, layoutDirection) / density.density).toInt()
    }

    @SuppressLint("JavascriptInterface")
    fun loadDexPluginsFromMemory(context: Context, webView: WebView) {
        if (fs == null) {
            Timber.e("IFileManager is null! Plugins not loaded.")
            return
        }

        val pluginsListFile = "/data/adb/modules/$modId/webroot/plugins.json"
        val pluginDir = "/data/adb/modules/$modId/webroot/plugins"

        if (!fs.exists(pluginsListFile)) {
            if (userPrefs.developerMode) Timber.w("plugins.json does not exist! Plugins not loaded.")
            return
        }

        val pluginsListJson = fs.readText(pluginsListFile)

        val jsonAdapter = moshi.adapter<List<String>>(List::class.java)
        val pluginsList: List<String>? = jsonAdapter.fromJson(pluginsListJson)

        if (pluginsList.isNullOrEmpty()) {
            Timber.d("plugins.json for $modId is invalid or empty! Plugins not loaded.")
            return
        }

        if (!fs.exists(pluginDir)) {
            if (userPrefs.developerMode) Timber.i("$modId has no plugins.")
            return
        }

        fs.list(pluginDir, true)
            .filter { it.endsWith(".dex") || it.endsWith(".jar") || it.endsWith(".apk") }
            .forEach { dexPath ->
                try {
                    val dexFileBytes = fs.readBytes(dexPath)
                    val dexFileBuffer = ByteBuffer.wrap(dexFileBytes);

                    val loader = InMemoryDexClassLoader(dexFileBuffer, context.classLoader)

                    pluginsList.forEach { className ->
                        try {
                            val clazz = loader.loadClass(className)

                            val instanceName = clazz.getPluginField<String>("instanceName")
                            val instance = clazz.getPluginMethod<Any>(
                                name = "instance",
                                listOf(Context::class.java, WebView::class.java) to listOf(context, webView),
                                listOf(WebView::class.java, Context::class.java) to listOf(webView, context),
                                listOf(Context::class.java) to listOf(context),
                                listOf(WebView::class.java) to listOf(webView),
                                emptyList<Class<*>>() to emptyList(),
                            )

                            if (instanceName == null) {
                                Timber.e("Class $className does not have an interfaceName field")
                                return
                            }

                            if (instance == null) {
                                Timber.e("Class $className does not have an instance method")
                                return
                            }

                            clazz.setPluginField("isProviderAlive", isProviderAlive)
                            clazz.setPluginField("rootShell", rootShell)
                            clazz.setPluginField("rootVersionName", versionName)
                            clazz.setPluginField("rootVersionCode", versionCode)
                            clazz.setPluginField("fileManager", fs)
                            clazz.setPluginField("rootPlatform", platform)

                            Timber.d("Added plugin $instanceName from dex file $dexPath")

                            webView.addJavascriptInterface(
                                instance,
                                instanceName
                            )
                        } catch (e: ClassNotFoundException) {
                            Timber.e("Class $className not found in dex file $dexPath")
                        } catch (e: Exception) {
                            Timber.e(
                                "Error instantiating class $className from dex file $dexPath",
                                e
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("Error loading plugin from dex file: $dexPath", e)
                }
            }
    }

    private fun Class<*>.setPluginField(name: String, value: Any) {
        try {
            val field = getDeclaredField(name)
            field.isAccessible = true
            field.set(null, value)
        } catch (e: Exception) {
            Timber.w("Failed to set field $name in $modId")
        }
    }

    private inline fun <reified T> Class<*>.getPluginField(name: String, instance: Any): T? =
        try {
            getDeclaredField(name).apply { isAccessible = true }.get(instance) as? T
        } catch (e: Exception) {
            null
        }

    private inline fun <reified T> Class<*>.getPluginMethod(
        name: String,
        parameterTypes: List<Class<*>>,
        args: List<Any>,
    ): T? =
        try {
            getDeclaredMethod(name, *parameterTypes.toTypedArray()).apply { isAccessible = true }
                .invoke(null, *args.toTypedArray()) as? T
        } catch (e: Exception) {
            null
        }

    private inline fun <reified T> Class<*>.getPluginField(name: String): T? = try {
        getDeclaredField(name).apply { isAccessible = true }.get(null) as? T
    } catch (e: Exception) {
        null
    }

    private inline fun <reified T> Class<*>.getPluginMethod(
        name: String,
        vararg parameterSets: Pair<List<Class<*>>, List<Any>>,
    ): T? {
        val method = declaredMethods.find { it.name == name }
            ?: return null.also { Timber.w("Method $name not found in $this") }

        for ((params, args) in parameterSets) {
            try {
                if (method.parameterTypes.contentEquals(params.toTypedArray())) {
                    return method.apply { isAccessible = true }.invoke(null, *args.toTypedArray()) as? T
                }
            } catch (e: Exception) {
                Timber.i("Skipping $name with parameters ${params.joinToString()}: ${e.message}")
            }
        }
        return null
    }


    @AssistedFactory
    interface Factory {
        fun create(
            modId: String,
        ): WebUIViewModel
    }
}


