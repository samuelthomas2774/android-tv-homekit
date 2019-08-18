package uk.org.fancy.AndroidTvHomeKit.Philips.xtv;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.util.Log;

public class XTvUtil {
    private final static String TAG = "HomeKit:XTvUtil";
    final static String digestAuthRealm = "XTV";
    final static String digestAuthNonceKey = "mySecretServerKey";
    final static String encryptionKey = "ZmVay1EQVFOaZhwQ4Kv81ypLAZNczV9sG4KkseXWn1NEk6cXmPKO/MCa9sryslvLCFMnNe4Z4CPXzToowvhHvA==";

    public static String decrypt(String data) {
        if (data.endsWith("\n")) data = data.substring(0, data.length() - 1);

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), 0, 16, "AES");

            byte[] decoded = Base64.getDecoder().decode(data);
            byte[] iv = new byte[16];
            System.arraycopy(decoded, 0, iv, 0, 16);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] encrypted = new byte[(decoded.length - 16)];
            System.arraycopy(decoded, 16, encrypted, 0, decoded.length - 16);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(2, secretKeySpec, ivSpec);
            return new String(cipher.doFinal(encrypted), "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting " + data + ": " + e.toString());
            return null;
        }
    }

    public static String encrypt(String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(encryptionKey), 0, 16, "AES");

            byte[] iv = new byte[16];
            (new SecureRandom()).nextBytes(iv);
            IvParameterSpec ipSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(1, secretKeySpec, ipSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

            byte[] result = new byte[(iv.length + encrypted.length)];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting " + data + ": " + e.toString());
            return null;
        }
    }

    public static String generateServerNonce() {
        long timestamp = System.currentTimeMillis();
        String hash1 = hash(timestamp + ":" + digestAuthNonceKey);
        return Base64.getEncoder().encodeToString((timestamp + ":" + hash1).getBytes());
    }

    public static String generateDigestResponse(String username, String password, String method, String url, String nonce) {
        String ha1 = hash(username + ":" + digestAuthRealm + ":" + password);
        String ha2 = hash(method + ":" + url);

        return hash(ha1 + ":" + nonce + ":" + ha2);
    }

    public static String generateAuthorizationHeader(String username, String password, String method, String url) {
        String nonce = generateServerNonce();

        String response = generateDigestResponse(username, password, method, url, nonce);

        return "Digest username=\"" + username + "\", realm=\"" + digestAuthRealm + "\", " +
            "nonce=\"" + nonce + "\", uri=\"" + url + "\", response=\"" + response + "\", " +
            "algorithm=MD5";
    }

    private static String hash(String originalString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(originalString.getBytes());

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException err) {
            throw new RuntimeException("Error calculating hash", err);
        }
    }
}
