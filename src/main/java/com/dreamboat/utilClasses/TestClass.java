package com.dreamboat.utilClasses;

public class TestClass {
    // Volatile flag to control the loop
    private static volatile boolean keepRunning = true;

    public static void main(String[] args) {
        // Start the endless loop in a separate thread
        Thread loopThread = new Thread(() -> {
            while (keepRunning) {
                try {
                    System.out.println("Loop is running...");
                    Thread.sleep(1000); // Simulate work with a delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread interrupted.");
                }
            }
            System.out.println("Loop stopped.");
        });

        loopThread.start();

        // Simulate external input to stop the loop
        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            System.out.println("Press Enter to stop the loop...");
            scanner.nextLine(); // Wait for user input
        }

        // Change the flag to stop the loop
        keepRunning = false;

        try {
            loopThread.join(); // Wait for the loop thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Program terminated.");
    }
}

