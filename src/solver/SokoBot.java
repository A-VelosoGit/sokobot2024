package solver;
import java.util.*;


public class SokoBot {

  static class State {
    private int[] player;  // Player's position [row, col]
    private List<int[]> boxes;  // List of box positions [row, col]

    public State(int[] player, List<int[]> boxes) {
      this.player = player;
      this.boxes = boxes;
    }

    public int[] getPlayer() {
      return player;
    }

    public List<int[]> getBoxes() {
      return boxes;
    }

    // Heuristic: Sum of Manhattan distances, will be improved
    public int distance(List<int[]> goals) {
      int totalDistance = 0;
      for (int[] box : boxes) {
        int minDist = Integer.MAX_VALUE;
        for (int[] goal : goals) {
          int dist = Math.abs(goal[0] - box[0]) + Math.abs(goal[1] - box[1]);
          minDist = Math.min(minDist, dist);
        }
        totalDistance += minDist;
      }
      return totalDistance;
    }

    // Check if two states are equal (player and box positions)
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      State state = (State) obj;
      return Arrays.equals(player, state.player) && boxes.equals(state.boxes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(player), boxes);
    }
  }

  static class Node implements Comparable<Node> {
    private Character move;
    private State state;  // The current state (player position and box positions)
    private int cost;  // Cost to reach this node (g(n))
    private Node parent;  // Parent node (to reconstruct the path)
    private int heuristic;  // Heuristic value (h(n))

    public Node(Character move, State state, int cost, int heuristic, Node parent) {
      this.move = move;
      this.state = state;
      this.cost = cost;
      this.heuristic = heuristic;
      this.parent = parent;
    }

    public Character getMove() {
      return move;
    }

    public State getState() {
      return state;
    }

    public int getCost() {
      return cost;
    }

    public int getHeuristic() {
      return heuristic;
    }

    public Node getParent() {
      return parent;
    }

    // f(n) = g(n) + h(n)
    public int getTotalCost() {
      return cost + heuristic;
    }

    @Override //node value = total cost
    public int compareTo(Node other) {
      return Integer.compare(this.getTotalCost(), other.getTotalCost());
    }
  }

  // Directions
  private static final char[] DIRECTIONS = {'u', 'd', 'l', 'r'};

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    long startTime = System.nanoTime();
    int stateCount = 0; //debug thing
    int[] player = new int[2];
    List<int[]> boxes = new ArrayList<>();
    List<int[]> goals = new ArrayList<>();
    PriorityQueue<Node> frontier = new PriorityQueue<>();  // Priority queue
    Set<State> visited = new HashSet<>();  // Check visited nodes

    // Read data
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        if (mapData[i][j] == '.')
          goals.add(new int[]{i, j});

        if (itemsData[i][j] == '$')
          boxes.add(new int[]{i, j});

        if (itemsData[i][j] == '@')
          player = new int[]{i, j};
      }
    }

    State base = new State(player, boxes);
    frontier.add(new Node(null, base, 0, base.distance(goals), null));

    // Main search loop
    while (!frontier.isEmpty()) {
      Node currentNode = frontier.poll();
      State currentState = currentNode.getState();

      if (currentNode.getHeuristic() == 0) {
        long endTime = System.nanoTime();
        long timeTaken = endTime - startTime;
        System.out.println("Time taken: " + (timeTaken/1_000_000) + "ms"); //debug thing
        System.out.println("States visited: " + stateCount); //debug thing
        return printSolution(currentNode);
      }

      if (visited.contains(currentState)) {
        continue;
      }
      visited.add(currentState);
      stateCount++;

      /*if state has stuck box
      if (isDeadlock(currentState, mapData)) {
        System.out.println("Deadlock detected");
        System.out.println("States visited: " + stateCount);
        continue;
      }*/

      for (char dir : DIRECTIONS) {

        // scuffed pruning
        if (currentNode.getParent() != null) {
          if (dir == 'u' && currentNode.getMove() == 'd' ||
              dir == 'd' && currentNode.getMove() == 'u' ||
              dir == 'l' && currentNode.getMove() == 'r' ||
              dir == 'r' && currentNode.getMove() == 'l')
            continue;
        }

        State newState = move(currentState, dir, mapData);
        if (newState != null && !visited.contains(newState)) {
          int newCost = currentNode.getCost() + 1;  // Increment cost by 1, probably inefficient?
          int heuristic = newState.distance(goals);
          frontier.add(new Node(dir, newState, newCost, heuristic, currentNode));  // Add new state to queue
        }
      }
    }

    return "No solution";
  }

  private boolean isDeadlock(State state, char[][] walls) { // will rework
    List<int[]> boxes = state.getBoxes();
    for (int[] box : boxes) {
      if (isBoxStuckInCorner(box, walls)) {
        return true;  // If any box is stuck in a corner, return true (deadlock)
      }
    }
    return false;
  }

  private static boolean isBoxStuckInCorner(int[] box, char[][] walls) {
    int row = box[0];
    int col = box[1];

    // If corner is a goal point
    if (walls[row][col] == '.')
      return false; // Not stuck

    // Check surrounding tiles
    boolean upBlocked = (walls[row - 1][col] == '#');
    boolean downBlocked = (walls[row + 1][col] == '#');
    boolean leftBlocked = (walls[row][col - 1] == '#');
    boolean rightBlocked = (walls[row][col + 1] == '#');

    // Box is stuck in a corner if two adjacent sides are blocked, else false
    return (upBlocked && leftBlocked) || (upBlocked && rightBlocked) ||
            (downBlocked && leftBlocked) || (downBlocked && rightBlocked);
  }

  // Move the player in the given direction and push boxes if necessary
  private static State move(State state, char dir, char[][] walls) {
    int[] player = state.getPlayer();
    List<int[]> boxes = state.getBoxes();
    int[] dirCoord = new int[2];
    
    switch(dir) {
      case 'u': dirCoord = new int[]{-1, 0};
        break;
      case 'd': dirCoord = new int[]{1, 0};
        break;
      case 'l': dirCoord = new int[]{0, -1};
        break;
      case 'r': dirCoord = new int[]{0, 1};
        break;
    }
    
    // Move player
    int newPlayerRow = player[0] + dirCoord[0];
    int newPlayerCol = player[1] + dirCoord[1];

    // Check if new position is valid (not a wall)
    if (walls[newPlayerRow][newPlayerCol] == '#') {
      return null;
    }

    // Check if there is a box at the new player position
    for (int i = 0; i < boxes.size(); i++) {
      int[] box = boxes.get(i);
      if (box[0] == newPlayerRow && box[1] == newPlayerCol) {
        // Try to push the box
        int newBoxRow = box[0] + dirCoord[0];
        int newBoxCol = box[1] + dirCoord[1];

        // Check if the box can be pushed (not a wall or another box)
        if (walls[newBoxRow][newBoxCol] == '#' || isBoxAt(boxes, newBoxRow, newBoxCol)) {
          return null;
        }

        int[] newBox = {newBoxRow, newBoxCol};

        // Check if box gets stuck in corner
        if (isBoxStuckInCorner(newBox, walls))
          return null;

        // Create a new state with the pushed box
        List<int[]> newBoxes = new ArrayList<>(boxes);
        newBoxes.set(i, new int[]{newBoxRow, newBoxCol});
        return new State(new int[]{newPlayerRow, newPlayerCol}, newBoxes);
      }
    }

    // No box at the new position, return a new state with just the player moved
    return new State(new int[]{newPlayerRow, newPlayerCol}, boxes);
  }

  // Check if a box is at the given position
  private static boolean isBoxAt(List<int[]> boxes, int row, int col) {
    for (int[] box : boxes) {
      if (box[0] == row && box[1] == col)
        return true;
    }
    return false;
  }

  // Print the solution by tracing back the nodes
  private static String printSolution(Node node) {
    List<Character> moves = new ArrayList<>();
    while (node.getParent() != null) {
      moves.add(node.getMove());
      node = node.getParent();
    }
    Collections.reverse(moves);

    StringBuilder sequence = new StringBuilder();
    for (Character move : moves) {
      sequence.append(move);
    }
    System.out.println(sequence);
    return sequence.toString();
  }

}
