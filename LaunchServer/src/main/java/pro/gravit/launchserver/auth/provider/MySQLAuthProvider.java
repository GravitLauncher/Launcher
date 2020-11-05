package pro.gravit.launchserver.auth.provider;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.Auth2FAPassword;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launcher.request.auth.password.AuthTOTPPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MySQLAuthProvider extends AuthProvider {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean flagsEnabled;

    @Override
    public void init(LaunchServer srv) {
        super.init(srv);
        if (query == null) LogHelper.error("[Verify][AuthProvider] query cannot be null");
        if (message == null) LogHelper.error("[Verify][AuthProvider] message cannot be null");
        if (mySQLHolder == null) LogHelper.error("[Verify][AuthProvider] mySQLHolder cannot be null");
    }

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws SQLException, AuthException {

        if (!(password instanceof Auth2FAPassword || password instanceof AuthPlainPassword)) throw new AuthException("This password type not supported");
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        AuthPlainPassword first;
        AuthTOTPPassword second;
        if(password instanceof Auth2FAPassword) {
            first = (AuthPlainPassword) ((Auth2FAPassword) password).firstPassword;
            second = (AuthTOTPPassword) ((Auth2FAPassword) password).secondPassword;
        } else {
            first = (AuthPlainPassword) password;
            second = null;
        }
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"login", login, "password", first.password, "ip", ip};
            for (int i = 0; i < queryParams.length; i++)
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                if (set.next()){
                    if (set.getBoolean("has_mfa")){
                        if (second == null){
                            return authError(AuthRequestEvent.TWO_FACTOR_NEED_ERROR_MESSAGE);
                        }else{
                            boolean successful = verifier.isValidCode(set.getString("secret"), second.totp);
                            return successful ? new AuthProviderResult(set.getString(1), SecurityHelper.randomStringToken(), new ClientPermissions(
                                    set.getLong(2), flagsEnabled ? set.getLong(3) : 0)) : authError(AuthRequestEvent.TWO_FACTOR_BAD_MESSAGE);
                        }
                    } else {
                        return new AuthProviderResult(set.getString(1), SecurityHelper.randomStringToken(), new ClientPermissions(
                                set.getLong(2), flagsEnabled ? set.getLong(3) : 0));
                    }
                }
                return authError(message);
            }
        }

    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}
