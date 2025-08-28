package pro.gravit.launcher.runtime.backend;

import oshi.SystemInfo;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.OptionalFile;
import pro.gravit.launcher.base.profiles.optional.OptionalView;
import pro.gravit.launcher.core.LauncherNetworkAPI;
import pro.gravit.launcher.core.api.features.ProfileFeatureAPI;
import pro.gravit.launcher.core.backend.LauncherBackendAPI;
import pro.gravit.launcher.runtime.utils.SystemMemory;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.JavaHelper;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class ProfileSettingsImpl implements LauncherBackendAPI.ClientProfileSettings {
    transient ClientProfile profile;
    transient LauncherBackendImpl backend;
    @LauncherNetworkAPI
    private Map<MemoryClass, Long> ram;
    @LauncherNetworkAPI
    private Set<Flag> flags;
    @LauncherNetworkAPI
    private Set<String> enabled;
    @LauncherNetworkAPI
    private String saveJavaPath;
    transient OptionalView view;
    transient volatile JavaHelper.JavaVersion selectedJava;

    public ProfileSettingsImpl() {
    }

    public ProfileSettingsImpl(ClientProfile profile) {
        this.profile = profile;
        var def = profile.getSettings();
        this.ram = new HashMap<>();
        this.ram.put(MemoryClass.TOTAL, ((long)def.ram) << 20);
        this.flags = new HashSet<>();
        if(def.autoEnter) {
            this.flags.add(Flag.AUTO_ENTER);
        }
        if(def.fullScreen) {
            this.flags.add(Flag.FULLSCREEN);
        }
        this.view = new OptionalView(profile);
    }

    @Override
    public long getReservedMemoryBytes(MemoryClass memoryClass) {
        return ram.getOrDefault(memoryClass, 0L);
    }

    @Override
    public long getMaxMemoryBytes(MemoryClass memoryClass) {
        try {
            return SystemMemory.getPhysicalMemorySize();
        } catch (Throwable e) {
            SystemInfo systemInfo = new SystemInfo();
            return systemInfo.getHardware().getMemory().getTotal();
        }
    }

    @Override
    public void setReservedMemoryBytes(MemoryClass memoryClass, long value) {
        this.ram.put(memoryClass, value);
    }

    @Override
    public Set<Flag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    @Override
    public Set<Flag> getAvailableFlags() {
        Set<Flag> set = new HashSet<>();
        set.add(Flag.AUTO_ENTER);
        set.add(Flag.FULLSCREEN);
        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            set.add(Flag.LINUX_WAYLAND_SUPPORT);
        }
        if(backend.hasPermission("launcher.debug.skipfilemonitor")) {
            set.add(Flag.DEBUG_SKIP_FILE_MONITOR);
        }
        return set;
    }

    @Override
    public boolean hasFlag(Flag flag) {
        return flags.contains(flag);
    }

    @Override
    public void addFlag(Flag flag) {
        flags.add(flag);
    }

    @Override
    public void removeFlag(Flag flag) {
        flags.remove(flag);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Set<ProfileFeatureAPI.OptionalMod> getAllOptionals() {
        return (Set) view.all;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Set<ProfileFeatureAPI.OptionalMod> getEnabledOptionals() {
        return (Set) view.enabled;
    }

    @Override
    public void enableOptional(ProfileFeatureAPI.OptionalMod mod, ChangedOptionalStatusCallback callback) {
        view.enable((OptionalFile) mod, true, callback::onChanged);
    }

    @Override
    public void disableOptional(ProfileFeatureAPI.OptionalMod mod, ChangedOptionalStatusCallback callback) {
        view.disable((OptionalFile) mod, callback::onChanged);
    }

    @Override
    public JavaHelper.JavaVersion getSelectedJava() {
        if(selectedJava == null) {
            selectedJava = getRecommendedJava();
        }
        return selectedJava;
    }

    @Override
    public JavaHelper.JavaVersion getRecommendedJava() {
        JavaHelper.JavaVersion result = null;
        try {
            for(var java : backend.getAvailableJava().get()) {
                if(isRecommended(java)) {
                    return (JavaHelper.JavaVersion) java;
                }
                if(isCompatible(java)) {
                    if(result == null) {
                        result = (JavaHelper.JavaVersion) java;
                        continue;
                    }
                    if(result.getMajorVersion() < java.getMajorVersion()) {
                        result = (JavaHelper.JavaVersion) java;
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void setSelectedJava(LauncherBackendAPI.Java java) {
        selectedJava = (JavaHelper.JavaVersion) java;
    }

    @Override
    public boolean isRecommended(LauncherBackendAPI.Java java) {
        return java.getMajorVersion() == profile.getRecommendJavaVersion();
    }

    @Override
    public boolean isCompatible(LauncherBackendAPI.Java java) {
        return java.getMajorVersion() >= profile.getMinJavaVersion() && java.getMajorVersion() <= profile.getMaxJavaVersion();
    }

    @Override
    public ProfileSettingsImpl copy() {
        ProfileSettingsImpl cloned = new ProfileSettingsImpl();
        cloned.backend = backend;
        cloned.profile = profile;
        cloned.ram = new HashMap<>(ram);
        cloned.flags = new HashSet<>(flags);
        cloned.enabled = new HashSet<>(enabled);
        if(view != null) {
            cloned.view = new OptionalView(profile, view);
        }
        cloned.selectedJava = selectedJava;
        cloned.saveJavaPath = saveJavaPath;
        return cloned;
    }

    public void updateEnabledMods() {
        enabled = new HashSet<>();
        for(var e : view.enabled) {
            enabled.add(e.name);
        }
        if(selectedJava != null) {
            saveJavaPath = selectedJava.getPath().toAbsolutePath().toString();
        }
    }

    public void initAfterGson(ClientProfile profile, LauncherBackendImpl backend) {
        this.backend = backend;
        this.profile = profile;
        this.view = new OptionalView(profile);
        for(var e : enabled) {
            var opt = profile.getOptionalFile(e);
            if(opt == null) {
                continue;
            }
            enableOptional(opt, (var1, var2) -> {});
        }
        if(this.saveJavaPath != null) {
            backend.getAvailableJava().thenAccept((javas) -> {
                for(var java : javas) {
                    if(!isCompatible(java)) {
                        continue;
                    }
                    if(java.getPath() == null) {
                        continue;
                    }
                    if(java.getPath().toAbsolutePath().toString().equals(this.saveJavaPath)) {
                        this.selectedJava = (JavaHelper.JavaVersion) java;
                        return;
                    }
                }
            });
        }
    }
}
