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
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.os.Vibrator;

import java.util.ArrayList;

import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.TWOFINGER;
import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.THREEFINGER;
import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.TWOXTWOFINGER;

public class Chorded extends InputMethodService {

    private View kv;
    private InputConnection ic;
    private Vibrator vibrator;

    private HuffmanTree tree;
    private HuffmanTree sym_tree;
    private HuffmanNode curNode;
    private int buttonpress_current;
    private int buttonpress_chord;
    private int[] keylookup;
    enum KeyboardType {
        TWOFINGER,
        THREEFINGER,
        TWOXTWOFINGER
    }

    private final boolean isChorded = true;
    private final KeyboardType kType = TWOXTWOFINGER;
    private final boolean remove_views = true;
    private final boolean rotate_view = true;
    private boolean caps = true;
    private boolean sym = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_STEM_1:
                if(curNode != tree.root && curNode != sym_tree.root) {
                    resetRoot();
                    buttonpress_chord = 0;
                    relabelKeys();
                } else {
                    ic.deleteSurroundingText(1, 0);
                    updateText();
                }
                return true;
            case KeyEvent.KEYCODE_STEM_2:
                caps = !caps;
                relabelKeys();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return true;
    }

    private void resetRoot() {
        curNode = sym ? sym_tree.root : tree.root;
    }

    private final View.OnTouchListener onPress = new View.OnTouchListener()
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
                            buttonpress_current = buttonpress_current | 0b0001;
                            buttonpress_chord = buttonpress_chord | 0b0001;
                            break;
                        case R.id.chord_two:
                            buttonpress_current = buttonpress_current | 0b0010;
                            buttonpress_chord = buttonpress_chord | 0b0010;
                            break;
                        case R.id.chord_three:
                            buttonpress_current = buttonpress_current | 0b0100;
                            buttonpress_chord = buttonpress_chord | 0b0100;
                            break;
                        case R.id.chord_four:
                            buttonpress_current = buttonpress_current | 0b1000;
                            buttonpress_chord = buttonpress_chord | 0b1000;
                            break;
                        case R.id.button_backspace:
                            if(curNode == tree.root || curNode == sym_tree.root)
                            {
                                ic.deleteSurroundingText(1, 0);
                            }
                            break;
                        case R.id.button_shift:
                            caps = !caps;
                            relabelKeys();
                            break;
                        case R.id.button_sym:
                            sym = !sym;
                            resetRoot();
                            relabelKeys();
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
                        case R.id.chord_four:
                            switch(button.getId())
                            {
                                case R.id.chord_one:
                                    buttonpress_current = buttonpress_current & ~0b0001;
                                    break;
                                case R.id.chord_two:
                                    buttonpress_current = buttonpress_current & ~0b0010;
                                    break;
                                case R.id.chord_three:
                                    buttonpress_current = buttonpress_current & ~0b0100;
                                    break;
                                case R.id.chord_four:
                                    buttonpress_current = buttonpress_current & ~0b1000;
                            }
                            // Only commit when all are released
                            if(buttonpress_current != 0)
                            {
                                break;
                            }
                            // Invalid chord. Do nothing.
                            if(keylookup[buttonpress_chord] == -1) return true;
                            if(curNode.children.size() >= keylookup[buttonpress_chord])
                            {
                                curNode = curNode.children.get(keylookup[buttonpress_chord]);
                                if(curNode.children.size() == 0)
                                {
                                    // Reached the end. Commit the current letter.
                                    vibrator.vibrate(25);
                                    String inputchar = curNode.resultString;
                                    if(caps && inputchar.length() == 1 && Character.isAlphabetic(inputchar.charAt(0)))
                                    {
                                        inputchar = inputchar.toUpperCase();
                                        caps = !caps;
                                    }
                                    ic.commitText(String.valueOf(inputchar), 1);
                                    resetRoot();
                                } else {
                                    vibrator.vibrate(15);
                                }
                                buttonpress_chord = 0;
                            } else {
                                // Invalid coding. Reset to root.
                                buttonpress_chord = 0;
                                resetRoot();
                            }
                            break;
                        case R.id.button_sym:
                            break;
                        default:
                            buttonpress_chord = 0;
                            resetRoot();
                    }
                    relabelKeys();
                    updateText();
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
                if (isChorded)
                {
                    tree = new HuffmanTree(3);
                    sym_tree = new HuffmanTree(3);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1, 2};
                } else {
                    sym_tree = new HuffmanTree(2);
                    tree = new HuffmanTree(2);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1};
                }
                break;
            case THREEFINGER:
                kv = getLayoutInflater().inflate(R.layout.threechord, null);
                if(isChorded)
                {
                    sym_tree = new HuffmanTree(6);
                    // Middle finger is strongest. My device did not support 3-finger tap.
                    tree = new HuffmanTree(6);
                    // Key chords should be in descending order of relative effort.
                    // To aid the visual, this would be the LEFT hand.
                    //    0   1   2   3   4   5
                    //  001 010 011 100 101 110

                    keylookup = new int[]{ -1,  1,  0,  3,  2,  5,  4};
                } else {
                    sym_tree = new HuffmanTree(2);
                    tree = new HuffmanTree(2);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1, 2};
                }
                final Button chord_three = kv.findViewById(R.id.chord_three);
                chord_three.setOnTouchListener(onPress);
                break;
            case TWOXTWOFINGER:
                kv = getLayoutInflater().inflate(R.layout.twoxtwochord, null);
                if(isChorded)
                {
                    sym_tree = new HuffmanTree(8);
                    // Middle finger is strongest. My device did not support 3-finger tap.
                    tree = new HuffmanTree(8);
                    // Key chords should be in descending order of relative effort.
                    // To aid the visual, this would be the LEFT hand.
                    //  12
                    //  34
                    //                      0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
                    //                        10 01 11 00 10 01 11 00 10 01 11 00 10 01 11
                    //                        00 00 00 10 10 10 10 01 01 01 01 11 11 11 11
                    keylookup = new int[]{ -1, 1, 0, 4, 3,-1, 6,-1, 2, 7,-1,-1, 5,-1,-1,-1};
                } else {
                    sym_tree = new HuffmanTree(2);
                    tree = new HuffmanTree(2);
                    //                     01  10  11
                    keylookup = new int[]{-1, 0, 1, 2};
                }
                final Button chord_threex = kv.findViewById(R.id.chord_three);
                chord_threex.setOnTouchListener(onPress);
                final Button chord_fourx = kv.findViewById(R.id.chord_four);
                chord_fourx.setOnTouchListener(onPress);
                break;
        }
        View button_bksp = kv.findViewById(R.id.button_backspace);
        if(remove_views && hardware_buttons_count > 1)
        {
            ((ViewGroup) button_bksp.getParent()).removeView(button_bksp);
        } else
        {
            button_bksp.setOnTouchListener(onPress);
        }

        View button_shift = kv.findViewById(R.id.button_shift);
        if(remove_views && hardware_buttons_count > 2)
        {
            ((ViewGroup) button_shift.getParent()).removeView(button_shift);
        } else {
            button_shift.setOnTouchListener(onPress);
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

        if(rotate_view)
        {
            kv.setRotation(-20.0f);
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        buttonpress_chord = 0;
        buttonpress_current = 0;

        ArrayList<HuffmanNode> inp = new ArrayList<>();
        inp.add(new HuffmanNode("e", 12.702));
        inp.add(new HuffmanNode("t", 9.056));
        inp.add(new HuffmanNode("a", 8.167));
        inp.add(new HuffmanNode("o", 7.507));
        inp.add(new HuffmanNode("i", 6.966));
        inp.add(new HuffmanNode("n", 6.749));
        inp.add(new HuffmanNode("s", 6.327));
        inp.add(new HuffmanNode("h", 6.094));
        inp.add(new HuffmanNode("r", 5.987));
        inp.add(new HuffmanNode("d", 4.253));
        inp.add(new HuffmanNode("l", 4.025));
        inp.add(new HuffmanNode("c", 2.782));
        inp.add(new HuffmanNode("u", 2.758));
        inp.add(new HuffmanNode("m", 2.406));
        inp.add(new HuffmanNode("w", 2.360));
        inp.add(new HuffmanNode("f", 2.228));
        inp.add(new HuffmanNode("g", 2.015));
        inp.add(new HuffmanNode("y", 1.974));
        inp.add(new HuffmanNode("p", 1.929));
        inp.add(new HuffmanNode("b", 1.492));
        inp.add(new HuffmanNode("v", 0.978));
        inp.add(new HuffmanNode("k", 0.772));
        inp.add(new HuffmanNode("j", 0.153));
        inp.add(new HuffmanNode("x", 0.150));
        inp.add(new HuffmanNode("q", 0.095));
        inp.add(new HuffmanNode("z", 0.074));
        tree.CreateEncoding(inp);

        ArrayList<HuffmanNode> sym_inp = new ArrayList<>();
        sym_inp.add(new HuffmanNode(". ", 5.0, "."));
        sym_inp.add(new HuffmanNode("! ", 2.0, "!"));
        sym_inp.add(new HuffmanNode("? ", 2.0, "?"));
        sym_inp.add(new HuffmanNode("0", 0.6));
        sym_inp.add(new HuffmanNode("1", 0.6));
        sym_inp.add(new HuffmanNode("2", 0.5));
        sym_inp.add(new HuffmanNode("3", 0.5));
        sym_inp.add(new HuffmanNode("4", 0.5));
        sym_inp.add(new HuffmanNode("5", 0.5));
        sym_inp.add(new HuffmanNode("6", 0.5));
        sym_inp.add(new HuffmanNode("7", 0.5));
        sym_inp.add(new HuffmanNode("8", 0.5));
        sym_inp.add(new HuffmanNode("9", 0.5));
        sym_inp.add(new HuffmanNode("@", 0.25));
        sym_inp.add(new HuffmanNode("#", 0.25));
        sym_inp.add(new HuffmanNode("$", 0.25));
        sym_inp.add(new HuffmanNode("%", 0.25));
        sym_inp.add(new HuffmanNode("^", 0.25));
        sym_inp.add(new HuffmanNode("&", 0.25));
        sym_inp.add(new HuffmanNode("*", 0.25));
        sym_inp.add(new HuffmanNode("(", 0.25));
        sym_inp.add(new HuffmanNode(")", 0.25));
        sym_inp.add(new HuffmanNode("-", 0.25));
        sym_inp.add(new HuffmanNode("_", 0.25));
        sym_inp.add(new HuffmanNode(":", 0.25));
        sym_inp.add(new HuffmanNode(";", 0.25));
        sym_inp.add(new HuffmanNode("/", 0.25));
        sym_inp.add(new HuffmanNode("|", 0.25));
        sym_inp.add(new HuffmanNode("+", 0.25));
        sym_inp.add(new HuffmanNode("=", 0.25));
        sym_inp.add(new HuffmanNode("\\", 0.25));
        sym_tree.CreateEncoding(sym_inp);

        resetRoot();
        relabelKeys();
        updateText();
        return kv;
    }

    private String getKeyLabel(int[] chords, boolean is_caps)
    {
        StringBuilder label = new StringBuilder();
        //String label = "";
        for(int chord: chords)
        {
            label.append((curNode.children.size() >= keylookup[chord])? curNode.children.get(keylookup[chord]).displayString + "\n" : "\n");
        }
        return is_caps ? label.toString().toUpperCase() : label.toString();
    }

    private void relabelKeys() {
        Button chord_one = kv.findViewById(R.id.chord_one);
        Button chord_two = kv.findViewById(R.id.chord_two);

        if(!isChorded)
        {
            chord_one.setText(getKeyLabel(new int[] {1}, caps));
            chord_one.invalidate();
            chord_two.setText(getKeyLabel(new int[] {2}, caps));
            chord_two.invalidate();

            if(kType == THREEFINGER)
            {
                Button chord_three = kv.findViewById(R.id.chord_three);
                chord_three.setText(getKeyLabel(new int[] {3}, caps));
                chord_three.invalidate();
            }
            return;
        }

        switch(kType) {
            case TWOFINGER:
                chord_one.setText(getKeyLabel(new int[]{1,3}, caps));
                chord_one.invalidate();
                chord_two.setText(getKeyLabel(new int[]{2,3}, caps));
                chord_two.invalidate();
                break;
            case THREEFINGER:
                chord_one.setText(getKeyLabel(new int[]{1,3,5}, caps));
                chord_one.invalidate();
                chord_two.setText(getKeyLabel(new int[]{2,3,6}, caps));
                chord_two.invalidate();
                Button chord_three = kv.findViewById(R.id.chord_three);
                chord_three.setText(getKeyLabel(new int[]{4,6,5}, caps));
                chord_three.invalidate();
                break;
            case TWOXTWOFINGER:
                chord_one.setText(getKeyLabel(new int[]{1,3,9}, caps));
                chord_one.invalidate();
                chord_two.setText(getKeyLabel(new int[]{2,3,6}, caps));
                chord_two.invalidate();
                Button chord_threex = kv.findViewById(R.id.chord_three);
                chord_threex.setText(getKeyLabel(new int[]{4,6,12}, caps));
                chord_threex.invalidate();
                Button chord_fourx = kv.findViewById(R.id.chord_four);
                chord_fourx.setText(getKeyLabel(new int[]{8,9,12}, caps));
                chord_fourx.invalidate();
                break;
        }
    }

    private void updateText()
    {
        ExtractEditText editText = kv.findViewById(R.id.inputExtractEditText);
        ExtractedTextRequest etr=new ExtractedTextRequest();
        etr.token=0;
        ExtractedText et = ic.getExtractedText(etr, 0);
        // retry
        if(et == null)
        {
            ic =  getCurrentInputConnection();
            et = ic.getExtractedText(etr, 0);
        }
        editText.setExtractedText(et);
    }
}