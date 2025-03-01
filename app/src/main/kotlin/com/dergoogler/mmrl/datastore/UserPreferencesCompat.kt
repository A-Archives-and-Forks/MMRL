package com.dergoogler.mmrl.datastore

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.dergoogler.mmrl.app.Const
import com.dergoogler.mmrl.datastore.modules.ModulesMenuCompat
import com.dergoogler.mmrl.datastore.repositories.RepositoriesMenuCompat
import com.dergoogler.mmrl.datastore.repository.RepositoryMenuCompat
import com.dergoogler.mmrl.ui.navigation.MainScreen
import com.dergoogler.mmrl.ui.theme.Colors
import dev.dergoogler.mmrl.compat.BuildCompat
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class UserPreferencesCompat(
    val workingMode: WorkingMode,
    val darkMode: DarkMode,
    val themeColor: Int,
    val deleteZipFile: Boolean,
    val useDoh: Boolean,
    val downloadPath: File,
    val confirmReboot: Boolean,
    val terminalTextWrap: Boolean,
    val datePattern: String,
    val autoUpdateRepos: Boolean,
    val autoUpdateReposInterval: Int,
    val checkModuleUpdates: Boolean,
    val checkModuleUpdatesInterval: Int,
    val checkAppUpdates: Boolean,
    val checkAppUpdatesPreReleases: Boolean,
    val hideFingerprintInHome: Boolean,
    val homepage: String,
    val webUiDevUrl: String,
    val developerMode: Boolean,
    val useWebUiDevUrl: Boolean,
    val useShellForModuleStateChange: Boolean,
    val useShellForModuleAction: Boolean,
    val webuiAllowRestrictedPaths: Boolean,
    val clearInstallTerminal: Boolean,
    val allowCancelInstall: Boolean,
    val allowCancelAction: Boolean,
    val blacklistAlerts: Boolean,
    val injectEruda: List<String>,
    val allowedFsModules: List<String>,
    val allowedKsuModules: List<String>,
    val repositoryMenu: RepositoryMenuCompat,
    val repositoriesMenu: RepositoriesMenuCompat,
    val modulesMenu: ModulesMenuCompat,
) {
    constructor(original: UserPreferences) : this(
        workingMode = original.workingMode,
        darkMode = original.darkMode,
        themeColor = original.themeColor,
        deleteZipFile = original.deleteZipFile,
        useDoh = original.useDoh,
        downloadPath = original.downloadPath.ifEmpty { Const.PUBLIC_DOWNLOADS.absolutePath }
            .let(::File),
        confirmReboot = original.confirmReboot,
        terminalTextWrap = original.terminalTextWrap,
        datePattern = original.datePattern,
        autoUpdateRepos = original.autoUpdateRepos,
        autoUpdateReposInterval = original.autoUpdateReposInterval,
        checkModuleUpdates = original.checkModuleUpdates,
        checkModuleUpdatesInterval = original.checkModuleUpdatesInterval,
        checkAppUpdates = original.checkAppUpdates,
        checkAppUpdatesPreReleases = original.checkAppUpdatesPreReleases,
        hideFingerprintInHome = original.hideFingerprintInHome,
        homepage = original.homepage,
        webUiDevUrl = original.webUiDevUrl,
        developerMode = original.developerMode,
        useWebUiDevUrl = original.useWebUiDevUrl,
        useShellForModuleStateChange = original.useShellForModuleStateChange,
        useShellForModuleAction = original.useShellForModuleAction,
        webuiAllowRestrictedPaths = original.webuiAllowRestrictedPaths,
        clearInstallTerminal = original.clearInstallTerminal,
        allowCancelInstall = original.allowCancelInstall,
        allowCancelAction = original.allowCancelAction,
        blacklistAlerts = original.blacklistAlerts,
        injectEruda = original.injectEruda.split(","),
        allowedFsModules = original.allowedFsModules.split(","),
        allowedKsuModules = original.allowedKsuModules.split(","),
        repositoryMenu = when {
            original.hasRepositoryMenu() -> RepositoryMenuCompat(original.repositoryMenu)
            else -> RepositoryMenuCompat.default()
        },
        repositoriesMenu = when {
            original.hasRepositoriesMenu() -> RepositoriesMenuCompat(original.repositoriesMenu)
            else -> RepositoriesMenuCompat.default()
        },
        modulesMenu = when {
            original.hasModulesMenu() -> ModulesMenuCompat(original.modulesMenu)
            else -> ModulesMenuCompat.default()
        })

    @Composable
    fun isDarkMode() = when (darkMode) {
        DarkMode.ALWAYS_OFF -> false
        DarkMode.ALWAYS_ON -> true
        else -> isSystemInDarkTheme()
    }

    fun toProto(): UserPreferences =
        UserPreferences.newBuilder()
            .setWorkingMode(workingMode)
            .setDarkMode(darkMode)
            .setThemeColor(themeColor).setDeleteZipFile(deleteZipFile)
            .setUseDoh(useDoh)
            .setDownloadPath(downloadPath.path)
            .setConfirmReboot(confirmReboot)
            .setTerminalTextWrap(terminalTextWrap)
            .setDatePattern(datePattern)
            .setAutoUpdateRepos(autoUpdateRepos)
            .setAutoUpdateReposInterval(autoUpdateReposInterval)
            .setCheckModuleUpdates(checkModuleUpdates)
            .setCheckModuleUpdatesInterval(checkModuleUpdatesInterval)
            .setCheckAppUpdates(checkAppUpdates)
            .setCheckAppUpdatesPreReleases(checkAppUpdatesPreReleases)
            .setHideFingerprintInHome(hideFingerprintInHome)
            .setHomepage(homepage)
            .setWebUiDevUrl(webUiDevUrl)
            .setDeveloperMode(developerMode)
            .setUseWebUiDevUrl(useWebUiDevUrl)
            .setUseShellForModuleStateChange(useShellForModuleStateChange)
            .setUseShellForModuleAction(useShellForModuleAction)
            .setWebuiAllowRestrictedPaths(webuiAllowRestrictedPaths)
            .setBlacklistAlerts(blacklistAlerts)
            .setInjectEruda(injectEruda.joinToString(","))
            .setClearInstallTerminal(clearInstallTerminal)
            .setAllowCancelInstall(allowCancelInstall)
            .setAllowCancelAction(allowCancelAction)
            .setAllowedFsModules(allowedFsModules.joinToString(","))
            .setAllowedKsuModules(allowedKsuModules.joinToString(","))
            .setRepositoryMenu(repositoryMenu.toProto())
            .setRepositoriesMenu(repositoriesMenu.toProto())
            .setModulesMenu(modulesMenu.toProto())
            .build()

    companion object {
        fun default() = UserPreferencesCompat(
            workingMode = WorkingMode.FIRST_SETUP,
            darkMode = DarkMode.FOLLOW_SYSTEM,
            themeColor = if (BuildCompat.atLeastS) Colors.Dynamic.id else Colors.Pourville.id,
            deleteZipFile = false,
            useDoh = false,
            downloadPath = Const.PUBLIC_DOWNLOADS,
            confirmReboot = true,
            terminalTextWrap = false,
            datePattern = "d MMMM yyyy",
            autoUpdateRepos = true,
            autoUpdateReposInterval = 6,
            checkModuleUpdates = true,
            checkModuleUpdatesInterval = 2,
            checkAppUpdates = true,
            checkAppUpdatesPreReleases = false,
            hideFingerprintInHome = true,
            homepage = MainScreen.Home.route,
            webUiDevUrl = "http://0.0.0.0:8080",
            developerMode = false,
            useWebUiDevUrl = false,
            clearInstallTerminal = true,
            allowCancelInstall = false,
            allowCancelAction = false,
            useShellForModuleStateChange = true,
            useShellForModuleAction = true,
            webuiAllowRestrictedPaths = false,
            blacklistAlerts = true,
            injectEruda = emptyList(),
            allowedFsModules = emptyList(),
            allowedKsuModules = emptyList(),
            repositoriesMenu = RepositoriesMenuCompat.default(),
            repositoryMenu = RepositoryMenuCompat.default(),
            modulesMenu = ModulesMenuCompat.default()
        )

        val WorkingMode.isRoot: Boolean
            get() {
                return this == WorkingMode.MODE_MAGISK || this == WorkingMode.MODE_KERNEL_SU || this == WorkingMode.MODE_KERNEL_SU_NEXT || this == WorkingMode.MODE_APATCH || this == WorkingMode.MODE_SHIZUKU
            }

        val WorkingMode.isNonRoot: Boolean
            get() {
                return this == WorkingMode.MODE_NON_ROOT
            }

        val WorkingMode.isSetup: Boolean
            get() {
                return this == WorkingMode.FIRST_SETUP
            }
    }
}


@OptIn(ExperimentalContracts::class)
inline fun <R> UserPreferencesCompat.developerMode(block: UserPreferencesCompat.() -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (developerMode) block() else null
}

@OptIn(ExperimentalContracts::class)
inline fun <R> UserPreferencesCompat.developerMode(
    also: UserPreferencesCompat.() -> Boolean,
    block: UserPreferencesCompat.() -> R,
): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (developerMode && also()) block() else null
}

@OptIn(ExperimentalContracts::class)
inline fun UserPreferencesCompat.developerMode(
    also: UserPreferencesCompat.() -> Boolean
): Boolean {
    contract {
        callsInPlace(also, InvocationKind.AT_MOST_ONCE)
    }

    return developerMode && also()
}

@OptIn(ExperimentalContracts::class)
inline fun <R> UserPreferencesCompat.developerMode(
    also: UserPreferencesCompat.() -> Boolean,
    default: R,
    block: UserPreferencesCompat.() -> R,
): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return if (developerMode && also()) block() else default
}
