package com.dreamboat.LeetCode;

import java.util.*;

public class HLTest {
    public void iterateSet(Set<String> hs){
        for (String h : hs) {
            System.out.print(h + " ");
        }
        System.out.println();
    }
    public String[] convertToArray(HashSet<String> hs){
        String[] to_Array = new String[(hs.size())];
         hs.toArray(to_Array);
        return to_Array;
    }
    public void compareHashSet(HashSet<String> hs1,HashSet<String> hs2){
        for(String element : hs1){
            System.out.println("Does hs2 have "+element+" from hs1? "+ (hs2.contains(element)? "Yes, only "+element+" will be retained" : "No"));
        }
        hs1.retainAll(hs2);
    }
    public static void main(String[] args){

        HLTest hlt = new HLTest();

        HashSet<String> hs = new HashSet<>();
        HashSet<String> hs1 = new HashSet<>();
        TreeSet<String> hset;


        hs.add("Nihal");
        hs.add("DragonX");
        hs.add("CapAmerica");
        hs.add("Superman");

        hs1.add("Nihal");
        hs1.add("Superman");

        TreeSet<String> ts = new TreeSet<>(hs);

        hlt.iterateSet(hs);
        hlt.iterateSet(ts);
        hlt.compareHashSet(hs,hs1);
        hset = (TreeSet<String>) ts.clone();

        System.out.println(hset);
        System.out.println(Arrays.toString(hlt.convertToArray(hs)));
    }
}
