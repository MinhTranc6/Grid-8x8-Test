package constants;

public class GridConstants {
    public static final int GRID_SIZE = 8;
    public static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // U, D, L, R
    public static final char[] DIRECTION_CHARS = {'U', 'D', 'L', 'R'};
    public static final int TOTAL_CELLS = GRID_SIZE * GRID_SIZE;
    public static final long[] POSITION_MASKS = initializePositionMasks();

    private static long[] initializePositionMasks() {
        long[] masks = new long[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            masks[i] = 1L << i;
        }
        return masks;
    }
} 