package info.tuxcat.feilen.chorded;

import java.util.ArrayList;

public class HuffmanNode {
    String resultString = "";
    String displayString = "";
    double frequency = 0.0;
    ArrayList<HuffmanNode> children = new ArrayList<HuffmanNode>();
    HuffmanNode(ArrayList<HuffmanNode> input)
    {
        for(int i = input.size() - 1; i >=0; --i)
        {
            frequency += input.get(i).frequency;
            displayString += input.get(i).displayString;
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
