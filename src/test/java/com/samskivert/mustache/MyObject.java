package com.samskivert.mustache;

public class MyObject {
    private String foo = "this is foo value";

    public String getFoo() {
        return foo;
    }

    public String getFooValue(String length, String align) {
        return foo + "1234";
    }
}