package grid;

import static constants.GridConstants.*;
import static utils.GridUtils.*;

public class MovesManager {
    private static final byte[][][] VALID_MOVES = new byte[GRID_SIZE][GRID_SIZE][];
    private static final byte[][][] PRIORITIZED_MOVES = new byte[GRID_SIZE][GRID_SIZE][];

    static {
        initializeValidMoves();
        initializePrioritizedMoves();
    }

    private static void initializeValidMoves() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int validCount = 0;
                byte[] temp = new byte[4];

                for (byte dir = 0; dir < DIRECTIONS.length; dir++) {
                    int newRow = row + DIRECTIONS[dir][0];
                    int newCol = col + DIRECTIONS[dir][1];
                    if (isValidCell(newRow, newCol)) {
                        temp[validCount++] = dir;
                    }
                }

                VALID_MOVES[row][col] = new byte[validCount];
                System.arraycopy(temp, 0, VALID_MOVES[row][col], 0, validCount);
            }
        }
    }

    private static void initializePrioritizedMoves() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                byte[] validMoves = VALID_MOVES[row][col];
                byte[] prioritized = new byte[validMoves.length];
                System.arraycopy(validMoves, 0, prioritized, 0, validMoves.length);

                for (int i = 0; i < prioritized.length - 1; i++) {
                    for (int j = 0; j < prioritized.length - i - 1; j++) {
                        if (getMovePriority(row, col, prioritized[j]) >
                                getMovePriority(row, col, prioritized[j + 1])) {
                            byte temp = prioritized[j];
                            prioritized[j] = prioritized[j + 1];
                            prioritized[j + 1] = temp;
                        }
                    }
                }
                PRIORITIZED_MOVES[row][col] = prioritized;
            }
        }
    }

    public static byte[] getPrioritizedMoves(int row, int col) {
        return PRIORITIZED_MOVES[row][col];
    }
} 