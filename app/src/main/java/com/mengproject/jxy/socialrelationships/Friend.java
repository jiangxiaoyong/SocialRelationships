package com.mengproject.jxy.socialrelationships;

/**
 * Created by jxy on 27/01/15.
 */
public class Friend {

    public String name;
    public Double relativity;

    public Friend(String name, Number relativity) {
        this.name = name;
        this.relativity = relativity.doubleValue();
    }
}
