package info.tuxcat.feilen.chorded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by feilen on 10/11/18.
 */

public class HuffmanTree {
    HuffmanNode root;
    private final int factor;

    HuffmanTree(int factor)
    {
        this.factor = factor;
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

