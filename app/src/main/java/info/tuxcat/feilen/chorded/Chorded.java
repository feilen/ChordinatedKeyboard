package info.tuxcat.feilen.chorded;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.inputmethodservice.ExtractEditText;
import android.inputmethodservice.InputMethodService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.os.Vibrator;

import java.util.ArrayList;

public class Chorded extends InputMethodService {

    // Service connections
    private View root_view;
    private InputConnection ic;
    @Nullable
    private Vibrator vibrator;

    // Tree and traversal
    private HuffmanTree tree;
    private HuffmanTree sym_tree;
    private HuffmanNode curNode;
    private int[] keylookup;

    // Settings
    private final SettingsContainer settings = new SettingsContainer();

    // Current vars
    @NonNull
    private CapsType caps = CapsType.SHIFT;
    @NonNull
    private SymType sym = SymType.SYM_OFF;
    private int buttonpress_current;
    private int buttonpress_chord;

    enum KeyboardType {
        TWOFINGER,
        THREEFINGER,
        TWOXTWOFINGER,
        TWOXTWOFINGERHALFSTRETCH,
        TWOXTWOFINGERNOSTRETCH
    }

    // TODO: This really needs to be a bitmasked interface
    enum SwipeDirection {
        NONE,
        SHORT_UP,
        SHORT_DOWN,
        SHORT_LEFT,
        SHORT_RIGHT,
        MEDIUM_UP,
        MEDIUM_DOWN,
        MEDIUM_LEFT,
        MEDIUM_RIGHT,
        LONG_UP,
        LONG_DOWN,
        LONG_LEFT,
        LONG_RIGHT
    }

    enum SwipeDistance {
        NONE,
        SHORT,
        MEDIUM,
        LONG
    }

    enum CapsType {
        LOWER,
        SHIFT,
        CAPS
    }

    enum SymType {
        SYM_ON,
        SYM_OFF,
        SYM_IN_TREE
    }

    enum VibrationType {
        CHORD_SUBTREE,
        CHORD_SUBMIT,
        SWIPE
    }

    private static float norm(float n1, float n2)
    {
        return (float)Math.sqrt((n1 * n1) + (n2 * n2));
    }

