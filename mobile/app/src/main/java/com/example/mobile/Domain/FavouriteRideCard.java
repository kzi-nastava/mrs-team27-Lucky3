package com.example.mobile.Domain;

public class FavouriteRideCard {
    private final String start;
    private final String end;

    public FavouriteRideCard(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }
}
