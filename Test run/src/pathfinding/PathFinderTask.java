package pathfinding;

import grid.MovesManager;
import static constants.GridConstants.*;
import static utils.GridUtils.*;

public class PathFinderTask extends Thread {
    private final int threadId;
    private final int startRow;
    private final int startCol;
    private final int secondRow;
    private final int secondCol;
    private long pathCount = 0;
    private final long[] localVisited;
    private final String pathConstraints;
    private final long startTime;
    private volatile boolean isRunning;

    public PathFinderTask(int id, int startRow, int startCol, int secondRow, int secondCol, 
                         String pathConstraints, long startTime) {
        this.threadId = id;
        this.startRow = startRow;
        this.startCol = startCol;
        this.secondRow = secondRow;
        this.secondCol = secondCol;
        this.localVisited = new long[GRID_SIZE];
        this.pathConstraints = pathConstraints;
        this.startTime = startTime;
        this.isRunning = true;
    }

    @Override
    public void run() {
        System.out.println("Thread " + threadId + " starting at positions (" +
                startRow + "," + startCol + ") -> (" + secondRow + "," + secondCol + ")");

        long threadStartTime = System.currentTimeMillis();

        setLocalVisited(0, 0, true);
        setLocalVisited(startRow, startCol, true);
        setLocalVisited(secondRow, secondCol, true);

        findPathsParallel(3, secondRow, secondCol);

        long threadEndTime = System.currentTimeMillis();
        System.out.println("Thread " + threadId + " completed in " +
                (threadEndTime - threadStartTime) / 1000.0 + "s. Found " + pathCount + " paths");
    }

    public long getPathCount() {
        return pathCount;
    }

    public void stopRunning() {
        isRunning = false;
    }

    private void setLocalVisited(int row, int col, boolean value) {
        if (value) {
            localVisited[row] |= POSITION_MASKS[col];
        } else {
            localVisited[row] &= ~POSITION_MASKS[col];
        }
    }

    private boolean isLocalCellVisited(int row, int col) {
        return (localVisited[row] & POSITION_MASKS[col]) != 0;
    }

    private void findPathsParallel(int steps, int row, int col) {
        if (!isRunning) return;

        if (steps == TOTAL_CELLS) {
            if (row == GRID_SIZE - 1 && col == 0) {
                pathCount++;
                if (pathCount % 100000 == 0) {
                    System.out.println("Thread " + threadId + " found " + pathCount + " paths");
                    checkAndPrintTime();
                }
            }
            return;
        }

        if (!isPathViableParallel(steps, row, col)) {
            return;
        }

        for (byte dirIndex : MovesManager.getPrioritizedMoves(row, col)) {
            int newRow = row + DIRECTIONS[dirIndex][0];
            int newCol = col + DIRECTIONS[dirIndex][1];

            if (!isLocalCellVisited(newRow, newCol)) {
                char dirChar = DIRECTION_CHARS[dirIndex];
                char allowedMove = getConstraint(pathConstraints, steps - 1);

                if (allowedMove == '*' || allowedMove == dirChar) {
                    setLocalVisited(newRow, newCol, true);
                    findPathsParallel(steps + 1, newRow, newCol);
                    setLocalVisited(newRow, newCol, false);
                }
            }
        }
    }

    private boolean isPathViableParallel(int steps, int row, int col) {
        if (row == GRID_SIZE - 1 && col == 0 && steps < TOTAL_CELLS) {
            return false;
        }

        int remainingMoves = TOTAL_CELLS - steps;
        int distanceToEnd = Math.abs(row - (GRID_SIZE - 1)) + Math.abs(col);
        if (distanceToEnd > remainingMoves) {
            return false;
        }

        return !hasIsolatedCellsParallel();
    }

    private boolean hasIsolatedCellsParallel() {
        int unvisitedCount = countUnvisitedCellsParallel();
        if (unvisitedCount == 0) return false;

        long[] tempVisited = new long[GRID_SIZE];
        int connectedCells = optimizedFloodFillParallel(tempVisited,
                findUnvisitedRowParallel(), findUnvisitedColParallel(), unvisitedCount);
        return connectedCells != unvisitedCount;
    }

    private int countUnvisitedCellsParallel() {
        int count = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (!isLocalCellVisited(row, col)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int findUnvisitedRowParallel() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (!isLocalCellVisited(row, col)) {
                    return row;
                }
            }
        }
        return -1;
    }

    private int findUnvisitedColParallel() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (!isLocalCellVisited(row, col)) {
                    return col;
                }
            }
        }
        return -1;
    }

    private int optimizedFloodFillParallel(long[] tempVisited, int row, int col, int unvisitedCount) {
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE ||
                isLocalCellVisited(row, col) || (tempVisited[row] & POSITION_MASKS[col]) != 0) {
            return 0;
        }

        tempVisited[row] |= POSITION_MASKS[col];
        int count = 1;

        for (int[] dir : DIRECTIONS) {
            count += optimizedFloodFillParallel(tempVisited, row + dir[0], col + dir[1], unvisitedCount);
            if (count == unvisitedCount) break;
        }

        return count;
    }

    private void checkAndPrintTime() {
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - startTime) / 1000;

        if (elapsedSeconds > 0 && elapsedSeconds % 10 == 0) {
            System.out.println("Thread " + threadId + " - Time elapsed: " + elapsedSeconds + "s");
        }
    }
} 