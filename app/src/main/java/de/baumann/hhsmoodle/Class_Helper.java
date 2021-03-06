/*
    This file is part of the HHS Moodle WebApp.

    HHS Moodle WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HHS Moodle WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.hhsmoodle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.concurrent.Executor;

class Class_Helper {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    // Messages, Toasts, ...

    static SpannableString textSpannable(String text) {
        SpannableString s;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            s = new SpannableString(Html.fromHtml(text));
        }
        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }

    // Activities -> start, end, ...

    static void switchToActivity(Activity activity, Class to) {
        Intent intent = new Intent(activity, to);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (Exception ignore) {
            }
            activity.finish();
        }).start();
    }

    // used Methods

    static void switchIcon(Activity activity, String string, ImageView be) {
        assert be != null;

        switch (string) {
            case "15":
                be.setImageResource(R.drawable.circle_pink);
                break;
            case "16":
                be.setImageResource(R.drawable.circle_purple);
                break;
            case "17":
                be.setImageResource(R.drawable.circle_blue);
                break;
            case "18":
                be.setImageResource(R.drawable.circle_teal);
                break;
            case "19":
                be.setImageResource(R.drawable.circle_green);
                break;
            case "20":
                be.setImageResource(R.drawable.circle_lime);
                break;
            case "21":
                be.setImageResource(R.drawable.circle_yellow);
                break;
            case "22":
                be.setImageResource(R.drawable.circle_orange);
                break;
            case "23":
                be.setImageResource(R.drawable.circle_brown);
                break;
            default:
                be.setImageResource(R.drawable.circle_red);
                break;
        }
    }

    // Strings, files, ...

    static String secString(String string) {
        return string.replaceAll("'", "\'\'");
    }

    static void grantPermissionsStorage(final Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
            int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(Class_Helper.textSpannable(activity.getString(R.string.app_permissions)));
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(view -> {
                    activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_ASK_PERMISSIONS);
                    bottomSheetDialog.cancel();
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView);
            }
        }
    }

    static void setBottomSheetBehavior(final BottomSheetDialog dialog, final View view) {
        BottomSheetBehavior mBehavior = BottomSheetBehavior.from((View) view.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.cancel();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    // Security
    @SuppressLint("ApplySharedPref")
    static void setLoginData(final Activity activity, Runnable runOnSuccess, Runnable runOnCancel) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final View dialogView = View.inflate(activity, R.layout.dialog_edit_login, null);
            final EditText moodle_userName = dialogView.findViewById(R.id.moodle_userName);
            moodle_userName.setText(PreferenceHelper.getUsername(activity));
            final EditText moodle_userPW = dialogView.findViewById(R.id.moodle_userPW);
            moodle_userPW.setText(PreferenceHelper.getPassword(activity));
            builder.setView(dialogView);
            builder.setPositiveButton(R.string.toast_yes, (dialog, whichButton) -> {
                final String username = moodle_userName.getText().toString().trim();
                final String password = moodle_userPW.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(activity, activity.getString(R.string.login_text_edit), Toast.LENGTH_SHORT).show();
                    setLoginData(activity, runOnSuccess, runOnCancel);
                } else {
                    PreferenceHelper.setUsernamePassword(activity, username, password);
                    dialog.cancel();
                    runOnSuccess.run();
                }
            });
            builder.setNegativeButton(R.string.toast_cancel, (dialog, whichButton) -> {
                dialog.cancel();
                runOnCancel.run();
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void checkAuthentication(final Activity activity) {
        boolean biometric = PreferenceHelper.isBiometric(activity);

        if (biometric)
            checkFingerprint(activity);
        else
            checkPin(activity);
    }

    private static void checkPin(final Activity activity) {
        String protect = PreferenceHelper.getPin(activity);
        if (protect.length() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final View dialogView = View.inflate(activity, R.layout.dialog_enter_pin, null);
            final TextView text = dialogView.findViewById(R.id.pass_userPin);
            Button ib0 = dialogView.findViewById(R.id.button0);
            assert ib0 != null;
            ib0.setOnClickListener(view -> enterNum(dialogView, "0"));

            Button ib1 = dialogView.findViewById(R.id.button1);
            assert ib1 != null;
            ib1.setOnClickListener(view -> enterNum(dialogView, "1"));

            Button ib2 = dialogView.findViewById(R.id.button2);
            assert ib2 != null;
            ib2.setOnClickListener(view -> enterNum(dialogView, "2"));

            Button ib3 = dialogView.findViewById(R.id.button3);
            assert ib3 != null;
            ib3.setOnClickListener(view -> enterNum(dialogView, "3"));

            Button ib4 = dialogView.findViewById(R.id.button4);
            assert ib4 != null;
            ib4.setOnClickListener(view -> enterNum(dialogView, "4"));

            Button ib5 = dialogView.findViewById(R.id.button5);
            assert ib5 != null;
            ib5.setOnClickListener(view -> enterNum(dialogView, "5"));

            Button ib6 = dialogView.findViewById(R.id.button6);
            assert ib6 != null;
            ib6.setOnClickListener(view -> enterNum(dialogView, "6"));

            Button ib7 = dialogView.findViewById(R.id.button7);
            assert ib7 != null;
            ib7.setOnClickListener(view -> enterNum(dialogView, "7"));

            Button ib8 = dialogView.findViewById(R.id.button8);
            assert ib8 != null;
            ib8.setOnClickListener(view -> enterNum(dialogView, "8"));

            Button ib9 = dialogView.findViewById(R.id.button9);
            assert ib9 != null;
            ib9.setOnClickListener(view -> enterNum(dialogView, "9"));


            ImageButton enter = dialogView.findViewById(R.id.imageButtonEnter);
            assert enter != null;

            final ImageButton cancel = dialogView.findViewById(R.id.imageButtonCancel);
            assert cancel != null;
            cancel.setOnClickListener(view -> text.setText(""));

            final Button clear = dialogView.findViewById(R.id.buttonReset);
            assert clear != null;
            clear.setOnClickListener(view -> {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView1 = View.inflate(activity, R.layout.dialog_action, null);
                TextView textView = dialogView1.findViewById(R.id.dialog_text);
                textView.setText(activity.getString(R.string.pw_forgotten_dialog));
                Button action_ok = dialogView1.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(view1 -> {
                    try {
                        // clearing app data
                        Runtime runtime = Runtime.getRuntime();
                        runtime.exec("pm clear com.asdoi.studon");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                });
                bottomSheetDialog.setContentView(dialogView1);
                bottomSheetDialog.show();
                Class_Helper.setBottomSheetBehavior(bottomSheetDialog, dialogView1);
            });

            builder.setView(dialogView);
            builder.setOnCancelListener(dialog -> activity.finishAffinity());

            final AlertDialog dialog = builder.create();
            // Display the custom alert dialog on interface
            dialog.show();

            enter.setOnClickListener(view -> {
                String Password = text.getText().toString().trim();

                if (Password.equals(protect)) {
                    dialog.dismiss();
                } else {
                    Toast.makeText(activity, activity.getString(R.string.toast_wrongPW), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static void checkFingerprint(final Activity activity) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt((FragmentActivity) activity,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(activity, activity.getString(R.string.authentication_error) + " " + errString, Toast.LENGTH_SHORT).show();
                activity.finishAffinity();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(activity, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                activity.finishAffinity();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_login_title))
                .setSubtitle(activity.getString(R.string.biometric_login_message))
//                .setNegativeButtonText("Use account password")
                .setDeviceCredentialAllowed(true)
                .build();


        biometricPrompt.authenticate(promptInfo);
    }

    private static void enterNum(View view, String number) {
        TextView text = view.findViewById(R.id.pass_userPin);
        String textNow = text.getText().toString().trim();
        String pin = textNow + number;
        text.setText(pin);
    }
}