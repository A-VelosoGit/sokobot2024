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

    // Heuristic: Sum of Manhattan distances
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

    /*
    // Check if this state is a goal (all boxes on goals)
    public boolean isGoal(List<int[]> goals) {
      for (int[] box : boxes) {
        boolean onGoal = false;
        for (int[] goal : goals) {
          if (box[0] == goal[0] && box[1] == goal[1]) {
            onGoal = true;
            break;
          }
        }
        if (!onGoal) return false;
      }
      return true;
    }
   */

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

    @Override
    public int compareTo(Node other) {
      return Integer.compare(this.getTotalCost(), other.getTotalCost());
    }
  }

  // Directions
  private static final char[] DIRECTIONS = {'u', 'd', 'l', 'r'};

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    long startTime = System.nanoTime();
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

    while (!frontier.isEmpty()) {
      Node currentNode = frontier.poll();
      State currentState = currentNode.getState();

      if (currentNode.getHeuristic() == 0) {
        long endTime = System.nanoTime();
        long timeTaken = endTime - startTime;
        System.out.println("Time taken: " + (timeTaken/1_000_000) + "ms"); //debug thing
        return printSolution(currentNode);
      }

      if (visited.contains(currentState)) {
        continue;
      }
      visited.add(currentState);

      //if state has stuck box
      if (isDeadlock(currentState, mapData)) {
        continue;
      }

      for (char dir : DIRECTIONS) {
        State newState = move(currentState, dir, mapData);
        if (newState != null && !visited.contains(newState)) {
          int newCost = currentNode.getCost() + 1;  // Increment cost by 1, probably inefficient
          int heuristic = newState.distance(goals);
          frontier.add(new Node(dir, newState, newCost, heuristic, currentNode));  // Add new state to queue
        }
      }
    }

    return "No solution";
  }

  private boolean isDeadlock(State state, char[][] walls) {
    List<int[]> boxes = state.getBoxes();
    for (int[] box : boxes) {
      // Check if the box is in a corner
      if (walls[box[0]][box[1]] == '#' &&
              walls[box[0] + 1][box[1]] == '#' || walls[box[0]][box[1] + 1] == '#') {
        return true; // Box is stuck in a corner
      }
    }
    return false;
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
      if (box[0] == row && box[1] == col) {
        return true;
      }
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
