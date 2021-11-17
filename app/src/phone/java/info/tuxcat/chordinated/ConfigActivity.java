// Main configuration activity, what gets launched when you select the app.
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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class ConfigActivity extends Activity {
    private SettingsContainer settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        settings = new SettingsContainer();
        settings.loadSettings(getApplicationContext());

        final Button button_reset_settings = findViewById(R.id.button_reset_settings);
        button_reset_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.resetSettings();
                settings.saveSettings(getApplicationContext());
                finish();
            }
        });

        final Switch mSymSwitch = findViewById(R.id.sym_switch);
        mSymSwitch.setChecked(settings.symbols_in_tree);
        mSymSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.symbols_in_tree = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        final Switch autoshiftswitch = findViewById(R.id.auto_shift_switch);
        autoshiftswitch.setChecked(settings.auto_shift);
        autoshiftswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.auto_shift = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch mSpaceSwitch = findViewById(R.id.space_switch);
        mSpaceSwitch.setChecked(settings.space_in_tree);
        mSpaceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.space_in_tree = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch mLeftSwitch = findViewById(R.id.left_handed_switch);
        mLeftSwitch.setChecked(settings.left_handed_mode);
        mLeftSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.left_handed_mode = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch chordpressswitch = findViewById(R.id.vibrate_chord_press_switch);
        chordpressswitch.setChecked(settings.vibration_type.contains(Chorded.VibrationType.CHORD_SUBTREE));
        chordpressswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((Switch) view).isChecked()) {
                    settings.vibration_type.add(Chorded.VibrationType.CHORD_SUBTREE);
                } else {
                    settings.vibration_type.remove(Chorded.VibrationType.CHORD_SUBTREE);
                }
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch chordsubmitswitch = findViewById(R.id.vibrate_letter_submitted_switch);
        chordsubmitswitch.setChecked(settings.vibration_type.contains(Chorded.VibrationType.CHORD_SUBMIT));
        chordsubmitswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((Switch) view).isChecked()) {
                    settings.vibration_type.add(Chorded.VibrationType.CHORD_SUBMIT);
                } else {
                    settings.vibration_type.remove(Chorded.VibrationType.CHORD_SUBMIT);
                }
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch swipeswitch = findViewById(R.id.vibrate_on_swipe_switch);
        swipeswitch.setChecked(settings.vibration_type.contains(Chorded.VibrationType.SWIPE));
        swipeswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((Switch) view).isChecked()) {
                    settings.vibration_type.add(Chorded.VibrationType.SWIPE);
                } else {
                    settings.vibration_type.remove(Chorded.VibrationType.SWIPE);
                }
                settings.saveSettings(getApplicationContext());
            }
        });

        RadioGroup layoutgroup = findViewById(R.id.layout_radiogroup);
        if (true) { //settings.can_chord) {
            for (Chorded.KeyboardType kbt: Chorded.KeyboardType.values()) {
                if(kbt.toString().contains("SETUP")) continue;
                RadioButton button = new RadioButton(this);
                // button.setId(n); // Set to something usable so we can lookup later
                button.setText(kbt.toString());
                button.setChecked(settings.keyboard_type == kbt);
                layoutgroup.addView(button);
            }
        } else {
            // Don't let them configure the layout if it's unchorded.
            ((LinearLayout) layoutgroup.getParent()).removeView(findViewById(R.id.layout_header));
            ((LinearLayout) layoutgroup.getParent()).removeView(layoutgroup);
        }
        layoutgroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int id) {
                        for(int i = 0; i < radioGroup.getChildCount(); i++)
                        {
                            View v = radioGroup.getChildAt(i);
                            if(v.getId() == id) {
                                settings.keyboard_type = Chorded.KeyboardType.valueOf((String) ((RadioButton)v).getText());
                            }
                        }
                        settings.saveSettings(getApplicationContext());
                    }
                }
        );

        RadioGroup comfortgroup = findViewById(R.id.comfort_radiogroup);
        if (BuildConfig.FLAVOR == "wearable") {
            for (int i = 0; i < comfortgroup.getChildCount(); i++) {
                RadioButton button = (RadioButton) comfortgroup.getChildAt(i);
                switch (button.getId()) {
                    case R.id.radio_comfort0:
                        button.setChecked(settings.comfort_angle == 0.0f);
                        break;
                    case R.id.radio_comfort5:
                        button.setChecked(settings.comfort_angle == -5.0f);
                        break;
                    case R.id.radio_comfort10:
                        button.setChecked(settings.comfort_angle == -10.0f);
                        break;
                    case R.id.radio_comfort15:
                        button.setChecked(settings.comfort_angle == -15.0f);
                        break;
                    case R.id.radio_comfort20:
                        button.setChecked(settings.comfort_angle == -20.0f);
                        break;
                }
            }
            comfortgroup.setOnCheckedChangeListener(
                    new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(RadioGroup radioGroup, int id) {
                            switch (id) {
                                case R.id.radio_comfort0:
                                    settings.comfort_angle = 0.0f;
                                    break;
                                case R.id.radio_comfort5:
                                    settings.comfort_angle = -5.0f;
                                    break;
                                case R.id.radio_comfort10:
                                    settings.comfort_angle = -10.0f;
                                    break;
                                case R.id.radio_comfort15:
                                    settings.comfort_angle = -15.0f;
                                    break;
                                case R.id.radio_comfort20:
                                    settings.comfort_angle = -20.0f;
                                    break;
                            }
                            settings.saveSettings(getApplicationContext());
                        }
                    }
            );
        } else {
            ((LinearLayout) comfortgroup.getParent()).removeView(findViewById(R.id.comfort_header));
            ((LinearLayout) comfortgroup.getParent()).removeView(comfortgroup);
        }
    }
}
