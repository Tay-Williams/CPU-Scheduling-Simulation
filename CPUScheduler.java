import java.util.*;

public class CPUScheduler {
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
                FCFS.main(new String[0]);
                break;
            case 2:
                Round_Robin_Scheduling.main(new String[0]);
                break;
            case 3:
                PriorityScheduler.main(new String[0]);
                break;
            case 4:
                System.out.println("Exiting...");
                break;
            default:
                System.out.println("Invalid choice!");
        }

        sc.close();
    }
}

class FCFS {
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
        List<int[]>[] ioOps = new List[n];

        for (int i = 0; i < n; i++) {
            System.out.println("\nProcess " + (i + 1));
            System.out.print("Arrival Time: ");
            arrival[i] = sc.nextInt();
            System.out.print("Burst Time: ");
            burst[i] = sc.nextInt();
            System.out.print("I/O Start Time: ");
            int ioStart = sc.nextInt();
            System.out.print("I/O Duration: ");
            int ioDur = sc.nextInt();

            List<int[]> list = new ArrayList<>();
            if (ioStart >= burst[i] || ioStart == 0) {
                list.add(new int[]{burst[i], 0});
            } else {
                list.add(new int[]{ioStart, ioDur});
                list.add(new int[]{burst[i] - ioStart, 0});
            }
            ioOps[i] = list;
        }

        simulateFCFS(n, arrival, burst, ioOps);
    }

    static void simulateFCFS(int n, int[] arrival, int[] burst, List<int[]>[] ioOps) {
        int[] completion = new int[n];
        int[] waiting = new int[n];
        int[] segment = new int[n];
        int[] lastReady = new int[n];
        int[] remaining = new int[n];

        for (int i = 0; i < n; i++) {
            remaining[i] = ioOps[i].get(0)[0];
            segment[i] = 0;
            lastReady[i] = arrival[i];
        }

        PriorityQueue<int[]> events = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        for (int i = 0; i < n; i++) {
            events.add(new int[]{arrival[i], 0, i});
        }

        Queue<Integer> ready = new LinkedList<>();
        Queue<Integer> ioWait = new LinkedList<>();
        boolean ioBusy = false;
        int cpu = -1;
        int time = 0;
        int completed = 0;

        while (completed < n) {
            if (cpu == -1 && !ready.isEmpty()) {
                cpu = ready.poll();
                waiting[cpu] += time - lastReady[cpu];
                events.add(new int[]{time + remaining[cpu], 1, cpu});
            }

            int[] e = events.poll();
            if (e == null) {
                if (cpu == -1 && ready.isEmpty() && !ioBusy) {
                    int nextArrival = Integer.MAX_VALUE;
                    for (int i = 0; i < n; i++) {
                        if (remaining[i] > 0 && arrival[i] > time) nextArrival = Math.min(nextArrival, arrival[i]);
                    }
                    if (nextArrival != Integer.MAX_VALUE) time = nextArrival;
                    else time++;
                }
                continue;
            }

            time = e[0];
            int type = e[1];
            int pid = e[2];

            if (type == 0) {
                lastReady[pid] = time;
                ready.add(pid);
            } else if (type == 1) {
                int ioDur = ioOps[pid].get(segment[pid])[1];
                if (ioDur > 0) {
                    if (!ioBusy) {
                        ioBusy = true;
                        events.add(new int[]{time + ioDur, 2, pid});
                    } else {
                        ioWait.add(pid);
                    }
                } else {
                    segment[pid]++;
                    if (segment[pid] < ioOps[pid].size()) {
                        remaining[pid] = ioOps[pid].get(segment[pid])[0];
                        lastReady[pid] = time;
                        ready.add(pid);
                    } else {
                        completion[pid] = time;
                        completed++;
                    }
                }
                cpu = -1;
            } else {
                ioBusy = false;
                segment[pid]++;
                if (segment[pid] < ioOps[pid].size()) {
                    remaining[pid] = ioOps[pid].get(segment[pid])[0];
                    lastReady[pid] = time;
                    ready.add(pid);
                } else {
                    completion[pid] = time;
                    completed++;
                }
                if (!ioWait.isEmpty()) {
                    int next = ioWait.poll();
                    int dur = ioOps[next].get(segment[next])[1];
                    ioBusy = true;
                    events.add(new int[]{time + dur, 2, next});
                }
            }
        }

        System.out.println("\n========== FCFS SCHEDULING RESULTS ==========");
        System.out.println("PID\tArrival\tBurst\tCompletion\tWaiting\tTurnaround");
        System.out.println("----------------------------------------------------------");

        double totalWaiting = 0, totalTurnaround = 0;
        for (int i = 0; i < n; i++) {
            int tat = completion[i] - arrival[i];
            System.out.printf("%d\t%d\t%d\t%d\t\t%d\t%d\n", (i + 1), arrival[i], burst[i], completion[i], waiting[i], tat);
            totalWaiting += waiting[i];
            totalTurnaround += tat;
        }

        System.out.println("----------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWaiting / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaround / n);
    }
}

