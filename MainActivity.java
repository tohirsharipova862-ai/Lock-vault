package com.example.applock;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends Activity {

    // =========================================================
    // COLORS — T3
    // =========================================================

    private static final int BG =
            Color.rgb(13, 12, 18);

    private static final int SURFACE =
            Color.rgb(25, 23, 32);

    private static final int SURFACE_2 =
            Color.rgb(31, 28, 40);

    private static final int SURFACE_3 =
            Color.rgb(38, 35, 47);

    private static final int BORDER_SOFT =
            Color.rgb(51, 47, 63);

    private static final int WHITE =
            Color.rgb(247, 246, 251);

    private static final int TEXT_2 =
            Color.rgb(177, 172, 190);

    private static final int TEXT_3 =
            Color.rgb(112, 108, 125);

    private static final int PURPLE =
            Color.rgb(155, 105, 235);

    private static final int PURPLE_LIGHT =
            Color.rgb(201, 174, 255);

    private static final int PURPLE_SURFACE =
            Color.rgb(53, 40, 70);

    private static final int GREEN =
            Color.rgb(93, 218, 155);

    private static final int GREEN_SURFACE =
            Color.rgb(27, 59, 47);

    private static final int RED =
            Color.rgb(235, 105, 119);

    // =========================================================
    // TABS
    // =========================================================

    private static final String TAB_HOME =
            "home";

    private static final String TAB_APPS =
            "apps";

    private static final String TAB_VAULT =
            "vault";

    private static final String TAB_SETTINGS =
            "settings";

    // =========================================================
    // ICON TYPES
    // =========================================================

    private static final int ICON_HOME = 1;
    private static final int ICON_APPS = 2;
    private static final int ICON_VAULT = 3;
    private static final int ICON_SETTINGS = 4;
    private static final int ICON_LOCK = 5;
    private static final int ICON_SECURITY = 6;
    private static final int ICON_PERMISSION = 7;
    private static final int ICON_PRIVACY = 8;
    private static final int ICON_BIOMETRIC = 9;
    private static final int ICON_CHECK = 10;
    private static final int ICON_PHOTO = 11;
    private static final int ICON_VIDEO = 12;
    private static final int ICON_NOTE = 13;
    private static final int ICON_BELL = 14;
    private static final int ICON_PREMIUM = 15;
    private static final int ICON_RESET = 16;

    // =========================================================
    // PREFERENCES
    // =========================================================

    private static final String PREFS_NAME =
            "lockvault_preferences";

    private static final String KEY_LOCKED_APPS =
            "locked_apps";

    private static final String KEY_PIN_HASH =
            "pin_hash";

    private static final String KEY_PIN_SALT =
            "pin_salt";

    private static final String KEY_LEGACY_PIN =
            "private_pin";

    private static final String KEY_LANGUAGE =
            "language";

    private static final String KEY_PRIVACY_MODE =
            "privacy_mode";

    private static final String KEY_BIOMETRIC_ENABLED =
            "biometric_enabled";

    private static final String KEY_NOTIFICATIONS =
            "security_notifications";

    private static final String KEY_VAULT_PHOTOS =
            "vault_photos";

    private static final String KEY_VAULT_VIDEOS =
            "vault_videos";

    private static final String KEY_VAULT_NOTES =
            "vault_notes";

    // =========================================================
    // VAULT
    // =========================================================

    private static final String VAULT_PHOTOS_DIR =
            "photos";

    private static final String VAULT_VIDEOS_DIR =
            "videos";

    private static final String VAULT_NOTES_DIR =
            "notes";

    private static final int REQUEST_VAULT_PHOTO =
            3101;

    private static final int REQUEST_VAULT_VIDEO =
            3102;

    // =========================================================
    // DATA
    // =========================================================

    private SharedPreferences preferences;

    private Typeface regular;
    private Typeface medium;
    private Typeface bold;

    private LinearLayout root;

    private String currentTab =
            TAB_HOME;

    private Set<String> lockedPackages =
            new HashSet<>();

    private final List<AppItem> allApps =
            new ArrayList<>();

    private boolean appsLoading =
            false;

    private boolean openingAppLock =
            false;

    private long lastAppLockOpenTime =
            0L;

    private AppItem pendingApp;

    private String firstPin =
            "";

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        regular =
                Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL
                );

        medium =
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                );

        bold =
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                );

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        migrateLegacyPinIfNeeded();

        loadLockedApps();

        syncVaultCounters();

        showHome();
    }

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * ВАЖНО:
         * после возврата из системного экрана Accessibility
         * состояние проверяется заново.
         *
         * Никакого автоматического повторного открытия
         * системных настроек здесь нет.
         */
        openingAppLock = false;

        if (TAB_APPS.equals(currentTab)) {

            if (hasPin()
                    &&
                    isAccessibilityServiceEnabled()
                    &&
                    hasUsageStatsPermission()) {

                showAppLock();
            }
        }
    }

    // =========================================================
    // HOME
    // =========================================================

    private void showHome() {

        currentTab =
                TAB_HOME;

        loadLockedApps();
        syncVaultCounters();

        root =
                vertical();

        root.setBackground(
                appBackground()
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(
                true
        );

        scroll.setVerticalScrollBarEnabled(
                false
        );

        scroll.setOverScrollMode(
                View.OVER_SCROLL_NEVER
        );

        LinearLayout content =
                vertical();

        content.setPadding(
                dp(22),
                dp(22),
                dp(22),
                dp(31)
        );

        scroll.addView(
                content
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // =====================================================
        // BRAND
        // =====================================================

        LinearLayout header =
                horizontal();

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView logo =
                text(
                        "L",
                        19,
                        WHITE,
                        bold
                );

        logo.setGravity(
                Gravity.CENTER
        );

        logo.setBackground(
                gradient(
                        new int[]{
                                Color.rgb(150, 100, 232),
                                Color.rgb(100, 65, 170)
                        },
                        dp(15)
                )
        );

        header.addView(
                logo,
                params(
                        dp(46),
                        dp(46)
                )
        );

        LinearLayout brand =
                vertical();

        LinearLayout.LayoutParams brandParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        brandParams.leftMargin =
                dp(12);

        brand.addView(
                text(
                        "LockVault",
                        20,
                        WHITE,
                        bold
                )
        );

        TextView brandSubtitle =
                text(
                        "PRIVATE SECURITY",
                        8,
                        TEXT_3,
                        medium
                );

        brandSubtitle.setLetterSpacing(
                0.15f
        );

        LinearLayout.LayoutParams brandSubtitleParams =
                wrap();

        brandSubtitleParams.topMargin =
                dp(3);

        brand.addView(
                brandSubtitle,
                brandSubtitleParams
        );

        header.addView(
                brand,
                brandParams
        );

        content.addView(
                header
        );

        gap(
                content,
                26
        );

        // =====================================================
        // PROTECTION HERO
        // =====================================================

        boolean accessibility =
                isAccessibilityServiceEnabled();

        boolean usage =
                hasUsageStatsPermission();

        boolean pin =
                hasPin();

        boolean protectionActive =
                accessibility
                        &&
                        usage
                        &&
                        pin;

        LinearLayout hero =
                vertical();

        hero.setPadding(
                dp(21),
                dp(21),
                dp(21),
                dp(21)
        );

        hero.setBackground(
                protectionActive
                        ? activeProtectionBackground(
                                dp(26)
                        )
                        : glassCardBackground(
                                dp(26)
                        )
        );

        if (protectionActive) {

            applyActiveGlow(
                    hero
            );

        } else {

            applySoftDepth(
                    hero
            );
        }

        LinearLayout heroTop =
                horizontal();

        heroTop.setGravity(
                Gravity.CENTER_VERTICAL
        );

        heroTop.addView(
                createFeatureVectorIcon(
                        ICON_SECURITY
                ),
                params(
                        dp(62),
                        dp(62)
                )
        );

        LinearLayout heroInfo =
                vertical();

        LinearLayout.LayoutParams heroInfoParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        heroInfoParams.leftMargin =
                dp(14);

        heroInfo.addView(
                text(
                        protectionActive
                                ? "Protection Active"
                                : "Protection Ready",
                        18,
                        WHITE,
                        bold
                )
        );

        TextView heroSubtitle =
                text(
                        protectionActive
                                ? "Your private apps are protected."
                                : "Complete security setup to activate protection.",
                        9,
                        TEXT_2,
                        regular
                );

        heroSubtitle.setLineSpacing(
                dp(2),
                1f
        );

        LinearLayout.LayoutParams heroSubtitleParams =
                wrap();

        heroSubtitleParams.topMargin =
                dp(5);

        heroInfo.addView(
                heroSubtitle,
                heroSubtitleParams
        );

        heroTop.addView(
                heroInfo,
                heroInfoParams
        );

        heroTop.addView(
                createPill(
                        protectionActive
                                ? "ACTIVE"
                                : "SETUP",
                        protectionActive
                                ? GREEN
                                : PURPLE_LIGHT,
                        protectionActive
                                ? GREEN_SURFACE
                                : PURPLE_SURFACE
                ),
                params(
                        dp(64),
                        dp(30)
                )
        );

        hero.addView(
                heroTop
        );

        gap(
                hero,
                19
        );

        LinearLayout stats =
                horizontal();

        stats.addView(
                createHomeStat(
                        String.valueOf(
                                lockedPackages.size()
                        ),
                        "Protected"
                ),
                new LinearLayout.LayoutParams(
                        0,
                        dp(66),
                        1
                )
        );

        horizontalGap(
                stats,
                8
        );

        stats.addView(
                createHomeStat(
                        String.valueOf(
                                getVaultItemCount()
                        ),
                        "Vault"
                ),
                new LinearLayout.LayoutParams(
                        0,
                        dp(66),
                        1
                )
        );

        horizontalGap(
                stats,
                8
        );

        stats.addView(
                createHomeStat(
                        calculateSecurityScore()
                                + "%",
                        "Security"
                ),
                new LinearLayout.LayoutParams(
                        0,
                        dp(66),
                        1
                )
        );

        hero.addView(
                stats
        );

        content.addView(
                hero
        );

        gap(
                content,
                27
        );

        content.addView(
                sectionTitle(
                        "Protection",
                        lockedPackages.size()
                                + " apps"
                )
        );

        gap(
                content,
                11
        );

        LinearLayout appLockCard =
                createFeatureCard();

        appLockCard.addView(
                createFeatureVectorIcon(
                        ICON_APPS
                ),
                params(
                        dp(54),
                        dp(54)
                )
        );

        LinearLayout appLockInfo =
                vertical();

        LinearLayout.LayoutParams appLockInfoParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        appLockInfoParams.leftMargin =
                dp(13);

        appLockInfo.addView(
                text(
                        "App Lock",
                        13,
                        WHITE,
                        medium
                )
        );

        TextView appLockSubtitle =
                text(
                        "Protect private applications",
                        9,
                        TEXT_3,
                        regular
                );

        LinearLayout.LayoutParams appLockSubtitleParams =
                wrap();

        appLockSubtitleParams.topMargin =
                dp(5);

        appLockInfo.addView(
                appLockSubtitle,
                appLockSubtitleParams
        );

        appLockCard.addView(
                appLockInfo,
                appLockInfoParams
        );

        appLockCard.addView(
                createPill(
                        String.valueOf(
                                lockedPackages.size()
                        ),
                        PURPLE_LIGHT,
                        PURPLE_SURFACE
                ),
                params(
                        dp(44),
                        dp(30)
                )
        );

        pressEffect(
                appLockCard
        );

        appLockCard.setOnClickListener(
                v -> {

                    haptic(v);

                    openAppLockSafely();
                }
        );

        content.addView(
                appLockCard
        );

        gap(
                content,
                10
        );

        LinearLayout vaultCard =
                createFeatureCard();

        vaultCard.addView(
                createFeatureVectorIcon(
                        ICON_VAULT
                ),
                params(
                        dp(54),
                        dp(54)
                )
        );

        LinearLayout vaultInfo =
                vertical();

        LinearLayout.LayoutParams vaultInfoParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        vaultInfoParams.leftMargin =
                dp(13);

        vaultInfo.addView(
                text(
                        "Private Vault",
                        13,
                        WHITE,
                        medium
                )
        );

        TextView vaultSubtitle =
                text(
                        "Photos, videos and private notes",
                        9,
                        TEXT_3,
                        regular
                );

        LinearLayout.LayoutParams vaultSubtitleParams =
                wrap();

        vaultSubtitleParams.topMargin =
                dp(5);

        vaultInfo.addView(
                vaultSubtitle,
                vaultSubtitleParams
        );

        vaultCard.addView(
                vaultInfo,
                vaultInfoParams
        );

        vaultCard.addView(
                createPill(
                        String.valueOf(
                                getVaultItemCount()
                        ),
                        GREEN,
                        GREEN_SURFACE
                ),
                params(
                        dp(44),
                        dp(30)
                )
        );

        pressEffect(
                vaultCard
        );

        vaultCard.setOnClickListener(
                v -> {

                    haptic(v);

                    showVaultScreen();
                }
        );

        content.addView(
                vaultCard
        );

        gap(
                content,
                10
        );

        LinearLayout securityCard =
                createFeatureCard();

        securityCard.addView(
                createFeatureVectorIcon(
                        ICON_SECURITY
                ),
                params(
                        dp(54),
                        dp(54)
                )
        );

        LinearLayout securityInfo =
                vertical();

        LinearLayout.LayoutParams securityInfoParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        securityInfoParams.leftMargin =
                dp(13);

        securityInfo.addView(
                text(
                        "Security Center",
                        13,
                        WHITE,
                        medium
                )
        );

        TextView securitySubtitle =
                text(
                        getSecurityLevel(
                                calculateSecurityScore()
                        ),
                        9,
                        TEXT_3,
                        regular
                );

        LinearLayout.LayoutParams securitySubtitleParams =
                wrap();

        securitySubtitleParams.topMargin =
                dp(5);

        securityInfo.addView(
                securitySubtitle,
                securitySubtitleParams
        );

        securityCard.addView(
                securityInfo,
                securityInfoParams
        );

        securityCard.addView(
                createPill(
                        calculateSecurityScore()
                                + "%",
                        calculateSecurityScore() >= 70
                                ? GREEN
                                : PURPLE_LIGHT,
                        calculateSecurityScore() >= 70
                                ? GREEN_SURFACE
                                : PURPLE_SURFACE
                ),
                params(
                        dp(55),
                        dp(30)
                )
        );

        pressEffect(
                securityCard
        );

        securityCard.setOnClickListener(
                v -> {

                    haptic(v);

                    showSecurityCenterScreen();
                }
        );

        content.addView(
                securityCard
        );

        root.addView(
                createBottomNavigation(
                        TAB_HOME
                )
        );

        setContentView(
                root
        );

        animateScreen(
                content
        );
    }

    private View createHomeStat(
            String value,
            String label
    ) {

        LinearLayout box =
                vertical();

        box.setGravity(
                Gravity.CENTER
        );

        box.setBackground(
                glassCardBackground(
                        dp(16)
                )
        );

        box.addView(
                text(
                        value,
                        17,
                        WHITE,
                        bold
                )
        );

        TextView labelView =
                text(
                        label,
                        8,
                        TEXT_3,
                        regular
                );

        LinearLayout.LayoutParams labelParams =
                wrap();

        labelParams.topMargin =
                dp(4);

        box.addView(
                labelView,
                labelParams
        );

        return box;
    }

    // =========================================================
    // ACCESSIBILITY — FIXED
    // =========================================================

    private boolean isAccessibilityServiceEnabled() {

        /*
         * Главный фикс:
         * проверяем конкретно AppLockService,
         * а не просто наличие любой Accessibility-службы.
         */
        ComponentName expectedComponent =
                new ComponentName(
                        this,
                        AppLockService.class
                );

        String expectedFull =
                expectedComponent.flattenToString();

        String expectedShort =
                expectedComponent.flattenToShortString();

        try {

            int accessibilityEnabled =
                    Settings.Secure.getInt(
                            getContentResolver(),
                            Settings.Secure.ACCESSIBILITY_ENABLED,
                            0
                    );

            if (accessibilityEnabled != 1) {

                return false;
            }

            String enabledServices =
                    Settings.Secure.getString(
                            getContentResolver(),
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    );

            if (enabledServices == null ||
                    enabledServices.trim().isEmpty()) {

                return false;
            }

            TextUtils.SimpleStringSplitter splitter =
                    new TextUtils.SimpleStringSplitter(
                            ':'
                    );

            splitter.setString(
                    enabledServices
            );

            while (splitter.hasNext()) {

                String value =
                        splitter.next();

                if (value == null) {
                    continue;
                }

                ComponentName enabledComponent =
                        ComponentName.unflattenFromString(
                                value
                        );

                if (enabledComponent != null &&
                        enabledComponent.equals(
                                expectedComponent
                        )) {

                    return true;
                }

                /*
                 * Дополнительная совместимость с некоторыми
                 * прошивками Android, которые возвращают
                 * немного отличающийся формат ComponentName.
                 */
                if (value.equalsIgnoreCase(
                        expectedFull
                )
                        ||
                        value.equalsIgnoreCase(
                                expectedShort
                        )) {

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }

    private boolean hasUsageStatsPermission() {

        try {

            AppOpsManager appOps =
                    (AppOpsManager)
                            getSystemService(
                                    Context.APP_OPS_SERVICE
                            );

            if (appOps == null) {
                return false;
            }

            int mode;

            if (android.os.Build.VERSION.SDK_INT >= 29) {

                mode =
                        appOps.unsafeCheckOpNoThrow(
                                AppOpsManager.OPSTR_GET_USAGE_STATS,
                                android.os.Process.myUid(),
                                getPackageName()
                        );

            } else {

                mode =
                        appOps.checkOpNoThrow(
                                AppOpsManager.OPSTR_GET_USAGE_STATS,
                                android.os.Process.myUid(),
                                getPackageName()
                        );
            }

            return mode ==
                    AppOpsManager.MODE_ALLOWED;

        } catch (Exception ignored) {

            return false;
        }
    }

    private void showAccessibilityPermissionDialog() {

        /*
         * Не показываем диалог, если разрешение
         * уже действительно включено.
         */
        if (isAccessibilityServiceEnabled()) {

            if (hasUsageStatsPermission()) {

                showAppLock();

            } else {

                showUsageAccessDialog();
            }

            return;
        }

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        LinearLayout box =
                vertical();

        box.setPadding(
                dp(22),
                dp(22),
                dp(22),
                dp(20)
        );

        box.setBackground(
                glassDialogBackground()
        );

        box.addView(
                text(
                        "Accessibility Protection",
                        20,
                        WHITE,
                        bold
                )
        );

        gap(
                box,
                8
        );

        TextView description =
                text(
                        "Enable LockVault in Accessibility settings so protected apps can be detected.",
                        11,
                        TEXT_2,
                        regular
                );

        description.setLineSpacing(
                dp(3),
                1f
        );

        box.addView(
                description
        );

        gap(
                box,
                18
        );

        TextView open =
                createPrimaryButton(
                        "Open Settings"
                );

        box.addView(
                open,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        gap(
                box,
                7
        );

        TextView cancel =
                createGhostButton(
                        "Cancel"
                );

        box.addView(
                cancel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(43)
                )
        );

        open.setOnClickListener(
                v -> {

                    haptic(v);

                    dialog.dismiss();

                    openingAppLock =
                            false;

                    try {

                        Intent intent =
                                new Intent(
                                        Settings.ACTION_ACCESSIBILITY_SETTINGS
                                );

                        startActivity(
                                intent
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Could not open Accessibility settings.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        cancel.setOnClickListener(
                v -> {

                    openingAppLock =
                            false;

                    dialog.dismiss();
                }
        );

        dialog.setOnDismissListener(
                d ->
                        openingAppLock = false
        );

        dialog.setContentView(
                box
        );

        showStyledDialog(
                dialog
        );
    }

    private void showUsageAccessDialog() {

        if (hasUsageStatsPermission()) {

            showAppLock();
            return;
        }

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        LinearLayout box =
                vertical();

        box.setPadding(
                dp(22),
                dp(22),
                dp(22),
                dp(20)
        );

        box.setBackground(
                glassDialogBackground()
        );

        box.addView(
                text(
                        "Usage Access",
                        20,
                        WHITE,
                        bold
                )
        );

        gap(
                box,
                8
        );

        box.addView(
                text(
                        "Allow LockVault usage access to improve protected-app detection.",
                        11,
                        TEXT_2,
                        regular
                )
        );

        gap(
                box,
                18
        );

        TextView open =
                createPrimaryButton(
                        "Open Settings"
                );

        box.addView(
                open,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(52)
                )
        );

        gap(
                box,
                7
        );

        TextView cancel =
                createGhostButton(
                        "Cancel"
                );

        box.addView(
                cancel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(43)
                )
        );

        open.setOnClickListener(
                v -> {

                    haptic(v);

                    dialog.dismiss();

                    openingAppLock =
                            false;

                    try {

                        Intent intent =
                                new Intent(
                                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                                );

                        startActivity(
                                intent
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Could not open Usage Access settings.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        cancel.setOnClickListener(
                v -> {

                    openingAppLock =
                            false;

                    dialog.dismiss();
                }
        );

        dialog.setOnDismissListener(
                d ->
                        openingAppLock = false
        );

        dialog.setContentView(
                box
        );

        showStyledDialog(
                dialog
        );
    }

    private void checkRequiredPermissions() {

        openingAppLock =
                false;

        if (!hasPin()) {

            showCreatePinDialog();
            return;
        }

        if (!isAccessibilityServiceEnabled()) {

            showAccessibilityPermissionDialog();
            return;
        }

        if (!hasUsageStatsPermission()) {

            showUsageAccessDialog();
            return;
        }

        showAppLock();
    }

    // =========================================================
    // PIN
    // =========================================================

    private boolean hasPin() {

        if (preferences == null) {
            return false;
        }

        String hash =
                preferences.getString(
                        KEY_PIN_HASH,
                        null
                );

        String salt =
                preferences.getString(
                        KEY_PIN_SALT,
                        null
                );

        return hash != null
                &&
                !hash.isEmpty()
                &&
                salt != null
                &&
                !salt.isEmpty();
    }

    private boolean isValidPin(
            String pin
    ) {

        if (pin == null ||
                pin.length() != 4) {

            return false;
        }

        for (int i = 0;
             i < pin.length();
             i++) {

            if (!Character.isDigit(
                    pin.charAt(i)
            )) {

                return false;
            }
        }

        return true;
    }

    private boolean savePin(
            String pin
    ) {

        if (!isValidPin(pin) ||
                preferences == null) {

            return false;
        }

        try {

            byte[] salt =
                    new byte[16];

            new SecureRandom()
                    .nextBytes(
                            salt
                    );

            byte[] hash =
                    derivePinHash(
                            pin,
                            salt
                    );

            String saltString =
                    Base64.encodeToString(
                            salt,
                            Base64.NO_WRAP
                    );

            String hashString =
                    Base64.encodeToString(
                            hash,
                            Base64.NO_WRAP
                    );

            return preferences
                    .edit()
                    .putString(
                            KEY_PIN_SALT,
                            saltString
                    )
                    .putString(
                            KEY_PIN_HASH,
                            hashString
                    )
                    .remove(
                            KEY_LEGACY_PIN
                    )
                    .commit();

        } catch (Exception ignored) {

            return false;
        }
    }

    private boolean verifyPin(
            String pin
    ) {

        if (pin == null ||
                preferences == null) {

            return false;
        }

        try {

            String saltString =
                    preferences.getString(
                            KEY_PIN_SALT,
                            null
                    );

            String hashString =
                    preferences.getString(
                            KEY_PIN_HASH,
                            null
                    );

            if (saltString == null ||
                    hashString == null) {

                return false;
            }

            byte[] salt =
                    Base64.decode(
                            saltString,
                            Base64.NO_WRAP
                    );

            byte[] savedHash =
                    Base64.decode(
                            hashString,
                            Base64.NO_WRAP
                    );

            byte[] enteredHash =
                    derivePinHash(
                            pin,
                            salt
                    );

            return MessageDigest.isEqual(
                    savedHash,
                    enteredHash
            );

        } catch (Exception ignored) {

            return false;
        }
    }

    private byte[] derivePinHash(
            String pin,
            byte[] salt
    ) throws Exception {

        PBEKeySpec spec =
                new PBEKeySpec(
                        pin.toCharArray(),
                        salt,
                        120000,
                        256
                );

        try {

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            return factory
                    .generateSecret(
                            spec
                    )
                    .getEncoded();

        } finally {

            spec.clearPassword();
        }
    }

    private void migrateLegacyPinIfNeeded() {

        if (preferences == null ||
                hasPin()) {

            return;
        }

        String legacy =
                preferences.getString(
                        KEY_LEGACY_PIN,
                        null
                );

        if (legacy == null ||
                legacy.isEmpty()) {

            return;
        }

        if (isValidPin(
                legacy
        )) {

            savePin(
                    legacy
            );
        }
    }

    // =========================================================
    // SECURITY
    // =========================================================

    private void performSecurityCheck() {

        loadLockedApps();
        migrateLegacyPinIfNeeded();
        syncVaultCounters();
    }

    private int calculateSecurityScore() {

        int score =
                0;

        if (hasPin()) {
            score += 30;
        }

        if (isAccessibilityServiceEnabled()) {
            score += 30;
        }

        if (hasUsageStatsPermission()) {
            score += 25;
        }

        if (!lockedPackages.isEmpty()) {
            score += 15;
        }

        return Math.min(
                100,
                score
        );
    }

    private String getSecurityLevel(
            int score
    ) {

        if (score >= 85) {
            return "Excellent protection";
        }

        if (score >= 70) {
            return "Protected";
        }

        if (score >= 40) {
            return "Needs attention";
        }

        return "Setup required";
    }

    private int getSecurityColor(
            int score
    ) {

        return score >= 70
                ? GREEN
                : PURPLE_LIGHT;
    }

    private String getLastSecurityCheckText() {

        return new SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
        ).format(
                new Date()
        );
    }

    private void addSecurityEvent(
            String value
    ) {

        /*
         * События пока хранятся только как лёгкая
         * локальная логика. UI T3 не меняем.
         */
    }

    // =========================================================
    // VAULT CORE
    // =========================================================

    private File getVaultDirectory(
            String name
    ) {

        File base =
                new File(
                        getFilesDir(),
                        "private_vault"
                );

        if (!base.exists()) {
            base.mkdirs();
        }

        File directory =
                new File(
                        base,
                        name
                );

        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory;
    }

    private int countVaultFiles(
            String directory
    ) {

        File[] files =
                getVaultDirectory(
                        directory
                ).listFiles();

        return files == null
                ? 0
                : files.length;
    }

    private List<File> getVaultFiles(
            String directory
    ) {

        List<File> result =
                new ArrayList<>();

        File[] files =
                getVaultDirectory(
                        directory
                ).listFiles();

        if (files != null) {

            Collections.addAll(
                    result,
                    files
            );

            Collections.sort(
                    result,
                    (first, second) ->
                            Long.compare(
                                    second.lastModified(),
                                    first.lastModified()
                            )
            );
        }

        return result;
    }

    private void syncVaultCounters() {

        if (preferences == null) {
            return;
        }

        preferences
                .edit()
                .putInt(
                        KEY_VAULT_PHOTOS,
                        countVaultFiles(
                                VAULT_PHOTOS_DIR
                        )
                )
                .putInt(
                        KEY_VAULT_VIDEOS,
                        countVaultFiles(
                                VAULT_VIDEOS_DIR
                        )
                )
                .putInt(
                        KEY_VAULT_NOTES,
                        countVaultFiles(
                                VAULT_NOTES_DIR
                        )
                )
                .apply();
    }

    private void openVaultPicker(
            String type
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            if ("video".equals(type)) {

                intent.setType(
                        "video/*"
                );

                startActivityForResult(
                        intent,
                        REQUEST_VAULT_VIDEO
                );

            } else {

                intent.setType(
                        "image/*"
                );

                startActivityForResult(
                        intent,
                        REQUEST_VAULT_PHOTO
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open file picker.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            return;
        }

        if (requestCode ==
                REQUEST_VAULT_PHOTO) {

            importVaultFile(
                    data.getData(),
                    VAULT_PHOTOS_DIR,
                    "photo"
            );

        } else if (requestCode ==
                REQUEST_VAULT_VIDEO) {

            importVaultFile(
                    data.getData(),
                    VAULT_VIDEOS_DIR,
                    "video"
            );
        }
    }

    private void importVaultFile(
            Uri uri,
            String directory,
            String prefix
    ) {

        if (uri == null) {
            return;
        }

        FileInputStream unused =
                null;

        java.io.InputStream input =
                null;

        FileOutputStream output =
                null;

        try {

            input =
                    getContentResolver()
                            .openInputStream(
                                    uri
                            );

            if (input == null) {
                return;
            }

            File target =
                    new File(
                            getVaultDirectory(
                                    directory
                            ),
                            prefix
                                    + "_"
                                    + System.currentTimeMillis()
                                    + ".vault"
                    );

            output =
                    new FileOutputStream(
                            target
                    );

            byte[] buffer =
                    new byte[8192];

            int read;

            while ((read =
                    input.read(
                            buffer
                    )) != -1) {

                output.write(
                        buffer,
                        0,
                        read
                );
            }

            output.flush();

            syncVaultCounters();

            addSecurityEvent(
                    "Private "
                            + prefix
                            + " added"
            );

            Toast.makeText(
                    this,
                    "Added to Private Vault.",
                    Toast.LENGTH_SHORT
            ).show();

            showVaultScreen();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not add file.",
                    Toast.LENGTH_SHORT
            ).show();

        } finally {

            try {

                if (input != null) {
                    input.close();
                }

            } catch (Exception ignored) {
            }

            try {

                if (output != null) {
                    output.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private void showCreateVaultNoteDialog() {

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        LinearLayout box =
                vertical();

        box.setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(18)
        );

        box.setBackground(
                glassDialogBackground()
        );

        box.addView(
                text(
                        "New Private Note",
                        20,
                        WHITE,
                        bold
                )
        );

        gap(
                box,
                8
        );

        EditText note =
                new EditText(this);

        note.setTextColor(
                WHITE
        );

        note.setHintTextColor(
                TEXT_3
        );

        note.setHint(
                "Write your private note..."
        );

        note.setTextSize(
                12
        );

        note.setTypeface(
                regular
        );

        note.setGravity(
                Gravity.TOP
                        |
                        Gravity.START
        );

        note.setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14)
        );

        note.setBackground(
                softCardBackground(
                        dp(16)
                )
        );

        box.addView(
                note,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(180)
                )
        );

        gap(
                box,
                14
        );

        TextView save =
                createPrimaryButton(
                        "Save Note"
                );

        box.addView(
                save,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(50)
                )
        );

        save.setOnClickListener(
                v -> {

                    String value =
                            note.getText()
                                    .toString()
                                    .trim();

                    if (value.isEmpty()) {

                        Toast.makeText(
                                this,
                                "Write something first.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    FileOutputStream output =
                            null;

                    try {

                        File file =
                                new File(
                                        getVaultDirectory(
                                                VAULT_NOTES_DIR
                                        ),
                                        "note_"
                                                + System.currentTimeMillis()
                                                + ".txt"
                                );

                        output =
                                new FileOutputStream(
                                        file
                                );

                        output.write(
                                value.getBytes(
                                        "UTF-8"
                                )
                        );

                        output.flush();

                        syncVaultCounters();

                        addSecurityEvent(
                                "Private note created"
                        );

                        dialog.dismiss();

                        showVaultScreen();

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Could not save note.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } finally {

                        try {

                            if (output != null) {
                                output.close();
                            }

                        } catch (Exception ignored) {
                        }
                    }
                }
        );

        dialog.setContentView(
                box
        );

        showStyledDialog(
                dialog
        );
    }

    // =========================================================
    // VAULT 3-COLUMN FULL SCREEN GALLERY
    // =========================================================

    private void showVaultPhotos() {

        showVaultMediaGrid(
                VAULT_PHOTOS_DIR,
                false,
                "Photos"
        );
    }

    private void showVaultVideos() {

        showVaultMediaGrid(
                VAULT_VIDEOS_DIR,
                true,
                "Videos"
        );
    }

    private void showVaultMediaGrid(
            String directory,
            boolean video,
            String titleValue
    ) {

        List<File> files =
                getVaultFiles(
                        directory
                );

        if (files.isEmpty()) {

            Toast.makeText(
                    this,
                    video
                            ? "No private videos yet."
                            : "No private photos yet.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        LinearLayout root =
                vertical();

        root.setBackgroundColor(
                BG
        );

        root.setPadding(
                dp(14),
                dp(16),
                dp(14),
                dp(14)
        );

        LinearLayout header =
                horizontal();

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView close =
                text(
                        "‹",
                        31,
                        WHITE,
                        regular
                );

        close.setGravity(
                Gravity.CENTER
        );

        close.setBackground(
                softCardBackground(
                        dp(14)
                )
        );

        close.setOnClickListener(
                v ->
                        dialog.dismiss()
        );

        header.addView(
                close,
                params(
                        dp(44),
                        dp(44)
                )
        );

        TextView title =
                text(
                        titleValue,
                        20,
                        WHITE,
                        bold
                );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        titleParams.leftMargin =
                dp(13);

        header.addView(
                title,
                titleParams
        );

        TextView count =
                createPill(
                        String.valueOf(
                                files.size()
                        ),
                        PURPLE_LIGHT,
                        PURPLE_SURFACE
                );

        header.addView(
                count,
                params(
                        dp(44),
                        dp(30)
                )
        );

        root.addView(
                header
        );

        gap(
                root,
                16
        );

        ScrollView scroll =
                new ScrollView(this);

        scroll.setVerticalScrollBarEnabled(
                false
        );

        LinearLayout rows =
                vertical();

        /*
         * Полноэкранная сетка:
         * ровно 3 элемента в каждом ряду.
         */
        for (int index = 0;
             index < files.size();
             index += 3) {

            LinearLayout row =
                    horizontal();

            row.setGravity(
                    Gravity.TOP
            );

            for (int column = 0;
                 column < 3;
                 column++) {

                int position =
                        index + column;

                LinearLayout.LayoutParams cellParams =
                        new LinearLayout.LayoutParams(
                                0,
                                dp(118),
                                1
                        );

                if (column > 0) {
                    cellParams.leftMargin =
                            dp(6);
                }

                if (position <
                        files.size()) {

                    File file =
                            files.get(
                                    position
                            );

                    View cell =
                            createVaultMediaCell(
                                    file,
                                    video,
                                    dialog
                            );

                    row.addView(
                            cell,
                            cellParams
                    );

                } else {

                    Space empty =
                            new Space(this);

                    row.addView(
                            empty,
                            cellParams
                    );
                }
            }

            rows.addView(
                    row
            );

            if (index + 3 <
                    files.size()) {

                gap(
                        rows,
                        6
                );
            }
        }

        scroll.addView(
                rows
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        dialog.setContentView(
                root
        );

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setBackgroundDrawable(
                    solid(
                            BG,
                            0
                    )
            );

            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        dialog.setOnShowListener(
                value -> {

                    Window shownWindow =
                            dialog.getWindow();

                    if (shownWindow != null) {

                        shownWindow.setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        );

                        shownWindow.setStatusBarColor(
                                BG
                        );

                        shownWindow.setNavigationBarColor(
                                BG
                        );
                    }
                }
        );

        dialog.show();
    }

    private View createVaultMediaCell(
            File file,
            boolean video,
            Dialog galleryDialog
    ) {

        LinearLayout cell =
                vertical();

        cell.setGravity(
                Gravity.CENTER
        );

        cell.setPadding(
                dp(4),
                dp(4),
                dp(4),
                dp(4)
        );

        cell.setBackground(
                softCardBackground(
                        dp(15)
                )
        );

        ImageView preview =
                new ImageView(this);

        preview.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        if (!video) {

            try {

                android.graphics.Bitmap bitmap =
                        android.graphics.BitmapFactory
                                .decodeFile(
                                        file.getAbsolutePath()
                                );

                if (bitmap != null) {

                    preview.setImageBitmap(
                            bitmap
                    );

                } else {

                    preview.setImageDrawable(
                            new LockVaultIconDrawable(
                                    ICON_PHOTO,
                                    PURPLE_LIGHT,
                                    2
                            )
                    );
                }

            } catch (Exception ignored) {

                preview.setImageDrawable(
                        new LockVaultIconDrawable(
                                ICON_PHOTO,
                                PURPLE_LIGHT,
                                2
                        )
                );
            }

        } else {

            /*
             * Видео остаётся визуально понятным даже
             * на устройствах/AIDE без дополнительных
             * библиотек thumbnail.
             */
            preview.setImageDrawable(
                    new LockVaultIconDrawable(
                            ICON_VIDEO,
                            PURPLE_LIGHT,
                            2
                    )
            );
        }

        cell.addView(
                preview,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        pressEffect(
                cell
        );

        cell.setOnClickListener(
                v -> {

                    haptic(v);

                    showVaultMediaViewer(
                            file,
                            video,
                            galleryDialog
                    );
                }
        );

        return cell;
    }

    private void showVaultMediaViewer(
            File file,
            boolean video,
            Dialog galleryDialog
    ) {

        if (file == null ||
                !file.exists()) {

            return;
        }

        final Dialog viewer =
                new Dialog(this);

        viewer.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        LinearLayout root =
                vertical();

        root.setGravity(
                Gravity.CENTER
        );

        root.setBackgroundColor(
                BG
        );

        root.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        LinearLayout header =
                horizontal();

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView close =
                text(
                        "×",
                        30,
                        WHITE,
                        regular
                );

        close.setGravity(
                Gravity.CENTER
        );

        close.setOnClickListener(
                v ->
                        viewer.dismiss()
        );

        header.addView(
                text(
                        video
                                ? "Private Video"
                                : "Private Photo",
                        18,
                        WHITE,
                        bold
                ),
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        header.addView(
                close,
                params(
                        dp(44),
                        dp(44)
                )
        );

        root.addView(
                header
        );

        gap(
                root,
                12
        );

        if (!video) {

            ImageView image =
                    new ImageView(this);

            image.setScaleType(
                    ImageView.ScaleType.FIT_CENTER
            );

            try {

                android.graphics.Bitmap bitmap =
                        android.graphics.BitmapFactory
                                .decodeFile(
                                        file.getAbsolutePath()
                                );

                image.setImageBitmap(
                        bitmap
                );

            } catch (Exception ignored) {
            }

            root.addView(
                    image,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1
                    )
            );

        } else {

            LinearLayout videoHolder =
                    vertical();

            videoHolder.setGravity(
                    Gravity.CENTER
            );

            videoHolder.setBackground(
                    softCardBackground(
                            dp(22)
                    )
            );

            videoHolder.addView(
                    createVectorIcon(
                            ICON_VIDEO,
                            PURPLE_LIGHT,
                            4
                    ),
                    params(
                            dp(72),
                            dp(72)
                    )
            );

            gap(
                    videoHolder,
                    14
            );

            videoHolder.addView(
                    text(
                            "Private video",
                            15,
                            WHITE,
                            medium
                    )
            );

            root.addView(
                    videoHolder,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1
                    )
            );
        }

        gap(
                root,
                12
        );

        TextView delete =
                createGhostButton(
                        "Delete"
                );

        delete.setTextColor(
                RED
        );

        delete.setOnClickListener(
                v -> {

                    haptic(v);

                    if (file.delete()) {

                        syncVaultCounters();

                        viewer.dismiss();

                        if (galleryDialog != null &&
                                galleryDialog.isShowing()) {

                            galleryDialog.dismiss();
                        }

                        if (video) {

                            showVaultVideos();

                        } else {

                            showVaultPhotos();
                        }
                    }
                }
        );

        root.addView(
                delete,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(48)
                )
        );

        viewer.setContentView(
                root
        );

        viewer.setOnShowListener(
                value -> {

                    Window window =
                            viewer.getWindow();

                    if (window != null) {

                        window.setLayout(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        );

                        window.setStatusBarColor(
                                BG
                        );

                        window.setNavigationBarColor(
                                BG
                        );
                    }
                }
        );

        viewer.show();
    }

    private String formatVaultFileDate(
            File file
    ) {

        if (file == null) {
            return "";
        }

        return new SimpleDateFormat(
                "dd MMM · HH:mm",
                Locale.getDefault()
        ).format(
                new Date(
                        file.lastModified()
                )
        );
    }

    // =========================================================
    // LANGUAGE
    // =========================================================

    private String getLanguageDisplayName() {

        String language =
                preferences.getString(
                        KEY_LANGUAGE,
                        "en"
                );

        if ("ru".equals(language)) {
            return "Русский";
        }

        if ("de".equals(language)) {
            return "Deutsch";
        }

        if ("ja".equals(language)) {
            return "日本語";
        }

        if ("en-gb".equals(language)) {
            return "English (UK)";
        }

        return "English";
    }

    private void showLanguageDialog() {

        final Dialog dialog =
                new Dialog(this);

        dialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        LinearLayout box =
                vertical();

        box.setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(18)
        );

        box.setBackground(
                glassDialogBackground()
        );

        box.addView(
                text(
                        "Language",
                        20,
                        WHITE,
                        bold
                )
        );

        gap(
                box,
                14
        );

        String[][] languages = {
                {"English", "en"},
                {"English (UK)", "en-gb"},
                {"Русский", "ru"},
                {"Deutsch", "de"},
                {"日本語", "ja"}
        };

        for (int i = 0;
             i < languages.length;
             i++) {

            final String name =
                    languages[i][0];

            final String code =
                    languages[i][1];

            TextView row =
                    createGhostButton(
                            name
                    );

            row.setGravity(
                    Gravity.CENTER_VERTICAL
                            |
                            Gravity.START
            );

            row.setPadding(
                    dp(15),
                    0,
                    dp(15),
                    0
            );

            row.setOnClickListener(
                    v -> {

                        preferences
                                .edit()
                                .putString(
                                        KEY_LANGUAGE,
                                        code
                                )
                                .apply();

                        dialog.dismiss();

                        refreshCurrentScreen();
                    }
            );

            box.addView(
                    row,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(48)
                    )
            );

            if (i <
                    languages.length - 1) {

                gap(
                        box,
                        7
                );
            }
        }

        dialog.setContentView(
                box
        );

        showStyledDialog(
                dialog
        );
    }

    private String tr(
            String key
    ) {

        /*
         * Оставляем стабильный fallback, чтобы проект
         * собирался без обязательных XML resources.
         */
        if ("security_center".equals(key)) return "Security Center";
        if ("security_status".equals(key)) return "Security status";
        if ("protected".equals(key)) return "Protected";
        if ("action_required".equals(key)) return "Action required";
        if ("accessibility_protection".equals(key)) return "Accessibility protection";
        if ("usage_access".equals(key)) return "Usage access";
        if ("private_pin".equals(key)) return "Private PIN";
        if ("check".equals(key)) return "Check";
        if ("app_lock".equals(key)) return "App Lock";
        if ("protected_apps".equals(key)) return "protected apps";
        if ("protect_private_apps".equals(key)) return "Protect private applications";
        if ("apps".equals(key)) return "Apps";
        if ("private_vault".equals(key)) return "Private Vault";
        if ("your_private_space".equals(key)) return "Your private space";
        if ("vault_description".equals(key)) return "Private files stored locally on your device.";
        if ("local".equals(key)) return "Local";
        if ("photos".equals(key)) return "Photos";
        if ("videos".equals(key)) return "Videos";
        if ("notes".equals(key)) return "Notes";
        if ("private_collections".equals(key)) return "Private collections";
        if ("private_items".equals(key)) return "private items";
        if ("add_photo".equals(key)) return "Add photo";
        if ("add_video".equals(key)) return "Add video";
        if ("new_note".equals(key)) return "New note";
        if ("no_private_items".equals(key)) return "No private items";
        if ("view".equals(key)) return "View";
        if ("delete".equals(key)) return "Delete";
        if ("close".equals(key)) return "Close";
        if ("settings".equals(key)) return "Settings";
        if ("privacy_controls".equals(key)) return "Privacy controls";
        if ("security".equals(key)) return "Security";
        if ("setup".equals(key)) return "Setup";
        if ("active".equals(key)) return "Active";
        if ("privacy_tools".equals(key)) return "Privacy tools";
        if ("stored_locally".equals(key)) return "Stored locally";
        if ("language".equals(key)) return "Language";
        if ("home".equals(key)) return "Home";
        if ("vault".equals(key)) return "Vault";
        if ("cancel".equals(key)) return "Cancel";

        return key;
    }

    // =========================================================
    // DIALOG
    // =========================================================

    private void showStyledDialog(
            Dialog dialog
    ) {

        if (dialog == null) {
            return;
        }

        dialog.setOnShowListener(
                value -> {

                    Window window =
                            dialog.getWindow();

                    if (window != null) {

                        window.setBackgroundDrawableResource(
                                android.R.color.transparent
                        );

                        window.setLayout(
                                (int) (
                                        getResources()
                                                .getDisplayMetrics()
                                                .widthPixels
                                                * 0.90f
                                ),
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                    }
                }
        );

        dialog.show();
    }

    private void refreshCurrentScreen() {

        if (TAB_APPS.equals(
                currentTab
        )) {

            showAppLock();

        } else if (TAB_VAULT.equals(
                currentTab
        )) {

            showVaultScreen();

        } else if (TAB_SETTINGS.equals(
                currentTab
        )) {

            showSettingsScreen();

        } else {

            showHome();
        }
    }

    // =========================================================
    // BACKGROUNDS
    // =========================================================


    private class AmbientBackgroundDrawable
            extends Drawable {

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        @Override
        public void draw(
                Canvas canvas
        ) {

            if (canvas == null) {
                return;
            }

            int width =
                    getBounds().width();

            int height =
                    getBounds().height();

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    BG
            );

            canvas.drawRect(
                    getBounds(),
                    paint
            );

            drawGlow(
                    canvas,
                    width * 0.12f,
                    height * 0.05f,
                    Math.max(width, height) * 0.28f,
                    Color.argb(82, 130, 82, 220)
            );

            drawGlow(
                    canvas,
                    width * 0.94f,
                    height * 0.27f,
                    Math.max(width, height) * 0.22f,
                    Color.argb(54, 93, 218, 155)
            );

            drawGlow(
                    canvas,
                    width * 0.50f,
                    height * 1.03f,
                    Math.max(width, height) * 0.24f,
                    Color.argb(46, 201, 174, 255)
            );
        }

        private void drawGlow(
                Canvas canvas,
                float cx,
                float cy,
                float radius,
                int color
        ) {

            paint.setColor(
                    color
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    radius,
                    paint
            );
        }

        @Override
        public void setAlpha(
                int alpha
        ) {

            paint.setAlpha(
                    alpha
            );
        }

        @Override
        public void setColorFilter(
                android.graphics.ColorFilter colorFilter
        ) {

            paint.setColorFilter(
                    colorFilter
            );
        }

        @Override
        public int getOpacity() {

            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    private Drawable appBackground() {

        return new PremiumBackgroundDrawable(
                BG,
                new int[]{
                        Color.argb(84, 130, 82, 220),
                        Color.argb(50, 93, 218, 155),
                        Color.argb(44, 201, 174, 255)
                },
                new float[]{
                        0.12f,
                        0.94f,
                        0.50f
                },
                new float[]{
                        0.05f,
                        0.27f,
                        1.03f
                },
                new float[]{
                        0.28f,
                        0.22f,
                        0.24f
                }
        );
    }

    private Drawable glassCardBackground(
            float radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                Color.rgb(31, 28, 41),
                                Color.rgb(22, 21, 29)
                        }
                );

        drawable.setCornerRadius(
                radius
        );

        drawable.setStroke(
                dp(1),
                BORDER_SOFT
        );

        return drawable;
    }

    private Drawable softCardBackground(
            float radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                SURFACE
        );

        drawable.setCornerRadius(
                radius
        );

        drawable.setStroke(
                dp(1),
                BORDER_SOFT
        );

        return drawable;
    }

    private Drawable activeProtectionBackground(
            float radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                Color.rgb(39, 31, 52),
                                Color.rgb(25, 22, 33)
                        }
                );

        drawable.setCornerRadius(
                radius
        );

        drawable.setStroke(
                dp(1),
                Color.rgb(75, 57, 99)
        );

        return drawable;
    }

    private Drawable glassDialogBackground() {

        GradientDrawable drawable =
                new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{
                                Color.rgb(31, 28, 42),
                                Color.rgb(20, 19, 27)
                        }
                );

        drawable.setCornerRadius(
                dp(25)
        );

        drawable.setStroke(
                dp(1),
                Color.rgb(67, 57, 82)
        );

        return drawable;
    }

    private void applySoftDepth(
            View view
    ) {

        if (view == null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {

            view.setElevation(
                    dp(3)
            );
        }
    }

    private void applyActiveGlow(
            View view
    ) {

        if (view == null) {
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {

            view.setElevation(
                    dp(6)
            );
        }
    }

    // =========================================================
    // VECTOR ICONS
    // =========================================================

    private ImageView createVectorIcon(
            int iconType,
            int color,
            int stroke
    ) {

        ImageView view =
                new ImageView(this);

        view.setImageDrawable(
                new LockVaultIconDrawable(
                        iconType,
                        color,
                        stroke
                )
        );

        view.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        return view;
    }

    private View createFeatureVectorIcon(
            int iconType
    ) {

        LinearLayout holder =
                horizontal();

        holder.setGravity(
                Gravity.CENTER
        );

        holder.setBackground(
                gradient(
                        new int[]{
                                Color.rgb(55, 42, 73),
                                Color.rgb(35, 30, 47)
                        },
                        dp(16)
                )
        );

        holder.addView(
                createVectorIcon(
                        iconType,
                        PURPLE_LIGHT,
                        3
                ),
                params(
                        dp(29),
                        dp(29)
                )
        );

        return holder;
    }

    private View createPremiumVectorIcon() {

        LinearLayout holder =
                horizontal();

        holder.setGravity(
                Gravity.CENTER
        );

        holder.setBackground(
                gradient(
                        new int[]{
                                Color.rgb(83, 57, 116),
                                Color.rgb(46, 35, 62)
                        },
                        dp(21)
                )
        );

        holder.addView(
                createVectorIcon(
                        ICON_PREMIUM,
                        PURPLE_LIGHT,
                        4
                ),
                params(
                        dp(38),
                        dp(38)
                )
        );

        return holder;
    }

    private static class LockVaultIconDrawable
            extends Drawable {

        private final int type;

        private final Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        private final Path path =
                new Path();

        private final int strokeWidth;

        LockVaultIconDrawable(
                int type,
                int color
        ) {

            this(
                    type,
                    color,
                    3
            );
        }

        LockVaultIconDrawable(
                int type,
                int color,
                int strokeWidth
        ) {

            this.type =
                    type;

            this.strokeWidth =
                    strokeWidth;

            paint.setColor(
                    color
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeCap(
                    Paint.Cap.ROUND
            );

            paint.setStrokeJoin(
                    Paint.Join.ROUND
            );
        }

        @Override
        public void draw(
                Canvas canvas
        ) {

            if (canvas == null) {
                return;
            }

            float width =
                    getBounds().width();

            float height =
                    getBounds().height();

            float size =
                    Math.min(
                            width,
                            height
                    );

            float left =
                    getBounds().left
                            +
                            (width - size)
                                    / 2f;

            float top =
                    getBounds().top
                            +
                            (height - size)
                                    / 2f;

            float cx =
                    left
                            +
                            size / 2f;

            float cy =
                    top
                            +
                            size / 2f;

            float unit =
                    size / 24f;

            paint.setStrokeWidth(
                    Math.max(
                            1f,
                            unit
                                    *
                                    strokeWidth
                                    *
                                    0.65f
                    )
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            path.reset();

            switch (type) {

                case ICON_HOME:

                    path.moveTo(
                            cx - 8 * unit,
                            cy
                    );

                    path.lineTo(
                            cx,
                            cy - 7 * unit
                    );

                    path.lineTo(
                            cx + 8 * unit,
                            cy
                    );

                    path.moveTo(
                            cx - 6 * unit,
                            cy - unit
                    );

                    path.lineTo(
                            cx - 6 * unit,
                            cy + 7 * unit
                    );

                    path.lineTo(
                            cx + 6 * unit,
                            cy + 7 * unit
                    );

                    path.lineTo(
                            cx + 6 * unit,
                            cy - unit
                    );

                    canvas.drawPath(
                            path,
                            paint
                    );

                    break;

                case ICON_APPS:

                    drawRoundedSquare(
                            canvas,
                            cx - 7 * unit,
                            cy - 7 * unit,
                            5 * unit,
                            paint
                    );

                    drawRoundedSquare(
                            canvas,
                            cx + 2 * unit,
                            cy - 7 * unit,
                            5 * unit,
                            paint
                    );

                    drawRoundedSquare(
                            canvas,
                            cx - 7 * unit,
                            cy + 2 * unit,
                            5 * unit,
                            paint
                    );

                    drawRoundedSquare(
                            canvas,
                            cx + 2 * unit,
                            cy + 2 * unit,
                            5 * unit,
                            paint
                    );

                    break;

                case ICON_VAULT:

                    canvas.drawRoundRect(
                            new android.graphics.RectF(
                                    cx - 8 * unit,
                                    cy - 7 * unit,
                                    cx + 8 * unit,
                                    cy + 7 * unit
                            ),
                            3 * unit,
                            3 * unit,
                            paint
                    );

                    canvas.drawCircle(
                            cx,
                            cy,
                            3.2f * unit,
                            paint
                    );

                    canvas.drawLine(
                            cx,
                            cy - 3 * unit,
                            cx,
                            cy + 3 * unit,
                            paint
                    );

                    canvas.drawLine(
                            cx - 3 * unit,
                            cy,
                            cx + 3 * unit,
                            cy,
                            paint
                    );

                    break;

                case ICON_SETTINGS:

                    canvas.drawCircle(
                            cx,
                            cy,
                            3.2f * unit,
                            paint
                    );

                    for (int i = 0;
                         i < 8;
                         i++) {

                        double angle =
                                Math.PI
                                        * 2
                                        * i
                                        / 8;

                        float x1 =
                                cx
                                        +
                                        (float) Math.cos(angle)
                                                *
                                                6 * unit;

                        float y1 =
                                cy
                                        +
                                        (float) Math.sin(angle)
                                                *
                                                6 * unit;

                        float x2 =
                                cx
                                        +
                                        (float) Math.cos(angle)
                                                *
                                                8 * unit;

                        float y2 =
                                cy
                                        +
                                        (float) Math.sin(angle)
                                                *
                                                8 * unit;

                        canvas.drawLine(
                                x1,
                                y1,
                                x2,
                                y2,
                                paint
                        );
                    }

                    break;

                case ICON_LOCK:

                    canvas.drawRoundRect(
                            new android.graphics.RectF(
                                    cx - 7 * unit,
                                    cy - unit,
                                    cx + 7 * unit,
                                    cy + 8 * unit
                            ),
                            2.5f * unit,
                            2.5f * unit,
                            paint
                    );

                    canvas.drawArc(
                            new android.graphics.RectF(
                                    cx - 5 * unit,
                                    cy - 8 * unit,
                                    cx + 5 * unit,
                                    cy + 3 * unit
                            ),
                            180,
                            180,
                            false,
                            paint
                    );

                    break;

                case ICON_SECURITY:
                case ICON_PERMISSION:

                    path.moveTo(
                            cx,
                            cy - 9 * unit
                    );

                    path.lineTo(
                            cx + 7 * unit,
                            cy - 6 * unit
                    );

                    path.lineTo(
                            cx + 6 * unit,
                            cy + 2 * unit
                    );

                    path.quadTo(
                            cx + 4 * unit,
                            cy + 7 * unit,
                            cx,
                            cy + 9 * unit
                    );

                    path.quadTo(
                            cx - 4 * unit,
                            cy + 7 * unit,
                            cx - 6 * unit,
                            cy + 2 * unit
                    );

                    path.lineTo(
                            cx - 7 * unit,
                            cy - 6 * unit
                    );

                    path.close();

                    canvas.drawPath(
                            path,
                            paint
                    );

                    if (type ==
                            ICON_SECURITY) {

                        path.reset();

                        path.moveTo(
                                cx - 3 * unit,
                                cy
                        );

                        path.lineTo(
                                cx - unit,
                                cy + 2 * unit
                        );

                        path.lineTo(
                                cx + 4 * unit,
                                cy - 3 * unit
                        );

                        canvas.drawPath(
                                path,
                                paint
                        );
                    }

                    break;

                case ICON_PRIVACY:

                    path.moveTo(
                            cx - 9 * unit,
                            cy
                    );

                    path.quadTo(
                            cx,
                            cy - 8 * unit,
                            cx + 9 * unit,
                            cy
                    );

                    path.quadTo(
                            cx,
                            cy + 8 * unit,
                            cx - 9 * unit,
                            cy
                    );

                    path.close();

                    canvas.drawPath(
                            path,
                            paint
                    );

                    canvas.drawCircle(
                            cx,
                            cy,
                            2.8f * unit,
                            paint
                    );

                    break;

                case ICON_BIOMETRIC:

                    canvas.drawArc(
                            new android.graphics.RectF(
                                    cx - 7 * unit,
                                    cy - 8 * unit,
                                    cx + 7 * unit,
                                    cy + 8 * unit
                            ),
                            200,
                            140,
                            false,
                            paint
                    );

                    canvas.drawArc(
                            new android.graphics.RectF(
                                    cx - 5 * unit,
                                    cy - 6 * unit,
                                    cx + 5 * unit,
                                    cy + 6 * unit
                            ),
                            195,
                            150,
                            false,
                            paint
                    );

                    canvas.drawArc(
                            new android.graphics.RectF(
                                    cx - 3 * unit,
                                    cy - 4 * unit,
                                    cx + 3 * unit,
                                    cy + 5 * unit
                            ),
                            180,
                            180,
                            false,
                            paint
                    );

                    break;

                case ICON_CHECK:

                    path.moveTo(
                            cx - 7 * unit,
                            cy
                    );

                    path.lineTo(
                            cx - 2 * unit,
                            cy + 5 * unit
                    );

                    path.lineTo(
                            cx + 8 * unit,
                            cy - 6 * unit
                    );

                    canvas.drawPath(
                            path,
                            paint
                    );

                    break;

                case ICON_PHOTO:

                    canvas.drawRoundRect(
                            new android.graphics.RectF(
                                    cx - 8 * unit,
                                    cy - 7 * unit,
                                    cx + 8 * unit,
                                    cy + 7 * unit
                            ),
                            2 * unit,
                            2 * unit,
                            paint
                    );

                    canvas.drawCircle(
                            cx + 4 * unit,
                            cy - 3 * unit,
                            1.5f * unit,
                            paint
                    );

                    path.moveTo(
                            cx - 7 * unit,
                            cy + 5 * unit
                    );

                    path.lineTo(
                            cx - 2 * unit,
                            cy
                    );

                    path.lineTo(
                            cx + unit,
                            cy + 3 * unit
                    );

                    path.lineTo(
                            cx + 4 * unit,
                            cy
                    );

                    path.lineTo(
                            cx + 8 * unit,
                            cy + 5 * unit
                    );

                    canvas.drawPath(
                            path,
                            paint
                    );

                    break;

                case ICON_VIDEO:

                    canvas.drawRoundRect(
                            new android.graphics.RectF(
                                    cx - 9 * unit,
                                    cy - 6 * unit,
                                    cx + 3 * unit,
                                    cy + 6 * unit
                            ),
                            2 * unit,
                            2 * unit,
                            paint
                    );

                    path.moveTo(
                            cx + 3 * unit,
                            cy - 3 * unit
                    );

                    path.lineTo(
                            cx + 9 * unit,
                            cy - 6 * unit
                    );

                    path.lineTo(
                            cx + 9 * unit,
                            cy + 6 * unit
                    );

                    path.lineTo(
                            cx + 3 * unit,
                            cy + 3 * unit
                    );

                    canvas.drawPath(
                            path,
                            paint
                    );

                    break;

                case ICON_NOTE:

                    canvas.drawRoundRect(
                            new android.graphics.RectF(
                                    cx - 7 * unit,
                                    cy - 9 * unit,
                                    cx + 7 * unit,
                                    cy + 9 * unit
                            ),
                            2 * unit,
                            2 * unit,
                            paint
                    );

                    canvas.drawLine(
                            cx - 4 * unit,
                            cy - 3 * unit,
                            cx + 4 * unit,
                            cy - 3 * unit,
                            paint
                    );

                    canvas.drawLine(
                            cx - 4 * unit,
                            cy + unit,
                            cx + 4 * unit,
                            cy + unit,
                            paint
                    );

                    canvas.drawLine(
                            cx - 4 * unit,
                            cy + 5 * unit,
                            cx + unit,
                            cy + 5 * unit,
                            paint
                    );

                    break;

                case ICON_BELL:

                    canvas.drawArc(
                            new android.graphics.RectF(
                                    cx - 6 * unit,
                                    cy - 7 * unit,
                                    cx + 6 * unit,
                                    cy + 6 * unit
                            ),
                            180,
                            180,
                            false,
                            paint
                    );

                    canvas.drawLine(
                            cx - 6 * unit,
                            cy,
                            cx - 7 * unit,
                            cy + 5 * unit,
                            paint
                    );

                    canvas.drawLine(
                            cx + 6 * unit,
                            cy,
                            cx + 7 * unit,
                            cy + 5 * unit,
                            paint
                    );

                    canvas.drawLine(
                            cx - 7 * unit,
                            cy + 5 * unit,
                            cx + 7 * unit,
                            cy + 5 * unit,
                            paint
                    );

                    break;

                case ICON_PREMIUM:

                    path.moveTo(
                            cx - 9 * unit,
                            cy - 4 * unit
                    );

                    path.lineTo(
                            cx - 4 * unit,
                            cy + 7 * unit
                    );

                    path.lineTo(
                            cx + 4 * unit,
                            cy + 7 * unit
                    );

                    path.lineTo(
                            cx + 9 * unit,
                            cy - 4 * unit
                    );

                    path.lineTo(
                            cx + 4 * unit,
                            cy
                    );

                    path.lineTo(
                            cx,
                            cy - 7 * unit
                    );

                    path.lineTo(
                            cx - 4 * unit,
                            cy
                    );

                    path.close();

                    canvas.drawPath(
                            path,
                            paint
                    );

                    break;

                case ICON_RESET:

                    canvas.drawArc(
                            new android.graphics.RectF(
                                    cx - 7 * unit,
                                    cy - 7 * unit,
                                    cx + 7 * unit,
                                    cy + 7 * unit
                            ),
                            35,
                            285,
                            false,
                            paint
                    );

                    path.moveTo(
                            cx - 8 * unit,
                            cy - 5 * unit
                    );

                    path.lineTo(
                            cx - 8 * unit,
                            cy + unit
                    );

                    path.lineTo(
                            cx - 2 * unit,
                            cy - unit
                    );

                    canvas.drawPath(
                            path,
                            paint
                    );

                    break;
            }
        }

        private void drawRoundedSquare(
                Canvas canvas,
                float x,
                float y,
                float size,
                Paint paint
        ) {

            canvas.drawRoundRect(
                    new android.graphics.RectF(
                            x,
                            y,
                            x + size,
                            y + size
                    ),
                    size * 0.25f,
                    size * 0.25f,
                    paint
            );
        }

        @Override
        public void setAlpha(
                int alpha
        ) {

            paint.setAlpha(
                    alpha
            );

            invalidateSelf();
        }

        @Override
        public void setColorFilter(
                android.graphics.ColorFilter colorFilter
        ) {

            paint.setColorFilter(
                    colorFilter
            );

            invalidateSelf();
        }

        @Override
        public int getOpacity() {

            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    // =========================================================
    // SECURITY CENTER — T3
    // ЧАСТЬ 2 НАЧИНАЕТСЯ ОТСЮДА
    // =========================================================
    // ЧАСТЬ 2/2 — MainActivity.java
// Продолжение сразу после:
// SECURITY CENTER — T3
// ЧАСТЬ 2 НАЧИНАЕТСЯ ОТСЮДА

private void showSecurityCenterScreen() {

    currentTab = TAB_SETTINGS;

    performSecurityCheck();

    root = vertical();
    root.setBackground(
            appBackground()
    );

    ScrollView scroll =
            new ScrollView(this);

    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.setOverScrollMode(
            View.OVER_SCROLL_NEVER
    );

    LinearLayout content =
            vertical();

    content.setPadding(
            dp(22),
            dp(20),
            dp(22),
            dp(31)
    );

    scroll.addView(content);

    root.addView(
            scroll,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1
            )
    );

    content.addView(
            createScreenHeader(
                    tr("security_center"),
                    tr("security_status"),
                    this::showHome
            )
    );

    gap(content, 24);

    int score =
            calculateSecurityScore();

    LinearLayout hero =
            vertical();

    hero.setPadding(
            dp(21),
            dp(21),
            dp(21),
            dp(21)
    );

    hero.setBackground(
            activeProtectionBackground(
                    dp(25)
            )
    );

    applyActiveGlow(hero);

    LinearLayout heroTop =
            horizontal();

    heroTop.setGravity(
            Gravity.CENTER_VERTICAL
    );

    LinearLayout icon =
            horizontal();

    icon.setGravity(
            Gravity.CENTER
    );

    icon.setBackground(
            gradient(
                    new int[]{
                            Color.rgb(79, 57, 106),
                            Color.rgb(42, 34, 57)
                    },
                    dp(19)
            )
    );

    icon.addView(
            createVectorIcon(
                    ICON_SECURITY,
                    PURPLE_LIGHT,
                    4
            ),
            params(dp(37), dp(37))
    );

    heroTop.addView(
            icon,
            params(dp(64), dp(64))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(15);

    info.addView(
            text(
                    score + "%",
                    27,
                    WHITE,
                    bold
            )
    );

    TextView level =
            text(
                    getSecurityLevel(score),
                    10,
                    getSecurityColor(score),
                    medium
            );

    LinearLayout.LayoutParams levelParams =
            wrap();

    levelParams.topMargin =
            dp(4);

    info.addView(
            level,
            levelParams
    );

    heroTop.addView(
            info,
            infoParams
    );

    heroTop.addView(
            createPill(
                    score >= 70
                            ? tr("protected")
                            : tr("action_required"),
                    score >= 70
                            ? GREEN
                            : PURPLE_LIGHT,
                    score >= 70
                            ? GREEN_SURFACE
                            : PURPLE_SURFACE
            ),
            params(dp(86), dp(30))
    );

    hero.addView(heroTop);

    gap(hero, 18);

    TextView lastCheck =
            text(
                    "Last check · "
                            + getLastSecurityCheckText(),
                    9,
                    TEXT_3,
                    regular
            );

    hero.addView(lastCheck);

    content.addView(hero);

    gap(content, 26);

    content.addView(
            sectionTitle(
                    tr("security_status"),
                    score + "%"
            )
    );

    gap(content, 11);

    LinearLayout checks =
            vertical();

    checks.setPadding(
            dp(16),
            dp(15),
            dp(16),
            dp(15)
    );

    checks.setBackground(
            softCardBackground(
                    dp(19)
            )
    );

    checks.addView(
            createSecurityActionRow(
                    ICON_PERMISSION,
                    tr("accessibility_protection"),
                    isAccessibilityServiceEnabled(),
                    () -> {
                        if (!isAccessibilityServiceEnabled()) {
                            showAccessibilityPermissionDialog();
                        }
                    }
            )
    );

    gap(checks, 11);

    checks.addView(
            createSecurityActionRow(
                    ICON_SECURITY,
                    tr("usage_access"),
                    hasUsageStatsPermission(),
                    () -> {
                        if (!hasUsageStatsPermission()) {
                            showUsageAccessDialog();
                        }
                    }
            )
    );

    gap(checks, 11);

    checks.addView(
            createSecurityActionRow(
                    ICON_LOCK,
                    tr("private_pin"),
                    hasPin(),
                    () -> {
                        if (!hasPin()) {
                            showCreatePinDialog();
                        } else {
                            showChangePinDialog();
                        }
                    }
            )
    );

    content.addView(checks);

    gap(content, 24);

    TextView scan =
            createPrimaryButton(
                    tr("check")
            );

    scan.setOnClickListener(
            v -> {

                haptic(v);

                performSecurityCheck();

                Toast.makeText(
                        this,
                        tr("security_status")
                                + ": "
                                + calculateSecurityScore()
                                + "%",
                        Toast.LENGTH_SHORT
                ).show();

                showSecurityCenterScreen();
            }
    );

    content.addView(
            scan,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
            )
    );

    root.addView(
            createBottomNavigation(
                    TAB_SETTINGS
            )
    );

    setContentView(root);

    animateScreen(content);
}

private View createSecurityActionRow(
        int iconType,
        String titleValue,
        boolean enabled,
        Runnable action
) {

    LinearLayout row =
            horizontal();

    row.setGravity(
            Gravity.CENTER_VERTICAL
    );

    row.setPadding(
            dp(2),
            dp(4),
            dp(2),
            dp(4)
    );

    LinearLayout icon =
            horizontal();

    icon.setGravity(
            Gravity.CENTER
    );

    icon.setBackground(
            solid(
                    enabled
                            ? GREEN_SURFACE
                            : PURPLE_SURFACE,
                    dp(13)
            )
    );

    icon.addView(
            createVectorIcon(
                    iconType,
                    enabled
                            ? GREEN
                            : PURPLE_LIGHT,
                    2
            ),
            params(dp(23), dp(23))
    );

    row.addView(
            icon,
            params(dp(42), dp(42))
    );

    TextView title =
            text(
                    titleValue,
                    11,
                    WHITE,
                    medium
            );

    LinearLayout.LayoutParams titleParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    titleParams.leftMargin =
            dp(11);

    row.addView(
            title,
            titleParams
    );

    row.addView(
            createPill(
                    enabled
                            ? "ON"
                            : "FIX",
                    enabled
                            ? GREEN
                            : PURPLE_LIGHT,
                    enabled
                            ? GREEN_SURFACE
                            : PURPLE_SURFACE
            ),
            params(dp(48), dp(28))
    );

    if (action != null) {

        pressEffect(row);

        row.setOnClickListener(
                v -> {

                    haptic(v);

                    action.run();
                }
        );
    }

    return row;
}

// =========================================================
// APP LOCK SAFE OPEN
// =========================================================

private void openAppLockSafely() {

    long now =
            System.currentTimeMillis();

    if (openingAppLock) {
        return;
    }

    if (now - lastAppLockOpenTime < 700) {
        return;
    }

    lastAppLockOpenTime = now;
    openingAppLock = true;

    migrateLegacyPinIfNeeded();

    if (!hasPin()) {

        openingAppLock = false;

        showCreatePinDialog();

        return;
    }

    /*
     * Проверяем Accessibility только один раз.
     * Исправленная функция из ЧАСТИ 1 правильно
     * определяет включённую службу.
     */
    if (!isAccessibilityServiceEnabled()) {

        openingAppLock = false;

        showAccessibilityPermissionDialog();

        return;
    }

    if (!hasUsageStatsPermission()) {

        openingAppLock = false;

        showUsageAccessDialog();

        return;
    }

    openingAppLock = false;

    showAppLock();
}

// =========================================================
// APP LOCK
// =========================================================

private void showAppLock() {

    currentTab = TAB_APPS;
    openingAppLock = false;

    loadLockedApps();

    root = vertical();
    root.setBackground(
            appBackground()
    );

    ScrollView scroll =
            new ScrollView(this);

    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.setOverScrollMode(
            View.OVER_SCROLL_NEVER
    );

    LinearLayout content =
            vertical();

    content.setPadding(
            dp(22),
            dp(20),
            dp(22),
            dp(31)
    );

    scroll.addView(content);

    root.addView(
            scroll,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1
            )
    );

    content.addView(
            createScreenHeader(
                    tr("app_lock"),
                    lockedPackages.size()
                            + " "
                            + tr("protected_apps"),
                    this::showHome
            )
    );

    gap(content, 24);

    LinearLayout hero =
            vertical();

    hero.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
    );

    hero.setBackground(
            activeProtectionBackground(
                    dp(24)
            )
    );

    applySoftDepth(hero);

    LinearLayout top =
            horizontal();

    top.setGravity(
            Gravity.CENTER_VERTICAL
    );

    top.addView(
            createFeatureVectorIcon(
                    ICON_APPS
            ),
            params(dp(58), dp(58))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(14);

    info.addView(
            text(
                    tr("app_lock"),
                    18,
                    WHITE,
                    bold
            )
    );

    TextView subtitle =
            text(
                    tr("protect_private_apps"),
                    10,
                    TEXT_2,
                    regular
            );

    LinearLayout.LayoutParams subtitleParams =
            wrap();

    subtitleParams.topMargin =
            dp(5);

    info.addView(
            subtitle,
            subtitleParams
    );

    top.addView(
            info,
            infoParams
    );

    top.addView(
            createPill(
                    String.valueOf(
                            lockedPackages.size()
                    ),
                    PURPLE_LIGHT,
                    PURPLE_SURFACE
            ),
            params(dp(44), dp(30))
    );

    hero.addView(top);

    content.addView(hero);

    gap(content, 25);

    content.addView(
            sectionTitle(
                    tr("apps"),
                    tr("protected_apps")
            )
    );

    gap(content, 11);

    if (allApps.isEmpty() &&
            !appsLoading) {

        loadInstalledApps();
    }

    if (appsLoading) {

        LinearLayout loading =
                vertical();

        loading.setGravity(
                Gravity.CENTER
        );

        loading.setPadding(
                dp(20),
                dp(35),
                dp(20),
                dp(35)
        );

        loading.setBackground(
                softCardBackground(
                        dp(20)
                )
        );

        loading.addView(
                text(
                        "Loading applications...",
                        11,
                        TEXT_2,
                        medium
                )
        );

        content.addView(loading);

    } else if (allApps.isEmpty()) {

        LinearLayout empty =
                vertical();

        empty.setGravity(
                Gravity.CENTER
        );

        empty.setPadding(
                dp(20),
                dp(35),
                dp(20),
                dp(35)
        );

        empty.setBackground(
                softCardBackground(
                        dp(20)
                )
        );

        empty.addView(
                text(
                        "No applications found.",
                        11,
                        TEXT_2,
                        medium
                )
        );

        content.addView(empty);

    } else {

        for (int i = 0;
             i < allApps.size();
             i++) {

            AppItem app =
                    allApps.get(i);

            content.addView(
                    createAppLockRow(app)
            );

            if (i <
                    allApps.size() - 1) {

                gap(content, 9);
            }
        }
    }

    root.addView(
            createBottomNavigation(
                    TAB_APPS
            )
    );

    setContentView(root);

    animateScreen(content);
}

private View createAppLockRow(
        AppItem app
) {

    LinearLayout card =
            horizontal();

    card.setGravity(
            Gravity.CENTER_VERTICAL
    );

    card.setPadding(
            dp(14),
            dp(13),
            dp(14),
            dp(13)
    );

    boolean locked =
            app != null &&
                    lockedPackages.contains(
                            app.packageName
                    );

    card.setBackground(
            locked
                    ? activeProtectionBackground(dp(19))
                    : softCardBackground(dp(19))
    );

    ImageView icon =
            new ImageView(this);

    icon.setScaleType(
            ImageView.ScaleType.CENTER_INSIDE
    );

    if (app != null &&
            app.icon != null) {

        icon.setImageDrawable(
                app.icon
        );

    } else {

        icon.setImageDrawable(
                new LockVaultIconDrawable(
                        ICON_APPS,
                        PURPLE_LIGHT,
                        3
                )
        );
    }

    card.addView(
            icon,
            params(dp(45), dp(45))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(12);

    String appName =
            app == null
                    ? ""
                    : app.name;

    String packageName =
            app == null
                    ? ""
                    : app.packageName;

    info.addView(
            text(
                    appName,
                    12,
                    WHITE,
                    medium
            )
    );

    TextView packageView =
            text(
                    packageName,
                    8,
                    TEXT_3,
                    regular
            );

    packageView.setSingleLine(true);

    packageView.setEllipsize(
            TextUtils.TruncateAt.END
    );

    LinearLayout.LayoutParams packageParams =
            wrap();

    packageParams.topMargin =
            dp(4);

    info.addView(
            packageView,
            packageParams
    );

    card.addView(
            info,
            infoParams
    );

    TextView state =
            createPill(
                    locked
                            ? "LOCKED"
                            : "OPEN",
                    locked
                            ? GREEN
                            : TEXT_3,
                    locked
                            ? GREEN_SURFACE
                            : SURFACE_3
            );

    card.addView(
            state,
            params(dp(66), dp(30))
    );

    pressEffect(card);

    card.setOnClickListener(
            v -> {

                haptic(v);

                if (app == null) {
                    return;
                }

                toggleAppLock(app);
            }
    );

    return card;
}

private void toggleAppLock(
        AppItem app
) {

    if (app == null ||
            app.packageName == null ||
            app.packageName.isEmpty()) {

        return;
    }

    if (app.packageName.equals(
            getPackageName()
    )) {

        Toast.makeText(
                this,
                "LockVault cannot lock itself.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    migrateLegacyPinIfNeeded();

    if (!hasPin()) {

        pendingApp = app;

        showCreatePinDialog();

        return;
    }

    if (lockedPackages.contains(
            app.packageName
    )) {

        lockedPackages.remove(
                app.packageName
        );

        saveLockedApps();

        addSecurityEvent(
                app.name
                        + " protection disabled"
        );

    } else {

        lockedPackages.add(
                app.packageName
        );

        saveLockedApps();

        addSecurityEvent(
                app.name
                        + " protected"
        );
    }

    showAppLock();
}

// =========================================================
// INSTALLED APPS
// =========================================================

private void loadInstalledApps() {

    if (appsLoading) {
        return;
    }

    appsLoading = true;

    new Thread(
            () -> {

                final List<AppItem> loaded =
                        new ArrayList<>();

                try {

                    PackageManager manager =
                            getPackageManager();

                    Intent launcher =
                            new Intent(
                                    Intent.ACTION_MAIN,
                                    null
                            );

                    launcher.addCategory(
                            Intent.CATEGORY_LAUNCHER
                    );

                    List<ResolveInfo> apps =
                            manager.queryIntentActivities(
                                    launcher,
                                    0
                            );

                    Set<String> added =
                            new HashSet<>();

                    if (apps != null) {

                        for (ResolveInfo resolve :
                                apps) {

                            if (resolve == null ||
                                    resolve.activityInfo == null) {

                                continue;
                            }

                            String packageName =
                                    resolve
                                            .activityInfo
                                            .packageName;

                            if (packageName == null ||
                                    packageName.isEmpty()) {

                                continue;
                            }

                            if (packageName.equals(
                                    getPackageName()
                            )) {

                                continue;
                            }

                            if (added.contains(
                                    packageName
                            )) {

                                continue;
                            }

                            added.add(
                                    packageName
                            );

                            CharSequence label =
                                    resolve.loadLabel(
                                            manager
                                    );

                            Drawable icon =
                                    resolve.loadIcon(
                                            manager
                                    );

                            AppItem item =
                                    new AppItem();

                            item.name =
                                    label == null
                                            ? packageName
                                            : label.toString();

                            item.packageName =
                                    packageName;

                            item.icon =
                                    icon;

                            loaded.add(
                                    item
                            );
                        }
                    }

                    Collections.sort(
                            loaded,
                            new Comparator<AppItem>() {

                                @Override
                                public int compare(
                                        AppItem first,
                                        AppItem second
                                ) {

                                    String firstName =
                                            first == null ||
                                                    first.name == null
                                                    ? ""
                                                    : first.name;

                                    String secondName =
                                            second == null ||
                                                    second.name == null
                                                    ? ""
                                                    : second.name;

                                    return firstName
                                            .compareToIgnoreCase(
                                                    secondName
                                            );
                                }
                            }
                    );

                } catch (Exception ignored) {
                }

                runOnUiThread(
                        () -> {

                            allApps.clear();

                            allApps.addAll(
                                    loaded
                            );

                            appsLoading = false;

                            if (!isFinishing() &&
                                    TAB_APPS.equals(
                                            currentTab
                                    )) {

                                showAppLock();
                            }
                        }
                );
            }
    ).start();
}

private static class AppItem {

    String name = "";

    String packageName = "";

    Drawable icon;
}

// =========================================================
// LOCKED APPS PREFERENCES
// =========================================================

private void loadLockedApps() {

    if (preferences == null) {
        return;
    }

    try {

        Set<String> saved =
                preferences.getStringSet(
                        KEY_LOCKED_APPS,
                        new HashSet<>()
                );

        lockedPackages =
                saved == null
                        ? new HashSet<>()
                        : new HashSet<>(saved);

        if (lockedPackages.remove(
                getPackageName()
        )) {

            saveLockedApps();
        }

    } catch (Exception e) {

        lockedPackages =
                new HashSet<>();
    }
}

private void saveLockedApps() {

    if (preferences == null) {
        return;
    }

    Set<String> safeCopy =
            new HashSet<>(
                    lockedPackages
            );

    safeCopy.remove(
            getPackageName()
    );

    preferences
            .edit()
            .putStringSet(
                    KEY_LOCKED_APPS,
                    safeCopy
            )
            .commit();
}

// =========================================================
// CREATE PIN
// =========================================================

private void showCreatePinDialog() {

    migrateLegacyPinIfNeeded();

    if (hasPin()) {

        if (pendingApp != null) {

            AppItem app =
                    pendingApp;

            pendingApp = null;

            toggleAppLock(app);

        } else if (isAccessibilityServiceEnabled() &&
                hasUsageStatsPermission()) {

            showAppLock();

        } else {

            checkRequiredPermissions();
        }

        return;
    }

    firstPin = "";

    final Dialog dialog =
            new Dialog(this);

    dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
    );

    LinearLayout box =
            vertical();

    box.setPadding(
            dp(22),
            dp(23),
            dp(22),
            dp(20)
    );

    box.setBackground(
            glassDialogBackground()
    );

    LinearLayout iconHolder =
            horizontal();

    iconHolder.setGravity(
            Gravity.CENTER
    );

    iconHolder.addView(
            createFeatureVectorIcon(
                    ICON_LOCK
            ),
            params(dp(62), dp(62))
    );

    box.addView(iconHolder);

    gap(box, 17);

    TextView title =
            text(
                    "Create Private PIN",
                    20,
                    WHITE,
                    bold
            );

    title.setGravity(
            Gravity.CENTER
    );

    box.addView(title);

    gap(box, 7);

    TextView subtitle =
            text(
                    "Create a 4-digit PIN to protect your applications.",
                    11,
                    TEXT_2,
                    regular
            );

    subtitle.setGravity(
            Gravity.CENTER
    );

    box.addView(subtitle);

    gap(box, 18);

    EditText pin =
            createPinInput(
                    "••••"
            );

    box.addView(
            pin,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(54)
            )
    );

    gap(box, 12);

    EditText confirm =
            createPinInput(
                    "Confirm PIN"
            );

    box.addView(
            confirm,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(54)
            )
    );

    gap(box, 17);

    TextView save =
            createPrimaryButton(
                    "Create PIN"
            );

    box.addView(
            save,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
            )
    );

    gap(box, 7);

    TextView cancel =
            createGhostButton(
                    tr("cancel")
            );

    box.addView(
            cancel,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(43)
            )
    );

    save.setOnClickListener(
            v -> {

                haptic(v);

                String first =
                        pin
                                .getText()
                                .toString();

                String second =
                        confirm
                                .getText()
                                .toString();

                if (!isValidPin(first)) {

                    Toast.makeText(
                            this,
                            "PIN must contain exactly 4 digits.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                if (!first.equals(
                        second
                )) {

                    Toast.makeText(
                            this,
                            "PIN codes do not match.",
                            Toast.LENGTH_SHORT
                    ).show();

                    confirm.setText("");

                    return;
                }

                if (!savePin(first)) {

                    Toast.makeText(
                            this,
                            "Could not save PIN.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                addSecurityEvent(
                        "Private PIN created"
                );

                dialog.dismiss();

                Toast.makeText(
                        this,
                        "Private PIN created.",
                        Toast.LENGTH_SHORT
                ).show();

                if (pendingApp != null) {

                    AppItem app =
                            pendingApp;

                    pendingApp = null;

                    toggleAppLock(app);

                } else {

                    checkRequiredPermissions();
                }
            }
    );

    cancel.setOnClickListener(
            v -> {

                pendingApp = null;

                openingAppLock = false;

                dialog.dismiss();
            }
    );

    dialog.setOnDismissListener(
            value ->
                    openingAppLock = false
    );

    dialog.setContentView(box);

    showStyledDialog(dialog);
}

// =========================================================
// CHANGE PIN
// =========================================================

private void showChangePinDialog() {

    migrateLegacyPinIfNeeded();

    if (!hasPin()) {

        showCreatePinDialog();

        return;
    }

    final Dialog dialog =
            new Dialog(this);

    dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
    );

    LinearLayout box =
            vertical();

    box.setPadding(
            dp(22),
            dp(22),
            dp(22),
            dp(20)
    );

    box.setBackground(
            glassDialogBackground()
    );

    box.addView(
            text(
                    "Change Private PIN",
                    20,
                    WHITE,
                    bold
            )
    );

    gap(box, 8);

    box.addView(
            text(
                    "Enter your current PIN before creating a new one.",
                    11,
                    TEXT_2,
                    regular
            )
    );

    gap(box, 17);

    EditText oldPin =
            createPinInput(
                    "Current PIN"
            );

    box.addView(
            oldPin,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
            )
    );

    gap(box, 10);

    EditText newPin =
            createPinInput(
                    "New PIN"
            );

    box.addView(
            newPin,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
            )
    );

    gap(box, 10);

    EditText confirm =
            createPinInput(
                    "Confirm new PIN"
            );

    box.addView(
            confirm,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
            )
    );

    gap(box, 16);

    TextView change =
            createPrimaryButton(
                    "Change PIN"
            );

    box.addView(
            change,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
            )
    );

    gap(box, 7);

    TextView cancel =
            createGhostButton(
                    tr("cancel")
            );

    box.addView(
            cancel,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(43)
            )
    );

    change.setOnClickListener(
            v -> {

                haptic(v);

                String oldValue =
                        oldPin
                                .getText()
                                .toString();

                String newValue =
                        newPin
                                .getText()
                                .toString();

                String confirmValue =
                        confirm
                                .getText()
                                .toString();

                if (!verifyPin(
                        oldValue
                )) {

                    Toast.makeText(
                            this,
                            "Incorrect current PIN.",
                            Toast.LENGTH_SHORT
                    ).show();

                    oldPin.setText("");

                    return;
                }

                if (!isValidPin(
                        newValue
                )) {

                    Toast.makeText(
                            this,
                            "New PIN must contain exactly 4 digits.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                if (!newValue.equals(
                        confirmValue
                )) {

                    Toast.makeText(
                            this,
                            "PIN codes do not match.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                if (savePin(
                        newValue
                )) {

                    addSecurityEvent(
                            "Private PIN changed"
                    );

                    dialog.dismiss();

                    Toast.makeText(
                            this,
                            "PIN changed.",
                            Toast.LENGTH_SHORT
                    ).show();

                    refreshCurrentScreen();

                } else {

                    Toast.makeText(
                            this,
                            "Could not change PIN.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
    );

    cancel.setOnClickListener(
            v -> dialog.dismiss()
    );

    dialog.setContentView(box);

    showStyledDialog(dialog);
}

private EditText createPinInput(
        String hint
) {

    EditText input =
            new EditText(this);

    input.setTextColor(
            WHITE
    );

    input.setHintTextColor(
            TEXT_3
    );

    input.setHint(
            hint
    );

    input.setTextSize(
            17
    );

    input.setTypeface(
            medium
    );

    input.setGravity(
            Gravity.CENTER
    );

    input.setSingleLine(
            true
    );

    input.setInputType(
            android.text.InputType.TYPE_CLASS_NUMBER
                    |
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
    );

    input.setPadding(
            dp(15),
            0,
            dp(15),
            0
    );

    input.setBackground(
            softCardBackground(
                    dp(16)
            )
    );

    input.addTextChangedListener(
            new TextWatcher() {

                private boolean editing =
                        false;

                @Override
                public void beforeTextChanged(
                        CharSequence s,
                        int start,
                        int count,
                        int after
                ) {
                }

                @Override
                public void onTextChanged(
                        CharSequence s,
                        int start,
                        int before,
                        int count
                ) {
                }

                @Override
                public void afterTextChanged(
                        Editable editable
                ) {

                    if (editing ||
                            editable == null) {

                        return;
                    }

                    String value =
                            editable.toString();

                    if (value.length() <= 4) {
                        return;
                    }

                    editing = true;

                    input.setText(
                            value.substring(
                                    0,
                                    4
                            )
                    );

                    input.setSelection(
                            input.length()
                    );

                    editing = false;
                }
            }
    );

    return input;
}

// =========================================================
// VAULT SCREEN
// =========================================================

private void showVaultScreen() {

    currentTab = TAB_VAULT;

    syncVaultCounters();

    root = vertical();
    root.setBackground(
            appBackground()
    );

    ScrollView scroll =
            new ScrollView(this);

    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.setOverScrollMode(
            View.OVER_SCROLL_NEVER
    );

    LinearLayout content =
            vertical();

    content.setPadding(
            dp(22),
            dp(20),
            dp(22),
            dp(31)
    );

    scroll.addView(content);

    root.addView(
            scroll,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1
            )
    );

    content.addView(
            createScreenHeader(
                    tr("private_vault"),
                    tr("your_private_space"),
                    this::showHome
            )
    );

    gap(content, 24);

    LinearLayout hero =
            vertical();

    hero.setPadding(
            dp(21),
            dp(21),
            dp(21),
            dp(20)
    );

    hero.setBackground(
            activeProtectionBackground(
                    dp(25)
            )
    );

    applyActiveGlow(hero);

    LinearLayout top =
            horizontal();

    top.setGravity(
            Gravity.CENTER_VERTICAL
    );

    top.addView(
            createFeatureVectorIcon(
                    ICON_VAULT
            ),
            params(dp(62), dp(62))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(14);

    info.addView(
            text(
                    tr("your_private_space"),
                    17,
                    WHITE,
                    bold
            )
    );

    TextView description =
            text(
                    tr("vault_description"),
                    9,
                    TEXT_2,
                    regular
            );

    description.setLineSpacing(
            dp(2),
            1f
    );

    LinearLayout.LayoutParams descriptionParams =
            wrap();

    descriptionParams.topMargin =
            dp(5);

    info.addView(
            description,
            descriptionParams
    );

    top.addView(
            info,
            infoParams
    );

    top.addView(
            createPill(
                    tr("local"),
                    GREEN,
                    GREEN_SURFACE
            ),
            params(dp(58), dp(29))
    );

    hero.addView(top);

    gap(hero, 18);

    LinearLayout stats =
            horizontal();

    stats.addView(
            createHomeStat(
                    String.valueOf(
                            countVaultFiles(
                                    VAULT_PHOTOS_DIR
                            )
                    ),
                    tr("photos")
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(66),
                    1
            )
    );

    horizontalGap(stats, 8);

    stats.addView(
            createHomeStat(
                    String.valueOf(
                            countVaultFiles(
                                    VAULT_VIDEOS_DIR
                            )
                    ),
                    tr("videos")
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(66),
                    1
            )
    );

    horizontalGap(stats, 8);

    stats.addView(
            createHomeStat(
                    String.valueOf(
                            countVaultFiles(
                                    VAULT_NOTES_DIR
                            )
                    ),
                    tr("notes")
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(66),
                    1
            )
    );

    hero.addView(stats);

    content.addView(hero);

    gap(content, 27);

    content.addView(
            sectionTitle(
                    tr("private_collections"),
                    getVaultItemCount()
                            + " "
                            + tr("private_items")
            )
    );

    gap(content, 11);

    content.addView(
            createVaultCollectionCard(
                    ICON_PHOTO,
                    tr("photos"),
                    countVaultFiles(
                            VAULT_PHOTOS_DIR
                    ),
                    tr("add_photo"),
                    () -> openVaultPicker(
                            "photo"
                    ),
                    this::showVaultPhotos
            )
    );

    gap(content, 10);

    content.addView(
            createVaultCollectionCard(
                    ICON_VIDEO,
                    tr("videos"),
                    countVaultFiles(
                            VAULT_VIDEOS_DIR
                    ),
                    tr("add_video"),
                    () -> openVaultPicker(
                            "video"
                    ),
                    this::showVaultVideos
            )
    );

    gap(content, 10);

    content.addView(
            createVaultCollectionCard(
                    ICON_NOTE,
                    tr("notes"),
                    countVaultFiles(
                            VAULT_NOTES_DIR
                    ),
                    tr("new_note"),
                    this::showCreateVaultNoteDialog,
                    this::showVaultNotes
            )
    );

    root.addView(
            createBottomNavigation(
                    TAB_VAULT
            )
    );

    setContentView(root);

    animateScreen(content);
}

private View createVaultCollectionCard(
        int iconType,
        String titleValue,
        int count,
        String actionText,
        Runnable addAction,
        Runnable openAction
) {

    LinearLayout card =
            horizontal();

    card.setGravity(
            Gravity.CENTER_VERTICAL
    );

    card.setPadding(
            dp(15),
            dp(15),
            dp(15),
            dp(15)
    );

    card.setBackground(
            glassCardBackground(
                    dp(20)
            )
    );

    card.addView(
            createFeatureVectorIcon(
                    iconType
            ),
            params(dp(54), dp(54))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(13);

    info.addView(
            text(
                    titleValue,
                    13,
                    WHITE,
                    medium
            )
    );

    TextView countText =
            text(
                    count == 0
                            ? tr("no_private_items")
                            : count
                            + " "
                            + tr("private_items"),
                    9,
                    TEXT_3,
                    regular
            );

    LinearLayout.LayoutParams countParams =
            wrap();

    countParams.topMargin =
            dp(5);

    info.addView(
            countText,
            countParams
    );

    card.addView(
            info,
            infoParams
    );

    LinearLayout actions =
            vertical();

    TextView add =
            createPill(
                    "+",
                    PURPLE_LIGHT,
                    PURPLE_SURFACE
            );

    add.setTextSize(17);

    actions.addView(
            add,
            params(dp(42), dp(30))
    );

    if (count > 0) {

        gap(actions, 6);

        TextView view =
                createPill(
                        tr("view")
                                .toUpperCase(
                                        Locale.getDefault()
                                ),
                        GREEN,
                        GREEN_SURFACE
                );

        actions.addView(
                view,
                params(dp(54), dp(27))
        );

        view.setOnClickListener(
                v -> {

                    haptic(v);

                    if (openAction != null) {
                        openAction.run();
                    }
                }
        );
    }

    card.addView(actions);

    add.setOnClickListener(
            v -> {

                haptic(v);

                if (addAction != null) {
                    addAction.run();
                }
            }
    );

    if (count > 0) {

        pressEffect(card);

        card.setOnClickListener(
                v -> {

                    haptic(v);

                    if (openAction != null) {
                        openAction.run();
                    }
                }
        );
    }

    return card;
}

// =========================================================
// NOTES VIEWER
// =========================================================

private void showVaultNotes() {

    List<File> files =
            getVaultFiles(
                    VAULT_NOTES_DIR
            );

    if (files.isEmpty()) {

        Toast.makeText(
                this,
                "No private notes yet.",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    final Dialog dialog =
            new Dialog(this);

    dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
    );

    LinearLayout container =
            vertical();

    container.setPadding(
            dp(18),
            dp(20),
            dp(18),
            dp(17)
    );

    container.setBackground(
            glassDialogBackground()
    );

    LinearLayout header =
            horizontal();

    header.setGravity(
            Gravity.CENTER_VERTICAL
    );

    header.addView(
            text(
                    tr("notes"),
                    19,
                    WHITE,
                    bold
            ),
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            )
    );

    TextView close =
            text(
                    "×",
                    27,
                    TEXT_2,
                    regular
            );

    close.setGravity(
            Gravity.CENTER
    );

    close.setOnClickListener(
            v -> dialog.dismiss()
    );

    header.addView(
            close,
            params(dp(40), dp(40))
    );

    container.addView(header);

    gap(container, 15);

    ScrollView scroll =
            new ScrollView(this);

    LinearLayout list =
            vertical();

    for (int i = 0;
         i < files.size();
         i++) {

        File file =
                files.get(i);

        list.addView(
                createVaultNoteItem(
                        file,
                        dialog
                )
        );

        if (i < files.size() - 1) {
            gap(list, 9);
        }
    }

    scroll.addView(list);

    container.addView(
            scroll,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(450)
            )
    );

    dialog.setContentView(container);

    showStyledDialog(dialog);
}

private View createVaultNoteItem(
        File file,
        Dialog parentDialog
) {

    LinearLayout card =
            horizontal();

    card.setGravity(
            Gravity.CENTER_VERTICAL
    );

    card.setPadding(
            dp(13),
            dp(13),
            dp(13),
            dp(13)
    );

    card.setBackground(
            softCardBackground(
                    dp(17)
            )
    );

    card.addView(
            createFeatureVectorIcon(
                    ICON_NOTE
            ),
            params(dp(50), dp(50))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(12);

    String noteText =
            readVaultNote(file);

    String preview =
            noteText;

    if (preview.length() > 42) {

        preview =
                preview.substring(
                        0,
                        42
                )
                        + "…";
    }

    info.addView(
            text(
                    preview,
                    11,
                    WHITE,
                    medium
            )
    );

    TextView date =
            text(
                    formatVaultFileDate(
                            file
                    ),
                    8,
                    TEXT_3,
                    regular
            );

    LinearLayout.LayoutParams dateParams =
            wrap();

    dateParams.topMargin =
            dp(5);

    info.addView(
            date,
            dateParams
    );

    card.addView(
            info,
            infoParams
    );

    card.addView(
            createPill(
                    tr("view")
                            .toUpperCase(
                                    Locale.getDefault()
                            ),
                    PURPLE_LIGHT,
                    PURPLE_SURFACE
            ),
            params(dp(56), dp(28))
    );

    pressEffect(card);

    card.setOnClickListener(
            v -> {

                haptic(v);

                showSingleVaultNote(
                        file,
                        parentDialog
                );
            }
    );

    return card;
}

private String readVaultNote(
        File file
) {

    if (file == null ||
            !file.exists()) {

        return "";
    }

    FileInputStream input =
            null;

    try {

        input =
                new FileInputStream(
                        file
                );

        byte[] buffer =
                new byte[
                        (int) Math.min(
                                file.length(),
                                1024 * 1024
                        )
                        ];

        int read =
                input.read(
                        buffer
                );

        if (read <= 0) {
            return "";
        }

        return new String(
                buffer,
                0,
                read,
                "UTF-8"
        );

    } catch (Exception ignored) {

        return "";

    } finally {

        try {

            if (input != null) {
                input.close();
            }

        } catch (Exception ignored) {
        }
    }
}

private void showSingleVaultNote(
        File file,
        Dialog notesDialog
) {

    final Dialog dialog =
            new Dialog(this);

    dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
    );

    LinearLayout box =
            vertical();

    box.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(18)
    );

    box.setBackground(
            glassDialogBackground()
    );

    box.addView(
            text(
                    tr("notes"),
                    19,
                    WHITE,
                    bold
            )
    );

    gap(box, 13);

    TextView note =
            text(
                    readVaultNote(file),
                    12,
                    TEXT_2,
                    regular
            );

    note.setLineSpacing(
            dp(3),
            1f
    );

    ScrollView scroll =
            new ScrollView(this);

    scroll.addView(note);

    box.addView(
            scroll,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(300)
            )
    );

    gap(box, 13);

    LinearLayout actions =
            horizontal();

    TextView delete =
            createGhostButton(
                    tr("delete")
            );

    delete.setTextColor(
            RED
    );

    delete.setOnClickListener(
            v -> {

                haptic(v);

                if (file != null &&
                        file.delete()) {

                    syncVaultCounters();

                    addSecurityEvent(
                            "Private note removed"
                    );

                    dialog.dismiss();

                    if (notesDialog != null &&
                            notesDialog.isShowing()) {

                        notesDialog.dismiss();
                    }

                    showVaultScreen();
                }
            }
    );

    actions.addView(
            delete,
            new LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1
            )
    );

    horizontalGap(actions, 8);

    TextView close =
            createPrimaryButton(
                    tr("close")
            );

    close.setOnClickListener(
            v -> dialog.dismiss()
    );

    actions.addView(
            close,
            new LinearLayout.LayoutParams(
                    0,
                    dp(46),
                    1
            )
    );

    box.addView(actions);

    dialog.setContentView(box);

    showStyledDialog(dialog);
}

// =========================================================
// SETTINGS
// =========================================================

private void showSettingsScreen() {

    currentTab = TAB_SETTINGS;

    root = vertical();
    root.setBackground(
            appBackground()
    );

    ScrollView scroll =
            new ScrollView(this);

    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.setOverScrollMode(
            View.OVER_SCROLL_NEVER
    );

    LinearLayout content =
            vertical();

    content.setPadding(
            dp(22),
            dp(20),
            dp(22),
            dp(31)
    );

    scroll.addView(content);

    root.addView(
            scroll,
            new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1
            )
    );

    content.addView(
            createScreenHeader(
                    tr("settings"),
                    tr("privacy_controls"),
                    this::showHome
            )
    );

    gap(content, 24);

    content.addView(
            sectionTitle(
                    tr("security"),
                    tr("protected")
            )
    );

    gap(content, 11);

    content.addView(
            createSettingsAction(
                    ICON_LOCK,
                    tr("private_pin"),
                    hasPin()
                            ? tr("protected")
                            : tr("setup"),
                    () -> {

                        if (hasPin()) {
                            showChangePinDialog();
                        } else {
                            showCreatePinDialog();
                        }
                    }
            )
    );

    gap(content, 9);

    content.addView(
            createSettingsAction(
                    ICON_PERMISSION,
                    tr("accessibility_protection"),
                    isAccessibilityServiceEnabled()
                            ? tr("active")
                            : tr("setup"),
                    () -> {

                        if (!isAccessibilityServiceEnabled()) {
                            showAccessibilityPermissionDialog();
                        }
                    }
            )
    );

    gap(content, 9);

    content.addView(
            createSettingsAction(
                    ICON_SECURITY,
                    tr("usage_access"),
                    hasUsageStatsPermission()
                            ? tr("active")
                            : tr("setup"),
                    () -> {

                        if (!hasUsageStatsPermission()) {
                            showUsageAccessDialog();
                        }
                    }
            )
    );

    gap(content, 25);

    content.addView(
            sectionTitle(
                    tr("privacy_tools"),
                    tr("local")
            )
    );

    gap(content, 11);

    boolean privacyMode =
            preferences.getBoolean(
                    KEY_PRIVACY_MODE,
                    false
            );

    content.addView(
            createSettingsToggle(
                    ICON_PRIVACY,
                    "Privacy mode",
                    tr("stored_locally"),
                    privacyMode,
                    value -> {

                        preferences
                                .edit()
                                .putBoolean(
                                        KEY_PRIVACY_MODE,
                                        value
                                )
                                .apply();

                        addSecurityEvent(
                                value
                                        ? "Privacy mode enabled"
                                        : "Privacy mode disabled"
                        );
                    }
            )
    );

    gap(content, 9);

    boolean biometric =
            preferences.getBoolean(
                    KEY_BIOMETRIC_ENABLED,
                    false
            );

    content.addView(
            createSettingsToggle(
                    ICON_BIOMETRIC,
                    "Biometric unlock",
                    "Optional local unlock",
                    biometric,
                    value -> {

                        preferences
                                .edit()
                                .putBoolean(
                                        KEY_BIOMETRIC_ENABLED,
                                        value
                                )
                                .apply();

                        addSecurityEvent(
                                value
                                        ? "Biometric preference enabled"
                                        : "Biometric preference disabled"
                        );
                    }
            )
    );

    gap(content, 9);

    boolean notifications =
            preferences.getBoolean(
                    KEY_NOTIFICATIONS,
                    true
            );

    content.addView(
            createSettingsToggle(
                    ICON_BELL,
                    "Security notifications",
                    "Important protection events",
                    notifications,
                    value -> {

                        preferences
                                .edit()
                                .putBoolean(
                                        KEY_NOTIFICATIONS,
                                        value
                                )
                                .apply();
                    }
            )
    );

    gap(content, 25);

    content.addView(
            sectionTitle(
                    tr("language"),
                    getLanguageDisplayName()
            )
    );

    gap(content, 11);

    content.addView(
            createSettingsAction(
                    ICON_SETTINGS,
                    tr("language"),
                    getLanguageDisplayName(),
                    this::showLanguageDialog
            )
    );

    gap(content, 25);

    content.addView(
            sectionTitle(
                    tr("security_center"),
                    calculateSecurityScore()
                            + "%"
            )
    );

    gap(content, 11);

    content.addView(
            createSettingsAction(
                    ICON_SECURITY,
                    tr("security_center"),
                    getSecurityLevel(
                            calculateSecurityScore()
                    ),
                    this::showSecurityCenterScreen
            )
    );

    root.addView(
            createBottomNavigation(
                    TAB_SETTINGS
            )
    );

    setContentView(root);

    animateScreen(content);
}

private View createSettingsAction(
        int iconType,
        String titleValue,
        String subtitleValue,
        Runnable action
) {

    LinearLayout row =
            horizontal();

    row.setGravity(
            Gravity.CENTER_VERTICAL
    );

    row.setPadding(
            dp(14),
            dp(13),
            dp(14),
            dp(13)
    );

    row.setBackground(
            softCardBackground(
                    dp(19)
            )
    );

    row.addView(
            createFeatureVectorIcon(
                    iconType
            ),
            params(dp(48), dp(48))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(12);

    info.addView(
            text(
                    titleValue,
                    12,
                    WHITE,
                    medium
            )
    );

    TextView subtitle =
            text(
                    subtitleValue,
                    9,
                    TEXT_3,
                    regular
            );

    LinearLayout.LayoutParams subtitleParams =
            wrap();

    subtitleParams.topMargin =
            dp(4);

    info.addView(
            subtitle,
            subtitleParams
    );

    row.addView(
            info,
            infoParams
    );

    TextView arrow =
            text(
                    "›",
                    25,
                    TEXT_3,
                    regular
            );

    arrow.setGravity(
            Gravity.CENTER
    );

    row.addView(
            arrow,
            params(dp(32), dp(40))
    );

    pressEffect(row);

    row.setOnClickListener(
            v -> {

                haptic(v);

                if (action != null) {
                    action.run();
                }
            }
    );

    return row;
}

private interface ToggleListener {

    void onChanged(
            boolean value
    );
}

private View createSettingsToggle(
        int iconType,
        String titleValue,
        String subtitleValue,
        boolean initialValue,
        ToggleListener listener
) {

    LinearLayout row =
            horizontal();

    row.setGravity(
            Gravity.CENTER_VERTICAL
    );

    row.setPadding(
            dp(14),
            dp(13),
            dp(14),
            dp(13)
    );

    row.setBackground(
            softCardBackground(
                    dp(19)
            )
    );

    row.addView(
            createFeatureVectorIcon(
                    iconType
            ),
            params(dp(48), dp(48))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(12);

    info.addView(
            text(
                    titleValue,
                    12,
                    WHITE,
                    medium
            )
    );

    TextView subtitle =
            text(
                    subtitleValue,
                    9,
                    TEXT_3,
                    regular
            );

    LinearLayout.LayoutParams subtitleParams =
            wrap();

    subtitleParams.topMargin =
            dp(4);

    info.addView(
            subtitle,
            subtitleParams
    );

    row.addView(
            info,
            infoParams
    );

    final boolean[] state = {
            initialValue
    };

    TextView toggle =
            createPill(
                    initialValue
                            ? "ON"
                            : "OFF",
                    initialValue
                            ? GREEN
                            : TEXT_3,
                    initialValue
                            ? GREEN_SURFACE
                            : SURFACE_3
            );

    row.addView(
            toggle,
            params(dp(48), dp(29))
    );

    pressEffect(row);

    row.setOnClickListener(
            v -> {

                haptic(v);

                state[0] =
                        !state[0];

                toggle.setText(
                        state[0]
                                ? "ON"
                                : "OFF"
                );

                toggle.setTextColor(
                        state[0]
                                ? GREEN
                                : TEXT_3
                );

                toggle.setBackground(
                        solid(
                                state[0]
                                        ? GREEN_SURFACE
                                        : SURFACE_3,
                                dp(20)
                        )
                );

                if (listener != null) {

                    listener.onChanged(
                            state[0]
                    );
                }
            }
    );

    return row;
}

// =========================================================
// COMMON SCREEN HEADER
// =========================================================

private View createScreenHeader(
        String titleValue,
        String subtitleValue,
        Runnable backAction
) {

    LinearLayout header =
            horizontal();

    header.setGravity(
            Gravity.CENTER_VERTICAL
    );

    TextView back =
            text(
                    "‹",
                    30,
                    TEXT_2,
                    regular
            );

    back.setGravity(
            Gravity.CENTER
    );

    back.setBackground(
            softCardBackground(
                    dp(14)
            )
    );

    pressEffect(back);

    back.setOnClickListener(
            v -> {

                haptic(v);

                if (backAction != null) {
                    backAction.run();
                }
            }
    );

    header.addView(
            back,
            params(dp(44), dp(44))
    );

    LinearLayout info =
            vertical();

    LinearLayout.LayoutParams infoParams =
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            );

    infoParams.leftMargin =
            dp(13);

    info.addView(
            text(
                    titleValue,
                    20,
                    WHITE,
                    bold
            )
    );

    TextView subtitle =
            text(
                    subtitleValue,
                    9,
                    TEXT_3,
                    regular
            );

    LinearLayout.LayoutParams subtitleParams =
            wrap();

    subtitleParams.topMargin =
            dp(3);

    info.addView(
            subtitle,
            subtitleParams
    );

    header.addView(
            info,
            infoParams
    );

    return header;
}

// =========================================================
// BOTTOM NAVIGATION
// =========================================================

private View createBottomNavigation(
        String selected
) {

    LinearLayout navigation =
            horizontal();

    navigation.setGravity(
            Gravity.CENTER
    );

    navigation.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(9)
    );

    GradientDrawable navigationBackground =
            new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            Color.rgb(27, 24, 36),
                            Color.rgb(15, 14, 21)
                    }
            );

    navigationBackground.setStroke(
            dp(1),
            Color.rgb(43, 39, 55)
    );

    navigation.setBackground(
            navigationBackground
    );

    navigation.addView(
            createNavItem(
                    ICON_HOME,
                    tr("home"),
                    TAB_HOME,
                    selected
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(59),
                    1
            )
    );

    navigation.addView(
            createNavItem(
                    ICON_APPS,
                    tr("apps"),
                    TAB_APPS,
                    selected
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(59),
                    1
            )
    );

    navigation.addView(
            createNavItem(
                    ICON_VAULT,
                    tr("vault"),
                    TAB_VAULT,
                    selected
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(59),
                    1
            )
    );

    navigation.addView(
            createNavItem(
                    ICON_SETTINGS,
                    tr("settings"),
                    TAB_SETTINGS,
                    selected
            ),
            new LinearLayout.LayoutParams(
                    0,
                    dp(59),
                    1
            )
    );

    return navigation;
}

