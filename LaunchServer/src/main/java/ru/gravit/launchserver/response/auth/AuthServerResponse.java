package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.AuthProviderPair;
import ru.gravit.launchserver.auth.hwid.HWIDException;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.util.Arrays;
import java.util.Collection;

public final class AuthServerResponse extends Response {
    private static String echo(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, '*');
        return new String(chars);
    }

    public AuthServerResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    @Override
    public void reply() throws Exception {
        String login = input.readString(SerializeLimits.MAX_LOGIN);
        String client = input.readString(SerializeLimits.MAX_CLIENT);
        String auth_id = input.readString(SerializeLimits.MAX_QUEUE_SIZE);
        byte[] encryptedPassword = input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH);
        // Decrypt password
        String password;
        try {
            password = IOHelper.decode(SecurityHelper.newRSADecryptCipher(server.privateKey).
                    doFinal(encryptedPassword));
        } catch (IllegalBlockSizeException | BadPaddingException ignored) {
            requestError("ServerPassword decryption error");
            return;
        }
        // Authenticate
        debug("ServerLogin: '%s', Password: '%s'", login, echo(password.length()));
        AuthProviderResult result;
        AuthProviderPair pair;
        if(auth_id.isEmpty()) pair = server.config.getAuthProviderPair();
        else pair = server.config.getAuthProviderPair(auth_id);
        if(pair == null) requestError("Auth type not found");
        AuthProvider provider = pair.provider;
        AuthResponse.AuthContext context = new AuthResponse.AuthContext(session, login, password.length(), null, client, null, ip, true);
        try {
            server.authHookManager.preHook(context, clientData);
            result = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(result.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", result.username));
                return;
            }
            Collection<ClientProfile> profiles = server.getProfiles();
            for (ClientProfile p : profiles) {
                if (p.getTitle().equals(client)) {
                    clientData.profile = p;
                }
            }
            if (clientData.profile == null) {
                throw new AuthException("Your profile is not found");
            }
            clientData.permissions = server.config.permissionsHandler.getPermissions(login);
            if (!clientData.permissions.canServer) {
                throw new AuthException("Your account cannot be a server");
            }
            clientData.type = Client.Type.SERVER;
            clientData.username = result.username;
            server.authHookManager.postHook(context, clientData);
        } catch (AuthException | HWIDException e) {
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth provider error");
            return;
        }
        debug("ServerAuth: '%s' -> '%s', '%s'", login, result.username, result.accessToken);
        clientData.isAuth = true;
        clientData.auth_id = auth_id;
        clientData.updateAuth();
        writeNoError(output);
        clientData.permissions.write(output);
    }
}
