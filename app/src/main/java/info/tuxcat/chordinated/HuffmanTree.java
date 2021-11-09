// Builds a Huffman coding tree from the given nodes, and allows traversal.
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
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by feilen on 10/11/18.
 */

class HuffmanTree {
    HuffmanNode root;
    private final int factor;

    HuffmanTree(int factor)
    {
        this.factor = factor;
    }

    public int getFactor()
    {
        return factor;
    }

    
    public void CreateEncoding(ArrayList<HuffmanNode> nodes)
    {
        // figure out the size of a full tree
        int nodecnt = factor;
        while(nodecnt < nodes.size())
        {
            nodecnt *= factor;
        }

        // extend base nodes to fill empty nodes into slots
        nodes.ensureCapacity(nodecnt);
        while(nodes.size() < nodecnt)
        {
            nodes.add(new HuffmanNode());
        }

        // sort entire list descending
        Collections.sort(nodes, new Comparator<HuffmanNode>() {
            @Override
            public int compare(HuffmanNode huffmanNode, HuffmanNode t1) {
                if(huffmanNode.frequency > t1.frequency)
                {
                    return -1;
                } else if (huffmanNode.frequency < t1.frequency)
                {
                    return 1;
                }
                return 0;
            }
        });

        // reduce by factor of <factor> until one is left
        while(nodes.size() > 1)
        {
            // Create a new node from the least <factor> children
            HuffmanNode newnode = new HuffmanNode(new ArrayList<>(nodes.subList(nodes.size() - factor, nodes.size())));
            // Delete the least <factor> children
            nodes.subList(nodes.size() - factor, nodes.size()).clear();
            // Insert the new node in-order
            boolean found = false;
            for(int i = 0; i < nodes.size(); i++)
            {
                if(nodes.get(i).frequency < newnode.frequency)
                {
                    nodes.add(i, newnode);
                    found = true;
                    break;
                }
            }
            if(!found)
            {
                nodes.add(newnode);
            }
        }

        root = nodes.get(0);
    }
}

