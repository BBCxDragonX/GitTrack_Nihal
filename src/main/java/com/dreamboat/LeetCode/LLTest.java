package com.dreamboat.LeetCode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LLTest {
    public void addLastElement(LinkedList<String> ll,String element){
        ll.addLast(element);
    }
    public void iterateList(LinkedList<String> ll){
        Iterator it = ll.descendingIterator();
        while(it.hasNext()){
            System.out.print(it.next() + " ");
        }
        System.out.println();
    }
    public void iterateUsingIndex(LinkedList<String> ll,int index1, int index2){
       for(String lil : ll.subList(index1,index2)){
           System.out.print(" "+lil);
        }
    }
    public static void main(String[] args){
        LLTest llt = new LLTest();
        LinkedList<String> list = new LinkedList<>();
        list.add("Mumbai");
        list.add("Madras");
        list.add("Madurai");
        list.add("Marmagoa");
        llt.addLastElement(list,"Hyderabad");
        llt.iterateList(list);
        llt.iterateUsingIndex(list,1,4);
    }
}
