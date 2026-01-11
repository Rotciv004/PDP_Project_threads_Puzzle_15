import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    // Configurare thread-uri
    private static final int NR_THREADS = 4;
    private static ExecutorService executorService;

    public static void main(String[] args) {
        try {
            // 1. Citire date
            Matrix initialState = Matrix.readFromFile();
            System.out.println("Start solving 15-Puzzle...");
            System.out.println("Initial Manhattan Distance: " + initialState.manhattan);

            // 2. Initializare Pool
            executorService = Executors.newFixedThreadPool(NR_THREADS);

            // 3. Rezolvare
            Matrix solution = solve(initialState);

            // 4. Afisare
            if (solution != null) {
                System.out.println(solution);
            } else {
                System.out.println("No solution found within limits.");
            }

            // 5. Cleanup
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Bucla principala IDA* (Iterative Deepening A*)
     */
    public static Matrix solve(Matrix root) throws ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        int bound = root.manhattan; // Limita inițială este estimarea euristică

        while (true) {
            // Pornim căutarea paralelă cu limita curentă (bound)
            Pair<Integer, Matrix> result = searchParallel(root, 0, bound, NR_THREADS);

            int distance = result.getFirst();

            if (distance == -1) {
                // S-a găsit soluția (codul -1 marchează succesul)
                System.out.println("Solved in " + (System.currentTimeMillis() - startTime) + "ms");
                return result.getSecond();
            } else if (distance == Integer.MAX_VALUE) {
                // Nu există soluție posibilă
                return null;
            } else {
                // Nu s-a găsit soluție în limita curentă.
                // Următoarea limită devine cea mai mică valoare care a depășit limita actuală.
                System.out.println("Bound " + bound + " finished. Increasing bound to " + distance + "...");
                bound = distance;
            }

            // Safety break (opțional, pentru a evita rularea infinită pe input-uri greșite)
            if (bound > 80) {
                System.out.println("Bound exceeded safety limit (80). Stopping.");
                return null;
            }
        }
    }

    /**
     * Căutare paralelizată.
     * Dacă nrThreads > 1, împarte munca la fii. Altfel, trece pe secvențial.
     */
    public static Pair<Integer, Matrix> searchParallel(Matrix current, int g, int bound, int nrThreads) throws ExecutionException, InterruptedException {
        int f = g + current.manhattan; // f = g + h

        // Pruning: Dacă estimarea depășește limita
        if (f > bound) {
            return new Pair<>(f, current);
        }

        // Verificăm dacă am ajuns la soluție (Manhattan = 0)
        if (current.manhattan == 0) {
            return new Pair<>(-1, current);
        }

        // Dacă nu mai avem thread-uri disponibile, continuăm secvențial
        if (nrThreads <= 1) {
            return searchSerial(current, g, bound);
        }

        List<Matrix> moves = current.generateMoves();
        int min = Integer.MAX_VALUE;

        // Lansăm task-uri pentru fiecare mutare posibilă
        List<Future<Pair<Integer, Matrix>>> futures = new ArrayList<>();
        int threadsPerTask = nrThreads / moves.size();

        // Dacă împărțirea dă 0 (ex: 4 mutări, 3 thread-uri), dăm măcar 1 thread
        if (threadsPerTask == 0) threadsPerTask = 1;

        for (Matrix next : moves) {
            final int threadsForChild = threadsPerTask;
            // Recursivitate paralelă
            futures.add(executorService.submit(() -> searchParallel(next, g + 1, bound, threadsForChild)));
        }

        // Colectăm rezultatele
        for (Future<Pair<Integer, Matrix>> future : futures) {
            Pair<Integer, Matrix> res = future.get();
            int t = res.getFirst();

            if (t == -1) {
                return res; // Am găsit soluția
            }
            if (t < min) {
                min = t; // Reținem cel mai mic bound depășit pentru iterația următoare
            }
        }

        return new Pair<>(min, current);
    }

    /**
     * Căutare secvențială (DFS standard pentru IDA*)
     * Folosită când adâncimea sau numărul de thread-uri dictează execuția pe un singur fir.
     */
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
                return res; // Soluție găsită
            }
            if (t < min) {
                min = t;
                solutionNode = res.getSecond();
            }
        }
        return new Pair<>(min, solutionNode);
    }
}