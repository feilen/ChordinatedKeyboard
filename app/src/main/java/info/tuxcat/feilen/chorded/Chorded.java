package info.tuxcat.feilen.chorded;

import android.content.Context;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.support.wearable.input.WearableButtons;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.os.Vibrator;
import android.widget.LinearLayout;

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
    enum KeyboardType {
        TWOFINGER,
        THREEFINGER,
        TWOXTWOFINGER,
        TWOXTWOFINGERNOSTRETCH
    }

    enum SwipeDirection {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    private SwipeDirection toSwipeDirection(float deltaX, float deltaY)
    {
        double direction = Math.atan2((double)deltaX, (double)deltaY) * (180/Math.PI);
        direction += comfort_angle;
        if(direction > 180.0)
        {
            direction -= 360.0;
        } else if (direction < -180)
        {
            direction += 360.0;
        }
        if(direction >= 45.0 && direction < 135.0)
        {
            return SwipeDirection.RIGHT;
        } else if (direction >= 135.0 || direction <= -135.0)
        {
            return SwipeDirection.UP;
        } else if ((direction < 45.0 && direction >= 0.0) || (direction < 0.0 && direction >= -45.0))
        {
            return SwipeDirection.DOWN;
        } else
        {
            return SwipeDirection.LEFT;
        }
    }

    private static float norm(float n1, float n2)
    {
        return (float)Math.sqrt((n1 * n1) + (n2 * n2));
    }

    private final KeyboardType kType = KeyboardType.TWOXTWOFINGERNOSTRETCH;
    private final boolean use_swipes = true;
    private final boolean space_in_tree = false;
    private final boolean sym_in_tree = false;
    private final float comfort_angle = -20.0f;

    private boolean caps = true;
    private boolean sym = false;

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
            updateText();
        }
    }

    private void performShift()
    {
        caps = !caps;
        relabelKeys();
    }

    private void performSpace()
    {
        ic.commitText(" ",  1);
        updateText();
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
                    if(use_swipes) {
                        float swipeX = eventtype.getX() - downX, swipeY = eventtype.getY() - downY;
                        float swipe_length = norm(swipeX, swipeY);
                        if (swipe_length >= kv.getWidth() / 3.0) {
                            // Detected a viable swipe.
                            switch (toSwipeDirection(swipeX, swipeY)) {
                                case UP:
                                    performShift();
                                    break;
                                case DOWN:
                                    toggleSym();
                                    break;
                                case LEFT:
                                    performBackspace();
                                    break;
                                case RIGHT:
                                    performSpace();
                                    break;
                            }
                            buttonpress_current = 0;
                            buttonpress_chord = 0;
                            break;
                        }
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
                                    relabelKeys();
                                    updateText();
                                } else {
                                    vibrator.vibrate(15);
                                    relabelKeys();
                                }
                                buttonpress_chord = 0;
                            } else {
                                // Invalid coding. Reset to root.
                                buttonpress_chord = 0;
                                resetRoot();
                                relabelKeys();
                            }
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
        sym_inp.add(new HuffmanNode(".", 0.5407939404659455));
        sym_inp.add(new HuffmanNode(",", 0.16461392538226494));
        sym_inp.add(new HuffmanNode("1", 0.045110034494593935));
        sym_inp.add(new HuffmanNode("-", 0.03695380629806522));
        sym_inp.add(new HuffmanNode("_", 0.03695380629806522));
        sym_inp.add(new HuffmanNode("\"", 0.025805651796693023));
        sym_inp.add(new HuffmanNode("0", 0.024383965675686684));
        sym_inp.add(new HuffmanNode("2", 0.024372053697076918));
        sym_inp.add(new HuffmanNode("9", 0.022183488907292485));
        sym_inp.add(new HuffmanNode("'", 0.021393207451059593));
        sym_inp.add(new HuffmanNode("3", 0.018042435881588208));
        sym_inp.add(new HuffmanNode(")", 0.017370302996660276));
        sym_inp.add(new HuffmanNode("(", 0.01719763603953939));
        sym_inp.add(new HuffmanNode("5", 0.015773199620595514));
        sym_inp.add(new HuffmanNode("4", 0.015417072867507797));
        sym_inp.add(new HuffmanNode("8", 0.014895139227337536));
        sym_inp.add(new HuffmanNode("6", 0.013784676288063728));
        sym_inp.add(new HuffmanNode(";", 0.01092166872416053));
        sym_inp.add(new HuffmanNode("7", 0.010618114153469436));
        sym_inp.add(new HuffmanNode(":", 0.010041084966759792));
        sym_inp.add(new HuffmanNode("?", 0.003963123413518657));
        sym_inp.add(new HuffmanNode("/", 0.001984415281369128));
        sym_inp.add(new HuffmanNode("!", 0.001533170494735705));
        sym_inp.add(new HuffmanNode("]", 0.0012173039860143954));
        sym_inp.add(new HuffmanNode("[", 0.0012062913038717994));
        sym_inp.add(new HuffmanNode("*", 0.001171742078651894));
        sym_inp.add(new HuffmanNode("&", 0.000851400999709061));
        sym_inp.add(new HuffmanNode("$", 0.000829164152217612));
        sym_inp.add(new HuffmanNode("%", 0.0007939120096654312));
        sym_inp.add(new HuffmanNode("=", 0.0003914106529181468));
        sym_inp.add(new HuffmanNode("^", 0.0003650821453991294));
        sym_inp.add(new HuffmanNode("°", 0.0002669752645165843));
        sym_inp.add(new HuffmanNode("+", 0.00026450019729451236));
        sym_inp.add(new HuffmanNode("§", 0.0002207307212671594));
        sym_inp.add(new HuffmanNode("£", 0.00018640812409159789));
        sym_inp.add(new HuffmanNode(">", 0.00018604714639533013));
        sym_inp.add(new HuffmanNode("\\", 0.00013057625188935318));
        sym_inp.add(new HuffmanNode("»", 0.00012577640397443574));
        sym_inp.add(new HuffmanNode("<", 0.00012313041643681097));
        sym_inp.add(new HuffmanNode("«", 0.0001098284730658925));
        sym_inp.add(new HuffmanNode("|", 0.00010639035604362342));
        sym_inp.add(new HuffmanNode("#", 7.807508221459735e-05));
        sym_inp.add(new HuffmanNode("~", 6.852052679622645e-05));
        sym_inp.add(new HuffmanNode("©", 6.358297471035486e-05));
        sym_inp.add(new HuffmanNode("{", 5.875344570821948e-05));
        sym_inp.add(new HuffmanNode("}", 3.563352108206465e-05));
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
        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        resetRoot();
        relabelKeys();
        updateText();
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