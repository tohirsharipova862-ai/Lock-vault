package com.example.applock;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class LockActivity extends Activity {

    // =========================================================
    // PREFERENCES
    // =========================================================

    private static final String PREFS_NAME =
            "lockvault_preferences";

    private static final String KEY_PIN_HASH =
            "pin_hash";

    private static final String KEY_PIN_SALT =
            "pin_salt";

    /*
     * Эти ключи ОБЯЗАТЕЛЬНО должны совпадать
     * с AppLockAccessibilityService.
     */
    private static final String KEY_UNLOCKED_PACKAGE =
            "temporarily_unlocked_package";

    private static final String KEY_UNLOCK_TIME =
            "temporarily_unlocked_time";

    // =========================================================
    // COLORS
    // =========================================================

    private final int BG =
            Color.rgb(10, 10, 15);

    private final int SURFACE =
            Color.rgb(25, 24, 34);

    private final int WHITE =
            Color.rgb(247, 246, 251);

    private final int TEXT =
            Color.rgb(164, 160, 178);

    private final int TEXT_DARK =
            Color.rgb(101, 98, 115);

    private final int PURPLE =
            Color.rgb(158, 117, 255);

    private final int PURPLE_LIGHT =
            Color.rgb(201, 177, 255);

    // =========================================================
    // DATA
    // =========================================================

    private Typeface regular;
    private Typeface medium;
    private Typeface bold;

    private String lockedPackage = "";

    private boolean unlocked = false;

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

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

        readLockedPackage(
                getIntent()
        );

        /*
         * LockVault никогда не должен
         * блокировать сам себя.
         */
        if (lockedPackage.equals(
                getPackageName()
        )) {

            finish();
            return;
        }

        showLockScreen();
    }

    // =========================================================
    // NEW INTENT
    // =========================================================

    @Override
    protected void onNewIntent(
            Intent intent
    ) {

        super.onNewIntent(intent);

        setIntent(intent);

        unlocked = false;

        readLockedPackage(
                intent
        );

        if (lockedPackage.equals(
                getPackageName()
        )) {

            finish();
            return;
        }

        showLockScreen();
    }

    // =========================================================
    // READ PACKAGE
    // =========================================================

    private void readLockedPackage(
            Intent intent
    ) {

        lockedPackage = "";

        if (intent == null) {
            return;
        }

        String value =
                intent.getStringExtra(
                        "locked_package"
                );

        if (value != null) {

            lockedPackage =
                    value;
        }
    }

    // =========================================================
    // LOCK SCREEN
    // =========================================================

    private void showLockScreen() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setPadding(
                dp(28),
                dp(45),
                dp(28),
                dp(25)
        );

        root.setBackgroundColor(BG);

        // =====================================================
        // TOP SPACE
        // =====================================================

        Space topSpace =
                new Space(this);

        root.addView(
                topSpace,
                new LinearLayout.LayoutParams(
                        1,
                        0,
                        1
                )
        );

        // =====================================================
        // ICON
        // =====================================================

        TextView icon =
                text(
                        "◆",
                        28,
                        PURPLE_LIGHT,
                        medium
                );

        icon.setGravity(
                Gravity.CENTER
        );

        GradientDrawable iconBackground =
                gradient(
                        new int[]{
                                Color.rgb(
                                        76,
                                        53,
                                        108
                                ),
                                Color.rgb(
                                        47,
                                        36,
                                        67
                                )
                        },
                        dp(30)
                );

        iconBackground.setStroke(
                dp(1),
                Color.rgb(
                        104,
                        76,
                        147
                )
        );

        icon.setBackground(
                iconBackground
        );

        icon.setElevation(
                dp(5)
        );

        root.addView(
                icon,
                params(
                        dp(88),
                        dp(88)
                )
        );

        gap(
                root,
                28
        );

        // =====================================================
        // SECURITY LABEL
        // =====================================================

        TextView security =
                text(
                        "LOCKVAULT SECURITY",
                        10,
                        PURPLE_LIGHT,
                        medium
                );

        security.setLetterSpacing(
                0.17f
        );

        security.setGravity(
                Gravity.CENTER
        );

        root.addView(
                security
        );

        gap(
                root,
                17
        );

        // =====================================================
        // TITLE
        // =====================================================

        TextView title =
                text(
                        "App Locked",
                        34,
                        WHITE,
                        bold
                );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(
                title
        );

        gap(
                root,
                12
        );

        TextView subtitle =
                text(
                        "Enter your 4-digit PIN to continue",
                        14,
                        TEXT,
                        regular
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        root.addView(
                subtitle
        );

        gap(
                root,
                32
        );

        // =====================================================
        // PIN DOTS
        // =====================================================

        LinearLayout dots =
                new LinearLayout(this);

        dots.setOrientation(
                LinearLayout.HORIZONTAL
        );

        dots.setGravity(
                Gravity.CENTER
        );

        View[] pinDots =
                new View[4];

        for (int i = 0;
             i < 4;
             i++) {

            View dot =
                    new View(this);

            dot.setBackground(
                    pinDot(false)
            );

            pinDots[i] =
                    dot;

            LinearLayout.LayoutParams p =
                    params(
                            dp(16),
                            dp(16)
                    );

            if (i > 0) {

                p.leftMargin =
                        dp(20);
            }

            dots.addView(
                    dot,
                    p
            );
        }

        root.addView(
                dots
        );

        gap(
                root,
                22
        );

        // =====================================================
        // HELPER
        // =====================================================

        TextView helper =
                text(
                        "Protected locally on your device",
                        10,
                        TEXT_DARK,
                        regular
                );

        helper.setGravity(
                Gravity.CENTER
        );

        root.addView(
                helper
        );

        gap(
                root,
                35
        );

        // =====================================================
        // KEYPAD
        // =====================================================

        LinearLayout keypad =
                new LinearLayout(this);

        keypad.setOrientation(
                LinearLayout.VERTICAL
        );

        keypad.setGravity(
                Gravity.CENTER
        );

        StringBuilder entered =
                new StringBuilder();

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"", "0", "⌫"}
        };

        for (int r = 0;
             r < keys.length;
             r++) {

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setGravity(
                    Gravity.CENTER
            );

            for (int c = 0;
                 c < 3;
                 c++) {

                String value =
                        keys[r][c];

                if (value.isEmpty()) {

                    Space empty =
                            new Space(this);

                    row.addView(
                            empty,
                            params(
                                    dp(82),
                                    dp(64)
                            )
                    );

                } else {

                    TextView key =
                            createKey(
                                    value
                            );

                    row.addView(
                            key,
                            params(
                                    dp(82),
                                    dp(64)
                            )
                    );

                    key.setOnClickListener(
                            v -> {

                                v.performHapticFeedback(
                                        HapticFeedbackConstants
                                                .KEYBOARD_TAP
                                );

                                String pressed =
                                        ((TextView) v)
                                                .getText()
                                                .toString();

                                // =================================
                                // DELETE
                                // =================================

                                if (pressed.equals(
                                        "⌫"
                                )) {

                                    if (entered.length()
                                            > 0) {

                                        entered.deleteCharAt(
                                                entered.length()
                                                        - 1
                                        );

                                        updateDots(
                                                pinDots,
                                                entered.length()
                                        );
                                    }

                                    return;
                                }

                                if (entered.length()
                                    >= 4) {

                                    return;
                                }

                                entered.append(
                                        pressed
                                );

                                updateDots(
                                        pinDots,
                                        entered.length()
                                );

                                // =================================
                                // VERIFY
                                // =================================

                                if (entered.length()
                                    == 4) {

                                    String pin =
                                            entered.toString();

                                    if (verifyPin(
                                            pin
                                    )) {

                                        /*
                                         * ВАЖНО:
                                         *
                                         * Перед закрытием LockActivity
                                         * записываем, какое приложение
                                         * пользователь разблокировал.
                                         *
                                         * AppLockAccessibilityService
                                         * увидит эти значения и НЕ
                                         * откроет LockActivity повторно.
                                         */
                                        unlockCurrentPackage();

                                    } else {

                                        Toast.makeText(
                                                this,
                                                "Incorrect PIN",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        entered.setLength(
                                                0
                                        );

                                        updateDots(
                                                pinDots,
                                                0
                                        );

                                        shakeDots(
                                                dots
                                        );
                                    }
                                }
                            }
                    );
                }

                if (c < 2) {

                    Space horizontal =
                            new Space(this);

                    row.addView(
                            horizontal,
                            params(
                                    dp(12),
                                    1
                            )
                    );
                }
            }

            keypad.addView(
                    row
            );

            if (r <
                    keys.length - 1) {

                gap(
                        keypad,
                        12
                );
            }
        }

        root.addView(
                keypad
        );

        // =====================================================
        // BOTTOM SPACE
        // =====================================================

        Space bottomSpace =
                new Space(this);

        root.addView(
                bottomSpace,
                new LinearLayout.LayoutParams(
                        1,
                        0,
                        1
                )
        );

        setContentView(
                root
        );
    }

    // =========================================================
    // SUCCESSFUL UNLOCK
    // =========================================================

