package com.dreamboat.serviceNmain;


import java.io.IOException;
import java.io.FileReader;
import java.util.Scanner;

public class textReplace {

    String FilePath = "D:\\AppFiles\\RamayanReplace.txt";
    public void readFile() throws IOException {
        Scanner sc = new Scanner(new FileReader(FilePath));
        String x = sc.nextLine();
        System.out.println(x);
        String[] a = x.split(" ");
        for(String w : a){
            System.out.println(w);
        }
        }
    public static void main(String[] args) throws IOException {
     textReplace t =new textReplace();
       t.readFile();
    }
}
