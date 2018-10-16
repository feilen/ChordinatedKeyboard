package info.tuxcat.feilen.chorded;

/**
 * Created by feilen on 10/13/18.
 */

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.TWOFINGER;
import static info.tuxcat.feilen.chorded.Chorded.KeyboardType.THREEFINGER;

public class Chorded extends InputMethodService
        implements OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;

    private HuffmanTree tree;
    private HuffmanNode curNode;
    enum KeyboardType {TWOFINGER, THREEFINGER};
    int pressed;
    Lock presslock;
    KeyboardType kType = THREEFINGER;


    //int[] keylookup = {0, 1, 2};

    int[] keylookup;

    private boolean caps = false;

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        switch(kType) {
            case TWOFINGER:
                keyboard = new Keyboard(this, R.xml.chorded);
                tree = new HuffmanTree(3);
                //                     01  10  11
                keylookup = new int[]{ -1,  0,  1,  3};
                break;
            case THREEFINGER:
                keyboard = new Keyboard(this, R.xml.chorded_three);
                // ugly hack ahead, swap the space and middle
                int middleheight = 0, spaceheight = 0;
                for(Key k: keyboard.getKeys())
                {
                    switch(k.codes[0])
                    {
                        case 32:
                            spaceheight = k.height;
                            break;
                        case 49:
                            middleheight = k.height;
                            break;
                    }
                }
                for(Key k: keyboard.getKeys())
                {
                    switch(k.codes[0])
                    {
                        case 32:
                            k.y += middleheight;
                            break;
                        case 49:
                            k.y -= spaceheight;
                            break;
                    }
                }
                tree = new HuffmanTree(7);
                //                          001 010 011 100 101 110 111
                keylookup = new int[]{ -1,  0,  1,  3,  2,  5,  4,  6};
                break;
        }
        kv.setKeyboard(keyboard);
        kv.setPreviewEnabled(false);
        kv.setOnKeyboardActionListener(this);
        kv.invalidateAllKeys();
        presslock = new ReentrantLock();
        pressed = 0;

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

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        //playClick(primaryCode);
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                keyboard.setShifted(caps);
                kv.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            case 48:
            case 49:
            case 50:
                break;
            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code) && caps) {
                    code = Character.toUpperCase(code);
                }
                ic.commitText(String.valueOf(code), 1);
        }
    }

    @Override
    public void onPress(int primaryCode) {
        presslock.lock();
        switch(primaryCode)
        {
            case 48:
                pressed = pressed | 0b001;
                Log.i("Chorded", "Pressed 1");
                break;
            case 49:
                pressed = pressed | 0b010;
                Log.i("Chorded", "Pressed 2");
                break;
            case 50:
                pressed = pressed | 0b100;
                Log.i("Chorded", "Pressed 3");
                break;
        }
        presslock.unlock();
    }

    @Override
    public void onRelease(int primaryCode) {
        presslock.lock();
        Log.i("Chorded", "Pressed release");
        switch(primaryCode)
        {
            case 48:
            case 49:
            case 50:
                if(curNode.children.size() >= keylookup[pressed])
                {
                    curNode = curNode.children.get(keylookup[pressed]);
                    if(curNode.children.size() == 0)
                    {
                        // Reached the end. Commit the current letter.
                        InputConnection ic = getCurrentInputConnection();
                        ic.commitText(String.valueOf(curNode.thechar), 1);
                        curNode = tree.root;
                    } else {
                        // Presumably indicate that a button was pressed
                    }
                    pressed = 0;
                } else {
                    // Invalid coding. Reset to root.
                    pressed = 0;
                    curNode = tree.root;
                }
                break;
            default:
                pressed = 0;
                curNode = tree.root;
        }
        relabelKeys();
        presslock.unlock();
    }

    private void relabelKeys() {
        for(Key k: keyboard.getKeys())
        {
            switch(k.codes[0])
            {
                case 48:
                    if(curNode.children.size() >= 0)
                    {
                        k.label = curNode.children.get(0).allchars;
                    } else {
                        k.label = "";
                    }
                    break;
                case 49:
                    if(curNode.children.size() >= 1)
                    {
                        k.label = curNode.children.get(1).allchars;
                    } else {
                        k.label = "";
                    }
                    break;
                case 50:
                    if(curNode.children.size() >= 2)
                    {
                        k.label = curNode.children.get(2).allchars;
                    } else {
                        k.label = "";
                    }
            }
            kv.invalidateAllKeys();
        }
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {

    }


}