    @NonNull
    private SwipeDirection toSwipeDirection(float deltaX, float deltaY, float swipeThresholdSmall, float swipeThresholdMedium, float swipeThresholdBig)
    {
        float swipe_length = norm(deltaX, deltaY);
        if(swipe_length < swipeThresholdSmall)
        {
            return SwipeDirection.NONE;
        }

        double direction = Math.atan2((double)deltaX, (double)deltaY) * (180/Math.PI) + (settings.left_handed_mode ? -settings.comfort_angle : settings.comfort_angle);
        SwipeDistance swipe_length_class = SwipeDistance.SHORT;
        if(swipe_length >= swipeThresholdMedium) swipe_length_class = SwipeDistance.MEDIUM;
        if(swipe_length >= swipeThresholdBig) swipe_length_class = SwipeDistance.LONG;

        if(direction > 180.0)
        {
            direction -= 360.0;
        } else if (direction < -180)
        {
            direction += 360.0;
        }

        if(direction >= 45.0 && direction < 135.0)
        {
            switch(swipe_length_class)
            {
                case SHORT: return SwipeDirection.SHORT_RIGHT;
                case MEDIUM: return SwipeDirection.MEDIUM_RIGHT;
                case LONG: return SwipeDirection.LONG_RIGHT;
            }
        } else if (direction >= 135.0 || direction <= -135.0)
        {
            switch(swipe_length_class)
            {
                case SHORT: return SwipeDirection.SHORT_UP;
                case MEDIUM: return SwipeDirection.MEDIUM_UP;
                case LONG: return SwipeDirection.LONG_UP;
            }
        } else if ((direction < 45.0 && direction >= 0.0) || (direction < 0.0 && direction >= -45.0))
        {
            switch(swipe_length_class)
            {
                case SHORT: return SwipeDirection.SHORT_DOWN;
                case MEDIUM: return SwipeDirection.MEDIUM_DOWN;
                case LONG: return SwipeDirection.LONG_DOWN;
            }
        } else
        {
            switch(swipe_length_class)
            {
                case SHORT: return SwipeDirection.SHORT_LEFT;
                case MEDIUM: return SwipeDirection.MEDIUM_LEFT;
                case LONG: return SwipeDirection.LONG_LEFT;
            }
        }
        return SwipeDirection.NONE;
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

    private void performVibrate(VibrationType type, long millis)
    {
        if(settings.vibration_type.contains(type)) {
            vibrator.vibrate(millis);
        }
    }

    private void performBackspace()
    {
        if(curNode != tree.root && curNode != sym_tree.root) {
            resetRoot();
            buttonpress_chord = 0;
            relabelKeys();
        } else {
            ic.deleteSurroundingText(1, 0);
            if(settings.auto_shift && ic.getTextBeforeCursor(1, 0).length() == 0)
            {
                caps = CapsType.SHIFT;
                relabelKeys();
            }
        }
    }

    private void performDeleteWord()
    {
        resetRoot();
        buttonpress_chord = 0;
        relabelKeys();
        int delete_idx = 1;
        // delete words up to 20 chars in length
        CharSequence prior_text = ic.getTextBeforeCursor(20, 0);
        if(prior_text.length() == 0)
        {
            return;
        }
        while((prior_text.length() - delete_idx - 1) >= 0
                && !Character.isSpaceChar(prior_text.charAt(prior_text.length() - delete_idx - 1)))
        {
            delete_idx++;
        }
        ic.deleteSurroundingText(delete_idx, 0);
        if(settings.auto_shift && ic.getTextBeforeCursor(1, 0).length() == 0)
        {
            caps = CapsType.SHIFT;
            relabelKeys();
        }
    }

    private void performShift()
    {
        switch(caps)
        {
            case LOWER:
                caps = CapsType.SHIFT;
                break;
            case SHIFT:
                caps = CapsType.CAPS;
                break;
            case CAPS:
                caps = CapsType.LOWER;
                break;
        }
        relabelKeys();
    }

    private void performSpace()
    {
        ic.commitText(" ",  1);
        resetRoot();
        buttonpress_chord = 0;
        relabelKeys();
    }

    private void toggleSym()
    {
        switch(sym)
        {
            case SYM_IN_TREE:
                return;
            case SYM_ON:
                sym = SymType.SYM_OFF;
                break;
            case SYM_OFF:
                sym = SymType.SYM_ON;
                break;
        }
        resetRoot();
        relabelKeys();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return true;
    }

    private void resetRoot() {
        switch(sym)
        {
            case SYM_IN_TREE:
            case SYM_OFF:
                curNode = tree.root;
                break;
            case SYM_ON:
                curNode = sym_tree.root;
        }
    }

    private float downX, downY;
    @Nullable
    private final View.OnTouchListener onPress = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(@NonNull View button, MotionEvent eventtype)
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
                    SwipeDirection swiped = toSwipeDirection(swipeX, swipeY, root_view.getWidth() / 5.0f, root_view.getWidth() / 2.5f, root_view.getWidth() / 1.33f);
                    if(swiped != SwipeDirection.NONE) {
                        switch (swiped) {
                            case SHORT_UP:
                                break;
                            case MEDIUM_UP:
                            case LONG_UP:
                                if (sym != SymType.SYM_ON) {
                                    performShift();
                                } else {
                                    toggleSym();
                                }
                                performVibrate(VibrationType.SWIPE, 35);
                                break;
                            case SHORT_DOWN:
                                break;
                            case MEDIUM_DOWN:
                            case LONG_DOWN:
                                toggleSym();
                                performVibrate(VibrationType.SWIPE, 35);
                                break;
                            case SHORT_LEFT:
                            case MEDIUM_LEFT:
                                performBackspace();
                                performVibrate(VibrationType.SWIPE, 25);
                                break;
                            case LONG_LEFT:
                                performDeleteWord();
                                performVibrate(VibrationType.SWIPE, 35);
                                break;
                            case SHORT_RIGHT:
                            case MEDIUM_RIGHT:
                            case LONG_RIGHT:
                                performSpace();
                                performVibrate(VibrationType.SWIPE, 25);
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
                                    break;
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
                                    performVibrate(VibrationType.CHORD_SUBMIT, 25);
                                    String inputchar = curNode.resultString;
                                    if ((caps != CapsType.LOWER) && inputchar.length() == 1 && Character.isAlphabetic(inputchar.charAt(0))) {
                                        inputchar = inputchar.toUpperCase();
                                        if(caps == CapsType.SHIFT)
                                        {
                                            caps = CapsType.LOWER;
                                        }
                                    }
                                    ic.commitText(String.valueOf(inputchar), 1);
                                    resetRoot();
                                } else {
                                    performVibrate(VibrationType.CHORD_SUBTREE, 15);
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

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        ic = getCurrentInputConnection();
        settings.loadSettings(getApplicationContext());
        //int hardware_buttons_count = WearableButtons.getButtonCount(getBaseContext());
        switch(settings.keyboard_type) {
            case TWOFINGER:
                root_view = getLayoutInflater().inflate(R.layout.twochord, null);
                tree = new HuffmanTree(3);
                sym_tree = new HuffmanTree(3);
                //                     01  10  11
                keylookup = settings.left_handed_mode ?
                        new int[]{-1, 1, 0, 2}:
                        new int[]{-1, 0, 1, 2};
                break;
            case THREEFINGER:
                root_view = getLayoutInflater().inflate(R.layout.threechord, null);
                sym_tree = new HuffmanTree(6);
                // Middle finger is strongest. My device did not support 3-finger tap.
                tree = new HuffmanTree(6);
                // Key chords should be in descending order of relative effort.
                // To aid the visual, this would be the LEFT hand.
                //    0   1   2   3   4   5
                //  001 010 011 100 101 110

                keylookup = new int[]{ -1,  1,  0,  3,  2,  5,  4};
                break;
            case TWOXTWOFINGER:
                // Key chords should be in descending order of relative effort.
                // To aid the visual, this would be the LEFT hand.
                //  12
                //  34
                root_view = getLayoutInflater().inflate(R.layout.twoxtwochord, null);
                sym_tree = new HuffmanTree(8);
                tree = new HuffmanTree(8);
                //                  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
                //                    10 01 11 00 10 01 11 00 10 01 11 00 10 01 11
                //                    00 00 00 10 10 10 10 01 01 01 01 11 11 11 11
                keylookup = new int[]{ -1, 1, 0, 4, 3,-1, 6,-1, 2, 7,-1,-1, 5,-1,-1,-1};
            case TWOXTWOFINGERHALFSTRETCH:
                // Same as above but ignore the bottom left, top right chord
                root_view = getLayoutInflater().inflate(R.layout.twoxtwochord, null);
                sym_tree = new HuffmanTree(7);
                tree = new HuffmanTree(7);
                keylookup = new int[]{ -1, 1, 0, 4, 3,-1, 6,-1, 2,-1,-1,-1, 5,-1,-1,-1};
                break;
            case TWOXTWOFINGERNOSTRETCH:
                // Same as above but ignore any chord requiring diagonals
                root_view = getLayoutInflater().inflate(R.layout.twoxtwochord, null);
                sym_tree = new HuffmanTree(6);
                tree = new HuffmanTree(6);
                keylookup = new int[]{ -1, 1, 0, 4, 3,-1,-1,-1, 2,-1,-1,-1, 5,-1,-1,-1};
                break;
        }
        final ExtractEditText eet = root_view.findViewById(R.id.inputExtractEditText);
        eet.setOnTouchListener(onPress);
        final Button button_return = root_view.findViewById(R.id.button_return);
        button_return.setOnTouchListener(onPress);
        final Button chord_one = root_view.findViewById(R.id.chord_one);
        chord_one.setOnTouchListener(onPress);
        final Button chord_two = root_view.findViewById(R.id.chord_two);
        chord_two.setOnTouchListener(onPress);
        final Button chord_three = root_view.findViewById(R.id.chord_three);
        if(chord_three != null) chord_three.setOnTouchListener(onPress);
        final Button chord_four = root_view.findViewById(R.id.chord_four);
        if(chord_four != null) chord_four.setOnTouchListener(onPress);

        root_view.setRotation(settings.left_handed_mode ?
                -settings.comfort_angle:
                settings.comfort_angle);
        if(settings.left_handed_mode)
        {
            // If in left-handed mode, literally swap the IDs of chords
            switch(settings.keyboard_type)
            {
                case TWOFINGER:
                    chord_one.setId(R.id.chord_two);
                    chord_two.setId(R.id.chord_one);
                    break;
                case THREEFINGER:
                    chord_one.setId(R.id.chord_three);
                    chord_three.setId(R.id.chord_one);
                    break;
                case TWOXTWOFINGER:
                case TWOXTWOFINGERHALFSTRETCH:
                case TWOXTWOFINGERNOSTRETCH:
                    chord_one.setId(R.id.chord_two);
                    chord_two.setId(R.id.chord_one);
                    chord_three.setId(R.id.chord_four);
                    chord_four.setId(R.id.chord_three);
                    break;
            }
        }

        // root_view has no root view and therefore has no idea what size it should be.
        WindowManager wm = (WindowManager) getBaseContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point total_size = new Point();
        display.getSize(total_size);
        root_view.setMinimumHeight(total_size.y);
        root_view.setMinimumWidth(total_size.x);

        // set space width to an ideal value
        DisplayMetrics displaymetrics = new DisplayMetrics();
        display.getMetrics(displaymetrics);
        int return_button_height = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 30, displaymetrics );
        double h = total_size.x/2.0;
        double a = h - return_button_height;
        double o = Math.sqrt((h*h) - (a*a));
        View space_inputlead = root_view.findViewById(R.id.space_inputlead);
        ViewGroup.LayoutParams lp = space_inputlead.getLayoutParams();
        lp.width = (int)(h - o);
        space_inputlead.setLayoutParams(lp);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        buttonpress_chord = 0;
        buttonpress_current = 0;

        // Letter frequencies sampled from wikipedia:
        // https://en.m.wikipedia.org/wiki/Letter_frequency
        // and later just my arbitrary Telegram chat logs.
        // Precompute numbers into one subtree
        HuffmanTree num_tree = new HuffmanTree(sym_tree.getFactor());
        ArrayList<HuffmanNode> num_inp = new ArrayList<>();
        num_inp.add(new HuffmanNode("8", 30295.0));
        num_inp.add(new HuffmanNode("6", 31896.0));
        num_inp.add(new HuffmanNode("4", 33565.0));
        num_inp.add(new HuffmanNode("5", 36502.0));
        num_inp.add(new HuffmanNode("3", 52537.0));
        num_inp.add(new HuffmanNode("2", 52537.0));
        num_inp.add(new HuffmanNode("1", 72710.0));
        num_inp.add(new HuffmanNode("0", 75707.0));
        num_inp.add(new HuffmanNode("7", 26553.0));
        num_inp.add(new HuffmanNode("9", 26710.0));
        num_tree.CreateEncoding(num_inp);
        num_tree.root.displayString = "0123456789";

        ArrayList<HuffmanNode> sym_inp = new ArrayList<>();
        sym_inp.add(num_tree.root);
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
        sym_inp.add(new HuffmanNode(">", 25074.0));
        sym_inp.add(new HuffmanNode("@", 25638.0));
        sym_inp.add(new HuffmanNode("^", 25639.0));
        sym_inp.add(new HuffmanNode(";", 25640.0));
        sym_inp.add(new HuffmanNode("*", 25641.0));
        sym_inp.add(new HuffmanNode("(", 27309.0));
        sym_inp.add(new HuffmanNode(")", 33618.0));
        sym_inp.add(new HuffmanNode("~", 35828.0));
        sym_inp.add(new HuffmanNode("\"", 60509.0));
        sym_inp.add(new HuffmanNode("-", 83988.0));
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

        if(settings.symbols_in_tree)
        {
            sym_tree.root.displayString = ".!?";
            sym_tree.root.frequency = 14.726;
            inp.add(sym_tree.root);
        }

        if(settings.space_in_tree)
        {
            // found by dividing out the average length of words in target language, and multiplying
            // by the sum of the above.. (English: 4.79)
            inp.add(new HuffmanNode(" ", 14.727, "␣"));
        }
        tree.CreateEncoding(inp);
        return root_view;
    }

    @Override
    public void onStartInput(@NonNull EditorInfo attribute, boolean restarting) {
        settings.loadSettings(getApplicationContext());
        if((attribute.inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) > 0)
        {
            caps = CapsType.CAPS;
        } else if (settings.auto_shift)
        {
            caps = CapsType.SHIFT;
        } else
        {
            caps = CapsType.LOWER;
        }
        ic = getCurrentInputConnection();
        // start out on sym for numbers
        int input_class = attribute.inputType & InputType.TYPE_MASK_CLASS;
        if(!settings.symbols_in_tree) {
            switch(input_class)
            {
                case InputType.TYPE_CLASS_NUMBER:
                case InputType.TYPE_CLASS_DATETIME:
                case InputType.TYPE_CLASS_PHONE:
                    sym = SymType.SYM_ON;
                    break;
                case InputType.TYPE_CLASS_TEXT:
                default:
                    sym = SymType.SYM_OFF;
                    break;
            }
        }
        else {
            sym = SymType.SYM_IN_TREE;
        }

        super.onStartInput(attribute, restarting);
    }

    @Override
    public void onStartInputView(@NonNull EditorInfo info, boolean restarting) {
        resetRoot();
        relabelKeys();
        resetText();

        ExtractEditText editText = root_view.findViewById(R.id.inputExtractEditText);
        if((info.inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) > 0)
        {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            editText.setTransformationMethod(null);
        }

        super.onStartInputView(info, restarting);
    }

    @NonNull
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
        Button chord_one = root_view.findViewById(R.id.chord_one);
        Button chord_two = root_view.findViewById(R.id.chord_two);
        Button chord_three = root_view.findViewById(R.id.chord_three);
        Button chord_four = root_view.findViewById(R.id.chord_four);

        switch (settings.keyboard_type) {
            case TWOFINGER:
                chord_one.setText(getKeyLabel(new int[]{1, 3}, caps != CapsType.LOWER));
                chord_two.setText(getKeyLabel(new int[]{2, 3}, caps != CapsType.LOWER));
                break;
            case THREEFINGER:
                chord_one.setText(getKeyLabel(new int[]{1, 3, 5}, caps != CapsType.LOWER));
                chord_two.setText(getKeyLabel(new int[]{2, 3, 6}, caps != CapsType.LOWER));
                chord_three.setText(getKeyLabel(new int[]{4, 6, 5}, caps != CapsType.LOWER));
                break;
            case TWOXTWOFINGER:
                chord_one.setText(getKeyLabel(new int[]{1, 3, 9}, caps != CapsType.LOWER));
                chord_two.setText(getKeyLabel(new int[]{2, 3, 6}, caps != CapsType.LOWER));
                chord_three.setText(getKeyLabel(new int[]{4, 6, 12}, caps != CapsType.LOWER));
                chord_four.setText(getKeyLabel(new int[]{8, 9, 12}, caps != CapsType.LOWER));
                break;
            case TWOXTWOFINGERHALFSTRETCH:
                chord_one.setText(getKeyLabel(new int[]{1, 3}, caps != CapsType.LOWER));
                chord_two.setText(getKeyLabel(new int[]{2, 3, 6}, caps != CapsType.LOWER));
                chord_three.setText(getKeyLabel(new int[]{4, 6, 12}, caps != CapsType.LOWER));
                chord_four.setText(getKeyLabel(new int[]{8, 12}, caps != CapsType.LOWER));
                break;
            case TWOXTWOFINGERNOSTRETCH:
                chord_one.setText(getKeyLabel(new int[]{1, 3}, caps != CapsType.LOWER));
                chord_two.setText(getKeyLabel(new int[]{2, 3}, caps != CapsType.LOWER));
                chord_three.setText(getKeyLabel(new int[]{4, 12}, caps != CapsType.LOWER));
                chord_four.setText(getKeyLabel(new int[]{8, 12}, caps != CapsType.LOWER));
                break;

        }
        if (caps == CapsType.CAPS && sym != SymType.SYM_ON)
        {
            chord_one.setPaintFlags(chord_one.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            chord_two.setPaintFlags(chord_two.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            if(chord_three != null) chord_three.setPaintFlags(chord_three.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            if(chord_four != null) chord_four.setPaintFlags(chord_four.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            chord_one.setPaintFlags(chord_one.getPaintFlags() & ~(Paint.UNDERLINE_TEXT_FLAG));
            chord_two.setPaintFlags(chord_two.getPaintFlags() & ~(Paint.UNDERLINE_TEXT_FLAG));
            if(chord_three != null) chord_three.setPaintFlags(chord_three.getPaintFlags() & ~(Paint.UNDERLINE_TEXT_FLAG));
            if(chord_four != null) chord_four.setPaintFlags(chord_four.getPaintFlags() & ~(Paint.UNDERLINE_TEXT_FLAG));
        }
        //noinspection ConstantConditions
        if(chord_one != null) chord_one.invalidate();
        //noinspection ConstantConditions
        if(chord_two != null) chord_two.invalidate();
        if(chord_three != null) chord_three.invalidate();
        if(chord_four != null) chord_four.invalidate();
    }

    @Override
    public void onUpdateExtractedText(int token, ExtractedText text) {
        ExtractEditText editText = root_view.findViewById(R.id.inputExtractEditText);
        editText.setExtractedText(text);
        super.onUpdateExtractedText(token, text);
    }

    private void resetText() {
        ExtractEditText editText = root_view.findViewById(R.id.inputExtractEditText);
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