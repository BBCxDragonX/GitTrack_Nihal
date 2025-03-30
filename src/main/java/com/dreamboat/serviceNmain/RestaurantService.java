package com.dreamboat.serviceNmain;

import com.dreamboat.practiceModel.MenuItems;
import com.dreamboat.practiceModel.Orders;
import com.dreamboat.practiceModel.RestaurantManager;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.util.*;

public class RestaurantService {
    private final List<MenuItems> menuItems;  // Use final for immutable data
    private Double totalBill;

    public RestaurantService() {
        this.menuItems = this.addItems(); // Initialize in constructor for single instance
    }

    public String toOrdersString(Orders orders) {
        return "order id: " + orders.getId() + "\n" + "SelectedMenuList: "+"\n"+ListOfMenuItems(orders.getOrdereditems()) + "\n" +"TotalBill: "+ orders.getTotal();
    }

    public String ListOfMenuItems(List<MenuItems> litems){
        String loitems = "";
        for(MenuItems mitems : litems){
            loitems = "\n" + toMenuItems(mitems);
        }
        return loitems;
    }

    public String toMenuItems(MenuItems menutems) {
        return "ItemName: " + menutems.getName() + "\nDescription: " + menutems.getDescription() + "\nPrice: " + menutems.getPrice();
    }

    private List<MenuItems> addItems() {
        List<MenuItems> menu = new ArrayList<>(); // Create list locally to avoid redundant instantiation
        menu.add(new MenuItems(1,"Chicken Biryani",200.0,"spicy chicken boneless piece with BASMATI rice"));
        menu.add(new MenuItems(2,"Chicken Mandi",200.0,"Juicy Al-Fahm piece with BASMATI rice"));
        menu.add(new MenuItems(3,"Chicken Frankie",90.0,"Butter chicken boneless pieces with cheese rolled between indian bread"));
        menu.add(new MenuItems(4,"Chicken Spicy Roll",60.0,"spicy chicken boneless strips dry between bun"));
        menu.add(new MenuItems(5,"Chicken Home made andhra Curry",90.0,"spicy chicken curry with homemade recipe"));
        menu.add(new MenuItems(6,"Egg 65",180.0,"Our most special item, which has the crunchy,spicy taste"));
        return menu;
    }

    public int chooseOptionFromMenu() {
        System.out.println("Enter the number beside your selected item to add it to the order or enter '0' ");
        for (MenuItems menutems : menuItems) {
            System.out.printf("%d,%s,price=%.1f,description=%s\n", menutems.getId(), menutems.getName(), menutems.getPrice(), menutems.getDescription());
        }
        Scanner id = new Scanner(System.in);
        return id.nextInt();
    }

    public MenuItems getMenuItemwithId() {
        int id = chooseOptionFromMenu();
        for (MenuItems menutems : menuItems) {
            if (menutems.getId() == id) {
                return menutems;
            }
        }
        return null;
    }

    public String writeItemToOrder() {
        List<MenuItems> ordditems = new ArrayList<>();
        Orders order = new Orders(1,ordditems,500);
        order.setId(1);
        boolean status = true;
        while(status){
            if(null == getMenuItemwithId()){
                System.out.println("You have not chosen anything");
            }
            else{
                ordditems.add(getMenuItemwithId());
                order.setOrdereditems(ordditems);
                System.out.println("Would you like to continue?\nIf Yes press 'anything else' or else press 'exit'");
                Scanner ch = new Scanner(System.in);
                if(ch.nextLine().equalsIgnoreCase("exit")){
                    status = false;
                }
                else{
                    writeItemToOrder();
                }
            }

        }return toOrdersString(order);
    }

    public static void main(String[] args) {
        RestaurantService rs = new RestaurantService();
        System.out.println(rs.writeItemToOrder());
    }
}