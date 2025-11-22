package pathfinding;

import pathfinding.PathFinderTask;
import static constants.GridConstants.*;
import static utils.GridUtils.*;

import java.util.Scanner;

public class Main {
    private static volatile long totalPaths = 0;
    
    public static void main(String[] args) {
        userInterface();
    }

    private static void userInterface() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter path constraints (63 characters of U/D/L/R/* or press Enter for no constraints):");
        String pathConstraints = scanner.nextLine().trim();

        final int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
        final int totalMoves = calculateTotalMoves();

        final int[][] moves = new int[totalMoves][4];
        populateMoves(moves);

        final int numThreads = Math.min(totalMoves, maxThreads);
        Thread[] workers = new Thread[numThreads];

        long startTime = System.currentTimeMillis();
        totalPaths = 0;

        executePathFinding(numThreads, totalMoves, moves, workers, pathConstraints, startTime);

        long endTime = System.currentTimeMillis();
        System.out.println("Input: " + pathConstraints);
        System.out.println("Total paths found: " + totalPaths);
        System.out.println("Time taken: " + (endTime - startTime) + " (ms)");
        
        scanner.close();
    }

    private static void executePathFinding(int numThreads, int totalMoves, int[][] moves, 
                                         Thread[] workers, String pathConstraints, long startTime) {
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            workers[i] = new Thread(() -> {
                for (int j = threadIndex; j < totalMoves; j += numThreads) {
                    PathFinderTask task = new PathFinderTask(
                            threadIndex,
                            moves[j][0], moves[j][1],
                            moves[j][2], moves[j][3],
                            pathConstraints,
                            startTime
                    );
                    task.start();
                    try {
                        task.join();
                        synchronized(Main.class) {
                            totalPaths += task.getPathCount();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            workers[i].start();
        }

        try {
            for (Thread worker : workers) {
                worker.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Search interrupted!");
        }
    }

    private static int calculateTotalMoves() {
        int count = 0;
        for (int[] direction1 : DIRECTIONS) {
            int firstRow = direction1[0];
            int firstCol = direction1[1];

            if (isValidCell(firstRow, firstCol)) {
                for (int[] direction2 : DIRECTIONS) {
                    int secondRow = firstRow + direction2[0];
                    int secondCol = firstCol + direction2[1];

                    if (isValidCell(secondRow, secondCol) &&
                            (secondRow != 0 || secondCol != 0)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static void populateMoves(int[][] moves) {
        int moveIndex = 0;
        for (int[] direction1 : DIRECTIONS) {
            int firstRow = direction1[0];
            int firstCol = direction1[1];

            if (isValidCell(firstRow, firstCol)) {
                for (int[] direction2 : DIRECTIONS) {
                    int secondRow = firstRow + direction2[0];
                    int secondCol = firstCol + direction2[1];

                    if (isValidCell(secondRow, secondCol) &&
                            (secondRow != 0 || secondCol != 0)) {
                        moves[moveIndex][0] = firstRow;
                        moves[moveIndex][1] = firstCol;
                        moves[moveIndex][2] = secondRow;
                        moves[moveIndex][3] = secondCol;
                        moveIndex++;
                    }
                }
            }
        }
    }
}