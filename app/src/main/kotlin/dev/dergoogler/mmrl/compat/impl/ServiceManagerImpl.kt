package dev.dergoogler.mmrl.compat.impl

import android.content.Context
import android.os.SELinux
import android.system.Os
import com.dergoogler.mmrl.Compat
import com.dergoogler.mmrl.app.Const
import com.dergoogler.mmrl.datastore.WorkingMode
import dev.dergoogler.mmrl.compat.core.BrickException
import dev.dergoogler.mmrl.compat.stub.IFileManager
import dev.dergoogler.mmrl.compat.stub.IKsuService
import dev.dergoogler.mmrl.compat.stub.IModuleManager
import dev.dergoogler.mmrl.compat.stub.IServiceManager
import kotlin.system.exitProcess

const val HELP_MESSAGE =
    """Try to remove root permission from MMRL, close MMRL, give again permissions and try again. If this issue persists please report this to our GitHub [issues](${Const.GITHUB_ISSUES_URL}) not on Google Play reviews!
**Required is following**
- Device specs
- Root provider
- Logs *(you can press the button at the end of the page to copy)*
- A way to reproduce the issue"""

internal class ServiceManagerImpl(
    private val context: Context,
    private val mode: WorkingMode,
) : IServiceManager.Stub() {
    private val main by lazy { Compat.ServiceShell }

    private val platform by lazy {
        when (mode) {
            WorkingMode.MODE_MAGISK -> Platform.Magisk
            WorkingMode.MODE_KERNEL_SU -> Platform.KernelSU
            WorkingMode.MODE_KERNEL_SU_NEXT -> Platform.KsuNext
            WorkingMode.MODE_APATCH -> Platform.APatch
            WorkingMode.MODE_NON_ROOT -> Platform.NonRoot
            else -> throw BrickException(
                "unsupported platform: $seLinuxContext",
                HELP_MESSAGE.trimIndent()
            )
        }
    }

    private val fileManager by lazy {
        FileManagerImpl()
    }

    private val ksuService by lazy {
        KsuServiceImpl(
            context = context
        )
    }

    private val moduleManager by lazy {
        when (platform) {
            Platform.Magisk -> MagiskModuleManagerImpl(
                shell = main,
                seLinuxContext = seLinuxContext,
                fileManager = fileManager
            )

            Platform.KernelSU -> KernelSUModuleManagerImpl(
                shell = main,
                seLinuxContext = seLinuxContext,
                fileManager = fileManager
            )

            Platform.KsuNext -> KsuNextModuleManagerImpl(
                shell = main,
                seLinuxContext = seLinuxContext,
                fileManager = fileManager
            )

            Platform.APatch -> APatchModuleManagerImpl(
                shell = main,
                seLinuxContext = seLinuxContext,
                fileManager = fileManager
            )

            else -> throw BrickException(
                "unsupported platform: $seLinuxContext",
                HELP_MESSAGE.trimIndent()
            )
        }
    }

    override fun getUid(): Int {
        return Os.getuid()
    }

    override fun getPid(): Int {
        return Os.getpid()
    }

    override fun getSELinuxContext(): String {
        return SELinux.getContext()
    }

    override fun currentPlatform(): String {
        return platform.name.lowercase()
    }

    override fun getModuleManager(): IModuleManager {
        return moduleManager
    }

    override fun getKsuService(): IKsuService {
        return ksuService
    }

    override fun getFileManager(): IFileManager {
        return fileManager
    }

    override fun destroy() {
        exitProcess(0)
    }
}