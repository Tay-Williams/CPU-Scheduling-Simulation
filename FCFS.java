import java.util.*;

public class FCFS {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes (minimum 20): ");
        int n = sc.nextInt();

        while (n < 20) {
            System.out.print("Must be at least 20. Enter again: ");
            n = sc.nextInt();
        }

        int[] arrival = new int[n];
        int[] burst = new int[n];

        for (int i = 0; i < n; i++) {
            System.out.println("\nProcess " + (i + 1));
            System.out.print("Arrival Time: ");
            arrival[i] = sc.nextInt();
            System.out.print("Burst Time: ");
            burst[i] = sc.nextInt();
            System.out.print("I/O Start Time (0 if none): ");
            sc.nextInt();
            System.out.print("I/O Duration (0 if none): ");
            sc.nextInt();
        }

        // Sort by arrival time
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Integer.compare(arrival[a], arrival[b]));

        int[] completion = new int[n];
        int[] waiting = new int[n];
        int[] turnaround = new int[n];
        int currentTime = 0;

        for (int idx : indices) {
            if (currentTime < arrival[idx]) {
                currentTime = arrival[idx];
            }
            completion[idx] = currentTime + burst[idx];
            turnaround[idx] = completion[idx] - arrival[idx];
            waiting[idx] = turnaround[idx] - burst[idx];
            currentTime = completion[idx];
        }

        System.out.println("\n========== FCFS SCHEDULING RESULTS ==========");
        System.out.println("PID\tArrival\tBurst\tWaiting\tTurnaround");
        System.out.println("------------------------------------------------");

        double totalWait = 0, totalTurn = 0;
        for (int i = 0; i < n; i++) {
            System.out.printf("%d\t%d\t%d\t%d\t%d\n", i + 1, arrival[i], burst[i], waiting[i], turnaround[i]);
            totalWait += waiting[i];
            totalTurn += turnaround[i];
        }

        System.out.println("------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWait / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurn / n);
    }
}