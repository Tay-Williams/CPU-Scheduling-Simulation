import java.util.*;

public class Priority {
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
        int[] priority = new int[n];

        for (int i = 0; i < n; i++) {
            System.out.println("\nProcess " + (i + 1));
            System.out.print("Arrival Time: ");
            arrival[i] = sc.nextInt();
            System.out.print("Burst Time: ");
            burst[i] = sc.nextInt();
            System.out.print("Priority (lower = higher priority): ");
            priority[i] = sc.nextInt();
            System.out.print("I/O Start Time (0 if none): ");
            sc.nextInt();
            System.out.print("I/O Duration (0 if none): ");
            sc.nextInt();
        }

        int[] remaining = burst.clone();
        int[] completion = new int[n];
        int time = 0;
        int completed = 0;

        while (completed < n) {
            int highest = Integer.MAX_VALUE;
            int selected = -1;

            // Find highest priority process that has arrived
            for (int i = 0; i < n; i++) {
                if (arrival[i] <= time && remaining[i] > 0) {
                    if (priority[i] < highest) {
                        highest = priority[i];
                        selected = i;
                    }
                }
            }

            if (selected == -1) {
                time++;
                continue;
            }

            remaining[selected]--;
            time++;

            if (remaining[selected] == 0) {
                completion[selected] = time;
                completed++;
            }
        }

        // Calculate waiting and turnaround
        System.out.println("\n========== PREEMPTIVE PRIORITY RESULTS ==========");
        System.out.println("PID\tArrival\tBurst\tPriority\tWaiting\tTurnaround");
        System.out.println("--------------------------------------------------------------");

        double totalWait = 0, totalTurn = 0;
        for (int i = 0; i < n; i++) {
            int turnaround = completion[i] - arrival[i];
            int waiting = turnaround - burst[i];
            System.out.printf("%d\t%d\t%d\t%d\t\t%d\t%d\n", i + 1, arrival[i], burst[i], priority[i], waiting, turnaround);
            totalWait += waiting;
            totalTurn += turnaround;
        }

        System.out.println("--------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWait / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurn / n);
    }
}
