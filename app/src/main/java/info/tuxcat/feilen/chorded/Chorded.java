package info.tuxcat.feilen.chorded;

import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.os.Vibrator;

import java.util.ArrayList;

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

    // Settings: eventually populate from user config
    private final KeyboardType kType = KeyboardType.TWOXTWOFINGERNOSTRETCH;
    private final boolean space_in_tree = false;
    private final boolean sym_in_tree = false;
    private final float comfort_angle = -20.0f;

    // Current vars
    private boolean caps = true;
    private boolean sym = false;

    enum KeyboardType {
        TWOFINGER,
        THREEFINGER,
        TWOXTWOFINGER,
        TWOXTWOFINGERNOSTRETCH
    }

    enum SwipeDirection {
        NONE,
        SHORT_UP,
        SHORT_DOWN,
        SHORT_LEFT,
        SHORT_RIGHT,
        LONG_UP,
        LONG_DOWN,
        LONG_LEFT,
        LONG_RIGHT
    }

    private static float norm(float n1, float n2)
    {
        return (float)Math.sqrt((n1 * n1) + (n2 * n2));
    }

    private SwipeDirection toSwipeDirection(float deltaX, float deltaY, float swipeThresholdSmall, float swipeThresholdBig)
    {
        float swipe_length = norm(deltaX, deltaY);
        if(swipe_length < swipeThresholdSmall)
        {
            return SwipeDirection.NONE;
        }

        double direction = Math.atan2((double)deltaX, (double)deltaY) * (180/Math.PI) + comfort_angle;
        boolean long_swipe = swipe_length < swipeThresholdBig;

        if(direction > 180.0)
        {
            direction -= 360.0;
        } else if (direction < -180)
        {
            direction += 360.0;
        }

        if(direction >= 45.0 && direction < 135.0)
        {
            return long_swipe ? SwipeDirection.SHORT_RIGHT : SwipeDirection.LONG_RIGHT;
        } else if (direction >= 135.0 || direction <= -135.0)
        {
            return long_swipe ? SwipeDirection.SHORT_UP : SwipeDirection.LONG_UP;
        } else if ((direction < 45.0 && direction >= 0.0) || (direction < 0.0 && direction >= -45.0))
        {
            return long_swipe ? SwipeDirection.SHORT_DOWN : SwipeDirection.LONG_DOWN;
        } else
        {
            return long_swipe ? SwipeDirection.SHORT_LEFT : SwipeDirection.LONG_LEFT;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_STEM_1:
                performBackspace();
                return true;
            case KeyEvent.KEYCODE_STEM_2:
                toggleSym();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void performBackspace()
    {
        if(curNode != tree.root && curNode != sym_tree.root) {
            resetRoot();
            buttonpress_chord = 0;
            relabelKeys();
        } else {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void performDeleteWord()
    {
        resetRoot();
        buttonpress_chord = 0;
        relabelKeys();
        CharSequence prior_text;
        // do...while ensures at least one deletion, in the case of starting with a space
        do {
            ic.deleteSurroundingText(1, 0);
            prior_text = ic.getTextBeforeCursor(1, 0);
        } while (prior_text.length() != 0 && !Character.isSpaceChar(prior_text.charAt(0)));
    }

    private void performShift()
    {
        caps = !caps;
        relabelKeys();
    }

    private void performSpace()
    {
        ic.commitText(" ",  1);
    }

    private void toggleSym()
    {
        sym = !sym;
        resetRoot();
        relabelKeys();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return true;
    }

    private void resetRoot() {
        curNode = sym && !sym_in_tree ? sym_tree.root : tree.root;
    }

    private float downX, downY;
    private final View.OnTouchListener onPress = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View button, MotionEvent eventtype)
        {
            switch(eventtype.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    downX = eventtype.getX();
                    downY = eventtype.getY();
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
                        case R.id.button_return:
                            sendDefaultEditorAction(true);
                            break;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    float swipeX = eventtype.getX() - downX, swipeY = eventtype.getY() - downY;
                    SwipeDirection swiped = toSwipeDirection(swipeX, swipeY, kv.getWidth() / 5.0f, kv.getWidth() / 1.66f);
                    if(swiped != SwipeDirection.NONE) {
                        switch (swiped) {
                            case SHORT_UP:
                            case LONG_UP:
                                if (!sym) {
                                    performShift();
                                } else {
                                    toggleSym();
                                }
                                break;
                            case SHORT_DOWN:
                            case LONG_DOWN:
                                toggleSym();
                                break;
                            case SHORT_LEFT:
                                performBackspace();
                                break;
                            case LONG_LEFT:
                                performDeleteWord();
                                break;
                            case SHORT_RIGHT:
                            case LONG_RIGHT:
                                performSpace();
                                break;
                        }
                        buttonpress_current = 0;
                        buttonpress_chord = 0;
                        break;
                    }
                    switch(button.getId()) {
                        case R.id.chord_one:
                        case R.id.chord_two:
                        case R.id.chord_three:
                        case R.id.chord_four:
                            switch (button.getId()) {
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
                            if (buttonpress_current != 0) {
                                break;
                            }
                            // Invalid chord. Do nothing.
                            if (keylookup[buttonpress_chord] == -1) return true;
                            if (curNode.children.size() >= keylookup[buttonpress_chord]) {
                                curNode = curNode.children.get(keylookup[buttonpress_chord]);
                                if (curNode.children.size() == 0) {
                                    // Reached the end. Commit the current letter.
                                    vibrator.vibrate(25);
                                    String inputchar = curNode.resultString;
                                    if (caps && inputchar.length() == 1 && Character.isAlphabetic(inputchar.charAt(0))) {
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
                            relabelKeys();
                            break;
                        default:
                            buttonpress_chord = 0;
                            resetRoot();
                            relabelKeys();
                    }
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
                tree = new HuffmanTree(3);
                sym_tree = new HuffmanTree(3);
                //                     01  10  11
                keylookup = new int[]{-1, 0, 1, 2};
                break;
            case THREEFINGER:
                kv = getLayoutInflater().inflate(R.layout.threechord, null);
                sym_tree = new HuffmanTree(6);
                // Middle finger is strongest. My device did not support 3-finger tap.
                tree = new HuffmanTree(6);
                // Key chords should be in descending order of relative effort.
                // To aid the visual, this would be the LEFT hand.
                //    0   1   2   3   4   5
                //  001 010 011 100 101 110

                keylookup = new int[]{ -1,  1,  0,  3,  2,  5,  4};
                final Button chord_three = kv.findViewById(R.id.chord_three);
                chord_three.setOnTouchListener(onPress);
                break;
            case TWOXTWOFINGER:
            case TWOXTWOFINGERNOSTRETCH:
                kv = getLayoutInflater().inflate(R.layout.twoxtwochord, null);
                // Key chords should be in descending order of relative effort.
                // To aid the visual, this would be the LEFT hand.
                //  12
                //  34
                if(kType == KeyboardType.TWOXTWOFINGER)
                {
                    sym_tree = new HuffmanTree(8);
                    tree = new HuffmanTree(8);
                    //                      0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
                    //                        10 01 11 00 10 01 11 00 10 01 11 00 10 01 11
                    //                        00 00 00 10 10 10 10 01 01 01 01 11 11 11 11
                    keylookup = new int[]{ -1, 1, 0, 4, 3,-1, 6,-1, 2, 7,-1,-1, 5,-1,-1,-1};
                } else {
                    sym_tree = new HuffmanTree(7);
                    tree = new HuffmanTree(7);
                    keylookup = new int[]{ -1, 1, 0, 4, 3,-1, 6,-1, 2,-1,-1,-1, 5,-1,-1,-1};
                }
                final Button chord_threex = kv.findViewById(R.id.chord_three);
                chord_threex.setOnTouchListener(onPress);
                final Button chord_fourx = kv.findViewById(R.id.chord_four);
                chord_fourx.setOnTouchListener(onPress);
                break;
        }
        final Button button_return = kv.findViewById(R.id.button_return);
        button_return.setOnTouchListener(onPress);
        final Button chord_one = kv.findViewById(R.id.chord_one);
        chord_one.setOnTouchListener(onPress);
        final Button chord_two = kv.findViewById(R.id.chord_two);
        chord_two.setOnTouchListener(onPress);

        kv.setRotation(comfort_angle);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        buttonpress_chord = 0;
        buttonpress_current = 0;

        // Letter frequencies sampled from wikipedia:
        // https://en.m.wikipedia.org/wiki/Letter_frequency
        // and later Googlebooks 1-grams newer than 1997:
        // https://storage.googleapis.com/books/ngrams/books/datasetsv2.html
        // All of these are simply the total of that character, divided by the count of 'e'.
        ArrayList<HuffmanNode> sym_inp = new ArrayList<>();
        sym_inp.add(new HuffmanNode("{", 1600.0));
        sym_inp.add(new HuffmanNode("}", 1600.0));
        sym_inp.add(new HuffmanNode("[", 1719.0));
        sym_inp.add(new HuffmanNode("]", 1797.0));
        sym_inp.add(new HuffmanNode("’", 2008.0));
        sym_inp.add(new HuffmanNode("#", 3360.0));
        sym_inp.add(new HuffmanNode("&", 3929.0));
        sym_inp.add(new HuffmanNode("$", 4472.0));
        sym_inp.add(new HuffmanNode("|", 4641.0));
        sym_inp.add(new HuffmanNode("+", 5671.0));
        sym_inp.add(new HuffmanNode("\\", 5864.0));
        sym_inp.add(new HuffmanNode("%", 7308.0));
        sym_inp.add(new HuffmanNode("=", 19406.0));
        sym_inp.add(new HuffmanNode("_", 21358.0));
        sym_inp.add(new HuffmanNode("<", 25073.0));
        sym_inp.add(new HuffmanNode("@", 25638.0));
        sym_inp.add(new HuffmanNode("7", 26553.0));
        sym_inp.add(new HuffmanNode("9", 26710.0));
        sym_inp.add(new HuffmanNode("(", 27309.0));
        sym_inp.add(new HuffmanNode("8", 30295.0));
        sym_inp.add(new HuffmanNode("6", 31896.0));
        sym_inp.add(new HuffmanNode(">", 33170.0));
        sym_inp.add(new HuffmanNode("4", 33565.0));
        sym_inp.add(new HuffmanNode(")", 33618.0));
        sym_inp.add(new HuffmanNode("~", 35828.0));
        sym_inp.add(new HuffmanNode("5", 36502.0));
        sym_inp.add(new HuffmanNode(";", 41921.0));
        sym_inp.add(new HuffmanNode("^", 46880.0));
        sym_inp.add(new HuffmanNode("3", 52537.0));
        sym_inp.add(new HuffmanNode("2", 52537.0));
        sym_inp.add(new HuffmanNode("\"", 60509.0));
        sym_inp.add(new HuffmanNode("1", 72710.0));
        sym_inp.add(new HuffmanNode("0", 75707.0));
        sym_inp.add(new HuffmanNode("-", 83988.0));
        sym_inp.add(new HuffmanNode("*", 116810.0));
        sym_inp.add(new HuffmanNode("!", 135396.0));
        sym_inp.add(new HuffmanNode("?", 162819.0));
        sym_inp.add(new HuffmanNode("/", 171341.0));
        sym_inp.add(new HuffmanNode(":", 182279.0));
        sym_inp.add(new HuffmanNode(",", 321388.0));
        sym_inp.add(new HuffmanNode("'", 434148.0));
        sym_inp.add(new HuffmanNode(".", 668936.0));
        sym_tree.CreateEncoding(sym_inp);

        ArrayList<HuffmanNode> inp = new ArrayList<>();
        inp.add(new HuffmanNode("e", 12.49));
        inp.add(new HuffmanNode("t", 9.28));
        inp.add(new HuffmanNode("a", 8.04));
        inp.add(new HuffmanNode("o", 7.64));
        inp.add(new HuffmanNode("i", 7.57));
        inp.add(new HuffmanNode("n", 7.23));
        inp.add(new HuffmanNode("s", 6.51));
        inp.add(new HuffmanNode("r", 6.28));
        inp.add(new HuffmanNode("h", 5.05));
        inp.add(new HuffmanNode("l", 4.07));
        inp.add(new HuffmanNode("d", 3.82));
        inp.add(new HuffmanNode("c", 3.34));
        inp.add(new HuffmanNode("u", 2.73));
        inp.add(new HuffmanNode("m", 2.51));
        inp.add(new HuffmanNode("f", 2.40));
        inp.add(new HuffmanNode("p", 2.14));
        inp.add(new HuffmanNode("g", 1.87));
        inp.add(new HuffmanNode("w", 1.68));
        inp.add(new HuffmanNode("y", 1.66));
        inp.add(new HuffmanNode("b", 1.48));
        inp.add(new HuffmanNode("v", 1.05));
        inp.add(new HuffmanNode("k", 0.54));
        inp.add(new HuffmanNode("x", 0.23));
        inp.add(new HuffmanNode("j", 0.16));
        inp.add(new HuffmanNode("q", 0.12));
        inp.add(new HuffmanNode("z", 0.09));

        if(sym_in_tree)
        {
            sym_tree.root.displayString = ".!?";
            sym_tree.root.frequency = 14.726;
            inp.add(sym_tree.root);
        }

        if(space_in_tree)
        {
            // found by dividing out the average length of words in target language, and multiplying
            // by the sum of the above.. (English: 4.79)
            inp.add(new HuffmanNode(" ", 14.727, "␣"));
        }
        tree.CreateEncoding(inp);
        return kv;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        caps = true;
        sym = false;
        ic =  getCurrentInputConnection();
        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        resetRoot();
        relabelKeys();
        resetText();
        super.onStartInputView(info, restarting);
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
            case TWOXTWOFINGERNOSTRETCH:
                chord_one.setText(getKeyLabel(new int[]{1,3}, caps));
                chord_one.invalidate();
                chord_two.setText(getKeyLabel(new int[]{2,3,6}, caps));
                chord_two.invalidate();
                Button chord_threexn = kv.findViewById(R.id.chord_three);
                chord_threexn.setText(getKeyLabel(new int[]{4,6,12}, caps));
                chord_threexn.invalidate();
                Button chord_fourxn = kv.findViewById(R.id.chord_four);
                chord_fourxn.setText(getKeyLabel(new int[]{8,12}, caps));
                chord_fourxn.invalidate();
                break;
        }
    }

    @Override
    public void onUpdateExtractedText(int token, ExtractedText text) {
        ExtractEditText editText = kv.findViewById(R.id.inputExtractEditText);
        editText.setExtractedText(text);
        super.onUpdateExtractedText(token, text);
    }

    private void resetText() {
        ExtractEditText editText = kv.findViewById(R.id.inputExtractEditText);
        ExtractedTextRequest etr = new ExtractedTextRequest();
        etr.token = 0;
        ExtractedText et = ic.getExtractedText(etr, 0);
        // retry
        int tries = 3;
        while (et == null && tries > 0) {
            ic = getCurrentInputConnection();
            et = ic.getExtractedText(etr, 0);
            tries--;
        }
        if (et != null) {
            editText.setExtractedText(et);
        }
    }
}