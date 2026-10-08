package com.group14.expensemanager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class SecurityManager {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_USE_FINGERPRINT = "UseFingerprint";
    private static final String KEY_USE_PIN = "UsePin";
    private static final String KEY_USER_PIN = "UserPin";

    private static boolean isSessionAuthenticated = false;

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String message);
        void onCancel();
    }

    public static boolean isSessionAuthenticated() {
        return isSessionAuthenticated;
    }

    public static void setAuthenticated(boolean authenticated) {
        isSessionAuthenticated = authenticated;
    }

    public static boolean isSecurityEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_USE_FINGERPRINT, false) || prefs.getBoolean(KEY_USE_PIN, false);
    }

    public static boolean usePin(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_USE_PIN, false);
    }

    public static boolean useFingerprint(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_USE_FINGERPRINT, false);
    }

    public static String getSavedPin(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_PIN, "");
    }

    public static void authenticate(FragmentActivity activity, AuthCallback callback) {
        if (useFingerprint(activity)) {
            authenticateBiometric(activity, callback);
        } else if (usePin(activity)) {
            callback.onFailure("PIN_REQUIRED");
        } else {
            callback.onSuccess();
        }
    }

    private static void authenticateBiometric(FragmentActivity activity, AuthCallback callback) {
        BiometricManager biometricManager = BiometricManager.from(activity);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK;

        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            callback.onFailure("Biometric not supported or not set up");
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                isSessionAuthenticated = true;
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onCancel();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onFailure("Invalid biometric");
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Mở khóa ứng dụng")
                .setSubtitle("Xác thực để tiếp tục")
                .setNegativeButtonText("Hủy")
                .setAllowedAuthenticators(authenticators)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
