package com.dreamboat.LeetCode;

import java.util.Iterator;
import java.util.TreeSet;

public class TreeSetTest {
    public void addColors(TreeSet<String> colors){
        colors.add("red");
        colors.add("blue");
        colors.add("white");
        colors.add("yellow");
        colors.add("black");
    }
    public void iterateTreeSet(TreeSet<String> colors){
        Iterator it = colors.descendingIterator();
        while(it.hasNext()){
            System.out.print(it.next()+" ");
        }
        System.out.println();
    }
    public void getFirstandLastElementandSize(TreeSet<String> ts){
        System.out.println(ts.first());
        System.out.println(ts.last());
        System.out.println(ts.size());
    }
    public static void main(String[] args){
        TreeSetTest tst = new TreeSetTest();
        TreeSet<String> ts = new TreeSet<>();

        tst.addColors(ts);
        tst.iterateTreeSet(ts);
        tst.getFirstandLastElementandSize(ts);
    }
}
