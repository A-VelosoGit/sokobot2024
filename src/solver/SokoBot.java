package solver;

public class SokoBot {

  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    /*
     * YOU NEED TO REWRITE THE IMPLEMENTATION OF THIS METHOD TO MAKE THE BOT SMARTER
     */
    /*
     * Default stupid behavior: Think (sleep) for 3 seconds, and then return a
     * sequence
     * that just moves left and right repeatedly.
     */
    String path = "";
    int p = -1, x = 0;
    try {
      /*
       * Find the player's location
       * x = row
       * p = col
       */
      do {
        String temp = new String(itemsData[x]);
        p = temp.indexOf('@');
      } while (p == -1 || x != width);

      
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return path;
  }

}
