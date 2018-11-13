package info.tuxcat.feilen.chorded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by feilen on 10/11/18.
 */

public class HuffmanTree {
    HuffmanNode root;
    int factor;

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

    public static void main(String[] args)
    {
        HuffmanTree tree = new HuffmanTree(7);
        tree.run();
    }

    public void run() {
        ArrayList<HuffmanNode> inp = new ArrayList<HuffmanNode>();
        inp.add(new HuffmanNode("e", 12.702));
        inp.add(new HuffmanNode("t", 9.056));
        inp.add(new HuffmanNode("a", 8.167));
        inp.add(new HuffmanNode("o", 7.507));
        inp.add(new HuffmanNode("i", 6.966));
        inp.add(new HuffmanNode("n", 6.749));
        inp.add(new HuffmanNode("s", 6.327));
        inp.add(new HuffmanNode("h", 6.094));
        inp.add(new HuffmanNode("r", 5.987));
        inp.add(new HuffmanNode("d", 4.253));
        inp.add(new HuffmanNode("l", 4.025));
        inp.add(new HuffmanNode("c", 2.782));
        inp.add(new HuffmanNode("u", 2.758));
        inp.add(new HuffmanNode("m", 2.406));
        inp.add(new HuffmanNode("w", 2.360));
        inp.add(new HuffmanNode("f", 2.228));
        inp.add(new HuffmanNode("g", 2.015));
        inp.add(new HuffmanNode("y", 1.974));
        inp.add(new HuffmanNode("p", 1.929));
        inp.add(new HuffmanNode("b", 1.492));
        inp.add(new HuffmanNode("v", 0.978));
        inp.add(new HuffmanNode("k", 0.772));
        inp.add(new HuffmanNode("j", 0.153));
        inp.add(new HuffmanNode("x", 0.150));
        inp.add(new HuffmanNode("q", 0.095));
        inp.add(new HuffmanNode("z", 0.074));
        this.CreateEncoding(inp);
        //this.PrintEncoding();
    }
}

