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
        int nodecnt = factor;
        while(nodecnt < nodes.size())
        {
            nodecnt *= factor;
        }

        // extend base nodes to fill empty nodes into slots
        while(nodes.size() < nodecnt)
        {
            nodes.add(new HuffmanNode());
        }

        // reduce by factor of <factor> until one is left
        while(nodes.size() > 1)
        {
            Collections.sort(nodes, new Comparator<HuffmanNode>() {
                @Override
                public int compare(HuffmanNode huffmanNode, HuffmanNode t1) {
                    if(huffmanNode.frequency < t1.frequency)
                    {
                        return -1;
                    } else if (huffmanNode.frequency > t1.frequency)
                    {
                        return 1;
                    }
                    return 0;
                }
            });

            HuffmanNode newnode = new HuffmanNode(new ArrayList<HuffmanNode>(nodes.subList(0, factor)));
            Collections.sort(newnode.children, new Comparator<HuffmanNode>() {
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
            nodes.subList(0, factor).clear();
            nodes.add(newnode);
        }

        root = nodes.get(0);
    }
}

