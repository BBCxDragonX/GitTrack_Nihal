package com.dreamboat.serviceNmain;

import com.dreamboat.practiceModel.Car;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class CarService {
    Car car = new Car();
    public void captureSpeed(){
        CarService cserv = new CarService();
        System.out.println(cserv.randomSpeedGenerator());
    }
    public int randomSpeedGenerator(){
        Random rndm = new Random();
        int x = rndm.nextInt(100);;
        car.setSpeed(x);
        return car.getSpeed() ;
    }

    public String runFlag(){
        Scanner sc = new Scanner(System.in);
        return sc.next();
    }
    public String Timer(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer complete");
            }
        },5000);
        return "noUse";
    }
}
