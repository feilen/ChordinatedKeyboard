package info.tuxcat.chordinated;

import java.util.ArrayList;

class HuffmanNode {
    String resultString = "";
    String displayString = "";
    double frequency = 0.0;
    ArrayList<HuffmanNode> children = new ArrayList<>();
    HuffmanNode(ArrayList<HuffmanNode> input)
    {
        for(HuffmanNode inputNode: input)
        {
            frequency += inputNode.frequency;
            displayString += inputNode.displayString;
        }
        // Hack, because the default encoding produces a label containing the word 'cum',
        // and like twenty people have pointed that out to me.
        if(displayString.length() >= 2 && displayString.substring(0, 2).equals("cu"))
        {
            displayString = displayString.replace("cu", "uc");
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
