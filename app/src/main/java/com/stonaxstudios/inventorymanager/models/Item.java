package com.stonaxstudios.inventorymanager.models;

import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;

/**
 * Created by Matthew on 6/17/2016.
 */
public class Item extends SugarRecord {

    @SerializedName("ItemNo")
    int itemNo;

    @SerializedName("Name")
    String Name;

    @SerializedName("Quantity")
    int Quantity;

    public int getItemNo() {
        return itemNo;
    }

    public void setItemNo(int itemNo) {
        this.itemNo = itemNo;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getQuantity() {
        return Quantity;
    }

    public void setQuantity(int quantity) {
        Quantity = quantity;
    }
}
