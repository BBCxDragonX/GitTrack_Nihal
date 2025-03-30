package com.dreamboat.LeetCode;

import java.util.HashMap;
import java.util.Map;

public class TwoSum {
    public int[] TwoSum(int[] nums, int target) {
        Map<Integer, Integer> numMap = new HashMap<>();
        int n = nums.length;

        for (int i = 0; i < n; i++) {
            numMap.put(nums[i], i);
        }
        return null;
    }
}
