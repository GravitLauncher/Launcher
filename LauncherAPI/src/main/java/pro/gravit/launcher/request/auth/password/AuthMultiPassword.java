package pro.gravit.launcher.request.auth.password;

import pro.gravit.launcher.request.auth.AuthRequest;

import java.util.List;

public class AuthMultiPassword implements AuthRequest.AuthPasswordInterface {
    public List<AuthRequest.AuthPasswordInterface> list;

    @Override
    public boolean check() {
        return list != null && list.stream().allMatch(l -> l != null && l.check());
    }

    @Override
    public boolean isAllowSave() {
        return list != null && list.stream().allMatch(l -> l != null && l.isAllowSave());
    }
}
