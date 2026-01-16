import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Matrix {
    private static final int[] dx = new int[]{0, -1, 0, 1};
    private static final int[] dy = new int[]{-1, 0, 1, 0};
    private static final String[] MOVE_NAMES = new String[]{"left", "up", "right", "down"};

    private final byte[][] tiles;
    private final int freeI;
    private final int freeJ;

    private final int numberOfSteps;
    private final Matrix previousState;
    private final String moveFromPrevious;
    public final int manhattan;

    public Matrix(byte[][] tiles, int freeI, int freeJ, int numberOfSteps, Matrix previousState, String moveFromPrevious) {
        this.tiles = tiles;
        this.freeI = freeI;
        this.freeJ = freeJ;
        this.numberOfSteps = numberOfSteps;
        this.previousState = previousState;
        this.moveFromPrevious = moveFromPrevious;
        this.manhattan = calculateManhattan();
    }

    public static Matrix readFromFile() throws IOException {
        byte[][] values = new byte[4][4];
        int freeI = -1, freeJ = -1;

        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader("input.in")))) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    values[i][j] = (byte) scanner.nextInt();
                    if (values[i][j] == 0) {
                        freeI = i;
                        freeJ = j;
                    }
                }
            }
        }
        return new Matrix(values, freeI, freeJ, 0, null, "");
    }

    private int calculateManhattan() {
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (tiles[i][j] != 0) {
                    int targetVal = tiles[i][j] - 1;
                    int targetI = targetVal / 4;
                    int targetJ = targetVal % 4;
                    sum += Math.abs(i - targetI) + Math.abs(j - targetJ);
                }
            }
        }
        return sum;
    }

    public List<Matrix> generateMoves() {
        List<Matrix> moves = new ArrayList<>();
        for (int k = 0; k < 4; k++) {
            int newI = freeI + dx[k];
            int newJ = freeJ + dy[k];

            if (newI >= 0 && newI < 4 && newJ >= 0 && newJ < 4) {
                if (previousState != null && newI == previousState.freeI && newJ == previousState.freeJ) {
                    continue;
                }

                byte[][] newTiles = new byte[4][4];
                for(int i=0; i<4; i++) {
                    newTiles[i] = tiles[i].clone();
                }

                newTiles[freeI][freeJ] = newTiles[newI][newJ];
                newTiles[newI][newJ] = 0;

                moves.add(new Matrix(newTiles, newI, newJ, numberOfSteps + 1, this, MOVE_NAMES[k]));
            }
        }
        return moves;
    }

    @Override
    public String toString() {
        Matrix current = this;
        List<String> path = new ArrayList<>();

        while (current.previousState != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Move: ").append(current.moveFromPrevious).append("\n");
            for (byte[] row : current.tiles) {
                sb.append(Arrays.toString(row)).append("\n");
            }
            sb.append("\n");
            path.add(sb.toString());
            current = current.previousState;
        }

        Collections.reverse(path);

        return "Solution found in " + numberOfSteps + " steps:\n" +
                String.join("", path);
    }
}