package info.tuxcat.feilen.chorded;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("SpellCheckingInspection")
class SettingsContainer {
    @SuppressWarnings("FieldCanBeLocal")
    private final String CURRENT_VERSION = "1.0";

    @Nullable
    private String version;
    boolean symbols_in_tree;
    boolean space_in_tree;
    final boolean enable_vibrate = true;
    final boolean vibrate_on_swipe = true;
    final float comfort_angle = -20.0f;
    Chorded.KeyboardType keyboard_type;

    public void saveSettings(@NonNull Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(R.string.preference_key_version), version);
        editor.putBoolean(context.getString(R.string.preference_key_sym_in_tree), symbols_in_tree);
        editor.putBoolean(context.getString(R.string.preference_key_space_in_tree), space_in_tree);
        switch(keyboard_type)
        {
            case TWOFINGER:
                editor.putString(context.getString(R.string.preference_key_layout), "TWOFINGER");
                break;
            case THREEFINGER:
                editor.putString(context.getString(R.string.preference_key_layout), "THREEFINGER");
                break;
            case TWOXTWOFINGER:
                editor.putString(context.getString(R.string.preference_key_layout), "TWOXTWOFINGER");
                break;
            case TWOXTWOFINGERNOSTRETCH:
                editor.putString(context.getString(R.string.preference_key_layout), "TWOXTWOFINGERNOSTRETCH");
                break;
        }
        editor.apply();
    }

    public void loadSettings(@NonNull Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        version = prefs.getString(context.getResources().getString(R.string.preference_key_version), "undefined");
        switch(version)
        {
            default:
            case "undefined":
                // Just set defaults.
                version = CURRENT_VERSION;
                symbols_in_tree = false;
                space_in_tree = false;
                keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERNOSTRETCH;
                break;
            case "1.0":
                symbols_in_tree = prefs.getBoolean(context.getResources().getString(R.string.preference_key_sym_in_tree), false);
                space_in_tree = prefs.getBoolean(context.getResources().getString(R.string.preference_key_space_in_tree), false);
                switch(prefs.getString(context.getResources().getString(R.string.preference_key_layout), "undefined"))
                {
                    case "TWOFINGER":
                        keyboard_type = Chorded.KeyboardType.TWOFINGER;
                        break;
                    case "THREEFINGER":
                        keyboard_type = Chorded.KeyboardType.THREEFINGER;
                        break;
                    case "TWOXTWOFINGER":
                        keyboard_type = Chorded.KeyboardType.TWOXTWOFINGER;
                        break;
                    case "undefined":
                    case "TWOXTWOFINGERNOSTRETCH":
                        keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERNOSTRETCH;
                        break;
                }
                break;
        }
        }
}
