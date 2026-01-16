import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    private static final int NR_THREADS = 4;
    private static ExecutorService executorService;

    public static void main(String[] args) {
        try {
            Matrix initialState = Matrix.readFromFile();
            System.out.println("Start solving 15-Puzzle...");
            System.out.println("Initial Manhattan Distance: " + initialState.manhattan);

            executorService = Executors.newFixedThreadPool(NR_THREADS);

            Matrix solution = solve(initialState);

            if (solution != null) {
                System.out.println(solution);
            } else {
                System.out.println("No solution found within limits.");
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static Matrix solve(Matrix root) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        int bound = root.manhattan;

        while (true) {
            Pair<Integer, Matrix> result = searchParallel(root, 0, bound, NR_THREADS);

            int distance = result.getFirst();

            if (distance == -1) {
                System.out.println("Solved in " + (System.currentTimeMillis() - startTime) + "ms");
                return result.getSecond();
            } else if (distance == Integer.MAX_VALUE) {
                return null;
            } else {
                System.out.println("Bound " + bound + " finished. Increasing bound to " + distance + "...");
                bound = distance;
            }

            if (bound > 80) {
                System.out.println("Bound exceeded safety limit (80). Stopping.");
                return null;
            }
        }
    }

    public static Pair<Integer, Matrix> searchParallel(Matrix current, int g, int bound, int nrThreads) throws ExecutionException, InterruptedException {
        int f = g + current.manhattan;

        if (f > bound) {
            return new Pair<>(f, current);
        }

        if (current.manhattan == 0) {
            return new Pair<>(-1, current);
        }

        if (nrThreads <= 1) {
            return searchSerial(current, g, bound);
        }

        List<Matrix> moves = current.generateMoves();
        int min = Integer.MAX_VALUE;

        List<Future<Pair<Integer, Matrix>>> futures = new ArrayList<>();
        int threadsPerTask = nrThreads / moves.size();

        if (threadsPerTask == 0) threadsPerTask = 1;

        for (Matrix next : moves) {
            final int threadsForChild = threadsPerTask;
            futures.add(executorService.submit(() -> searchParallel(next, g + 1, bound, threadsForChild)));
        }

        for (Future<Pair<Integer, Matrix>> future : futures) {
            Pair<Integer, Matrix> res = future.get();
            int t = res.getFirst();

            if (t == -1) {
                return res;
            }
            if (t < min) {
                min = t;
            }
        }

        return new Pair<>(min, current);
    }

    public static Pair<Integer, Matrix> searchSerial(Matrix current, int g, int bound) {
        int f = g + current.manhattan;

        if (f > bound) {
            return new Pair<>(f, current);
        }
        if (current.manhattan == 0) {
            return new Pair<>(-1, current);
        }

        int min = Integer.MAX_VALUE;
        Matrix solutionNode = null;

        for (Matrix next : current.generateMoves()) {
            Pair<Integer, Matrix> res = searchSerial(next, g + 1, bound);
            int t = res.getFirst();

            if (t == -1) {
                return res;
            }
            if (t < min) {
                min = t;
                solutionNode = res.getSecond();
            }
        }
        return new Pair<>(min, solutionNode);
    }
}