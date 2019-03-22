package ru.gravit.launchserver.response.auth;

import ru.gravit.launcher.OshiHWID;
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
import ru.gravit.launchserver.response.profile.ProfileByUUIDResponse;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public final class AuthResponse extends Response {
    private static String echo(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, '*');
        return new String(chars);
    }

    public AuthResponse(LaunchServer server, long session, HInput input, HOutput output, String ip, Client clientData) {
        super(server, session, input, output, ip, clientData);
    }

    public static class AuthContext {
        public AuthContext(long session, String login, int password_lenght, String customText, String client, String hwid, String ip, boolean isServerAuth) {
            this.session = session;
            this.login = login;
            this.password_lenght = password_lenght;
            this.customText = customText;
            this.client = client;
            this.hwid = hwid;
            this.ip = ip;
            this.isServerAuth = isServerAuth;
        }

        public long session;
        public String login;
        public int password_lenght; //Use AuthProvider for get password
        public String client;
        public String hwid;
        public String customText;
        public String ip;
        public boolean isServerAuth;
    }

    @Override
    public void reply() throws Exception {
        String login = input.readString(SerializeLimits.MAX_LOGIN);
        boolean isClient = input.readBoolean();
        String client = null;
        if (isClient)
            client = input.readString(SerializeLimits.MAX_CLIENT);
        String auth_id = input.readString(SerializeLimits.MAX_QUEUE_SIZE);
        String hwid_str = input.readString(SerializeLimits.MAX_HWID_STR);
        byte[] encryptedPassword = input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH);
        String customText = input.readString(SerializeLimits.MAX_CUSTOM_TEXT);
        // Decrypt password
        String password;
        try {
            password = IOHelper.decode(SecurityHelper.newRSADecryptCipher(server.privateKey).
                    doFinal(encryptedPassword));
        } catch (IllegalBlockSizeException | BadPaddingException ignored) {
            requestError("Password decryption error");
            return;
        }

        // Authenticate
        debug("Login: '%s', Password: '%s'", login, echo(password.length()));
        AuthProviderResult result;
        AuthProviderPair pair;
        if(auth_id.isEmpty()) pair = server.config.getAuthProviderPair();
        else pair = server.config.getAuthProviderPair(auth_id);
        if(pair == null) requestError("Auth type not found");
        AuthProvider provider = pair.provider;
        clientData.type = Client.Type.USER;
        AuthContext context = new AuthContext(session, login, password.length(), customText, client, hwid_str, ip, false);
        try {
            server.authHookManager.preHook(context, clientData);
            if (!clientData.checkSign) {
                throw new AuthException("You must using checkLauncher");
            }
            provider.preAuth(login,password,customText,ip);
            result = provider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(result.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", result.username));
                return;
            }
            if (isClient) {
                Collection<ClientProfile> profiles = server.getProfiles();
                for (ClientProfile p : profiles) {
                    if (p.getTitle().equals(client)) {
                        if (!p.isWhitelistContains(login)) {
                            throw new AuthException(server.config.whitelistRejectString);
                        }
                        clientData.profile = p;
                    }
                }
                if (clientData.profile == null) {
                    throw new AuthException("Your profile is not found");
                }
            }
            server.config.hwidHandler.check(OshiHWID.gson.fromJson(hwid_str, OshiHWID.class), result.username);
            server.authHookManager.postHook(context, clientData);
        } catch (AuthException | HWIDException e) {
            if (e.getMessage() == null) LogHelper.error(e);
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth provider error");
            return;
        }
        debug("Auth: '%s' -> '%s', '%s'", login, result.username, result.accessToken);
        clientData.isAuth = true;
        clientData.permissions = result.permissions;
        clientData.username = result.username;
        clientData.auth_id = auth_id;
        clientData.updateAuth();
        // Authenticate on server (and get UUID)
        UUID uuid;
        try {
            uuid = pair.handler.auth(result);
        } catch (AuthException e) {
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth handler error");
            return;
        }
        String protectToken = server.config.protectHandler.generateSecureToken(context);
        writeNoError(output);
        // Write profile and UUID
        ProfileByUUIDResponse.getProfile(server, uuid, result.username, client, clientData.auth.textureProvider).write(output);
        output.writeASCII(result.accessToken, -SecurityHelper.TOKEN_STRING_LENGTH);
        clientData.permissions.write(output);
        output.writeString(protectToken, SerializeLimits.MAX_CUSTOM_TEXT);
    }
}