class Round_Robin_Scheduling {
    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);

        System.out.print("Enter the number of processes to simulate (minimum of 20):");
        int n = keyboard.nextInt();

        while (n < 20) {
            System.out.print("\nError: Must add at least 20 processes. \nEnter number of processes: ");
            n = keyboard.nextInt();
        }

        int[] arrivalTimes = new int[n];
        int[] burstTimes = new int[n];
        List<int[]>[] IO_Operations = new List[n];

        for (int i = 0; i < n; i++) {
            System.out.println("\n\nInput for Process " + (i + 1) + ":");
            System.out.print("Arrival Time: ");
            arrivalTimes[i] = keyboard.nextInt();
            System.out.print("Burst Time: ");
            burstTimes[i] = keyboard.nextInt();
            System.out.print("I/O Start Time: ");
            int IO_Start = keyboard.nextInt();
            System.out.print("I/O Duration: ");
            int IO_Duration = keyboard.nextInt();

            List<int[]> IO_Pair_List = new ArrayList<>();
            if (IO_Start >= burstTimes[i]) {
                IO_Pair_List.add(new int[]{burstTimes[i], 0});
            } else {
                IO_Pair_List.add(new int[]{IO_Start, IO_Duration});
                IO_Pair_List.add(new int[]{burstTimes[i] - IO_Start, 0});
            }
            IO_Operations[i] = IO_Pair_List;
        }

        System.out.print("\nEnter Time Quantum Length: ");
        int timeQuantum = keyboard.nextInt();

        int[] remaining_CPU_Time = new int[n];
        int[] Current_IO_Pair = new int[n];
        int[] completionTime = new int[n];
        int[] waitingTime = new int[n];
        int[] Last_Ready_Time = new int[n];
        boolean[] finished = new boolean[n];

        for (int i = 0; i < n; i++) {
            remaining_CPU_Time[i] = IO_Operations[i].get(0)[0];
            Current_IO_Pair[i] = 0;
        }

        PriorityQueue<int[]> Sorted_Arrival_Times = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        for (int i = 0; i < n; i++) Sorted_Arrival_Times.add(new int[]{arrivalTimes[i], 0, i});

        Queue<Integer> readyQueue = new LinkedList<>();
        boolean IO_Busy = false;
        Queue<Integer> IO_Waiting_Queue = new LinkedList<>();
        int Current_CPU_Pid = -1;
        int currentTime = 0;

        while (!Sorted_Arrival_Times.isEmpty() || !readyQueue.isEmpty() || Current_CPU_Pid != -1 || IO_Busy || !IO_Waiting_Queue.isEmpty()) {
            while (!Sorted_Arrival_Times.isEmpty() && Sorted_Arrival_Times.peek()[0] <= currentTime) {
                int[] ev = Sorted_Arrival_Times.poll();
                if (ev[1] == 0) {
                    int pid = ev[2];
                    Last_Ready_Time[pid] = currentTime;
                    readyQueue.add(pid);
                }
            }

            if (Current_CPU_Pid == -1 && !readyQueue.isEmpty()) {
                Current_CPU_Pid = readyQueue.poll();
                waitingTime[Current_CPU_Pid] += currentTime - Last_Ready_Time[Current_CPU_Pid];
                int cpuSlice = Math.min(timeQuantum, remaining_CPU_Time[Current_CPU_Pid]);
                Sorted_Arrival_Times.add(new int[]{currentTime + cpuSlice, 1, Current_CPU_Pid});
            }

            int nextTime = Integer.MAX_VALUE;
            if (!Sorted_Arrival_Times.isEmpty()) nextTime = Sorted_Arrival_Times.peek()[0];
            if (Current_CPU_Pid != -1) nextTime = Math.min(nextTime, currentTime + Math.min(timeQuantum, remaining_CPU_Time[Current_CPU_Pid]));
            if (nextTime == Integer.MAX_VALUE) break;
            currentTime = nextTime;

            while (!Sorted_Arrival_Times.isEmpty() && Sorted_Arrival_Times.peek()[0] == currentTime) {
                int[] nextEvent = Sorted_Arrival_Times.poll();
                int eventType = nextEvent[1];
                int nextPID = nextEvent[2];

                if (eventType == 0) {
                    Last_Ready_Time[nextPID] = currentTime;
                    readyQueue.add(nextPID);
                } else if (eventType == 1) {
                    if (finished[nextPID]) continue;

                    int used = Math.min(timeQuantum, remaining_CPU_Time[nextPID]);
                    remaining_CPU_Time[nextPID] -= used;

                    if (remaining_CPU_Time[nextPID] == 0) {
                        int ioDuration = IO_Operations[nextPID].get(Current_IO_Pair[nextPID])[1];
                        if (ioDuration > 0) {
                            if (!IO_Busy) {
                                IO_Busy = true;
                                Sorted_Arrival_Times.add(new int[]{currentTime + ioDuration, 2, nextPID});
                            } else {
                                IO_Waiting_Queue.add(nextPID);
                            }
                        } else {
                            Current_IO_Pair[nextPID]++;
                            if (Current_IO_Pair[nextPID] < IO_Operations[nextPID].size()) {
                                remaining_CPU_Time[nextPID] = IO_Operations[nextPID].get(Current_IO_Pair[nextPID])[0];
                                Last_Ready_Time[nextPID] = currentTime;
                                readyQueue.add(nextPID);
                            } else {
                                completionTime[nextPID] = currentTime;
                                finished[nextPID] = true;
                            }
                        }
                    } else {
                        Last_Ready_Time[nextPID] = currentTime;
                        readyQueue.add(nextPID);
                    }

                    Current_CPU_Pid = -1;
                } else if (eventType == 2) {
                    IO_Busy = false;
                    Current_IO_Pair[nextPID]++;
                    if (Current_IO_Pair[nextPID] < IO_Operations[nextPID].size()) {
                        remaining_CPU_Time[nextPID] = IO_Operations[nextPID].get(Current_IO_Pair[nextPID])[0];
                        Last_Ready_Time[nextPID] = currentTime;
                        readyQueue.add(nextPID);
                    } else {
                        completionTime[nextPID] = currentTime;
                        finished[nextPID] = true;
                    }
                    if (!IO_Waiting_Queue.isEmpty()) {
                        int nextIOPid = IO_Waiting_Queue.poll();
                        int ioDuration = IO_Operations[nextIOPid].get(Current_IO_Pair[nextIOPid])[1];
                        IO_Busy = true;
                        Sorted_Arrival_Times.add(new int[]{currentTime + ioDuration, 2, nextIOPid});
                    }
                }
            }
        }

        System.out.println("\n***Round Robin Scheduling***");
        System.out.println("------------------------------------------------------------------------------");
        System.out.printf("%-8s %-15s %-18s %-15s %s\n", "PID", "Arrival Time", "Completion Time", "Waiting Time", "Turnaround Time");

        int totalWaitingTime = 0;
        int totalTurnaroundTime = 0;

        for (int i = 0; i < n; i++) {
            int turnaround = completionTime[i] - arrivalTimes[i];
            totalWaitingTime += waitingTime[i];
            totalTurnaroundTime += turnaround;
            System.out.printf("%-8d %-15d %-18d %-15d %d\n", i + 1, arrivalTimes[i], completionTime[i], waitingTime[i], turnaround);
        }
        System.out.println("------------------------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWaitingTime / (double) n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaroundTime / (double) n);
    }
}

