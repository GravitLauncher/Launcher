package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OptionalView {
    public Set<OptionalFile> enabled = new HashSet<>();
    public Map<OptionalFile, Set<OptionalFile>> dependenciesCountMap = new HashMap<>();
    public Set<OptionalFile> all;

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

    public void enable(OptionalFile file) {
        if (enabled.contains(file)) return;
        enabled.add(file);
        file.watchEvent(true);
        if (file.dependencies != null) {
            for (OptionalFile dep : file.dependencies) {
                Set<OptionalFile> dependenciesCount = dependenciesCountMap.computeIfAbsent(dep, k -> new HashSet<>());
                dependenciesCount.add(file);
                enable(dep);
            }
        }
        if (file.conflict != null) {
            for (OptionalFile conflict : file.conflict) {
                disable(conflict);
            }
        }
    }

    public void disable(OptionalFile file) {
        if (!enabled.remove(file)) return;
        file.watchEvent(false);
        Set<OptionalFile> dependenciesCount = dependenciesCountMap.get(file);
        if (dependenciesCount != null) {
            for (OptionalFile f : dependenciesCount) {
                if (f.isPreset) continue;
                disable(f);
            }
            dependenciesCount.clear();
        }
        if (file.dependencies != null) {
            for (OptionalFile f : file.dependencies) {
                if (!enabled.contains(f)) continue;
                dependenciesCount = dependenciesCountMap.get(f);
                if (dependenciesCount == null) {
                    disable(f);
                } else if (dependenciesCount.size() <= 1) {
                    dependenciesCount.clear();
                    disable(f);
                }
            }
        }
    }

    public OptionalView(ClientProfile profile) {
        this.all = profile.getOptional();
        for (OptionalFile f : this.all) {
            if (f.mark) enable(f);
        }
    }

    public OptionalView(OptionalView view) {
        this.enabled = new HashSet<>(view.enabled);
        this.dependenciesCountMap = new HashMap<>(view.dependenciesCountMap);
        this.all = view.all;
    }
}
