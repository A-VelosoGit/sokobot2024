package solver;
import java.util.*;

public class SokoBot {
  // Position class to track coordinates
  private static class Position {
    int x, y;
    Position(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Position position = (Position) o;
      return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y);
    }
  }

  // State class to represent game state
  private static class State {
    Position player;
    Set<Position> boxes;
    int cost;
    String moves;
    State parent;

    State(Position player, Set<Position> boxes, int cost, String moves, State parent) {
      this.player = player;
      this.boxes = new HashSet<>(boxes);
      this.cost = cost;
      this.moves = moves;
      this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      State state = (State) o;
      return player.equals(state.player) && boxes.equals(state.boxes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(player, boxes);
    }
  }

  private char[][] map;
  private Set<Position> goals;
  private static final int[] dx = {-1, 1, 0, 0}; // left, right, up, down
  private static final int[] dy = {0, 0, -1, 1};
  private static final char[] moves = {'l', 'r', 'u', 'd'};

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    // Initialize map and goal points
    this.map = mapData;
    this.goals = new HashSet<>();

    // Read data
    Position playerPos = null;
    Set<Position> boxes = new HashSet<>();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (mapData[y][x] == '.') {
          goals.add(new Position(x, y));
        }
        if (itemsData[y][x] == '@') {
          playerPos = new Position(x, y);
        } else if (itemsData[y][x] == '$') {
          boxes.add(new Position(x, y));
        }
      }
    }

    // Solve using A*
    return aStar(playerPos, boxes, width, height);
  }

  private String aStar(Position start, Set<Position> boxes, int width, int height) {
    PriorityQueue<State> frontier = new PriorityQueue<>((a, b) ->
            (a.cost + heuristic(a.boxes)) - (b.cost + heuristic(b.boxes))); // Compares g(n) + h(n)
    Set<String> explored = new HashSet<>();
    //int stateCount = 0; // debug thing

    State initial = new State(start, boxes, 0, "", null);
    frontier.add(initial);
    explored.add(stringState(initial));

    while (!frontier.isEmpty()) {
      State currentState = frontier.poll();

      // Check if we reached the goal
      if (heuristic(currentState.boxes) == 0) {
        //System.out.println("States visited: " + stateCount); // debug thing
        return currentState.moves;
      }

      // Try each direction
      for (int i = 0; i < 4; i++) {
        State newState = move(currentState, i, width, height);

        if (newState != null) {
          String stateStr = stringState(newState);
          if (!explored.contains(stateStr)) {
            //stateCount++; // debug thing
            explored.add(stateStr);
            frontier.add(newState);
          }
        }
      }
    }

    return ""; // No solution found
  }

  // Branch out state, returns null if invalid
  private State move(State currentState, int i, int width, int height) {
    Position newPlayer = new Position(
            currentState.player.x + dx[i],
            currentState.player.y + dy[i]
    );

    // Check if move is valid
    if (isWallAt(newPlayer, width, height)) return null;

    // Check if a box got pushed
    Position boxPos = null;
    Position newBoxPos = null;
    if (currentState.boxes.contains(newPlayer)) {
      boxPos = newPlayer;
      newBoxPos = new Position(
              newPlayer.x + dx[i],
              newPlayer.y + dy[i]
      );

      // Check if push is valid, deadlock detection
      if (isBoxStuck(newBoxPos, currentState.boxes, width, height))
        return null;
    }

    // Create new state, update box set if pushed
    Set<Position> newBoxes = new HashSet<>(currentState.boxes);
    if (boxPos != null) {
      newBoxes.remove(boxPos);
      newBoxes.add(newBoxPos);
    }

    // Cost prioritizes smallest amount of push count
    return new State(
            newPlayer,
            newBoxes,
            boxPos != null ? currentState.cost + 1 : currentState.cost,
            currentState.moves + moves[i],
            currentState
    );
  }

  //Checks if new object position is a wall
  private boolean isWallAt(Position pos, int width, int height) {
    return pos.x >= 0 && pos.x < width &&
            pos.y >= 0 && pos.y < height &&
            map[pos.y][pos.x] == '#';
  }

  // Deadlock detection, corner checking
  private boolean isBoxStuck(Position box, Set<Position> boxes, int width, int height) {

    if (isWallAt(box, width, height) || boxes.contains(box)) return true;

    if (goals.contains(box)) return false;

    Position left = new Position(box.x - 1, box.y);
    Position right = new Position(box.x + 1, box.y);
    Position up = new Position(box.x, box.y - 1);
    Position down = new Position(box.x, box.y + 1);

    // Check for corner deadlocks
    return (isWallAt(left, width, height) && isWallAt(up, width, height)) ||
            (isWallAt(right, width, height) && isWallAt(up, width, height)) ||
            (isWallAt(left, width, height) && isWallAt(down, width, height)) ||
            (isWallAt(right, width, height) && isWallAt(down, width, height));
  }

  // Heuristic function, Manhattan distance to a box's closest goal
  // Skips boxes on goals
  private int heuristic(Set<Position> boxes) {
    int total = 0;
    for (Position box : boxes) {
      if (!goals.contains(box)) {
        int minDist = Integer.MAX_VALUE;
        for (Position goal : goals) {
          int dist = Math.abs(box.x - goal.x) + Math.abs(box.y - goal.y);
          minDist = Math.min(minDist, dist);
        }
        total += minDist;
      }
    }
    return total;
  }

  // Convert player and box values to String
  // Made to access explored set faster
  private String stringState(State state) {
    StringBuilder sb = new StringBuilder();
    sb.append(state.player.x).append(',').append(state.player.y).append(';');
    List<Position> sortedBoxes = new ArrayList<>(state.boxes);
    sortedBoxes.sort((a, b) -> {
      if (a.y != b.y) return a.y - b.y;
      return a.x - b.x;
    });
    for (Position box : sortedBoxes) {
      sb.append(box.x).append(',').append(box.y).append(';');
    }
    return sb.toString();
  }
}