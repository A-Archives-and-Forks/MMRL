package dev.dergoogler.mmrl.compat.impl

import com.topjohnwu.superuser.Shell
import dev.dergoogler.mmrl.compat.content.BulkModule
import dev.dergoogler.mmrl.compat.content.ModuleCompatibility
import dev.dergoogler.mmrl.compat.impl.ksu.KsuNative
import dev.dergoogler.mmrl.compat.stub.IModuleOpsCallback
import dev.dergoogler.mmrl.compat.stub.IShell
import dev.dergoogler.mmrl.compat.stub.IShellCallback

internal class MagiskModuleManagerImpl(
    shell: Shell,
    seLinuxContext: String,
    fileManager: FileManagerImpl,
) : BaseModuleManagerImpl(
    shell = shell,
    seLinuxContext = seLinuxContext,
    fileManager = fileManager
) {
    override fun getManagerName(): String = "Magisk"

    override fun getModuleCompatibility() = ModuleCompatibility(
        hasMagicMount = true,
        canRestoreModules = true
    )

    override fun getVersion(): String = mVersion

    override fun getVersionCode(): Int = mVersionCode

    override fun isSafeMode(): Boolean = false

    override fun getLkmMode(): Int = 0
    override fun isLkmMode(): Boolean = false

    override fun setSuEnabled(enabled: Boolean): Boolean = true
    override fun isSuEnabled(): Boolean = true

    override fun getSuperUserCount(): Int = -1

    override fun enable(id: String, useShell: Boolean, callback: IModuleOpsCallback) {
        val dir = modulesDir.resolve(id)
        if (!dir.exists()) callback.onFailure(id, null)

        runCatching {
            dir.resolve("remove").apply { if (exists()) delete() }
            dir.resolve("disable").apply { if (exists()) delete() }
        }.onSuccess {
            callback.onSuccess(id)
        }.onFailure {
            callback.onFailure(id, it.message)
        }
    }

    override fun disable(id: String, useShell: Boolean, callback: IModuleOpsCallback) {
        val dir = modulesDir.resolve(id)
        if (!dir.exists()) return callback.onFailure(id, null)

        runCatching {
            dir.resolve("remove").apply { if (exists()) delete() }
            dir.resolve("disable").createNewFile()
        }.onSuccess {
            callback.onSuccess(id)
        }.onFailure {
            callback.onFailure(id, it.message)
        }
    }

    override fun remove(id: String, useShell: Boolean, callback: IModuleOpsCallback) {
        val dir = modulesDir.resolve(id)
        if (!dir.exists()) return callback.onFailure(id, null)

        runCatching {
            dir.resolve("disable").apply { if (exists()) delete() }
            dir.resolve("remove").createNewFile()
        }.onSuccess {
            callback.onSuccess(id)
        }.onFailure {
            callback.onFailure(id, it.message)
        }
    }

    override fun action(modId: String, legacy: Boolean, callback: IShellCallback): IShell {
        val cmds = arrayOf(
            "export ASH_STANDALONE=1",
            "export MAGISK=true",
            "export MAGISK_VER=${version}",
            "export MAGISKTMP=$(magisk --path)",
            "export MAGISK_VER_CODE=${versionCode}",
            "busybox sh /data/adb/modules/$modId/action.sh"
        )

        return action(
            cmd = cmds,
            callback = callback
        )
    }

    override fun install(
        path: String,
        bulkModules: List<BulkModule>,
        callback: IShellCallback,
    ): IShell =
        install(
            cmd = "magisk --install-module '${path}'",
            path = path,
            bulkModules = bulkModules,
            callback = callback
        )
}