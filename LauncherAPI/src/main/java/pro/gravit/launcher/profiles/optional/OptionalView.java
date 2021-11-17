package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class OptionalView {
    public Set<OptionalFile> enabled = new HashSet<>();
    public Map<OptionalFile, OptionalFileInstallInfo> installInfo = new HashMap<>();
    public Set<OptionalFile> all;

    public OptionalView(ClientProfile profile) {
        this.all = profile.getOptional();
        for (OptionalFile f : this.all) {
            if (f.mark) enable(f, true, null);
        }
    }

    public OptionalView(OptionalView view) {
        this.enabled = new HashSet<>(view.enabled);
        this.installInfo = new HashMap<>(view.installInfo);
        this.all = view.all;
    }

    @SuppressWarnings("unchecked")
    public <T extends OptionalAction> Set<T> getActionsByClass(Class<T> clazz) {
        Set<T> results = new HashSet<>();
        for (OptionalFile e : enabled) {
            if (e.actions != null) {
                for (OptionalAction a : e.actions) {
                    if (clazz.isAssignableFrom(a.getClass())) {
                        results.add((T) a);
                    }
                }
            }
        }
        return results;
    }

    public Set<OptionalAction> getEnabledActions() {
        Set<OptionalAction> results = new HashSet<>();
        for (OptionalFile e : enabled) {
            if (e.actions != null) {
                results.addAll(e.actions);
            }
        }
        return results;
    }

    public Set<OptionalAction> getDisabledActions() {
        Set<OptionalAction> results = new HashSet<>();
        for (OptionalFile e : all) {
            if (enabled.contains(e)) continue;
            if (e.actions != null) {
                results.addAll(e.actions);
            }
        }
        return results;
    }

    public void enable(OptionalFile file, boolean manual, BiConsumer<OptionalFile, Boolean> callback) {
        if (enabled.contains(file)) return;
        enabled.add(file);
        if (callback != null) callback.accept(file, true);
        OptionalFileInstallInfo installInfo = this.installInfo.get(file);
        if (installInfo == null) {
            installInfo = new OptionalFileInstallInfo();
            this.installInfo.put(file, installInfo);
        }
        installInfo.isManual = manual;
        if (file.dependencies != null) {
            for (OptionalFile dep : file.dependencies) {
                enable(dep, false, callback);
            }
        }
        if (file.conflict != null) {
            for (OptionalFile conflict : file.conflict) {
                disable(conflict, callback);
            }
        }
    }

    public void disable(OptionalFile file, BiConsumer<OptionalFile, Boolean> callback) {
        if (!enabled.remove(file)) return;
        if (callback != null) callback.accept(file, false);
        for (OptionalFile dep : all) {
            if (dep.dependencies != null && contains(file, dep.dependencies)) {
                disable(dep, callback);
            }
        }
        if (file.dependencies != null) {
            for (OptionalFile dep : file.dependencies) {
                OptionalFileInstallInfo installInfo = this.installInfo.get(dep);
                if (installInfo != null && !installInfo.isManual) {
                    disable(file, callback);
                }
            }
        }
    }

    private boolean contains(OptionalFile file, OptionalFile[] array) {
        for (OptionalFile e : array) {
            if (e == file) {
                return true;
            }
        }
        return false;
    }

    public static class OptionalFileInstallInfo {
        public boolean isManual;
    }
}
