import java.util.*;

class Process {
    int id;
    int arrival;
    int burst;
    int completion;
    int turnaround;
    int waiting;
    int remainingTime;
    
    // I/O tracking
    List<int[]> ioOperations; // each entry: [cpuBurst, ioDuration]
    int currentIoPair;
    boolean inIo;
    int ioFinishTime;
    
    Process(int id, int arrival, int burst) {
        this.id = id;
        this.arrival = arrival;
        this.burst = burst;
        this.remainingTime = burst;
        this.ioOperations = new ArrayList<>();
        this.currentIoPair = 0;
        this.inIo = false;
    }
}

public class FCFS {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of processes (minimum 20): ");
        int n = sc.nextInt();
        
        if (n < 20) {
            System.out.println("Warning: Minimum 20 processes required. Setting to 20.");
            n = 20;
        }

        Process[] processes = new Process[n];

        // Input for each process
        for (int i = 0; i < n; i++) {
            System.out.println("\n--- Process " + (i + 1) + " ---");
            System.out.print("Arrival Time: ");
            int arrival = sc.nextInt();

            System.out.print("Burst Time: ");
            int burst = sc.nextInt();

            System.out.print("I/O Start Time (0 if none): ");
            int ioStart = sc.nextInt();

            System.out.print("I/O Duration (0 if none): ");
            int ioDuration = sc.nextInt();

            processes[i] = new Process(i + 1, arrival, burst);
            
            // Setup I/O operations
            if (ioStart > 0 && ioStart < burst) {
                processes[i].ioOperations.add(new int[]{ioStart, ioDuration});
                processes[i].ioOperations.add(new int[]{burst - ioStart, 0});
            } else {
                processes[i].ioOperations.add(new int[]{burst, 0});
            }
        }

        fcfsSchedule(processes);
        sc.close();
    }

    public static void fcfsSchedule(Process[] processes) {
        // Sort by arrival time
        Arrays.sort(processes, Comparator.comparingInt(p -> p.arrival));

        int currentTime = 0;
        int completed = 0;
        int n = processes.length;
        
        // Track remaining CPU time for each process
        for (Process p : processes) {
            p.remainingTime = p.ioOperations.get(0)[0];
        }
        
        Queue<Process> readyQueue = new LinkedList<>();
        boolean ioBusy = false;
        Queue<Process> ioWaitingQueue = new LinkedList<>();
        Process currentProcess = null;
        int currentIoFinishTime = 0;
        
        // Add first process that arrives at time 0
        for (Process p : processes) {
            if (p.arrival == 0) {
                readyQueue.add(p);
            }
        }
        
        while (completed < n) {
            // Add newly arrived processes
            for (Process p : processes) {
                if (p.arrival == currentTime && !readyQueue.contains(p) && p != currentProcess && p.remainingTime > 0) {
                    readyQueue.add(p);
                }
            }
            
            // If CPU is idle and ready queue not empty, start next process
            if (currentProcess == null && !readyQueue.isEmpty()) {
                currentProcess = readyQueue.poll();
            }
            
            // Check if I/O finished
            if (ioBusy && currentTime >= currentIoFinishTime) {
                ioBusy = false;
                Process ioProcess = ioWaitingQueue.poll();
                if (ioProcess != null) {
                    // Start next I/O
                    int[] nextSegment = ioProcess.ioOperations.get(ioProcess.currentIoPair);
                    currentIoFinishTime = currentTime + nextSegment[1];
                    ioBusy = true;
                } else {
                    // Return process to ready queue after I/O
                    if (currentProcess != null && currentProcess.inIo) {
                        currentProcess.inIo = false;
                        currentProcess.currentIoPair++;
                        if (currentProcess.currentIoPair < currentProcess.ioOperations.size()) {
                            currentProcess.remainingTime = currentProcess.ioOperations.get(currentProcess.currentIoPair)[0];
                            readyQueue.add(currentProcess);
                        } else {
                            // Process completed
                            currentProcess.completion = currentTime;
                            currentProcess.turnaround = currentProcess.completion - currentProcess.arrival;
                            currentProcess.waiting = currentProcess.turnaround - currentProcess.burst;
                            completed++;
                            currentProcess = null;
                        }
                    }
                }
            }
            
            // Execute current process
            if (currentProcess != null && !currentProcess.inIo) {
                currentProcess.remainingTime--;
                currentTime++;
                
                // Check if need to do I/O
                int[] currentSegment = currentProcess.ioOperations.get(currentProcess.currentIoPair);
                int executedSoFar = currentProcess.burst - currentProcess.remainingTime;
                
                if (currentSegment[1] > 0 && executedSoFar >= currentSegment[0]) {
                    // Time for I/O
                    currentProcess.inIo = true;
                    if (!ioBusy) {
                        ioBusy = true;
                        currentIoFinishTime = currentTime + currentSegment[1];
                    } else {
                        ioWaitingQueue.add(currentProcess);
                    }
                    currentProcess = null;
                }
                // Check if CPU burst finished
                else if (currentProcess.remainingTime == 0) {
                    currentProcess.currentIoPair++;
                    if (currentProcess.currentIoPair < currentProcess.ioOperations.size()) {
                        currentProcess.remainingTime = currentProcess.ioOperations.get(currentProcess.currentIoPair)[0];
                        readyQueue.add(currentProcess);
                    } else {
                        currentProcess.completion = currentTime;
                        currentProcess.turnaround = currentProcess.completion - currentProcess.arrival;
                        currentProcess.waiting = currentProcess.turnaround - currentProcess.burst;
                        completed++;
                    }
                    currentProcess = null;
                }
            } else {
                currentTime++;
            }
        }
        
        printResults(processes);
    }

    public static void printResults(Process[] processes) {
        System.out.println("\n========== FCFS SCHEDULING RESULTS ==========");
        System.out.println("PID\tArrival\tBurst\tWaiting\tTurnaround");
        System.out.println("------------------------------------------------");
        
        double totalWaiting = 0, totalTurnaround = 0;
        
        // Sort by ID for display
        Arrays.sort(processes, Comparator.comparingInt(p -> p.id));
        
        for (Process p : processes) {
            System.out.printf("%d\t%d\t%d\t%d\t%d\n", 
                p.id, p.arrival, p.burst, p.waiting, p.turnaround);
            totalWaiting += p.waiting;
            totalTurnaround += p.turnaround;
        }
        
        System.out.println("------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWaiting / processes.length);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaround / processes.length);
    }
}