class PriorityScheduler {
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
        List<int[]>[] ioOps = new List[n];

        for (int i = 0; i < n; i++) {
            System.out.println("\nProcess " + (i + 1));
            System.out.print("Arrival Time: ");
            arrival[i] = sc.nextInt();
            System.out.print("Burst Time: ");
            burst[i] = sc.nextInt();
            System.out.print("Priority (lower = higher): ");
            priority[i] = sc.nextInt();
            System.out.print("I/O Start Time: ");
            int ioStart = sc.nextInt();
            System.out.print("I/O Duration: ");
            int ioDur = sc.nextInt();

            List<int[]> list = new ArrayList<>();
            if (ioStart >= burst[i] || ioStart == 0) {
                list.add(new int[]{burst[i], 0});
            } else {
                list.add(new int[]{ioStart, ioDur});
                list.add(new int[]{burst[i] - ioStart, 0});
            }
            ioOps[i] = list;
        }

        simulatePriority(n, arrival, burst, priority, ioOps);
    }

    static void simulatePriority(int n, int[] arrival, int[] burst, int[] priority, List<int[]>[] ioOps) {
        int[] remaining = new int[n];
        int[] segment = new int[n];
        int[] completion = new int[n];
        int[] waiting = new int[n];
        int[] lastReady = new int[n];

        for (int i = 0; i < n; i++) {
            remaining[i] = ioOps[i].get(0)[0];
            segment[i] = 0;
            lastReady[i] = arrival[i];
        }

        PriorityQueue<int[]> events = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        for (int i = 0; i < n; i++) events.add(new int[]{arrival[i], 0, i});

        PriorityQueue<Integer> ready = new PriorityQueue<>((a, b) -> {
            if (priority[a] != priority[b]) return Integer.compare(priority[a], priority[b]);
            return Integer.compare(arrival[a], arrival[b]);
        });

        Queue<Integer> ioWait = new LinkedList<>();
        boolean ioBusy = false;
        int cpu = -1;
        int time = 0;
        int completed = 0;

        while (completed < n) {
            while (!events.isEmpty() && events.peek()[0] <= time && events.peek()[1] == 0) {
                int[] ev = events.poll();
                int pid = ev[2];
                lastReady[pid] = time;
                ready.add(pid);
            }

            if (cpu != -1 && !ready.isEmpty() && priority[ready.peek()] < priority[cpu]) {
                lastReady[cpu] = time;
                ready.add(cpu);
                cpu = -1;
            }

            if (cpu == -1 && !ready.isEmpty()) {
                cpu = ready.poll();
                waiting[cpu] += time - lastReady[cpu];
            }

            int nextTime = Integer.MAX_VALUE;
            if (cpu != -1) nextTime = time + remaining[cpu];
            if (!events.isEmpty()) nextTime = Math.min(nextTime, events.peek()[0]);
            if (nextTime == Integer.MAX_VALUE) {
                if (!ready.isEmpty()) continue;
                if (!events.isEmpty()) {
                    time = events.peek()[0];
                    continue;
                }
                break;
            }

            int elapsed = nextTime - time;
            if (cpu != -1) remaining[cpu] -= elapsed;
            time = nextTime;

            while (!events.isEmpty() && events.peek()[0] == time && events.peek()[1] == 0) {
                int[] ev = events.poll();
                int pid = ev[2];
                lastReady[pid] = time;
                ready.add(pid);
            }

            if (cpu != -1 && remaining[cpu] == 0) {
                int ioDur = ioOps[cpu].get(segment[cpu])[1];
                if (ioDur > 0) {
                    if (!ioBusy) {
                        ioBusy = true;
                        events.add(new int[]{time + ioDur, 2, cpu});
                    } else {
                        ioWait.add(cpu);
                    }
                } else {
                    segment[cpu]++;
                    if (segment[cpu] < ioOps[cpu].size()) {
                        remaining[cpu] = ioOps[cpu].get(segment[cpu])[0];
                        lastReady[cpu] = time;
                        ready.add(cpu);
                    } else {
                        completion[cpu] = time;
                        completed++;
                    }
                }
                cpu = -1;
            }

            while (!events.isEmpty() && events.peek()[0] == time && events.peek()[1] == 2) {
                int[] ioEvent = events.poll();
                int pid = ioEvent[2];
                ioBusy = false;
                segment[pid]++;
                if (segment[pid] < ioOps[pid].size()) {
                    remaining[pid] = ioOps[pid].get(segment[pid])[0];
                    lastReady[pid] = time;
                    ready.add(pid);
                } else {
                    completion[pid] = time;
                    completed++;
                }
                if (!ioWait.isEmpty()) {
                    int next = ioWait.poll();
                    int dur = ioOps[next].get(segment[next])[1];
                    ioBusy = true;
                    events.add(new int[]{time + dur, 2, next});
                }
            }
        }

        System.out.println("\n========== PREEMPTIVE PRIORITY SCHEDULING RESULTS ==========");
        System.out.println("PID   Arrival Burst  Priority  Completion  Waiting  Turnaround");
        System.out.println("----------------------------------------------------------------");

        double totalWaiting = 0, totalTurnaround = 0;
        for (int i = 0; i < n; i++) {
            int tat = completion[i] - arrival[i];
            System.out.printf("%-5d %-7d %-6d %-9d %-11d %-8d %d\n", (i + 1), arrival[i], burst[i], priority[i], completion[i], waiting[i], tat);
            totalWaiting += waiting[i];
            totalTurnaround += tat;
        }

        System.out.println("----------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWaiting / n);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaround / n);
    }
}
