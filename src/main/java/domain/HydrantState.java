package domain;

import java.util.Objects;

/**
 * Immutable CLIPS-reported state of a single hydrant instance. Contains both the total number of connection slots and
 * how many of them are currently free.
 */
public record HydrantState(HydrantOutlets hydrant, int totalOutlets, int currentFree) {

    public HydrantState(HydrantOutlets hydrant, int totalOutlets, int currentFree) {
        this.hydrant = Objects.requireNonNull(hydrant, "hydrant must not be null");
        if (totalOutlets < 0)
            throw new IllegalArgumentException("totalOutlets cannot be negative: " + totalOutlets);

        if (currentFree < 0)
            throw new IllegalArgumentException("currentFree cannot be negative: " + currentFree);

        if (currentFree > totalOutlets)
            throw new IllegalArgumentException("currentFree cannot be greater than totalOutlets");

        this.totalOutlets = totalOutlets;
        this.currentFree = currentFree;
    }

    public boolean hasFreeConnection() {
        return currentFree > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HydrantState that)) return false;
        return totalOutlets == that.totalOutlets
                && currentFree == that.currentFree
                && Objects.equals(hydrant, that.hydrant);
    }

    @Override
    public String toString() {
        return hydrant + "(" + currentFree + "/" + totalOutlets + " free)";
    }
}