private View createNavItem(
        int iconType,
        String title,
        String tab,
        String selected
) {

    boolean active =
            tab.equals(
                    selected
            );

    LinearLayout item =
            vertical();

    item.setGravity(
            Gravity.CENTER
    );

    if (active) {

        item.setBackground(
                solid(
                        Color.rgb(40, 32, 53),
                        dp(17)
                )
        );
    }

    item.addView(
            createVectorIcon(
                    iconType,
                    active
                            ? PURPLE_LIGHT
                            : TEXT_3,
                    2
            ),
            params(dp(25), dp(25))
    );

    TextView label =
            text(
                    title,
                    8,
                    active
                            ? PURPLE_LIGHT
                            : TEXT_3,
                    active
                            ? medium
                            : regular
            );

    label.setGravity(
            Gravity.CENTER
    );

    LinearLayout.LayoutParams labelParams =
            wrap();

    labelParams.topMargin =
            dp(4);

    item.addView(
            label,
            labelParams
    );

    item.setContentDescription(
            title
    );

    item.setFocusable(
            true
    );

    pressEffect(item);

    item.setOnClickListener(
            v -> {

                haptic(v);

                if (TAB_HOME.equals(tab)) {

                    showHome();

                } else if (TAB_APPS.equals(tab)) {

                    openAppLockSafely();

                } else if (TAB_VAULT.equals(tab)) {

                    showVaultScreen();

                } else if (TAB_SETTINGS.equals(tab)) {

                    showSettingsScreen();
                }
            }
    );

    return item;
}

