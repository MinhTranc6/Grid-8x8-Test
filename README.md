# Grid 8x8 Path Enumerator

A Java backtracking engine that counts every valid Hamiltonian path on an `8×8` grid that starts at `(0,0)`, ends at `(7,0)`, and optionally follows a user‑provided string of 63 move constraints (`U`, `D`, `L`, `R`, `*`). The emphasis of the project is on the search strategy rather than UI or ancillary tooling, so this document focuses on how the solver works.

## Puzzle Recap

- You must walk through all 64 cells exactly once (63 moves).
- Start position: top‑left corner (`S`).
- Goal position: bottom‑left corner (`E`).
- The input sequence prescribes the direction to use on each move; `*` means "free choice".
- Output is the number of paths that satisfy both the coverage requirement and the direction sequence.

The unrestricted puzzle has `8,934,966` distinct solutions. Constraining the sequence narrows the search, but the solver must still verify that each candidate path visits every cell exactly once without leaving the board.

## Core Approach

### Depth‑First Search with Backtracking

1. **Grid encoding** – Every row uses a 64‑bit mask (`long`) to record visited cells. This allows O(1) marking/unmarking and cheap membership tests.
2. **Recursive DFS** – The solver explores moves step by step, backtracking as soon as a move fails a constraint or revisits a cell.
3. **Constraint gating** – For each depth, the character from the input string limits which directions are considered. A `*` leaves all legal moves available; otherwise only the specified direction is explored.

### Pruning Heuristics

These checks aggressively trim the search tree, which is critical because the algorithm parameters are intentionally plain (simple DFS without advanced heuristics):

- **Premature finish guard** – Reject paths that reach `(7,0)` before all 64 cells are covered.
- **Distance feasibility** – Compare remaining steps with Manhattan distance to the goal; if the remaining moves cannot reach the end, backtrack immediately.
- **Connectivity check** – Run a lightweight flood‑fill on the set of unvisited cells; if the remaining cells split into multiple components, no Hamiltonian completion is possible from the current state.
- **Move prioritization** – `MovesManager` pre‑computes valid moves for every cell and sorts them with a heuristic that prefers drifting downward and leftward (toward the exit). Ordering moves well reduces the work even when the pruning itself is moderate.

### Multithreaded Work Distribution

Because the pruning is helpful but not perfect, the project compensates by spreading the search across many threads:

- Before recursion begins, the engine enumerates every legal pair of starting moves (first hop out of `(0,0)` and the next hop). Each pair defines a distinct subproblem with three cells already visited.
- The available subproblems are distributed round‑robin across worker threads (up to `2 × CPU cores`). Each worker repeatedly spawns a `PathFinderTask`, which runs the DFS for its assigned starting pair.
- Tasks accumulate their local path counts and return them to the main thread, which combines the totals. Because tasks never share mutable state (apart from the synchronized counter update), scaling is mostly limited by CPU throughput.

This coarse splitting keeps the implementation straightforward while giving a meaningful speedup on modern multi‑core machines. The approach especially helps when the constraint string has many `*` characters, where each subtree is huge.

## Project Structure (high level)

- `pathfinding.Main` – CLI entry point: reads the 63‑character constraint string, configures worker threads, aggregates results, and prints timing via `System.currentTimeMillis()`.
- `pathfinding.PathFinderTask` – Encapsulates the DFS, visited bitmasks, pruning checks, and periodic logging.
- `grid.MovesManager` & `utils.GridUtils` – Precompute valid moves, prioritize directions, and expose helper methods (`isValidCell`, `getConstraint`, etc.).
- `constants.GridConstants` – Central home for board dimensions, direction vectors, and prebuilt bit masks.

Other files in the repository are exploratory or historical experiments; the classes above form the production solver.

## Running the Solver

```bash
cd "Test run/src"
javac pathfinding/Main.java
java pathfinding.Main
```

When prompted, paste a 63‑character string (`U/D/L/R/*`). Press Enter to treat every move as unconstrained.

Example:

```
Enter path constraints (63 characters of U/D/L/R/* or press Enter for no constraints):
*****DR******R******R********************R*D************L******
Input: *****DR******R******R********************R*D************L******
Total paths found: 5739
Time taken: 18000 (ms)
```

## Performance Notes

- The unconstrained search reproduces the known `8,934,966` solutions but is CPU intensive; expect tens of seconds on a standard desktop JVM.
- Constraint strings with many fixed directions run much faster because the branching factor falls.
- Logging is throttled to avoid flooding the console; periodic updates show per‑thread progress and total elapsed time.

## Extending or Experimenting

- Adjust `getMovePriority` to try different heuristics (e.g., Warnsdorff‑style that minimizes future options).
- Replace the flood‑fill with a more precise articulation‑point check if needed.
- Explore alternative decomposition strategies (e.g., split on longer prefixes) to improve load balancing across threads.

The current implementation favors clarity and a simple multi‑threading model so that the focus stays on the puzzle logic itself. Feel free to iterate on the heuristics now that the foundational approach is documented.

