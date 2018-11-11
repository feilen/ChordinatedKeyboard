package info.tuxcat.feilen.chorded;

/**
 * Created by feilen on 10/13/18.
 */

import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.os.Vibrator;

import java.util.ArrayList;

import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.TWOFINGER;
import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.THREEFINGER;

public class Chorded extends InputMethodService {

    private View kv;
    private InputConnection ic;
    Vibrator vibrator;

    private HuffmanTree tree;
    private HuffmanNode curNode;
    enum KeyboardType {TWOFINGER, THREEFINGER};
    boolean isChorded = true;
    int buttonpress_current;
    int buttonpress_chord;
    KeyboardType kType = TWOFINGER;

    int[] keylookup;

    private boolean caps = false;
    private boolean remove_views = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_STEM_1:
                ic.deleteSurroundingText(1, 0);
                updateText();
                return true;
            case KeyEvent.KEYCODE_STEM_2:
                caps = !caps;
                relabelKeys();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    View.OnTouchListener onPress = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View button, MotionEvent eventtype)
        {
            switch(eventtype.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    switch(button.getId())
                    {
                        case R.id.chord_one:
                            buttonpress_current = buttonpress_current | 0b001;
                            buttonpress_chord = buttonpress_chord | 0b001;
                            break;
                        case R.id.chord_two:
                            buttonpress_current = buttonpress_current | 0b010;
                            buttonpress_chord = buttonpress_chord | 0b010;
                            break;
                        case R.id.chord_three:
                            buttonpress_current = buttonpress_current | 0b100;
                            buttonpress_chord = buttonpress_chord | 0b100;
                            break;
                        case R.id.button_backspace:
                            ic.deleteSurroundingText(1, 0);
                            break;
                        case R.id.button_shift:
                            caps = !caps;
                            relabelKeys();
                            break;
                        case R.id.button_sym:
                            ic.commitText(".", 1);
                            break;
                        case R.id.button_return:
                            sendDefaultEditorAction(true);
                            break;
                        case R.id.button_space:
                            ic.commitText(" ",  1);
                            break;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if(!isChorded) break;
                    switch(button.getId())
                    {
                        case R.id.chord_one:
                        case R.id.chord_two:
                        case R.id.chord_three:
                            switch(button.getId())
                            {
                                case R.id.chord_one:
                                    buttonpress_current = buttonpress_current & ~0b001;
                                    break;
                                case R.id.chord_two:
                                    buttonpress_current = buttonpress_current & ~0b010;
                                    break;
                                case R.id.chord_three:
                                    buttonpress_current = buttonpress_current & ~0b100;
                                    break;
                            }
                            // Only commit when all are released
                            if(buttonpress_current != 0)
                            {
                                break;
                            }
                            if(curNode.children.size() >= keylookup[buttonpress_chord])
                            {
                                curNode = curNode.children.get(keylookup[buttonpress_chord]);
                                if(curNode.children.size() == 0)
                                {
                                    // Reached the end. Commit the current letter.
                                    vibrator.vibrate(15);
                                    char inputchar = curNode.thechar;
                                    if(caps && Character.isAlphabetic(inputchar))
                                    {
                                        inputchar = Character.toUpperCase(inputchar);
                                        caps = !caps;
                                    }
                                    ic.commitText(String.valueOf(inputchar), 1);
                                    curNode = tree.root;
                                } else {
                                    // Presumably indicate that a button was pressed
                                }
                                buttonpress_chord = 0;
                            } else {
                                // Invalid coding. Reset to root.
                                buttonpress_chord = 0;
                                curNode = tree.root;
                            }
                            break;
                        default:
                            buttonpress_chord = 0;
                            curNode = tree.root;
                    }
                    relabelKeys();
                    return true;
            }
            return false;
        }
    };

    @Override
    public View onCreateInputView() {
        ic = getCurrentInputConnection();
        int hardware_buttons_count = WearableButtons.getButtonCount(getBaseContext());
        switch(kType) {
            case TWOFINGER:
                kv = getLayoutInflater().inflate(R.layout.twochord, null);
                if(remove_views && hardware_buttons_count > 1)
                {
                    View bksp = kv.findViewById(R.id.button_backspace);
                    ((ViewGroup) bksp.getParent()).removeView(bksp);
                }

                if(remove_views && hardware_buttons_count > 2)
                {
                    View shift = kv.findViewById(R.id.button_shift);
                    ((ViewGroup) shift.getParent()).removeView(shift);
                }

                if (isChorded)
                {
                    tree = new HuffmanTree(3);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1, 2};
                } else {
                    tree = new HuffmanTree(2);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1};
                }
                break;
            case THREEFINGER:
                kv = getLayoutInflater().inflate(R.layout.threechord, null);
                if(isChorded)
                {
                    tree = new HuffmanTree(7);
                    //                          001 010 011 100 101 110 111
                    keylookup = new int[]{ -1,  0,  1,  3,  2,  5,  4,  6};
                } else {
                    tree = new HuffmanTree(2);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1, 2};
                }
                final Button chord_three = kv.findViewById(R.id.chord_three);
                chord_three.setOnTouchListener(onPress);
                break;
        }
        final Button button_space = kv.findViewById(R.id.button_space);
        button_space.setOnTouchListener(onPress);
        final Button button_sym = kv.findViewById(R.id.button_sym);
        button_sym.setOnTouchListener(onPress);
        final Button button_return = kv.findViewById(R.id.button_return);
        button_return.setOnTouchListener(onPress);
        final Button chord_one = kv.findViewById(R.id.chord_one);
        chord_one.setOnTouchListener(onPress);
        final Button chord_two = kv.findViewById(R.id.chord_two);
        chord_two.setOnTouchListener(onPress);
        if(!remove_views || hardware_buttons_count <= 1) {
            final Button button_backspace = kv.findViewById(R.id.button_backspace);
            button_backspace.setOnTouchListener(onPress);
        }
        if(!remove_views || hardware_buttons_count <= 2) {
            final Button button_shift = kv.findViewById(R.id.button_shift);
            button_shift.setOnTouchListener(onPress);
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        buttonpress_chord = 0;
        buttonpress_current = 0;

        ArrayList<HuffmanNode> inp = new ArrayList<HuffmanNode>();
        inp.add(new HuffmanNode('e', 12.702));
        inp.add(new HuffmanNode('t', 9.056));
        inp.add(new HuffmanNode('a', 8.167));
        inp.add(new HuffmanNode('o', 7.507));
        inp.add(new HuffmanNode('i', 6.966));
        inp.add(new HuffmanNode('n', 6.749));
        inp.add(new HuffmanNode('s', 6.327));
        inp.add(new HuffmanNode('h', 6.094));
        inp.add(new HuffmanNode('r', 5.987));
        inp.add(new HuffmanNode('d', 4.253));
        inp.add(new HuffmanNode('l', 4.025));
        inp.add(new HuffmanNode('c', 2.782));
        inp.add(new HuffmanNode('u', 2.758));
        inp.add(new HuffmanNode('m', 2.406));
        inp.add(new HuffmanNode('w', 2.360));
        inp.add(new HuffmanNode('f', 2.228));
        inp.add(new HuffmanNode('g', 2.015));
        inp.add(new HuffmanNode('y', 1.974));
        inp.add(new HuffmanNode('p', 1.929));
        inp.add(new HuffmanNode('b', 1.492));
        inp.add(new HuffmanNode('v', 0.978));
        inp.add(new HuffmanNode('k', 0.772));
        inp.add(new HuffmanNode('j', 0.153));
        inp.add(new HuffmanNode('x', 0.150));
        inp.add(new HuffmanNode('q', 0.095));
        inp.add(new HuffmanNode('z', 0.074));
        tree.CreateEncoding(inp);
        curNode = tree.root;
        relabelKeys();
        return kv;
    }

    private void relabelKeys() {
        Button chord_one = (Button) kv.findViewById(R.id.chord_one);
        Button chord_two = (Button) kv.findViewById(R.id.chord_two);
        String chord_one_label = "";
        String chord_two_label = "";

        if(!isChorded)
        {
            chord_one_label = (curNode.children.size() >= keylookup[1])? curNode.children.get(keylookup[1]).allchars : "";
            if(caps) chord_one_label = chord_one_label.toUpperCase();
            chord_one.setText(chord_one_label);
            chord_one.invalidate();

            if(caps) chord_two_label = chord_two_label.toUpperCase();
            chord_two_label = (curNode.children.size() >= keylookup[2])? curNode.children.get(keylookup[2]).allchars : "";
            chord_two.setText(chord_two_label);
            chord_two.invalidate();

            if(kType == THREEFINGER)
            {
                Button chord_three = (Button) kv.findViewById(R.id.chord_three);
                String chord_three_label = "";
                chord_three_label = (curNode.children.size() >= keylookup[3])? curNode.children.get(keylookup[3]).allchars : "";
                if(caps) chord_three_label = chord_three_label.toUpperCase();
                chord_three.setText(chord_three_label);
                chord_three.invalidate();
            }
            return;
        }

        switch(kType) {
            case TWOFINGER:


                chord_one_label += (curNode.children.size() >= keylookup[1])? curNode.children.get(keylookup[1]).allchars : "";
                chord_one_label += (curNode.children.size() >= keylookup[3])? "\n" + curNode.children.get(keylookup[3]).allchars : "\n";
                if(caps) chord_one_label = chord_one_label.toUpperCase();
                chord_one.setText(chord_one_label);
                chord_one.invalidate();

                chord_two_label += (curNode.children.size() >= keylookup[2])? curNode.children.get(keylookup[2]).allchars : "";
                chord_two_label += (curNode.children.size() >= keylookup[3])? "\n" + curNode.children.get(keylookup[3]).allchars : "\n";
                if(caps) chord_two_label = chord_two_label.toUpperCase();
                chord_two.setText(chord_two_label);
                chord_two.invalidate();

                break;
            case THREEFINGER:
                // bits are flipped horizontally
        /*
        100
        110
        101
        111
         */
                chord_one_label += (curNode.children.size() >= keylookup[1])? curNode.children.get(keylookup[1]).allchars : "";
                chord_one_label += (curNode.children.size() >= keylookup[3])? "\n" + curNode.children.get(keylookup[3]).allchars : "\n";
                chord_one_label += (curNode.children.size() >= keylookup[5])? "\n" + curNode.children.get(keylookup[5]).allchars : "\n";
                chord_one_label += (curNode.children.size() >= keylookup[7])? "\n" + curNode.children.get(keylookup[7]).allchars : "\n";
                if(caps) chord_one_label = chord_one_label.toUpperCase();
                chord_one.setText(chord_one_label);
                chord_one.invalidate();

        /*
        010
        110
        011
        111
         */
                chord_two_label += (curNode.children.size() >= keylookup[2])? curNode.children.get(keylookup[2]).allchars : "";
                chord_two_label += (curNode.children.size() >= keylookup[3])? "\n" + curNode.children.get(keylookup[3]).allchars : "\n";
                chord_two_label += (curNode.children.size() >= keylookup[6])? "\n" + curNode.children.get(keylookup[6]).allchars : "\n";
                chord_two_label += (curNode.children.size() >= keylookup[7])? "\n" + curNode.children.get(keylookup[7]).allchars : "\n";
                if(caps) chord_two_label = chord_two_label.toUpperCase();
                chord_two.setText(chord_two_label);
                chord_two.invalidate();

        /*
        001
        011
        101
        111
         */
                Button chord_three = (Button) kv.findViewById(R.id.chord_three);
                String chord_three_label = "";
                chord_three_label += (curNode.children.size() >= keylookup[4])? curNode.children.get(keylookup[4]).allchars : "";
                chord_three_label += (curNode.children.size() >= keylookup[6])? "\n" + curNode.children.get(keylookup[6]).allchars : "\n";
                chord_three_label += (curNode.children.size() >= keylookup[5])? "\n" + curNode.children.get(keylookup[5]).allchars : "\n";
                chord_three_label += (curNode.children.size() >= keylookup[7])? "\n" + curNode.children.get(keylookup[7]).allchars : "\n";
                if(caps) chord_three_label = chord_three_label.toUpperCase();
                chord_three.setText(chord_three_label);
                chord_three.invalidate();
                break;
        }
        updateText();
    }

    private void updateText()
    {
        ExtractEditText editText = kv.findViewById(R.id.inputExtractEditText);
        ExtractedTextRequest etr=new ExtractedTextRequest();
        etr.token=0;
        editText.setExtractedText(ic.getExtractedText(etr, 0));
    }
}