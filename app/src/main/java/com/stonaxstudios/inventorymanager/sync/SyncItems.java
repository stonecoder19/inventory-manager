package com.stonaxstudios.inventorymanager.sync;

import com.stonaxstudios.inventorymanager.rest.RestItems;
import com.stonaxstudios.inventorymanager.models.Item;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Matthew on 6/17/2016.
 */
public class SyncItems  {
    String url = "";

    public SyncItems(String url)
    {
        this.url = url;
    }

    public boolean syncItems() throws Exception
    {
        RestItems restItems = new RestItems(url);

        JSONObject jsonItems = restItems.getItems();

        if(jsonItems == null){
            return false;
        }




        int success = jsonItems.getInt("success");

        if(success == 0)
            return false;

        JSONArray items = jsonItems.getJSONArray("items");

        Item.deleteAll(Item.class); //delete items in database to avoid duplicates

        for(int i=0;i<items.length();i++)
        {
            Item itemObj = new Item();
            JSONObject item = items.getJSONObject(i);

            itemObj.setName(item.getString("Name"));
            itemObj.setItemNo(Integer.parseInt(item.getString("ItemNo")));
            itemObj.setQuantity(Integer.parseInt(item.getString("Quantity")));


            itemObj.save();  //create items and save to database


        }

        return true;

    }
}
