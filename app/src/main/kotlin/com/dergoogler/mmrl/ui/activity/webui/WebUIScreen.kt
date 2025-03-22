package com.dergoogler.mmrl.ui.activity.webui

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import com.dergoogler.mmrl.R
import com.dergoogler.mmrl.datastore.model.developerMode
import com.dergoogler.mmrl.ui.activity.webui.handlers.MMRLWebClient
import com.dergoogler.mmrl.ui.activity.webui.handlers.MMRLWebUIHandler
import com.dergoogler.mmrl.ui.activity.webui.handlers.SuFilePathHandler
import com.dergoogler.mmrl.ui.activity.webui.interfaces.ksu.AdvancedKernelSUAPI
import com.dergoogler.mmrl.ui.activity.webui.interfaces.ksu.BaseKernelSUAPI
import com.dergoogler.mmrl.ui.activity.webui.interfaces.mmrl.FileInterface
import com.dergoogler.mmrl.ui.activity.webui.interfaces.mmrl.MMRLInterface
import com.dergoogler.mmrl.ui.activity.webui.interfaces.mmrl.VersionInterface
import com.dergoogler.mmrl.ui.component.ConfirmDialog
import com.dergoogler.mmrl.ui.component.Loading
import com.dergoogler.mmrl.ui.providable.LocalUserPreferences
import com.dergoogler.mmrl.viewmodel.SettingsViewModel
import com.dergoogler.mmrl.viewmodel.WebUIViewModel
import dev.dergoogler.mmrl.compat.core.MMRLUriHandlerImpl
import timber.log.Timber


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebUIScreen(
    viewModel: WebUIViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val userPrefs = LocalUserPreferences.current
    val density = LocalDensity.current
    val browser = LocalUriHandler.current as MMRLUriHandlerImpl
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val filledTonalButtonColors = ButtonDefaults.filledTonalButtonColors()
    val cardColors = CardDefaults.cardColors()
    val isDarkMode = userPrefs.isDarkMode()
    val layoutDirection = LocalLayoutDirection.current

    val webView = WebView(context)
    WebView.setWebContentsDebuggingEnabled(userPrefs.developerMode)

    val insets = WindowInsets.systemBars
    LaunchedEffect(density, layoutDirection, insets) {
        viewModel.initInsets(density, layoutDirection, insets)
        Timber.d("Insets calculated: top = ${viewModel.topInset}, bottom = ${viewModel.bottomInset}, left = ${viewModel.leftInset}, right = ${viewModel.rightInset}")
    }

    val allowedFsApi = viewModel.modId in userPrefs.allowedFsModules
    val allowedKsuApi = viewModel.modId in userPrefs.allowedKsuModules

    if (!allowedKsuApi && !viewModel.hasRequestedAdvancedKernelSUAPI && viewModel.dialogRequestAdvancedKernelSUAPI) {
        ConfirmDialog(
            title = stringResource(R.string.allow_advanced_kernelsu_api),
            description = stringResource(R.string.allow_advanced_kernelsu_api_desc),
            onClose = {
                viewModel.hasRequestedAdvancedKernelSUAPI = true
                viewModel.dialogRequestAdvancedKernelSUAPI = false
            },
            onConfirm = {
                viewModel.dialogRequestAdvancedKernelSUAPI = false
                val newModules = userPrefs.allowedKsuModules + viewModel.modId
                settingsViewModel.setAllowedKsuModules(newModules)
                viewModel.recomposeCount++
            }
        )
    }

    if (viewModel.config.hasFileSystemPermission && !allowedFsApi && !viewModel.hasRequestFileSystemAPI && viewModel.dialogRequestFileSystemAPI) {
        ConfirmDialog(
            title = stringResource(R.string.allow_filesystem_api),
            description = stringResource(R.string.allow_filesystem_api_desc),
            onClose = {
                viewModel.hasRequestFileSystemAPI = true
                viewModel.dialogRequestFileSystemAPI = false
            },
            onConfirm = {
                viewModel.dialogRequestFileSystemAPI = false
                val newModules = userPrefs.allowedFsModules + viewModel.modId
                settingsViewModel.setAllowedFsModules(newModules)
                viewModel.recomposeCount++
            }
        )
    }

    if (viewModel.topInset != null && viewModel.bottomInset != null) {
        val webViewAssetLoader = remember(viewModel.topInset, viewModel.bottomInset) {
            WebViewAssetLoader.Builder()
                .setDomain("mui.kernelsu.org")
                .addPathHandler(
                    "/",
                    SuFilePathHandler(
                        directory = viewModel.webRoot,
                    )
                )
                .addPathHandler(
                    "/mmrl/assets/",
                    WebViewAssetLoader.AssetsPathHandler(context)
                )
                .addPathHandler(
                    "/mmrl/",
                    MMRLWebUIHandler(
                        viewModel = viewModel,
                        colorScheme = colorScheme,
                        typography = typography,
                        filledTonalButtonColors = filledTonalButtonColors,
                        cardColors = cardColors
                    )
                )
                .build()
        }

        key(viewModel.recomposeCount) {
            AndroidView(
                factory = {
                    webView.apply {
                        setBackgroundColor(colorScheme.background.toArgb())
                        background = ColorDrawable(colorScheme.background.toArgb())
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ ->
                            WindowInsetsCompat.CONSUMED
                        }

                        if (viewModel.config.hasPluginDexLoaderPermission) {
                            viewModel.loadDexPluginsFromMemory(context, this)
                        }
                        
                        webViewClient = MMRLWebClient(
                            context = context,
                            browser = browser,
                            webViewAssetLoader = webViewAssetLoader,
                            userPrefs = userPrefs,
                            viewModel = viewModel,
                        )

                        addJavascriptInterface(
                            VersionInterface(
                                context = context,
                                webView = this,
                                viewModel = viewModel,
                            ), "mmrl"
                        )
                    }
                },
                update = {
                    it.apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            userPrefs.developerMode({ useWebUiDevUrl }) {
                                mixedContentMode =
                                    android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            userAgentString = "DON'T TRACK ME DOWN MOTHERFUCKER!"
                        }

                        addJavascriptInterface(
                            MMRLInterface(
                                viewModel = viewModel,
                                context = context,
                                isDark = isDarkMode,
                                webView = this,
                                allowedFsApi = allowedFsApi,
                                allowedKsuApi = allowedKsuApi
                            ), "$${viewModel.sanitizedModId}"
                        )

                        addJavascriptInterface(
                            if (allowedKsuApi) {
                                AdvancedKernelSUAPI(context, this, userPrefs)
                            } else {
                                BaseKernelSUAPI(context, this)
                            }, "ksu"
                        )

                        if (viewModel.config.hasFileSystemPermission && allowedFsApi) {
                            addJavascriptInterface(
                                FileInterface(this, context),
                                viewModel.sanitizedModIdWithFile
                            )
                        }

                        loadUrl(viewModel.domainUrl)
                    }
                }
            )
        }
    } else {
        Loading()
    }
}
