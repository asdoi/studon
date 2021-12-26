package de.baumann.hhsmoodle;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class PreferenceHelper {
    private static final String ENCRYPTED_PREFS = "ENCRYPTED_PREFS";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences encryptedSharedPreferences;

    //Encrypted SharedPrefs
    private static SharedPreferences getEncryptedSharedPreferences(Context context) {
        if (encryptedSharedPreferences != null)
            return encryptedSharedPreferences;

        try {
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS,
                    new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return encryptedSharedPreferences;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getSharedPreferences(context);
    }

    static String getUsername(Context context) {
        return getEncryptedSharedPreferences(context).getString("username", "");
    }

    static String getPassword(Context context) {
        return getEncryptedSharedPreferences(context).getString("password", "");
    }

    static void setUsernamePassword(Context context, String username, String password) {
        getEncryptedSharedPreferences(context).edit().putString("username", username).putString("password", password).apply();
    }

    static String getPin(Context context) {
        return getEncryptedSharedPreferences(context).getString("settings_security_pin", "");
    }

    static void setPin(Context context, String value) {
        getEncryptedSharedPreferences(context).edit().putString("settings_security_pin", value).apply();
    }

    static void disablePin(Context context) {
        setPin(context, "");
    }


    //Unencrypted SharedPrefs
    private static SharedPreferences getSharedPreferences(Context context) {
        if (sharedPreferences == null)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        return sharedPreferences;
    }

    //Prefs set through PreferenceFragment (UI)
    static boolean isBiometric(Context context) {
        return getSharedPreferences(context).getBoolean("biometric", false);
    }

    static boolean isSwipe(Context context) {
        return getSharedPreferences(context).getBoolean("swipe", false);
    }

    static boolean isExternal(Context context) {
        return getSharedPreferences(context).getBoolean("external", true);
    }

    static boolean isConfirmDownloads(Context context) {
        return getSharedPreferences(context).getBoolean("confirm_download", false);
    }

    static boolean isNightMode(Context context) {
        return getSharedPreferences(context).getBoolean("nightMode", false);
    }

    static String getBookmarksSort(Context context) {
        return getSharedPreferences(context).getString("sortDBB", "title");
    }

    static int getColumns(Context context) {
        try {
            return Integer.parseInt(getSharedPreferences(context).getString("columns", "2"));
        } catch (Exception e) {
            return 2;
        }
    }

    static boolean isAutoUpdate(Context context) {
        return getSharedPreferences(context).getBoolean("auto_update", true);
    }

    static void disableAutoUpdate(Context context) {
        getSharedPreferences(context).edit().putBoolean("auto_update", false).apply();
    }

    //Internal Prefs
    static String getDefaultURL(Context context) {
        return getSharedPreferences(context).getString("favoriteURL", Activity_Main.DEFAULT_WEBSITE);
    }

    static String getDefaultTitle(Context context) {
        return getSharedPreferences(context).getString("favoriteTitle", context.getString(R.string.summary));
    }

    static void setDefault(Context context, String url, String title) {
        getSharedPreferences(context).edit().putString("favoriteURL", url).putString("favoriteTitle", title).apply();
    }
}