// =========================================================
// FEATURE CARD
// =========================================================

private LinearLayout createFeatureCard() {

    LinearLayout card =
            horizontal();

    card.setGravity(
            Gravity.CENTER_VERTICAL
    );

    card.setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14)
    );

    card.setBackground(
            glassCardBackground(
                    dp(20)
            )
    );

    applySoftDepth(card);

    return card;
}

// =========================================================
// SECTION TITLE
// =========================================================

private View sectionTitle(
        String titleValue,
        String rightValue
) {

    LinearLayout row =
            horizontal();

    row.setGravity(
            Gravity.CENTER_VERTICAL
    );

    TextView title =
            text(
                    titleValue,
                    11,
                    TEXT_2,
                    medium
            );

    title.setLetterSpacing(
            0.05f
    );

    row.addView(
            title,
            new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            )
    );

    if (rightValue != null &&
            !rightValue.isEmpty()) {

        TextView right =
                text(
                        rightValue,
                        9,
                        TEXT_3,
                        regular
                );

        row.addView(right);
    }

    return row;
}

// =========================================================
// BUTTONS
// =========================================================

private TextView createPrimaryButton(
        String value
) {

    TextView button =
            text(
                    value,
                    12,
                    WHITE,
                    medium
            );

    button.setGravity(
            Gravity.CENTER
    );

    button.setBackground(
            gradient(
                    new int[]{
                            Color.rgb(157, 108, 237),
                            Color.rgb(111, 72, 184)
                    },
                    dp(16)
            )
    );

    applySoftDepth(button);
    pressEffect(button);

    return button;
}

