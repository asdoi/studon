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


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class Activity_Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_system_Settings);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        PreferenceManager.setDefaultValues(this, R.xml.user_settings, false);
        setTitle(R.string.menu_more_settings);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
                .commit();
    }

    @SuppressWarnings("ConstantConditions")
    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.user_settings, rootKey);

            findPreference("settings_help").setOnPreferenceClickListener(preference -> {
                createInfoDialog(requireActivity(), R.string.dialog_help_title, R.string.dialog_help_text, () -> {
                });
                return false;
            });

            findPreference("settings_license").setOnPreferenceClickListener(preference -> {
                createInfoDialog(requireActivity(), R.string.dialog_license_title, R.string.dialog_license_text, () -> {
                });
                return false;
            });

            findPreference("settings_security_moodle").setOnPreferenceClickListener(preference -> {
                Class_Helper.setLoginData(getActivity(), () -> {
                    Intent i = getContext().getPackageManager().
                            getLaunchIntentForPackage(getContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    getActivity().finish();
                }, () -> {
                });
                return false;
            });

            final CheckBoxPreference biometric = findPreference("biometric");

            biometric.setOnPreferenceClickListener(preference -> {
                final Activity activity = getActivity();
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(activity));
                sharedPref.edit().putString("settings_security_pin", "").apply();
                return false;
            });

            findPreference("settings_security_pin").setOnPreferenceClickListener(preference -> {
                final Activity activity = getActivity();
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(activity));
                final String password = sharedPref.getString("settings_security_pin", "");
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_edit_pin, null);

                final EditText pass_userPW = dialogView.findViewById(R.id.pass_userPin);
                pass_userPW.setText(password);

                builder.setView(dialogView);
                builder.setTitle(R.string.settings_security_pin);
                builder.setPositiveButton(R.string.toast_yes, (dialog, whichButton) -> {
                    String inputTag = pass_userPW.getText().toString().trim();
                    sharedPref.edit().putString("settings_security_pin", inputTag).apply();
                    biometric.setChecked(false);
                });
                builder.setNegativeButton(R.string.toast_cancel, (dialog, whichButton) -> dialog.cancel());
                final AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            });
        }
    }

    public static void createInfoDialog(Activity activity, int p, int p2, Runnable runOnOk) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setPositiveButton(R.string.toast_yes, (dialog, id) -> {
            dialog.cancel();
            runOnOk.run();
        });
        builder.setTitle(p);
        builder.setMessage(Class_Helper.textSpannable(activity.getString(p2)));
        AlertDialog dialog = builder.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Class_Helper.switchToActivity(Activity_Settings.this, Activity_Main.class);
    }
}