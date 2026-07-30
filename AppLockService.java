package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class AppLockService extends AccessibilityService {

    // =========================================================
    // PREFERENCES
    // =========================================================

    private static final String PREFS_NAME =
            "lockvault_preferences";

    private static final String KEY_LOCKED_APPS =
            "locked_apps";

    /*
     * Эти ключи совпадают с LockActivity.
     */
    private static final String KEY_UNLOCKED_PACKAGE =
            "temporarily_unlocked_package";

    private static final String KEY_UNLOCK_TIME =
            "temporarily_unlocked_time";

    // =========================================================
    // STATE
    // =========================================================

    /*
     * Приложение, которое пользователь уже разблокировал.
     *
     * Пока пользователь находится внутри этого приложения,
     * повторный PIN не показывается.
     */
    private String unlockedPackage = "";

    /*
     * Последний внешний пакет, который реально был обнаружен.
     */
    private String lastExternalPackage = "";

    /*
     * Пакет, для которого LockActivity уже был запущен.
     *
     * Это защищает от десятков AccessibilityEvent подряд.
     */
    private String pendingLockPackage = "";

    private long lastLockRequestTime = 0L;

    private static final long LOCK_REQUEST_COOLDOWN =
            1200L;

    // =========================================================
    // ACCESSIBILITY EVENT
    // =========================================================

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        int type =
                event.getEventType();

        if (type !=
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                &&
                type !=
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED) {

            return;
        }

        /*
         * Сначала обязательно читаем результат
         * успешного PIN из LockActivity.
         */
        syncUnlockedPackage();

        CharSequence packageSequence =
                event.getPackageName();

        if (packageSequence == null) {
            return;
        }

        String packageName =
                packageSequence.toString();

        if (packageName == null ||
                packageName.trim().isEmpty()) {

            return;
        }

        // =====================================================
        // LOCKVAULT
        // =====================================================

        /*
         * LockActivity принадлежит пакету LockVault.
         *
         * Когда LockActivity появляется поверх защищённого
         * приложения, Accessibility присылает пакет самого
         * LockVault.
         *
         * Это НЕ означает, что пользователь реально покинул
         * защищённое приложение.
         */
        if (packageName.equals(
                getPackageName()
        )) {

            return;
        }

        // =====================================================
        // ALREADY UNLOCKED
        // =====================================================

        /*
         * Если пользователь успешно ввёл PIN для этого
         * приложения — разрешаем любые Activity, Dialog,
         * окна, меню и переходы внутри этого же package.
         *
         * PIN повторно НЕ появляется.
         */
        if (!unlockedPackage.isEmpty() &&
                packageName.equals(
                        unlockedPackage
                )) {

            lastExternalPackage =
                    packageName;

            pendingLockPackage = "";

            return;
        }

        // =====================================================
        // LEFT UNLOCKED APP
        // =====================================================

        /*
         * Здесь появился ДРУГОЙ внешний package.
         *
         * Значит пользователь действительно покинул
         * разблокированное приложение.
         *
         * После этого при следующем входе PIN снова
         * потребуется.
         */
        if (!unlockedPackage.isEmpty() &&
                !packageName.equals(
                        unlockedPackage
                )) {

            clearUnlockedPackage();
        }

        lastExternalPackage =
                packageName;

        // =====================================================
        // LOAD LOCKED APPS
        // =====================================================

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        Set<String> saved =
                preferences.getStringSet(
                        KEY_LOCKED_APPS,
                        new HashSet<>()
                );

        Set<String> lockedApps;

        if (saved == null) {

            lockedApps =
                    new HashSet<>();

        } else {

            lockedApps =
                    new HashSet<>(
                            saved
                    );
        }

        // =====================================================
        // NOT LOCKED
        // =====================================================

        if (!lockedApps.contains(
                packageName
        )) {

            pendingLockPackage = "";

            return;
        }

        // =====================================================
        // PREVENT DUPLICATE LOCK ACTIVITY
        // =====================================================

        long now =
                android.os.SystemClock.elapsedRealtime();

        if (packageName.equals(
                pendingLockPackage
        )
                &&
                now - lastLockRequestTime
                        < LOCK_REQUEST_COOLDOWN) {

            return;
        }

        pendingLockPackage =
                packageName;

        lastLockRequestTime =
                now;

        // =====================================================
        // SHOW LOCK
        // =====================================================

        showLockScreen(
                packageName
        );
    }

    // =========================================================
    // SYNC UNLOCK STATE
    // =========================================================

    private void syncUnlockedPackage() {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        String packageName =
                preferences.getString(
                        KEY_UNLOCKED_PACKAGE,
                        ""
                );

        if (packageName == null) {

            packageName = "";
        }

        if (packageName.isEmpty()) {

            return;
        }

        /*
         * LockActivity записывает package только после
         * правильного PIN.
         */
        unlockedPackage =
                packageName;

        pendingLockPackage = "";

        /*
         * Удаляем временный сигнал из SharedPreferences.
         *
         * Сам unlockedPackage остаётся в памяти сервиса
         * до тех пор, пока пользователь реально не перейдёт
         * в другой внешний package.
         */
        preferences
                .edit()
                .remove(
                        KEY_UNLOCKED_PACKAGE
                )
                .remove(
                        KEY_UNLOCK_TIME
                )
                .apply();
    }

    // =========================================================
    // CLEAR UNLOCK
    // =========================================================

    private void clearUnlockedPackage() {

        unlockedPackage = "";

        pendingLockPackage = "";

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        preferences
                .edit()
                .remove(
                        KEY_UNLOCKED_PACKAGE
                )
                .remove(
                        KEY_UNLOCK_TIME
                )
                .apply();
    }

    // =========================================================
    // SHOW LOCK ACTIVITY
    // =========================================================

    private void showLockScreen(
            String packageName
    ) {

        if (packageName == null ||
                packageName.isEmpty()) {

            return;
        }

        /*
         * Никогда не блокируем сам LockVault.
         */
        if (packageName.equals(
                getPackageName()
        )) {

            return;
        }

        try {

            android.content.Intent intent =
                    new android.content.Intent(
                            this,
                            LockActivity.class
                    );

            intent.putExtra(
                    "locked_package",
                    packageName
            );

            intent.addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                            |
                            android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                            |
                            android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            |
                            android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
            );

            startActivity(
                    intent
            );

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // SERVICE CONNECTED
    // =========================================================

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        unlockedPackage = "";

        lastExternalPackage = "";

        pendingLockPackage = "";

        lastLockRequestTime = 0L;

        /*
         * Удаляем старый временный unlock,
         * если Android перезапустил AccessibilityService.
         */
        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        preferences
                .edit()
                .remove(
                        KEY_UNLOCKED_PACKAGE
                )
                .remove(
                        KEY_UNLOCK_TIME
                )
                .apply();
    }

    // =========================================================
    // INTERRUPT
    // =========================================================

    @Override
    public void onInterrupt() {
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        unlockedPackage = "";

        lastExternalPackage = "";

        pendingLockPackage = "";

        super.onDestroy();
    }
}