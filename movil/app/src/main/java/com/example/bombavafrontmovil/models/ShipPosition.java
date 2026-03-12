package com.example.bombavafrontmovil.models;
import com.google.gson.annotations.SerializedName;

public class ShipPosition {
    @SerializedName("userShipId")
    private String userShipId;
    @SerializedName("position")
    private Position position;
    @SerializedName("orientation")
    private String orientation; // "N" (Norte/Vertical) o "E" (Este/Horizontal)

    public ShipPosition(String userShipId, Position position, String orientation) {
        this.userShipId = userShipId;
        this.position = position;
        this.orientation = orientation;
    }

    public String getUserShipId() { return userShipId; }
    public Position getPosition() { return position; }
    public String getOrientation() { return orientation; }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }
}