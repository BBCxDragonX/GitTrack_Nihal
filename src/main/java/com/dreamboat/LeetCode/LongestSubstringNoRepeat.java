package com.dreamboat.LeetCode;

import java.util.*;

public class LongestSubstringNoRepeat {
    public static void main(String[] args){
        String s = "abcabcbb";
        System.out.println(getLongestSubstring(s));
    }
    public static Integer getLongestSubstring(String str){
        int maxLength = 0;int left = 0;
        HashSet<Character> stringSet = new HashSet<>();
        int n = str.length();

        for(int right = 0; right<n;right++){
            if(!stringSet.contains(str.charAt(right))){
                stringSet.add(str.charAt(right));
                maxLength = Math.max(maxLength,right - left+1);
            }else{
                while(stringSet.contains(str.charAt(right))){
                    stringSet.remove(str.charAt(left));
                    left++;
                }
                stringSet.add(str.charAt(right));
            }
        }
        return maxLength;
    }
}
