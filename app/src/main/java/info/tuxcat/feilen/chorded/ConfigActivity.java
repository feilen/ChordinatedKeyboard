package info.tuxcat.feilen.chorded;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class ConfigActivity extends WearableActivity {
    SettingsContainer settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Enables Always-on
        setAmbientEnabled();

        settings = new SettingsContainer();
        settings.loadSettings(getApplicationContext());

        final Switch mSymSwitch = (Switch) findViewById(R.id.sym_switch);
        mSymSwitch.setChecked(settings.symbols_in_tree);
        mSymSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.symbols_in_tree = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        Switch mSpaceSwitch = (Switch) findViewById(R.id.space_switch);
        mSpaceSwitch.setChecked(settings.space_in_tree);
        mSpaceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settings.space_in_tree = ((Switch) view).isChecked();
                settings.saveSettings(getApplicationContext());
            }
        });

        RadioGroup layoutgroup = (RadioGroup) findViewById(R.id.layout_radiogroup);
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
                        case R.id.radio_2x2nostretch:
                            settings.keyboard_type = Chorded.KeyboardType.TWOXTWOFINGERNOSTRETCH;
                            break;
                    }
                    settings.saveSettings(getApplicationContext());
                }
            }
        );
    }
}
