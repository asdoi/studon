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

package de.baumann.hhsmoodle.helper;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.baumann.hhsmoodle.R;
import filechooser.ChooserDialog;

public class helper_main {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    public static void grantPermissions(final Activity from) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (sharedPref.getBoolean ("perm_notShow", false)){
                int hasWRITE_EXTERNAL_STORAGE = from.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    if (!from.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        new android.app.AlertDialog.Builder(from)
                                .setTitle(R.string.app_permissions_title)
                                .setMessage(helper_main.textSpannable(from.getString(R.string.app_permissions)))
                                .setNeutralButton(R.string.toast_notAgain, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        sharedPref.edit()
                                                .putBoolean("perm_notShow", false)
                                                .apply();
                                    }
                                })
                                .setPositiveButton(from.getString(R.string.toast_yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 23)
                                            from.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    REQUEST_CODE_ASK_PERMISSIONS);
                                    }
                                })
                                .setNegativeButton(from.getString(R.string.toast_cancel), null)
                                .show();
                        return;
                    }
                    from.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CODE_ASK_PERMISSIONS);
                }
            }
        }
    }

    public static SpannableString textSpannable (String text) {
        SpannableString s;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            s = new SpannableString(Html.fromHtml(text));
        }
        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }

    public static void switchToActivity(Activity from, Class to, String Extra, boolean finishFromActivity) {
        Intent intent = new Intent(from, to);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("url", Extra);
        from.startActivity(intent);
        if (finishFromActivity) {
            from.finish();
        }
    }

    public static void isOpened (Activity from) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putBoolean("isOpened", false)
                .apply();
    }

    public static void isClosed (Activity from) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        sharedPref.edit()
                .putBoolean("isOpened", true)
                .apply();
    }

    public static void resetStartTab (Activity from) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        String tabPref = sharedPref.getString("tabPref", "");
        if (tabPref.length() > 0) {
            sharedPref.edit()
                    .putString("tabPref", "")
                    .putString("tabMain", tabPref)
                    .apply();
        }
    }

    public static void editNote (final Activity from) {

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(from);
        final EditText titleInput;
        final EditText textInput;
        final String priority = sharedPref.getString("handleTextIcon", "");

        LayoutInflater inflater = from.getLayoutInflater();

        final ViewGroup nullParent = null;
        View dialogView = inflater.inflate(R.layout.dialog_editnote, nullParent);

        titleInput = (EditText) dialogView.findViewById(R.id.note_title_input);
        textInput = (EditText) dialogView.findViewById(R.id.note_text_input);
        titleInput.setText(sharedPref.getString("handleTextTitle", ""));
        titleInput.setSelection(titleInput.getText().length());
        textInput.setText(sharedPref.getString("handleTextText", ""));
        textInput.setSelection(textInput.getText().length());

        new Handler().postDelayed(new Runnable() {
            public void run() {
                helper_main.showKeyboard(from,titleInput);
            }
        }, 200);

        final ImageButton be = (ImageButton) dialogView.findViewById(R.id.imageButtonPri);
        assert be != null;

        switch (priority) {
            case "":
                be.setImageResource(R.drawable.pr_green);
                sharedPref.edit()
                        .putString("handleTextIcon", "")
                        .apply();
                break;
            case "!":
                be.setImageResource(R.drawable.pr_yellow);
                sharedPref.edit()
                        .putString("handleTextIcon", "!")
                        .apply();
                break;
            case "!!":
                be.setImageResource(R.drawable.pr_red);
                sharedPref.edit()
                        .putString("handleTextIcon", "!!")
                        .apply();
                break;
        }

        be.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                final helper_main.Item[] items = {
                        new Item(from.getString(R.string.note_priority_0), R.drawable.pr_green_1),
                        new Item(from.getString(R.string.note_priority_1), R.drawable.pr_yellow_1),
                        new Item(from.getString(R.string.note_priority_2), R.drawable.pr_red_1),
                };

                ListAdapter adapter = new ArrayAdapter<helper_main.Item>(
                        from,
                        android.R.layout.select_dialog_item,
                        android.R.id.text1,
                        items){
                    @NonNull
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        //Use super class to create the View
                        View v = super.getView(position, convertView, parent);
                        TextView tv = (TextView)v.findViewById(android.R.id.text1);
                        tv.setTextSize(18);
                        tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);
                        //Add margin between image and text (support various screen densities)
                        int dp5 = (int) (5 * from.getResources().getDisplayMetrics().density + 0.5f);
                        tv.setCompoundDrawablePadding(dp5);

                        return v;
                    }
                };

                new android.app.AlertDialog.Builder(from)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {
                                    be.setImageResource(R.drawable.pr_green);
                                    sharedPref.edit()
                                            .putString("handleTextIcon", "")
                                            .apply();
                                } else if (item == 1) {
                                    be.setImageResource(R.drawable.pr_yellow);
                                    sharedPref.edit()
                                            .putString("handleTextIcon", "!")
                                            .apply();
                                } else if (item == 2) {
                                    be.setImageResource(R.drawable.pr_red);
                                    sharedPref.edit()
                                            .putString("handleTextIcon", "!!")
                                            .apply();
                                }
                            }
                        }).show();
            }
        });

        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(from)
                .setView(dialogView)
                .setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        String seqno = sharedPref.getString("handleTextSeqno", "");

                        try {

                            final Database_Notes db = new Database_Notes(from);
                            String inputTitle = titleInput.getText().toString().trim();
                            String inputContent = textInput.getText().toString().trim();

                            db.addBookmark(inputTitle, inputContent, sharedPref.getString("handleTextIcon", ""));
                            db.close();

                            if (seqno.length() > 0) {
                                db.deleteNote((Integer.parseInt(seqno)));
                                sharedPref.edit()
                                        .putString("handleTextSeqno", "")
                                        .apply();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sharedPref.edit()
                                .putString("handleTextTitle", "")
                                .putString("handleTextText", "")
                                .putString("handleTextIcon", "")
                                .apply();
                        helper_main.setNotesList(from);
                        if (sharedPref.getString("fromPopup", "").equals("0")) {
                            sharedPref.edit()
                                    .putString("fromPopup", "")
                                    .apply();
                            from.recreate();
                        }

                    }
                })
                .setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        sharedPref.edit()
                                .putString("handleTextTitle", "")
                                .putString("handleTextText", "")
                                .putString("handleTextIcon", "")
                                .apply();
                        dialog.cancel();
                    }
                });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(final DialogInterface dialog) {
                if (sharedPref.getString("fromPopup", "").equals("0")) {
                    sharedPref.edit()
                            .putString("fromPopup", "")
                            .apply();
                }
            }
        });
        dialog.show();
    }

    private static class Item{
        public final String text;
        public final int icon;
        Item(String text, Integer icon) {
            this.text = text;
            this.icon = icon;
        }
        @Override
        public String toString() {
            return text;
        }
    }

    private static void setNotesList(final Activity from) {

        final ListView listView = (ListView)from.findViewById(R.id.notes);

        ArrayList<HashMap<String,String>> mapList = new ArrayList<>();

        try {
            Database_Notes db = new Database_Notes(from);
            ArrayList<String[]> bookmarkList = new ArrayList<>();
            db.getBookmarks(bookmarkList);
            if (bookmarkList.size() == 0) {
                db.loadInitialData();
                db.getBookmarks(bookmarkList);
            }
            db.close();

            for (String[] strAry : bookmarkList) {
                HashMap<String, String> map = new HashMap<>();
                map.put("seqno", strAry[0]);
                map.put("title", strAry[1]);
                map.put("cont", strAry[2]);
                map.put("icon", strAry[3]);
                mapList.add(map);
            }

            SimpleAdapter simpleAdapter = new SimpleAdapter(
                    from,
                    mapList,
                    R.layout.list_item_notes,
                    new String[] {"title", "cont"},
                    new int[] {R.id.textView_title_notes, R.id.textView_des_notes}
            ) {
                @Override
                public View getView (final int position, View convertView, ViewGroup parent) {

                    @SuppressWarnings("unchecked")
                    HashMap<String,String> map = (HashMap<String,String>)listView.getItemAtPosition(position);
                    final String title = map.get("title");
                    final String cont = map.get("cont");
                    final String seqnoStr = map.get("seqno");
                    final String icon = map.get("icon");

                    View v = super.getView(position, convertView, parent);
                    ImageView i=(ImageView) v.findViewById(R.id.icon_notes);

                    switch (icon) {
                        case "":
                            i.setImageResource(R.drawable.pr_green);
                            break;
                        case "!":
                            i.setImageResource(R.drawable.pr_yellow);
                            break;
                        case "!!":
                            i.setImageResource(R.drawable.pr_red);
                            break;
                    }

                    i.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View arg0) {

                            final helper_main.Item[] items = {
                                    new Item(from.getString(R.string.note_priority_0), R.drawable.pr_green_1),
                                    new Item(from.getString(R.string.note_priority_1), R.drawable.pr_yellow_1),
                                    new Item(from.getString(R.string.note_priority_2), R.drawable.pr_red_1),
                            };

                            ListAdapter adapter = new ArrayAdapter<helper_main.Item>(
                                    from,
                                    android.R.layout.select_dialog_item,
                                    android.R.id.text1,
                                    items){
                                @NonNull
                                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                                    //Use super class to create the View
                                    View v = super.getView(position, convertView, parent);
                                    TextView tv = (TextView)v.findViewById(android.R.id.text1);
                                    tv.setTextSize(18);
                                    tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);
                                    //Add margin between image and text (support various screen densities)
                                    int dp5 = (int) (5 * from.getResources().getDisplayMetrics().density + 0.5f);
                                    tv.setCompoundDrawablePadding(dp5);

                                    return v;
                                }
                            };

                            new AlertDialog.Builder(from)
                                    .setAdapter(adapter, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int item) {
                                            if (item == 0) {
                                                try {
                                                    final Database_Notes db = new Database_Notes(from);
                                                    db.deleteNote((Integer.parseInt(seqnoStr)));
                                                    db.addBookmark(title, cont, "");
                                                    db.close();
                                                    setNotesList(from);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                            } else if (item == 1) {
                                                try {
                                                    final Database_Notes db = new Database_Notes(from);
                                                    db.deleteNote((Integer.parseInt(seqnoStr)));
                                                    db.addBookmark(title, cont, "!");
                                                    db.close();
                                                    setNotesList(from);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                            } else if (item == 2) {
                                                try {
                                                    final Database_Notes db = new Database_Notes(from);
                                                    db.deleteNote((Integer.parseInt(seqnoStr)));
                                                    db.addBookmark(title, cont, "!!");
                                                    db.close();
                                                    setNotesList(from);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }).show();
                        }
                    });
                    return v;
                }
            };

            if (listView != null) {
                listView.setAdapter(simpleAdapter);
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static File newFile () {
        return  new File(Environment.getExternalStorageDirectory() + newFileDest() + newFileName());
    }

    public static String newFileDest () {
        return  ("/HHS_Moodle/");
    }

    public static String newFileName () {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_HH-mm-ss", Locale.getDefault());
        return  dateFormat.format(date) + ".jpg";
    }

    public static void showKeyboard(Activity from, EditText editText) {
        InputMethodManager imm = (InputMethodManager) from.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    public static void openFilePicker (final Activity activity, final View view, final String startDir) {

        new ChooserDialog().with(activity)
                .withStartFile(startDir)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(final File pathFile) {

                        final String fileExtension = pathFile.getAbsolutePath().substring(pathFile.getAbsolutePath().lastIndexOf("."));
                        final String fileName = pathFile.getAbsolutePath().substring(pathFile.getAbsolutePath().lastIndexOf("/")+1);
                        final String  fileNameWE = fileName.substring(0, fileName.lastIndexOf("."));

                        final CharSequence[] options = {
                                activity.getString(R.string.choose_menu_1),
                                activity.getString(R.string.choose_menu_2),
                                activity.getString(R.string.choose_menu_3),
                                activity.getString(R.string.choose_menu_4)};

                        final AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

                        dialog.setItems(options, new DialogInterface.OnClickListener() {
                            @SuppressWarnings("ResultOfMethodCallIgnored")
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                if (options[item].equals(activity.getString(R.string.choose_menu_1))) {

                                    String text = (activity.getString(R.string.toast_extension) + ": " + fileExtension);

                                    switch (fileExtension) {
                                        case ".gif":
                                        case ".bmp":
                                        case ".tiff":
                                        case ".svg":
                                        case ".png":
                                        case ".jpg":
                                        case ".jpeg":
                                            helper_main.openFile(activity, pathFile, "image/*", view);
                                            break;
                                        case ".m3u8":
                                        case ".mp3":
                                        case ".wma":
                                        case ".midi":
                                        case ".wav":
                                        case ".aac":
                                        case ".aif":
                                        case ".amp3":
                                        case ".weba":
                                            helper_main.openFile(activity, pathFile, "audio/*", view);
                                            break;
                                        case ".mpeg":
                                        case ".mp4":
                                        case ".ogg":
                                        case ".webm":
                                        case ".qt":
                                        case ".3gp":
                                        case ".3g2":
                                        case ".avi":
                                        case ".f4v":
                                        case ".flv":
                                        case ".h261":
                                        case ".h263":
                                        case ".h264":
                                        case ".asf":
                                        case ".wmv":
                                            helper_main.openFile(activity, pathFile, "video/*", view);
                                            break;
                                        case ".rtx":
                                        case ".csv":
                                        case ".txt":
                                        case ".vcs":
                                        case ".vcf":
                                        case ".css":
                                        case ".ics":
                                        case ".conf":
                                        case ".config":
                                        case ".java":
                                            helper_main.openFile(activity, pathFile, "text/*", view);
                                            break;
                                        case ".html":
                                            helper_main.openFile(activity, pathFile, "text/html", view);
                                            break;
                                        case ".apk":
                                            helper_main.openFile(activity, pathFile, "application/vnd.android.package-archive", view);
                                            break;
                                        case ".pdf":
                                            helper_main.openFile(activity, pathFile, "application/pdf", view);
                                            break;
                                        case ".doc":
                                            helper_main.openFile(activity, pathFile, "application/msword", view);
                                            break;
                                        case ".xls":
                                            helper_main.openFile(activity, pathFile, "application/vnd.ms-excel", view);
                                            break;
                                        case ".ppt":
                                            helper_main.openFile(activity, pathFile, "application/vnd.ms-powerpoint", view);
                                            break;
                                        case ".docx":
                                            helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", view);
                                            break;
                                        case ".pptx":
                                            helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.presentationml.presentation", view);
                                            break;
                                        case ".xlsx":
                                            helper_main.openFile(activity, pathFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", view);
                                            break;
                                        case ".odt":
                                            helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.text", view);
                                            break;
                                        case ".ods":
                                            helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.spreadsheet", view);
                                            break;
                                        case ".odp":
                                            helper_main.openFile(activity, pathFile, "application/vnd.oasis.opendocument.presentation", view);
                                            break;
                                        case ".zip":
                                            helper_main.openFile(activity, pathFile, "application/zip", view);
                                            break;
                                        case ".rar":
                                            helper_main.openFile(activity, pathFile, "application/x-rar-compressed", view);
                                            break;
                                        case ".epub":
                                            helper_main.openFile(activity, pathFile, "application/epub+zip", view);
                                            break;
                                        case ".cbz":
                                            helper_main.openFile(activity, pathFile, "application/x-cbz", view);
                                            break;
                                        case ".cbr":
                                            helper_main.openFile(activity, pathFile, "application/x-cbr", view);
                                            break;
                                        case ".fb2":
                                            helper_main.openFile(activity, pathFile, "application/x-fb2", view);
                                            break;
                                        case ".rtf":
                                            helper_main.openFile(activity, pathFile, "application/rtf", view);
                                            break;
                                        case ".opml":
                                            helper_main.openFile(activity, pathFile, "application/opml", view);
                                            break;

                                        default:
                                            Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                                            break;
                                    }

                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                    helper_main.openFilePicker(activity, view, dir);
                                }
                                if (options[item].equals(activity.getString(R.string.choose_menu_2))) {

                                    if (pathFile.exists()) {
                                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                                        sharingIntent.setType("image/png");
                                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                                        sharingIntent.putExtra(Intent.EXTRA_TEXT, fileName);
                                        Uri bmpUri = Uri.fromFile(pathFile);
                                        sharingIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                                        activity.startActivity(Intent.createChooser(sharingIntent, (activity.getString(R.string.app_share_file))));
                                    }
                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                    helper_main.openFilePicker(activity, view, dir);
                                }
                                if (options[item].equals(activity.getString(R.string.choose_menu_4))) {
                                    final AlertDialog.Builder dialog2 = new AlertDialog.Builder(activity);

                                    dialog2.setMessage(activity.getString(R.string.choose_delete));
                                    dialog2.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            pathFile.delete();
                                            new Handler().postDelayed(new Runnable() {
                                                public void run() {
                                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                                    helper_main.openFilePicker(activity, view, dir);
                                                }
                                            }, 500);
                                        }
                                    });
                                    dialog2.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.cancel();
                                        }
                                    });
                                    dialog2.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            // dialog dismiss without button press
                                            String dir = pathFile.getParentFile().getAbsolutePath();
                                            helper_main.openFilePicker(activity, view, dir);
                                        }
                                    });
                                    dialog2.show();
                                }
                                if (options[item].equals(activity.getString(R.string.choose_menu_3))) {

                                    final LinearLayout layout = new LinearLayout(activity);
                                    layout.setOrientation(LinearLayout.VERTICAL);
                                    layout.setGravity(Gravity.CENTER_HORIZONTAL);
                                    final EditText input = new EditText(activity);
                                    input.setSingleLine(true);
                                    input.setHint(activity.getString(R.string.choose_hint));
                                    input.setText(fileNameWE);
                                    layout.setPadding(30, 0, 50, 0);
                                    layout.addView(input);

                                    new Handler().postDelayed(new Runnable() {
                                        public void run() {
                                            helper_main.showKeyboard(activity,input);
                                        }
                                    }, 200);

                                    final AlertDialog.Builder dialog2 = new AlertDialog.Builder(activity);

                                    dialog2.setView(layout);
                                    dialog2.setMessage(activity.getString(R.string.choose_hint));
                                    dialog2.setPositiveButton(R.string.toast_yes, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            String inputTag = input.getText().toString().trim();

                                            File dir = pathFile.getParentFile();
                                            File to = new File(dir,inputTag + fileExtension);

                                            pathFile.renameTo(to);
                                            pathFile.delete();

                                            new Handler().postDelayed(new Runnable() {
                                                public void run() {
                                                    String dir = pathFile.getParentFile().getAbsolutePath();
                                                    helper_main.openFilePicker(activity, view, dir);
                                                }
                                            }, 500);
                                        }
                                    });
                                    dialog2.setNegativeButton(R.string.toast_cancel, new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.cancel();
                                        }
                                    });
                                    dialog2.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            // dialog dismiss without button press
                                            String dir = pathFile.getParentFile().getAbsolutePath();
                                            helper_main.openFilePicker(activity, view, dir);
                                        }
                                    });
                                    dialog2.show();
                                }
                            }
                        });
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                // dialog dismiss without button press
                                String dir = pathFile.getParentFile().getAbsolutePath();
                                helper_main.openFilePicker(activity, view, dir);
                            }
                        });
                        dialog.show();
                    }
                })
                .build()
                .show();
    }

    private static void openFile(Activity activity, File file, String string, View view) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(contentUri,string);

        } else {
            intent.setDataAndType(Uri.fromFile(file),string);
        }

        try {
            activity.startActivity (intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(view, R.string.toast_install_app, Snackbar.LENGTH_LONG).show();
        }
    }
}