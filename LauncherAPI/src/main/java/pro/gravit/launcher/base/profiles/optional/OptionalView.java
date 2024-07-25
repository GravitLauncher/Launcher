package pro.gravit.launcher.base.profiles.optional;

import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.optional.actions.OptionalAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
        fixDependencies();
    }

    public OptionalView(ClientProfile profile, OptionalView old) {
        this(profile);
        for(OptionalFile oldFile : old.all) {
            OptionalFile newFile = findByName(oldFile.name);
            if(newFile == null) {
                continue;
            }
            if(old.isEnabled(oldFile)) {
                enable(newFile, old.installInfo.get(oldFile).isManual, (file, status) -> {});
            } else {
                disable(newFile, (file, status) -> {});
            }
        }
        fixDependencies();
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

    public OptionalFile findByName(String name) {
        for(OptionalFile file : all) {
            if(name.equals(file.name)) {
                return file;
            }
        }
        return null;
    }

    public boolean isEnabled(OptionalFile file) {
        return enabled.contains(file);
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

    //Needed if dependency/conflict was added after mod declaring it and clients have their profiles with this mod enabled
    public void fixDependencies() {
        Set<OptionalFile> disabled = all.stream().filter(t -> !isEnabled(t)).collect(Collectors.toSet());
        for (OptionalFile file : disabled) {
            if (file.group != null && file.group.length > 0 && Arrays.stream(file.group).noneMatch(this::isEnabled)) {
                enable(file.group[0], false, null);
            }
        }
        for (OptionalFile file : enabled) {
            if (file.dependencies != null) {
                for (OptionalFile dep : file.dependencies) {
                    enable(dep, false, null);
                }
            }
            if (file.conflict != null) {
                for (OptionalFile conflict : file.conflict) {
                    disable(conflict, null);
                }
            }
            if (file.group != null) {
                for (OptionalFile member : file.group) {
                    disable(member, null);
                }
            }
        }
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
        OptionalFileInstallInfo installInfo = this.installInfo.computeIfAbsent(file, k -> new OptionalFileInstallInfo());
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
        if(file.group != null) {
            for(OptionalFile member : file.group) {
                disable(member, callback);
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
        if (file.group != null && file.group.length != 0) {
            if (Arrays.stream(file.group).noneMatch(this::isEnabled)) {
                enable(file.group[0], false, callback);
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
