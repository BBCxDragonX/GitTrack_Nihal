package com.dreamboat.LeetCode;

import java.util.Iterator;
import java.util.PriorityQueue;

public class PriorityQueueTest {
    public void addNumbers(PriorityQueue<Integer> pq){
        pq.add(69);
        pq.add(169);
        pq.add(1);
        pq.add(56);
        pq.add(269);
    }
    public void iteratePQ(PriorityQueue<Integer> pq){
        Iterator it = pq.iterator();
        while(it.hasNext()){
            System.out.print(it.next()+" ");
        }
    }
    public static void main(String[] args){
        PriorityQueueTest pqt = new PriorityQueueTest();
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pqt.addNumbers(pq);
        pqt.iteratePQ(pq);
    }
}
