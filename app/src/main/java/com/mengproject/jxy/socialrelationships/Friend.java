package com.mengproject.jxy.socialrelationships;

/**
 * Created by jxy on 27/01/15.
 */
public class Friend {

    public String name;
    public Double relativity;
    public String url;

    public Friend(String name, Number relativity, String url) {
        this.name = name;
        this.relativity = relativity.doubleValue();
        this.url = url;
    }
}