private TextView createGhostButton(
        String value
) {

    TextView button =
            text(
                    value,
                    11,
                    TEXT_2,
                    medium
            );

    button.setGravity(
            Gravity.CENTER
    );

    button.setBackground(
            softCardBackground(
                    dp(15)
            )
    );

    pressEffect(button);

    return button;
}

private TextView createPill(
        String value,
        int color,
        int background
) {

    TextView pill =
            text(
                    value,
                    8,
                    color,
                    medium
            );

    pill.setGravity(
            Gravity.CENTER
    );

    pill.setSingleLine(
            true
    );

    pill.setBackground(
            solid(
                    background,
                    dp(20)
            )
    );

    return pill;
}

// =========================================================
// VAULT COUNT
// =========================================================

private int getVaultItemCount() {

    if (preferences == null) {
        return 0;
    }

    return preferences.getInt(
            KEY_VAULT_PHOTOS,
            0
    )
            +
            preferences.getInt(
                    KEY_VAULT_VIDEOS,
                    0
            )
            +
            preferences.getInt(
                    KEY_VAULT_NOTES,
                    0
            );
}

// =========================================================
// PRESS EFFECT
// =========================================================

private void pressEffect(
        View view
) {

    if (view == null) {
        return;
    }

    view.setOnTouchListener(
            (v, event) -> {

                if (event.getAction() ==
                        MotionEvent.ACTION_DOWN) {

                    v.animate()
                            .scaleX(0.975f)
                            .scaleY(0.975f)
                            .setDuration(80)
                            .start();

                } else if (event.getAction() ==
                        MotionEvent.ACTION_UP ||
                        event.getAction() ==
                                MotionEvent.ACTION_CANCEL) {

                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(110)
                            .start();
                }

                return false;
            }
    );
}

