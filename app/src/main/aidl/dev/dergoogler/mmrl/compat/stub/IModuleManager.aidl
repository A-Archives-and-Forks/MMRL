package dev.dergoogler.mmrl.compat.stub;

import dev.dergoogler.mmrl.compat.content.LocalModule;
import dev.dergoogler.mmrl.compat.content.ModuleCompatibility;
import dev.dergoogler.mmrl.compat.content.BulkModule;
import dev.dergoogler.mmrl.compat.content.NullableBoolean;
import dev.dergoogler.mmrl.compat.stub.IShellCallback;
import dev.dergoogler.mmrl.compat.stub.IModuleOpsCallback;
import dev.dergoogler.mmrl.compat.stub.IShell;

interface IModuleManager {
    String getManagerName();
    String getVersion();
    int getVersionCode();
    List<LocalModule> getModules();
    ModuleCompatibility getModuleCompatibility();
    String getSeLinuxContext();
    LocalModule getModuleById(String id);
    LocalModule getModuleInfo(String zipPath);
    IShell getShell(in List<String> command, in LocalModule module, IShellCallback callback);
    oneway void reboot(String reason);
    oneway void enable(String id, boolean useShell, IModuleOpsCallback callback);
    oneway void disable(String id, boolean useShell, IModuleOpsCallback callback);
    oneway void remove(String id, boolean useShell, IModuleOpsCallback callback);
    IShell install(String path, in List<BulkModule> bulkModule, IShellCallback callback);
    IShell action(String modId, boolean legacy, IShellCallback callback);

    // General
    int getSuperUserCount();

    // KernelSU (Next) related
    NullableBoolean isLkmMode();
    boolean isSafeMode();
    boolean setSuEnabled(boolean enabled);
    boolean isSuEnabled();
    boolean uidShouldUmount(int uid);
}