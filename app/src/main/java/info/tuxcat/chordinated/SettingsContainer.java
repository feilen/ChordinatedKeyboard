// Settings and preferences handler.
// Copyright (C) 2021 Chelsea Jaggi

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.

// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
// more details.

// You should have received a copy of the GNU General Public License along with
// this program. If not, see https://www.gnu.org/licenses/.

package info.tuxcat.chordinated;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
class SettingsContainer {
    @SuppressWarnings("FieldCanBeLocal")
    private final String CURRENT_VERSION = "1.0";

    private String version = "undefined";
    boolean symbols_in_tree;
    boolean space_in_tree;
    boolean auto_shift;
    boolean left_handed_mode;
    boolean can_chord;
    float comfort_angle;
    EnumSet<Chorded.VibrationType> vibration_type = EnumSet.noneOf(Chorded.VibrationType.class);
    Chorded.KeyboardType keyboard_type;

    public boolean equals(SettingsContainer o)
    {
        return version.equals(o.version)
                && symbols_in_tree == o.symbols_in_tree
                && space_in_tree == o.space_in_tree
                && auto_shift == o.auto_shift
                && left_handed_mode == o.left_handed_mode
                && comfort_angle == o.comfort_angle
                && vibration_type.equals(o.vibration_type)
                && keyboard_type.equals(o.keyboard_type)
                && can_chord == o.can_chord;
    }

    public void saveSettings(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(context.getString(R.string.preference_key_version), version);
        editor.putBoolean(context.getString(R.string.preference_key_sym_in_tree), symbols_in_tree);
        editor.putBoolean(context.getString(R.string.preference_key_space_in_tree), space_in_tree);
        editor.putBoolean(context.getString(R.string.preference_key_auto_shift), auto_shift);
        editor.putBoolean(context.getString(R.string.preference_key_left_handed_mode), left_handed_mode);
        editor.putBoolean(context.getString(R.string.preference_key_can_chord), can_chord);
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
            case SETUP_CHECKING_CHORDS:
            case SETUP_CHORD_CONFIRMATION_DIALOG:
            case SETUP_WELCOME_SCREEN:
            case SETUP_SETTINGS_CONFIRMATION_DIALOG:
                editor.putString(context.getString(R.string.preference_key_layout), "SETUP");
                break;
            default:
                editor.putString(context.getString(R.string.preference_key_layout), keyboard_type.name());
        }
        editor.apply();
    }

    public void resetSettings()
    {
        version = CURRENT_VERSION;
        symbols_in_tree = false;
        space_in_tree = false;
        left_handed_mode = false;
        auto_shift = true;
        can_chord = false;
        keyboard_type = Chorded.KeyboardType.SETUP_WELCOME_SCREEN;
        vibration_type = EnumSet.allOf(Chorded.VibrationType.class);
        comfort_angle = -20.0f;
    }

    public void loadSettings(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        version = prefs.getString(context.getResources().getString(R.string.preference_key_version), "undefined");
        switch(version)
        {
            default:
            case "undefined":
                // Just set defaults.
                resetSettings();
                break;
            case "1.0":
                symbols_in_tree = prefs.getBoolean(context.getResources().getString(R.string.preference_key_sym_in_tree), false);
                space_in_tree = prefs.getBoolean(context.getResources().getString(R.string.preference_key_space_in_tree), false);
                left_handed_mode = prefs.getBoolean(context.getResources().getString(R.string.preference_key_left_handed_mode), false);
                can_chord = prefs.getBoolean(context.getString(R.string.preference_key_can_chord), false);
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
                    case "undefined":
                    case "SETUP":
                        keyboard_type = Chorded.KeyboardType.SETUP_WELCOME_SCREEN;
                        break;
                    default:
                        keyboard_type = Chorded.KeyboardType.valueOf(prefs.getString(context.getResources().getString(R.string.preference_key_layout), "SETUP_WELCOME_SCREEN"));
                }
                break;
        }
        }
}
