package info.tuxcat.feilen.chorded;

import android.support.annotation.NonNull;

import java.util.ArrayList;

class HuffmanNode {
    String resultString = "";
    String displayString = "";
    double frequency = 0.0;
    @NonNull
    ArrayList<HuffmanNode> children = new ArrayList<>();
    HuffmanNode(ArrayList<HuffmanNode> input)
    {
        for(HuffmanNode inputNode: input)
        {
            frequency += inputNode.frequency;
            displayString += inputNode.displayString;
        }
        children = input;
    }
    HuffmanNode(){}
    HuffmanNode(String a, double b, String ds) {
        resultString = a;
        displayString = ds;
        frequency = b;
    }
    HuffmanNode(String a, double b)
    {
        this(a, b, a);
    }
}
