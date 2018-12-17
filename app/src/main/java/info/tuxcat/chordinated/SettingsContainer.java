package info.tuxcat.chordinated;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
class SettingsContainer {
    @SuppressWarnings("FieldCanBeLocal")
    private final String CURRENT_VERSION = "1.0";

    @NonNull
    private String version = "undefined";
    boolean symbols_in_tree;
    boolean space_in_tree;
    boolean auto_shift;
    boolean left_handed_mode;
    float comfort_angle;
    EnumSet<Chorded.VibrationType> vibration_type = EnumSet.noneOf(Chorded.VibrationType.class);
    Chorded.KeyboardType keyboard_type;

    public boolean equals(@NonNull SettingsContainer o)
    {
        return version.equals(o.version)
                && symbols_in_tree == o.symbols_in_tree
                && space_in_tree == o.space_in_tree
                && auto_shift == o.auto_shift
                && left_handed_mode == o.left_handed_mode
                && comfort_angle == o.comfort_angle
                && vibration_type.equals(o.vibration_type)
                && keyboard_type.equals(o.keyboard_type);
    }

    public void saveSettings(@NonNull Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(R.string.preference_key_version), version);
        editor.putBoolean(context.getString(R.string.preference_key_sym_in_tree), symbols_in_tree);
        editor.putBoolean(context.getString(R.string.preference_key_space_in_tree), space_in_tree);
        editor.putBoolean(context.getString(R.string.preference_key_auto_shift), auto_shift);
        editor.putBoolean(context.getString(R.string.preference_key_left_handed_mode), left_handed_mode);
        editor.putFloat(context.getString(R.string.preference_key_comfort_angle), comfort_angle);

        // Store vibration
        Set<String> vtypes_str = new HashSet<>();
        for(Chorded.VibrationType vtype: vibration_type)
        {
            vtypes_str.add(vtype.name());
        }
        editor.putStringSet(context.getString(R.string.preference_key_vibration_type), vtypes_str);

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
            case TWOXTWOFINGERHALFSTRETCH:
                editor.putString(context.getString(R.string.preference_key_layout), "TWOXTWOFINGERHALFSTRETCH");
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
                left_handed_mode = false;
                auto_shift = true;
                keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERHALFSTRETCH;
                vibration_type = EnumSet.allOf(Chorded.VibrationType.class);
                comfort_angle = -20.0f;
                break;
            case "1.0":
                symbols_in_tree = prefs.getBoolean(context.getResources().getString(R.string.preference_key_sym_in_tree), false);
                space_in_tree = prefs.getBoolean(context.getResources().getString(R.string.preference_key_space_in_tree), false);
                left_handed_mode = prefs.getBoolean(context.getResources().getString(R.string.preference_key_left_handed_mode), false);
                auto_shift = prefs.getBoolean(context.getString(R.string.preference_key_auto_shift), true);
                comfort_angle = prefs.getFloat(context.getString(R.string.preference_key_comfort_angle), -20.0f);
                Set<String> vibration_set = prefs.getStringSet(context.getString(R.string.preference_key_vibration_type), new HashSet<>(Collections.singletonList("undefined")));
                if(vibration_set.contains("undefined")){
                    vibration_type = EnumSet.allOf(Chorded.VibrationType.class);
                } else {
                    for(String s: vibration_set)
                    {
                        vibration_type.add(Chorded.VibrationType.valueOf(s));
                    }
                }
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
                    case "TWOXTWOFINGERNOSTRETCH":
                        keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERNOSTRETCH;
                        break;
                    case "undefined":
                    case "TWOXTWOFINGERHALFSTRETCH":
                        keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERHALFSTRETCH;
                        break;
                }
                break;
        }
        }
}
