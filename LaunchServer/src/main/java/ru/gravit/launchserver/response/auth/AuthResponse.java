package ru.gravit.launchserver.response.auth;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.profiles.ClientProfile;
import ru.gravit.launcher.serialize.HInput;
import ru.gravit.launcher.serialize.HOutput;
import ru.gravit.launcher.serialize.SerializeLimits;
import ru.gravit.launcher.serialize.signed.SignedObjectHolder;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.hwid.HWID;
import ru.gravit.launchserver.auth.hwid.HWIDException;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.launchserver.response.Response;
import ru.gravit.launchserver.response.profile.ProfileByUUIDResponse;

public final class AuthResponse extends Response {
    private static String echo(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, '*');
        return new String(chars);
    }

    public AuthResponse(LaunchServer server, long session, HInput input, HOutput output, String ip) {
        super(server, session, input, output, ip);
    }

    @Override
    public void reply() throws Exception {
        String login = input.readString(SerializeLimits.MAX_LOGIN);
        String client = input.readString(SerializeLimits.MAX_CLIENT);
        long hwid_hdd = input.readLong();
        long hwid_cpu = input.readLong();
        long hwid_bios = input.readLong();
        byte[] encryptedPassword = input.readByteArray(SecurityHelper.CRYPTO_MAX_LENGTH);
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
        try {
            if (server.limiter.isLimit(ip)) {
                AuthProvider.authError(server.config.authRejectString);
                return;
            }
            result = server.config.authProvider.auth(login, password, ip);
            if (!VerifyHelper.isValidUsername(result.username)) {
                AuthProvider.authError(String.format("Illegal result: '%s'", result.username));
                return;
            }
            Collection<SignedObjectHolder<ClientProfile>> profiles = server.getProfiles();
            for(SignedObjectHolder<ClientProfile> p : profiles)
				if(p.object.getTitle().equals(client))
					if(!p.object.isWhitelistContains(login))
						throw new AuthException(server.config.whitelistRejectString);
            server.config.hwidHandler.check(HWID.gen(hwid_hdd, hwid_bios, hwid_cpu), result.username);
        } catch (AuthException | HWIDException e) {
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth provider error");
            return;
        }
        debug("Auth: '%s' -> '%s', '%s'", login, result.username, result.accessToken);
        // Authenticate on server (and get UUID)
        UUID uuid;
        try {
            uuid = server.config.authHandler.auth(result);
        } catch (AuthException e) {
            requestError(e.getMessage());
            return;
        } catch (Exception e) {
            LogHelper.error(e);
            requestError("Internal auth handler error");
            return;
        }
        writeNoError(output);
        // Write profile and UUID
        ProfileByUUIDResponse.getProfile(server, uuid, result.username, client).write(output);
        output.writeASCII(result.accessToken, -SecurityHelper.TOKEN_STRING_LENGTH);
    }
}
