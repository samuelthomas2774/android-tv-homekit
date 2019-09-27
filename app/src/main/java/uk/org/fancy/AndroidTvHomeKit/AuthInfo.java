package uk.org.fancy.AndroidTvHomeKit;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.util.function.Consumer;
import android.util.Log;

import io.github.hapjava.HomekitAuthInfo;
import io.github.hapjava.HomekitServer;

/**
 * This is a simple implementation that should never be used in actual production. The mac, salt, and privateKey
 * are being regenerated every time the application is started. The user store is also not persisted. This means pairing
 * needs to be re-done every time the app restarts.
 *
 * @author Andy Lintner
 */
public class AuthInfo implements HomekitAuthInfo {
    private static final String TAG = "HomeKit:AuthInfo";

    private final AuthState authState;

    Consumer<AuthState> callback;

    public AuthInfo() throws InvalidAlgorithmParameterException {
        this(new AuthState("031-45-154", HomekitServer.generateMac(), HomekitServer.generateSalt(),
            HomekitServer.generateKey()));
    }

    public AuthInfo(AuthState _authState) {
        authState = _authState;
        Log.i(TAG, "The PIN for pairing is " + authState.PIN);
    }

    @Override
    public String getPin() {
        return authState.PIN;
    }

    @Override
    public String getMac() {
        return authState.mac;
    }

    @Override
    public BigInteger getSalt() {
        return authState.salt;
    }

    @Override
    public byte[] getPrivateKey() {
        return authState.privateKey;
    }

    @Override
    public void createUser(String username, byte[] publicKey) {
        if (!authState.userKeyMap.containsKey(username)) {
            authState.userKeyMap.putIfAbsent(username, publicKey);
            Log.i(TAG, "Added pairing for " + username);
            notifyChange();
        } else {
            Log.i(TAG, "Already have a user for " + username);
        }
    }

    @Override
    public void removeUser(String username) {
        authState.userKeyMap.remove(username);
        Log.i(TAG, "Removed pairing for " + username);
        notifyChange();
    }

    @Override
    public byte[] getUserPublicKey(String username) {
        return authState.userKeyMap.get(username);
    }

    public boolean isPaired() {
        return !authState.userKeyMap.isEmpty();
    }

    public void onChange(Consumer<AuthState> _callback) {
        callback = _callback;
        notifyChange();
    }

    private void notifyChange() {
        if (callback != null) {
            callback.accept(authState);
        }
    }
}
