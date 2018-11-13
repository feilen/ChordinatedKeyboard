package info.tuxcat.feilen.chorded;

import java.util.ArrayList;

public class HuffmanNode {
    String resultString = "";
    String displayString = "";
    String allchars = "";
    double frequency = 0.0;
    ArrayList<HuffmanNode> children = new ArrayList<HuffmanNode>();
    HuffmanNode(ArrayList<HuffmanNode> input)
    {
        for(int i = input.size() - 1; i >=0; --i)
        {
            frequency += input.get(i).frequency;
            allchars += input.get(i).allchars;
        }
        children = input;
    }
    HuffmanNode(){}
    HuffmanNode(String a, double b, String ds) {
        resultString = a;
        displayString = ds;
        frequency = b;
        allchars = ds;
    }
    HuffmanNode(String a, double b)
    {
        this(a, b, a);
    }
}