private void unlockCurrentPackage() {

    if (unlocked) {
        return;
    }

    if (lockedPackage == null ||
            lockedPackage.isEmpty()) {

        finish();
        return;
    }

    unlocked = true;

    SharedPreferences preferences =
            getSharedPreferences(
                    PREFS_NAME,
                    MODE_PRIVATE
            );

    /*
     * Сначала разрешаем этому приложению открыться.
     * AccessibilityService увидит этот флаг
     * и не должен снова показывать PIN.
     */
    preferences
            .edit()
            .putString(
                    KEY_UNLOCKED_PACKAGE,
                    lockedPackage
            )
            .putLong(
                    KEY_UNLOCK_TIME,
                    SystemClock.elapsedRealtime()
            )
            .commit();

    try {

        /*
         * Получаем стандартный Intent запуска
         * именно того приложения, которое было
         * заблокировано.
         */
        Intent launchIntent =
                getPackageManager()
                        .getLaunchIntentForPackage(
                                lockedPackage
                        );

        if (launchIntent != null) {

            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            );

            startActivity(
                    launchIntent
            );
        }

    } catch (Exception e) {

        Toast.makeText(
                this,
                "Не удалось открыть приложение",
                Toast.LENGTH_SHORT
        ).show();
    }

    finish();

    overridePendingTransition(
            0,
            0
    );
}

    // =========================================================
    // VERIFY PIN
    // =========================================================

    private boolean verifyPin(
            String enteredPin
    ) {

        try {

            SharedPreferences preferences =
                    getSharedPreferences(
                            PREFS_NAME,
                            MODE_PRIVATE
                    );

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
                            enteredPin,
                            salt
                    );

            return MessageDigest.isEqual(
                    savedHash,
                    enteredHash
            );

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // HASH
    // =========================================================

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

    // =========================================================
    // SHAKE
    // =========================================================

    private void shakeDots(
            LinearLayout dots
    ) {

        dots.animate()
                .translationX(
                        dp(-10)
                )
                .setDuration(70)
                .withEndAction(
                        () ->
                                dots.animate()
                                        .translationX(
                                                dp(10)
                                        )
                                        .setDuration(70)
                                        .withEndAction(
                                                () ->
                                                        dots.animate()
                                                                .translationX(
                                                                        0
                                                                )
                                                                .setDuration(
                                                                        70
                                                                )
                                                                .start()
                                        )
                                        .start()
                )
                .start();
    }

    // =========================================================
    // KEY
    // =========================================================

    private TextView createKey(
            String value
    ) {

        TextView key =
                text(
                        value,
                        value.equals("⌫")
                                ? 20
                                : 22,
                        value.equals("⌫")
                                ? TEXT
                                : WHITE,
                        medium
                );

        key.setGravity(
                Gravity.CENTER
        );

        GradientDrawable bg =
                gradient(
                        new int[]{
                                Color.rgb(
                                        27,
                                        26,
                                        36
                                ),
                                Color.rgb(
                                        20,
                                        20,
                                        28
                                )
                        },
                        dp(20)
                );

        bg.setStroke(
                dp(1),
                Color.rgb(
                        49,
                        46,
                        62
                )
        );

        key.setBackground(
                bg
        );

        return key;
    }

    // =========================================================
    // PIN DOT
    // =========================================================

    private GradientDrawable pinDot(
            boolean active
    ) {

        GradientDrawable dot =
                new GradientDrawable();

        dot.setShape(
                GradientDrawable.OVAL
        );

        if (active) {

            dot.setColor(
                    PURPLE
            );

            dot.setStroke(
                    dp(2),
                    PURPLE_LIGHT
            );

        } else {

            dot.setColor(
                    SURFACE
            );

            dot.setStroke(
                    dp(1),
                    Color.rgb(
                            67,
                            63,
                            80
                    )
            );
        }

        return dot;
    }

    // =========================================================
    // UPDATE DOTS
    // =========================================================

    private void updateDots(
            View[] dots,
            int count
    ) {

        for (int i = 0;
             i < dots.length;
             i++) {

            dots[i].setBackground(
                    pinDot(
                            i < count
                    )
            );
        }
    }

    // =========================================================
    // TEXT
    // =========================================================

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

    // =========================================================
    // GRADIENT
    // =========================================================

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

    // =========================================================
    // GAP
    // =========================================================

    private void gap(
            LinearLayout parent,
            int height
    ) {

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

    // =========================================================
    // PARAMS
    // =========================================================

    private LinearLayout.LayoutParams params(
            int width,
            int height
    ) {

        return new LinearLayout.LayoutParams(
                width,
                height
        );
    }

    // =========================================================
    // DP
    // =========================================================

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

        /*
         * Кнопка назад НЕ разблокирует приложение.
         *
         * Если PIN не введён правильно,
         * отправляем пользователя на Home.
         */
        if (unlocked) {

            finish();
            return;
        }

        Intent home =
                new Intent(
                        Intent.ACTION_MAIN
                );

        home.addCategory(
                Intent.CATEGORY_HOME
        );

        home.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(
                home
        );

        finish();
    }
}