package com.example.applock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

public class AppLockAccessibilityService extends AccessibilityService {

    private static final String PREFS_NAME =
            "lockvault_preferences";

    private static final String KEY_LOCKED_APPS =
            "locked_apps";

    private static final String KEY_UNLOCKED_PACKAGE =
            "temporarily_unlocked_package";

    private static final String KEY_UNLOCK_TIME =
            "temporarily_unlocked_time";

    /*
     * После правильного PIN приложение считается
     * разблокированным, пока пользователь находится
     * внутри него.
     */
    private SharedPreferences preferences;

    private String currentPackage = "";

    private String unlockedPackage = "";

    private boolean lockScreenShowing = false;

    private long lastLockLaunchTime = 0;

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {

        if (event == null) {
            return;
        }

        int type =
                event.getEventType();

        /*
         * Нам достаточно события изменения окна.
         *
         * TYPE_WINDOW_CONTENT_CHANGED специально
         * НЕ используем, иначе PIN может появляться
         * при каждом действии внутри приложения.
         */
        if (type !=
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            return;
        }

        CharSequence packageSequence =
                event.getPackageName();

        if (packageSequence == null) {
            return;
        }

        String packageName =
                packageSequence.toString();

        if (packageName.isEmpty()) {
            return;
        }

        if (preferences == null) {

            preferences =
                    getSharedPreferences(
                            PREFS_NAME,
                            MODE_PRIVATE
                    );
        }

        /*
         * Системный интерфейс игнорируем.
         */
        if (packageName.equals(
                "com.android.systemui"
        )) {

            return;
        }

        /*
         * Когда появляется наш LockActivity,
         * Accessibility видит пакет LockVault.
         *
         * Это НЕ означает, что пользователь
         * покинул защищённое приложение.
         */
        if (packageName.equals(
                getPackageName()
        )) {

            return;
        }

        Set<String> saved =
                preferences.getStringSet(
                        KEY_LOCKED_APPS,
                        new HashSet<>()
                );

        Set<String> lockedApps =
                new HashSet<>(saved);

        /*
         * Пользователь действительно перешёл
         * в другое приложение.
         */
        if (!packageName.equals(
                currentPackage
        )) {

            /*
             * Если мы вышли из ранее разблокированного
             * приложения в другое настоящее приложение,
             * снимаем временную разблокировку.
             */
            if (!unlockedPackage.isEmpty()
                    &&
                    !packageName.equals(
                            unlockedPackage
                    )) {

                clearUnlock();
            }

            currentPackage =
                    packageName;

            lockScreenShowing =
                    false;
        }

        /*
         * Приложение вообще не защищено.
         */
        if (!lockedApps.contains(
                packageName
        )) {

            return;
        }

        /*
         * Проверяем флаг, который LockActivity
         * записывает после правильного PIN.
         */
        String savedUnlockedPackage =
                preferences.getString(
                        KEY_UNLOCKED_PACKAGE,
                        ""
                );

        if (packageName.equals(
                savedUnlockedPackage
        )) {

            unlockedPackage =
                    packageName;

            lockScreenShowing =
                    false;

            return;
        }

        /*
         * Если это приложение уже было
         * разблокировано в текущем сеансе,
         * повторно PIN не показываем.
         */
        if (packageName.equals(
                unlockedPackage
        )) {

            return;
        }

        /*
         * Не запускаем несколько LockActivity
         * одновременно.
         */
        if (lockScreenShowing) {
            return;
        }

        long now =
                SystemClock.elapsedRealtime();

        if (now - lastLockLaunchTime <
                700) {

            return;
        }

        lastLockLaunchTime =
                now;

        lockScreenShowing =
                true;

        showLockScreen(
                packageName
        );
    }

    private void showLockScreen(
            String packageName
    ) {

        try {

            Intent intent =
                    new Intent(
                            this,
                            LockActivity.class
                    );

            intent.putExtra(
                    "locked_package",
                    packageName
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            );

            startActivity(
                    intent
            );

        } catch (Exception e) {

            lockScreenShowing =
                    false;
        }
    }

    private void clearUnlock() {

        unlockedPackage =
                "";

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

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {

        lockScreenShowing =
                false;

        currentPackage =
                "";

        unlockedPackage =
                "";

        super.onDestroy();
    }
}