private void haptic(
        View view
) {

    if (view == null) {
        return;
    }

    try {

        view.performHapticFeedback(
                HapticFeedbackConstants
                        .KEYBOARD_TAP
        );

    } catch (Exception ignored) {
    }
}

private void animateScreen(
        View view
) {

    if (view == null) {
        return;
    }

    view.setAlpha(0f);

    view.setTranslationY(
            dp(8)
    );

    view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(
                    new DecelerateInterpolator()
            )
            .start();
}

// =========================================================
// BASIC UI HELPERS
// =========================================================

private LinearLayout vertical() {

    LinearLayout layout =
            new LinearLayout(this);

    layout.setOrientation(
            LinearLayout.VERTICAL
    );

    return layout;
}

private LinearLayout horizontal() {

    LinearLayout layout =
            new LinearLayout(this);

    layout.setOrientation(
            LinearLayout.HORIZONTAL
    );

    return layout;
}

private TextView text(
        String value,
        float size,
        int color,
        Typeface typeface
) {

    TextView view =
            new TextView(this);

    view.setText(
            value
    );

    view.setTextSize(
            size
    );

    view.setTextColor(
            color
    );

    view.setTypeface(
            typeface
    );

    view.setIncludeFontPadding(
            false
    );

    return view;
}

private GradientDrawable gradient(
        int[] colors,
        int radius
) {

    GradientDrawable drawable =
            new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    colors
            );

    drawable.setCornerRadius(
            radius
    );

    return drawable;
}

