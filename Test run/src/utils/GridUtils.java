package utils;

import static constants.GridConstants.*;

public class GridUtils {
    public static boolean isValidCell(int row, int col) {
        return row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;
    }

    public static int getMovePriority(int currentRow, int currentCol, byte direction) {
        int newRow = currentRow + DIRECTIONS[direction][0];
        int newCol = currentCol + DIRECTIONS[direction][1];

        int distanceToEnd = Math.abs(newRow - (GRID_SIZE - 1)) + Math.abs(newCol);

        int directionBonus = 0;
        if (DIRECTION_CHARS[direction] == 'D') directionBonus -= 2;
        if (DIRECTION_CHARS[direction] == 'L') directionBonus -= 1;

        return distanceToEnd + directionBonus;
    }

    public static char getConstraint(String pathConstraints, int index) {
        return index < pathConstraints.length() ? pathConstraints.charAt(index) : '*';
    }
} 