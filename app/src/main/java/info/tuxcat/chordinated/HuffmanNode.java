// Represents a single node in the Huffman tree.
// Copyright (C) 2021 Chelsea Jaggi

// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.

// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
// more details.

// You should have received a copy of the GNU General Public License along with
// this program. If not, see https://www.gnu.org/licenses/.

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
