package com.dreamboat.practiceModel;

import com.amazonaws.services.glue.model.Order;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class Orders {
    private Integer id;
    private List<MenuItems> Ordereditems;
    private double total;

    public Orders(Integer id,List<MenuItems> items, double total){
        this.id = id;
        this.Ordereditems = items;
        this.total = total;
    }
}
