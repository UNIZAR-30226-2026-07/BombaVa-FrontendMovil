package com.example.bombavafrontmovil.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FleetConfigRequest {
    @SerializedName("id")
    private String id;

    @SerializedName("deckName")
    private String deckName;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("shipIds")
    private List<ShipPosition> shipIds;

    public FleetConfigRequest(String deckName, List<ShipPosition> shipIds) {
        this.deckName = deckName;
        this.shipIds = shipIds;
    }

    public String getId() { return id; }
    public String getName() { return deckName; }
    public boolean isActive() { return isActive; } // 🔥 Añadimos su getter
    public List<ShipPosition> getShipPositions() { return shipIds; }
}