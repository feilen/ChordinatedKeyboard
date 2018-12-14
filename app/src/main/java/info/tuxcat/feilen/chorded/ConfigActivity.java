package info.tuxcat.feilen.chorded;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class ConfigActivity extends WearableActivity {
    private SettingsContainer settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Enables Always-on
        setAmbientEnabled();

        settings = new SettingsContainer();
        settings.loadSettings(getApplicationContext());

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

        RadioGroup layoutgroup = findViewById(R.id.layout_radiogroup);
        for(int i = 0; i < layoutgroup.getChildCount(); i++)
        {
            RadioButton button = (RadioButton) layoutgroup.getChildAt(i);
            switch(button.getId())
            {
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
            }
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