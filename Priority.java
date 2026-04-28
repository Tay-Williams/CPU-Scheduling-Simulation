import java.util.*;

class Process {
    int pid;
    int arrivalTime;
    int burstTime;
    int priority;
    int remainingTime;
    int waitingTime;
    int turnaroundTime;
    int completionTime;
    
    // I/O tracking
    List<int[]> ioOperations;
    int currentIoPair;
    boolean inIo;
    int ioFinishTime;
    int originalBurst;
    
    public Process(int pid, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.completionTime = 0;
        this.originalBurst = burstTime;
        this.ioOperations = new ArrayList<>();
        this.currentIoPair = 0;
        this.inIo = false;
    }
}

public class PriorityScheduler {
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        System.out.print("Enter number of processes (minimum 20): ");
        int n = sc.nextInt();
        
        if (n < 20) {
            System.out.println("Warning: Minimum 20 processes required. Setting to 20.");
            n = 20;
        }
        
        List<Process> processes = new ArrayList<>();
        
        // Input for each process
        for (int i = 0; i < n; i++) {
            System.out.println("\n--- Process " + (i + 1) + " ---");
            System.out.print("Arrival Time: ");
            int arrival = sc.nextInt();
            
            System.out.print("Burst Time: ");
            int burst = sc.nextInt();
            
            System.out.print("Priority (lower = higher priority): ");
            int priority = sc.nextInt();
            
            System.out.print("I/O Start Time (0 if none): ");
            int ioStart = sc.nextInt();
            
            System.out.print("I/O Duration (0 if none): ");
            int ioDuration = sc.nextInt();
            
            Process p = new Process(i + 1, arrival, burst, priority);
            
            // Setup I/O operations
            if (ioStart > 0 && ioStart < burst) {
                p.ioOperations.add(new int[]{ioStart, ioDuration});
                p.ioOperations.add(new int[]{burst - ioStart, 0});
            } else {
                p.ioOperations.add(new int[]{burst, 0});
            }
            
            processes.add(p);
        }
        
        // Run Preemptive Priority Scheduling
        List<Process> results = preemptivePriority(processes);
        
        // Print results
        System.out.println("\n========== PREEMPTIVE PRIORITY SCHEDULING RESULTS ==========");
        System.out.println("PID\tArrival\tBurst\tPriority\tWaiting\tTurnaround");
        System.out.println("--------------------------------------------------------------");
        
        double totalWaiting = 0;
        double totalTurnaround = 0;
        
        for (Process p : results) {
            System.out.printf("%d\t%d\t%d\t%d\t\t%d\t%d\n", 
                p.pid, p.arrivalTime, p.burstTime, p.priority, 
                p.waitingTime, p.turnaroundTime);
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
        }
        
        System.out.println("--------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWaiting / results.size());
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaround / results.size());
        
        sc.close();
    }
    
    public static List<Process> preemptivePriority(List<Process> processes) {
        // Create a copy to avoid modifying original
        List<Process> processList = new ArrayList<>();
        for (Process p : processes) {
            Process copy = new Process(p.pid, p.arrivalTime, p.burstTime, p.priority);
            copy.ioOperations = p.ioOperations;
            copy.remainingTime = p.ioOperations.get(0)[0];
            copy.originalBurst = p.originalBurst;
            processList.add(copy);
        }
        
        int time = 0;
        int completed = 0;
        int n = processList.size();
        Process currentProcess = null;
        boolean ioBusy = false;
        Queue<Process> ioWaitingQueue = new LinkedList<>();
        int currentIoFinishTime = 0;
        
        while (completed < n) {
            // Get all processes that have arrived and not in I/O
            List<Process> availableProcesses = new ArrayList<>();
            for (Process p : processList) {
                if (p.arrivalTime <= time && p.remainingTime > 0 && !p.inIo) {
                    availableProcesses.add(p);
                }
            }
            
            // Check if I/O finished
            if (ioBusy && time >= currentIoFinishTime) {
                ioBusy = false;
                if (!ioWaitingQueue.isEmpty()) {
                    Process nextIo = ioWaitingQueue.poll();
                    nextIo.inIo = true;
                    currentIoFinishTime = time + nextIo.ioOperations.get(nextIo.currentIoPair)[1];
                    ioBusy = true;
                } else if (currentProcess != null && currentProcess.inIo) {
                    currentProcess.inIo = false;
                    currentProcess.currentIoPair++;
                    if (currentProcess.currentIoPair < currentProcess.ioOperations.size()) {
                        currentProcess.remainingTime = currentProcess.ioOperations.get(currentProcess.currentIoPair)[0];
                    }
                }
            }
            
            if (!availableProcesses.isEmpty()) {
                // Sort by priority (lower number = higher priority)
                availableProcesses.sort(Comparator.comparingInt((Process p) -> p.priority)
                                                  .thenComparingInt(p -> p.arrivalTime));
                
                Process nextProcess = availableProcesses.get(0);
                
                // Preemption: if higher priority process arrives
                if (currentProcess != null && currentProcess != nextProcess && !currentProcess.inIo) {
                    // Current process gets preempted, add back to ready queue
                    processList.add(currentProcess);
                    currentProcess = null;
                }
                
                if (currentProcess == null) {
                    currentProcess = nextProcess;
                    processList.remove(currentProcess);
                }
                
                // Execute for 1 time unit
                if (!currentProcess.inIo) {
                    currentProcess.remainingTime--;
                    time++;
                    
                    // Check if need to do I/O
                    int[] currentSegment = currentProcess.ioOperations.get(currentProcess.currentIoPair);
                    int executedSoFar = currentProcess.originalBurst - currentProcess.remainingTime;
                    
                    if (currentSegment[1] > 0 && executedSoFar >= currentSegment[0]) {
                        currentProcess.inIo = true;
                        if (!ioBusy) {
                            ioBusy = true;
                            currentIoFinishTime = time + currentSegment[1];
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
                        } else {
                            currentProcess.completionTime = time;
                            currentProcess.turnaroundTime = currentProcess.completionTime - currentProcess.arrivalTime;
                            currentProcess.waitingTime = currentProcess.turnaroundTime - currentProcess.burstTime;
                            completed++;
                            currentProcess = null;
                        }
                    }
                } else {
                    time++;
                }
            } else {
                time++;
            }
        }
        
        return processList;
    }
}
