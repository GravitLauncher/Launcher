package pro.gravit.launcher.profiles.optional;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.actions.OptionalAction;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class OptionalView {
    public Set<OptionalFile> enabled = new HashSet<>();
    public Set<OptionalFile> all;

    @SuppressWarnings("unchecked")
    public<T extends OptionalAction> Set<T> getActionsByClass(Class<T> clazz)
    {
        Set<T> results = new HashSet<>();
        for(OptionalFile e : enabled)
        {
            if(e.actions != null)
            {
                for(OptionalAction a : e.actions)
                {
                    if(clazz.isAssignableFrom(a.getClass()))
                    {
                        results.add((T) a);
                    }
                }
            }
        }
        return results;
    }

    public Set<OptionalAction> getEnabledActions()
    {
        Set<OptionalAction> results = new HashSet<>();
        for(OptionalFile e : enabled)
        {
            if(e.actions != null)
            {
                results.addAll(e.actions);
            }
        }
        return results;
    }

    public Set<OptionalAction> getDisabledActions()
    {
        Set<OptionalAction> results = new HashSet<>();
        for(OptionalFile e : all)
        {
            if(enabled.contains(e)) continue;
            if(e.actions != null)
            {
                results.addAll(e.actions);
            }
        }
        return results;
    }

    public void enable(OptionalFile file)
    {
        if(enabled.contains(file)) return;
        enabled.add(file);
        file.watchEvent(true);
        if (file.dependencies != null) {
            for (OptionalFile dep : file.dependencies) {
                if (dep.dependenciesCount == null) dep.dependenciesCount = new HashSet<>();
                dep.dependenciesCount.add(file);
                enable(dep);
            }
        }
        if (file.conflict != null) {
            for (OptionalFile conflict : file.conflict) {
                disable(conflict);
            }
        }
    }
    public void disable(OptionalFile file)
    {
        if(!enabled.remove(file)) return;
        file.watchEvent(false);
        if (file.dependenciesCount != null) {
            for (OptionalFile f : file.dependenciesCount) {
                if (f.isPreset) continue;
                disable(f);
            }
            file.dependenciesCount.clear();
            file.dependenciesCount = null;
        }
        if (file.dependencies != null) {
            for (OptionalFile f : file.dependencies) {
                if (!enabled.contains(f)) continue;
                if (f.dependenciesCount == null) {
                    disable(f);
                } else if (f.dependenciesCount.size() <= 1) {
                    f.dependenciesCount.clear();
                    f.dependenciesCount = null;
                    disable(f);
                }
            }
        }
    }
    public OptionalView(ClientProfile profile)
    {
        this.all = profile.getOptional();
        for(OptionalFile f : this.all)
        {
            if(f.mark) enable(f);
        }
    }
}
