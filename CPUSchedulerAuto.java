import java.util.*;

public class CPUSchedulerAuto {
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        System.out.println("\n========================================");
        System.out.println("     CPU SCHEDULING SIMULATOR");
        System.out.println("========================================");
        System.out.println("1. First Come First Served (FCFS)");
        System.out.println("2. Round Robin (RR)");
        System.out.println("3. Preemptive Priority");
        System.out.println("4. Exit");
        System.out.print("\nEnter your choice: ");
        
        int choice = sc.nextInt();
        
        switch (choice) {
            case 1:
                runFCFS();
                break;
            case 2:
                runRR();
                break;
            case 3:
                runPriority();
                break;
            case 4:
                System.out.println("Exiting...");
                break;
            default:
                System.out.println("Invalid choice!");
        }
        
        sc.close();
    }
    
    public static void runFCFS() {
        System.out.println("\n========== FCFS with 20 processes ==========\n");
        
        int n = 20;
        int[] arrival = new int[n];
        int[] burst = new int[n];
        
        // Hardcoded 20 processes (arrival, burst)
        int[][] processes = {
            {0, 5}, {1, 4}, {2, 6}, {3, 3}, {4, 7},
            {5, 2}, {6, 5}, {7, 4}, {8, 6}, {9, 3},
            {10, 5}, {11, 4}, {12, 6}, {13, 3}, {14, 7},
            {15, 2}, {16, 5}, {17, 4}, {18, 6}, {19, 3}
        };
        
        for (int i = 0; i < n; i++) {
            arrival[i] = processes[i][0];
            burst[i] = processes[i][1];
        }
        
        // Sort by arrival time
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Integer.compare(arrival[a], arrival[b]));
        
        int currentTime = 0;
        int[] completion = new int[n];
        int[] waiting = new int[n];
        int[] turnaround = new int[n];
        
        for (int idx : indices) {
            if (currentTime < arrival[idx]) {
                currentTime = arrival[idx];
            }
            completion[idx] = currentTime + burst[idx];
            turnaround[idx] = completion[idx] - arrival[idx];
            waiting[idx] = turnaround[idx] - burst[idx];
            currentTime = completion[idx];
        }
        
        System.out.println("PID\tArrival\tBurst\tWaiting\tTurnaround");
        System.out.println("----------------------------------------");
        double totalWait = 0, totalTurn = 0;
        for (int i = 0; i < n; i++) {
            System.out.printf("%d\t%d\t%d\t%d\t%d\n", i+1, arrival[i], burst[i], waiting[i], turnaround[i]);
            totalWait += waiting[i];
            totalTurn += turnaround[i];
        }
        System.out.println("----------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWait / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurn / n);
    }
    
    public static void runRR() {
        System.out.println("\n========== Round Robin with 20 processes ==========\n");
        
        int n = 20;
        int quantum = 3;
        int[] arrival = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        int[] burst = {5,4,6,3,7,2,5,4,6,3,5,4,6,3,7,2,5,4,6,3};
        int[] remaining = burst.clone();
        int[] completion = new int[n];
        int[] waiting = new int[n];
        int time = 0;
        int completed = 0;
        Queue<Integer> queue = new LinkedList<>();
        boolean[] inQueue = new boolean[n];
        
        // Add first process
        queue.add(0);
        inQueue[0] = true;
        
        while (completed < n) {
            if (queue.isEmpty()) {
                time++;
                for (int i = 0; i < n; i++) {
                    if (arrival[i] <= time && remaining[i] > 0 && !inQueue[i]) {
                        queue.add(i);
                        inQueue[i] = true;
                    }
                }
                continue;
            }
            
            int current = queue.poll();
            inQueue[current] = false;
            
            int execTime = Math.min(quantum, remaining[current]);
            remaining[current] -= execTime;
            time += execTime;
            
            // Add new arrivals
            for (int i = 0; i < n; i++) {
                if (arrival[i] <= time && remaining[i] > 0 && !inQueue[i] && i != current) {
                    queue.add(i);
                    inQueue[i] = true;
                }
            }
            
            if (remaining[current] > 0) {
                queue.add(current);
                inQueue[current] = true;
            } else {
                completion[current] = time;
                completed++;
            }
        }
        
        System.out.println("PID\tArrival\tBurst\tWaiting\tTurnaround");
        System.out.println("----------------------------------------");
        double totalWait = 0, totalTurn = 0;
        for (int i = 0; i < n; i++) {
            int turn = completion[i] - arrival[i];
            int wait = turn - burst[i];
            System.out.printf("%d\t%d\t%d\t%d\t%d\n", i+1, arrival[i], burst[i], wait, turn);
            totalWait += wait;
            totalTurn += turn;
        }
        System.out.println("----------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWait / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurn / n);
        System.out.printf("Time Quantum Used: %d\n", quantum);
    }
    
    public static void runPriority() {
        System.out.println("\n========== Preemptive Priority with 20 processes ==========\n");
        
        int n = 20;
        int[] arrival = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
        int[] burst = {5,4,6,3,7,2,5,4,6,3,5,4,6,3,7,2,5,4,6,3};
        int[] priority = {3,1,2,3,1,2,3,1,2,3,1,2,3,1,2,3,1,2,3,1};
        int[] remaining = burst.clone();
        int[] completion = new int[n];
        int time = 0;
        int completed = 0;
        
        while (completed < n) {
            int highestPriority = Integer.MAX_VALUE;
            int selected = -1;
            
            for (int i = 0; i < n; i++) {
                if (arrival[i] <= time && remaining[i] > 0) {
                    if (priority[i] < highestPriority) {
                        highestPriority = priority[i];
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
        
        System.out.println("PID\tArrival\tBurst\tPriority\tWaiting\tTurnaround");
        System.out.println("----------------------------------------------------------");
        double totalWait = 0, totalTurn = 0;
        for (int i = 0; i < n; i++) {
            int turn = completion[i] - arrival[i];
            int wait = turn - burst[i];
            System.out.printf("%d\t%d\t%d\t%d\t\t%d\t%d\n", i+1, arrival[i], burst[i], priority[i], wait, turn);
            totalWait += wait;
            totalTurn += turn;
        }
        System.out.println("----------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWait / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurn / n);
    }
}