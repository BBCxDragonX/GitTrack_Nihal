package com.dreamboat.LeetCode;

import com.dreamboat.serviceNmain.AlphabeticalComparator;

import java.util.*;

public class addColors {
    ArrayList<String> colors = new ArrayList<>();
    int Length=5;
    Scanner sc = new Scanner(System.in);
    public ArrayList<String> addnewColors(){
        for(int i =0;i<Length;i++){
            colors.add(sc.nextLine());
        }
        return colors;
    }
    public List<String> insertAtFirst(List<String> insertFirstList){
       insertFirstList.add(0,"grey");
        return insertFirstList;
    }
    public void removeThirdElement(ArrayList<String> al){
        al.remove(3);
    }
    public String searchElement(ArrayList<String> al,String str){
       for(String color : al){
            if(color.equals(str)){
                System.out.print("your element is found at: ");
                return String.valueOf(al.indexOf(color));
            }
        }
        return "not found";
    }
    public void swapElements(ArrayList<String> al,int Index1,int Index2){
        String temp = al.get(Index1);
        al.set(Index1, al.get(Index2));
        al.set(Index2, temp);
    }
    public static void main(String[]args){
        addColors ac = new addColors();
        ArrayList<String> outputList = ac.addnewColors();
        ac.insertAtFirst(outputList);
        ac.removeThirdElement(outputList);
        System.out.println("The double operated List is  "+outputList);
        System.out.println(ac.searchElement(outputList,"red"));
        Collections.shuffle(outputList);
        System.out.println("The shuffled list is "+outputList);
        Collections.reverse(outputList);
        outputList.sort(new AlphabeticalComparator().reversed());
        System.out.println("The reversed list is "+outputList);
        ac.swapElements(outputList,0,2);
        System.out.println("The list looks like this after swap operation: "+outputList);
        System.out.println("The Extracted sublist is "+outputList.subList(1,3));
    }
}
