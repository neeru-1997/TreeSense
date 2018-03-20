package com.treesense;

class Coord
{
    double latitude, longitude;
    Coord(double latitude, double longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    Coord()
    {
        latitude = 0.0;
        longitude = 0.0;
    }
}