private GradientDrawable solid(
        int color,
        int radius
) {

    GradientDrawable drawable =
            new GradientDrawable();

    drawable.setColor(
            color
    );

    drawable.setCornerRadius(
            radius
    );

    return drawable;
}

private void gap(
        LinearLayout parent,
        int height
) {

    if (parent == null) {
        return;
    }

    Space space =
            new Space(this);

    parent.addView(
            space,
            params(
                    1,
                    dp(height)
            )
    );
}

private void horizontalGap(
        LinearLayout parent,
        int width
) {

    if (parent == null) {
        return;
    }

    Space space =
            new Space(this);

    parent.addView(
            space,
            params(
                    dp(width),
                    1
            )
    );
}

private LinearLayout.LayoutParams params(
        int width,
        int height
) {

    return new LinearLayout.LayoutParams(
            width,
            height
    );
}

private LinearLayout.LayoutParams wrap() {

    return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
    );
}

private int dp(
        int value
) {

    return (int) (
            value
                    *
                    getResources()
                            .getDisplayMetrics()
                            .density
    );
}

// =========================================================
// BACK
// =========================================================

@Override
public void onBackPressed() {

    if (!TAB_HOME.equals(
            currentTab
    )) {

        showHome();

        return;
    }

    super.onBackPressed();
}
}