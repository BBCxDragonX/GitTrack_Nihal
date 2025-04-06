package com.dreamboat.LeetCode;

import com.dreamboat.practiceModel.ListNode;

public class AddTwoNumbers {
    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode dummyHead = new ListNode(0);
        ListNode current = dummyHead;
        int carry = 0;

        while(l1!=null || l2!=null || carry!= 0 ){
            int x = (l1!=null)?l1.val:0;
            int y = (l2!=null)?l2.val:0;

            int sum = x+y+carry;
            carry = sum/10;
            int digit = sum%10;

            current.next = new ListNode(digit);
            current = current.next;

            if(l1!=null){l1=l1.next;}
            if(l2!=null){l2=l2.next;}

        }
        return dummyHead.next;
    }

    public static void main(String[] args){
        ListNode l1 = new ListNode(4,new ListNode(5,new ListNode(4)));
        ListNode l2 = new ListNode(3,new ListNode(7,new ListNode(4)));
        ListNode head = addTwoNumbers(l1,l2);
        while (head != null) {
            System.out.print(head.val);
            if (head.next != null) {
                System.out.print(" -> ");
            }
            head = head.next;
        }
    }
}





