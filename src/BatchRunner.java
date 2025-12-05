import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BatchRunner {

    // Configuration
    private static final int TOTAL_RUNS = 10; // Set to 30 or 50 as needed

    public static void main(String[] args) {

        // Create a single timestamp for the whole batch so they sort nicely
        // Format: dd-MM-yyyy-HH-mm-ss
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");
        String batchTimestamp = LocalDateTime.now().format(dtf);

        System.out.println("==========================================");
        System.out.println("STARTING BATCH EXECUTION: " + TOTAL_RUNS + " RUNS");
        System.out.println("Batch ID: " + batchTimestamp);
        System.out.println("==========================================\n");

        long batchStart = System.currentTimeMillis();

        for (int i = 1; i <= TOTAL_RUNS; i++) {

            // Generate the filename: GCPrun1_05-12-2025-11-06-32.csv
            String fileName = String.format("GCPrun%d_%s.csv", i, batchTimestamp);

            System.out.println(">>> STARTING RUN " + i + " OF " + TOTAL_RUNS);
            System.out.println(">>> Output: " + fileName);

            // Trigger the BenchmarkRunner
            // We pass the filename as an argument to the main method
            try {
                BenchmarkRunner.main(new String[]{fileName});
            } catch (Exception e) {
                System.err.println("CRITICAL ERROR IN RUN " + i);
                e.printStackTrace();
            }

            // Optional: Suggest Garbage Collection between runs to clear memory
            System.gc();

            System.out.println(">>> COMPLETED RUN " + i + "\n");
        }

        long batchEnd = System.currentTimeMillis();
        long totalMinutes = (batchEnd - batchStart) / 1000 / 60;

        System.out.println("==========================================");
        System.out.println("BATCH EXECUTION FINISHED");
        System.out.println("Total time: " + totalMinutes + " minutes");
        System.out.println("Files saved in: BenchmarkResults/");
        System.out.println("==========================================");
    }
}