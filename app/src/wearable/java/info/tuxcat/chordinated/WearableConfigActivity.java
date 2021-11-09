package info.tuxcat.chordinated;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class WearableConfigActivity extends WearableActivity {
    private SettingsContainer settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Enables Always-on
        setAmbientEnabled();

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
            public void onClick(@NonNull View view) {
                settings.symbols_in_tree = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        final Switch autoshiftswitch = findViewById(R.id.auto_shift_switch);
        autoshiftswitch.setChecked(settings.auto_shift);
        autoshiftswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                settings.auto_shift = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch mSpaceSwitch = findViewById(R.id.space_switch);
        mSpaceSwitch.setChecked(settings.space_in_tree);
        mSpaceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                settings.space_in_tree = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch mLeftSwitch = findViewById(R.id.left_handed_switch);
        mLeftSwitch.setChecked(settings.left_handed_mode);
        mLeftSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                settings.left_handed_mode = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch chordpressswitch = findViewById(R.id.vibrate_chord_press_switch);
        chordpressswitch.setChecked(settings.vibration_type.contains(Chorded.VibrationType.CHORD_SUBTREE));
        chordpressswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                if(((Switch) view).isChecked())
                {
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
            public void onClick(@NonNull View view) {
                if(((Switch) view).isChecked())
                {
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
            public void onClick(@NonNull View view) {
                if(((Switch) view).isChecked())
                {
                    settings.vibration_type.add(Chorded.VibrationType.SWIPE);
                } else {
                    settings.vibration_type.remove(Chorded.VibrationType.SWIPE);
                }
                settings.saveSettings(getApplicationContext());
            }
        });

        RadioGroup layoutgroup = findViewById(R.id.layout_radiogroup);
        if(settings.can_chord) {
            for (int i = 0; i < layoutgroup.getChildCount(); i++) {
                RadioButton button = (RadioButton) layoutgroup.getChildAt(i);
                switch (button.getId()) {
                    case R.id.radio_2finger:
                        button.setChecked(settings.keyboard_type == Chorded.KeyboardType.TWOFINGER);
                        break;
                    case R.id.radio_3finger:
                        button.setChecked(settings.keyboard_type == Chorded.KeyboardType.THREEFINGER);
                        break;
                    case R.id.radio_2x2:
                        button.setChecked(settings.keyboard_type == Chorded.KeyboardType.TWOXTWOFINGER);
                        break;
                    case R.id.radio_2x2halfstretch:
                        button.setChecked(settings.keyboard_type == Chorded.KeyboardType.TWOXTWOFINGERHALFSTRETCH);
                        break;
                    case R.id.radio_2x2nostretch:
                        button.setChecked(settings.keyboard_type == Chorded.KeyboardType.TWOXTWOFINGERNOSTRETCH);
                        break;
                    case R.id.radio_2x2nochord:
                        button.setChecked(settings.keyboard_type == Chorded.KeyboardType.TWOXTWOFINGERNOCHORD);
                        break;
                }
            }
        } else {
            // Don't let them configure the layout if it's unchorded.
            ((LinearLayout)layoutgroup.getParent()).removeView(layoutgroup);
        }
        layoutgroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int id) {
                        switch(id)
                        {
                            case R.id.radio_2finger:
                                settings.keyboard_type = Chorded.KeyboardType.TWOFINGER;
                                break;
                            case R.id.radio_3finger:
                                settings.keyboard_type = Chorded.KeyboardType.THREEFINGER;
                                break;
                            case R.id.radio_2x2:
                                settings.keyboard_type = Chorded.KeyboardType.TWOXTWOFINGER;
                                break;
                            case R.id.radio_2x2halfstretch:
                                settings.keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERHALFSTRETCH;
                                break;
                            case R.id.radio_2x2nostretch:
                                settings.keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERNOSTRETCH;
                                break;
                            case R.id.radio_2x2nochord:
                                settings.keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERNOCHORD;
                                break;
                        }
                        settings.saveSettings(getApplicationContext());
                    }
                }
        );

        RadioGroup comfortgroup = findViewById(R.id.comfort_radiogroup);
        for(int i = 0; i < comfortgroup.getChildCount(); i++)
        {
            RadioButton button = (RadioButton) comfortgroup.getChildAt(i);
            switch(button.getId())
            {
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
                        switch(id)
                        {
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
    }
}
