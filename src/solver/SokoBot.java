package solver;
import java.util.*;


public class SokoBot {

  /**
   * @param player Player's position [row, col]
   * @param boxes  List of box positions [row, col]
   */
  record State(int[] player, List<int[]> boxes) {

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

  /**
   * @param state     The current state (player position and box positions)
   * @param cost      Cost to reach this node (g(n))
   * @param parent    Parent node (to reconstruct the path)
   * @param heuristic Heuristic value (h(n))
   */
  record Node(Character move, State state, int cost, int heuristic, SokoBot.Node parent) implements Comparable<Node> {

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
    int stateCount = 0; //debug thing
    int[] player = new int[2];
    List<int[]> boxes = new ArrayList<>();
    List<int[]> goals = new ArrayList<>();
    PriorityQueue<Node> frontier = new PriorityQueue<>();  // Priority queue
    Set<State> explored = new HashSet<>();  // Check visited nodes
    int newCost; // cost variable for new nodes

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
      State currentState = currentNode.state();

      if (currentNode.heuristic() == 0) {
        System.out.println("States visited: " + stateCount); //debug thing
        return printSolution(currentNode);
      }

      if (explored.contains(currentState)) {
        continue;
      }
      explored.add(currentState);
      stateCount++;

      for (char dir : DIRECTIONS) {

        if (isReverseMove(currentNode, dir))
              continue;

        State newState = move(currentState, dir, mapData);
        if (newState != null && !explored.contains(newState)) {
          if(newState.boxes().equals(currentState.boxes())) // If no box moved
            newCost = currentNode.cost() + 1;  // Increment cost by 1
          else {
            newCost = currentNode.cost(); // Cost remain the same
          }
          int heuristic = newState.distance(goals);
          frontier.add(new Node(dir, newState, newCost, heuristic, currentNode));  // Add new state to queue
        }
      }
    }

    return ""; // No solution
  }

  // Pruning redundant paths, does not allow reverse moves if no boxes were changed from first move
  private static boolean isReverseMove(Node node, char dir) {
    if (node.parent() == null || node.move() == null) return false;

    boolean isReverse = (dir == 'u' && node.move() == 'd') ||
                        (dir == 'd' && node.move() == 'u') ||
                        (dir == 'l' && node.move() == 'r') ||
                        (dir == 'r' && node.move() == 'l');

    return isReverse && node.state().boxes().equals(node.parent().state().boxes());
  }

  private static boolean isBoxStuck(int[] box, char[][] map, List<int[]> boxes) {
    int boxRow = box[0];
    int boxCol = box[1];

    // If box is on a goal point
    if (map[boxRow][boxCol] == '.')
      return false; // Not stuck

    int[][] rotatePattern = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8},
            {2, 5, 8, 1, 4, 7, 0, 3, 6},
            {8, 7, 6, 5, 4, 3, 2, 1, 0},
            {6, 3, 0, 7, 4, 1, 8, 5, 2}
    };
    int[][] flipPattern = {
            {2, 1, 0, 5, 4, 3, 8, 7, 6},
            {0, 3, 6, 1, 4, 7, 2, 5, 8},
            {6, 7, 8, 3, 4, 5, 0, 1, 2},
            {8, 5, 2, 7, 4, 1, 6, 3, 0}
    };
    List<int[]> allPatterns = new ArrayList<>();
    allPatterns.addAll(Arrays.asList(rotatePattern));
    allPatterns.addAll(Arrays.asList(flipPattern));

    int[][] board = {
            {box[0] - 1, box[1] - 1}, {box[0] - 1, box[1]}, {box[0] - 1, box[1] + 1},
            {box[0], box[1] - 1},     {box[0], box[1]},     {box[0], box[1] + 1},
            {box[0] + 1, box[1] - 1}, {box[0] + 1, box[1]}, {box[0] + 1, box[1] + 1}
    };

    for (int[] pattern : allPatterns) {
      List<int[]> newBoard = new ArrayList<>();

      for (int index : pattern) {
        newBoard.add(Arrays.copyOf(board[index], board[index].length));
      }

      if (isWallAt(newBoard.get(1), map) && isWallAt(newBoard.get(5), map)) {
        return true;
      } else if (isBoxAt(newBoard.get(1), boxes) && isWallAt(newBoard.get(2), map) && isWallAt(newBoard.get(5), map)) {
        return true;
      } else if (isBoxAt(newBoard.get(1), boxes) && isWallAt(newBoard.get(2), map) && isBoxAt(newBoard.get(5), boxes)) {
        return true;
      } else if (isBoxAt(newBoard.get(1), boxes) && isBoxAt(newBoard.get(2), boxes) && isBoxAt(newBoard.get(5), boxes)) {
        return true;
      }
      else if (isBoxAt(newBoard.get(1), boxes) && isBoxAt(newBoard.get(6), boxes) && isWallAt(newBoard.get(2), map)
              && isWallAt(newBoard.get(3), map) && isWallAt(newBoard.get(8), map)) {
        return true;
      }
    }

    return false;
  }

  // Check if a wall is at the given position
  private static boolean isWallAt(int[] pos, char[][] map) {
    int row = pos[0];
    int col = pos[1];

    // Make sure to check bounds before accessing the map
    if (row >= 0 && row < map.length && col >= 0 && col < map[0].length) {
      return map[row][col] == '#';
    }

    return false;
  }

  // Check if a box is at the given position
  private static boolean isBoxAt(int[] pos, List<int[]> boxes) {
    int row = pos[0];
    int col = pos[1];

    for (int[] box : boxes) {
      if (box[0] == row && box[1] == col)
        return true;
    }
    return false;
  }

  // Move the player in the given direction and push boxes if necessary
  private static State move(State state, char dir, char[][] walls) {
    int[] player = state.player();
    List<int[]> boxes = state.boxes();
    int[] dirCoord = switch (dir) {
      case 'u' -> new int[]{-1, 0};
      case 'd' -> new int[]{1, 0};
      case 'l' -> new int[]{0, -1};
      case 'r' -> new int[]{0, 1};
      default -> new int[2];
    };

    // Move player
    int[] newPlayer = {player[0] + dirCoord[0], player[1] + dirCoord[1]};

    // Check if new position is valid (not a wall)
    if (isWallAt(newPlayer, walls)) {
      return null;
    }

    // Check if there is a box at the new player position
    for (int i = 0; i < boxes.size(); i++) {
      int[] box = boxes.get(i);
      if (box[0] == newPlayer[0] && box[1] == newPlayer[1]) {
        // Try to push the box
        int[] newBox = {box[0] + dirCoord[0], box[1] + dirCoord[1]};

        // Check if the box can be pushed (not a wall or another box)
        if (isWallAt(newBox, walls) || isBoxAt(newBox, boxes)) {
          return null;
        }

        // Check if box gets stuck in corner
        //if (isBoxStuckInCorner(newBox, walls))
        //  return null;

        // Create a new state with the pushed box
        List<int[]> newBoxes = new ArrayList<>(boxes);
        newBoxes.set(i, newBox);

        //Check if box hits deadlock
        if (isBoxStuck(newBox, walls, newBoxes))
          return null;

        return new State(newPlayer, newBoxes);
      }
    }

    // No box at the new position, return a new state with just the player moved
    return new State(newPlayer, boxes);
  }

  // Print the solution by tracing back the nodes
  private static String printSolution(Node node) {
    List<Character> moves = new ArrayList<>();
    while (node.parent() != null) {
      moves.add(node.move());
      node = node.parent();
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
