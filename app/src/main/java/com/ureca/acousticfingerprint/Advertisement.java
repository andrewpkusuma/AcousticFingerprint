package com.ureca.acousticfingerprint;

/**
 * Created by Andrew on 11/17/16.
 */

public class Advertisement {
    String name;
    //String summary;
    String details;
    String link;
    int imageID;
    Advertisement(String name, /*String summary, */String details, String link, int imageID) {
        this.name = name;
        //this.summary = summary;
        this.details = details;
        this.link = link;
        this.imageID = imageID;
    }
}
