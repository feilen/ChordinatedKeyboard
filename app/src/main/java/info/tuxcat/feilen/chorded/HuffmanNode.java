package info.tuxcat.feilen.chorded;

import java.util.ArrayList;

public class HuffmanNode {
    char thechar = '\0';
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
    HuffmanNode(char a, double b) {
        thechar = a;
        frequency = b;
        allchars = Character.toString(a);
    }
}
