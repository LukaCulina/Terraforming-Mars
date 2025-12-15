package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.model.Tile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class HexGridUtils {

    private HexGridUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final int[] HEXES_IN_ROW = {5, 6, 7, 8, 9, 8, 7, 6, 5};

    public static List<Tile> getAdjacentTiles(Tile centerTile, List<Tile> allTiles) {
        Set<Tile> adjacent = new HashSet<>();
        int r = centerTile.getRow();
        int c = centerTile.getCol();

        getTileAt(r, c - 1, allTiles).ifPresent(adjacent::add);
        getTileAt(r, c + 1, allTiles).ifPresent(adjacent::add);

        int[] rowsToCheck = {r - 1, r + 1};
        for (int nextR : rowsToCheck) {
            if (nextR >= 0 && nextR < HEXES_IN_ROW.length) {
                double colShift = (HEXES_IN_ROW[r] - HEXES_IN_ROW[nextR]) / 2.0;

                double correspondingCol = c - colShift;

                int c1 = (int) Math.floor(correspondingCol);
                int c2 = (int) Math.ceil(correspondingCol);

                getTileAt(nextR, c1, allTiles).ifPresent(adjacent::add);
                if (c1 != c2) {
                    getTileAt(nextR, c2, allTiles).ifPresent(adjacent::add);
                }
            }
        }
        return new ArrayList<>(adjacent);
    }

    public static Optional<Tile> getTileAt(int row, int col, List<Tile> allTiles) {
        if (row < 0 || row >= HEXES_IN_ROW.length || col < 0 || col >= HEXES_IN_ROW[row]) {
            return Optional.empty();
        }

        return allTiles.stream()
                .filter(t -> t.getRow() == row && t.getCol() == col)
                .findFirst();
    }

    public static int[] getHexesInRow() {
        return HEXES_IN_ROW;
    }
}
