package com.dreamboat.utilClasses;

import com.dreamboat.serviceNmain.CarService;

public class Speedometer {
   private static volatile boolean keepRunning = true;

    public static void main(String[] args) {
        // Start the endless loop in a separate thread
        System.out.println("Press Enter to stop the loop...");
        CarService cserv = new CarService();
        Thread loopThread = new Thread(() -> {
            while (keepRunning) {
                try {
                    cserv.captureSpeed();
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
