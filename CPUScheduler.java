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

        //ensures users enter at least 20 processes
        while (n < 20) {
            System.out.print("Must be at least 20. Enter again: ");
            n = sc.nextInt();
        }

        //arrays to store cpu inputs
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

            //checks to see the following:
            //if the I/O starts after the burst time then no I/O actually occurs (because it's fully done with the CPU)
            //if ioStart = 0 meaning there is no I/O operation
            if (ioStart >= burst[i] || ioStart == 0) {

                //just store one segment for the total burst time
                list.add(new int[]{burst[i], 0});
            } else {

                //a process first runs until the process needs to execute I/O, the runs again for the remaining burst time afterwards
                list.add(new int[]{ioStart, ioDur});
                list.add(new int[]{burst[i] - ioStart, 0});
            }

            ioOps[i] = list;
        }

        simulateFCFS(n, arrival, burst, ioOps);
    }

    static void simulateFCFS(int n, int[] arrival, int[] burst, List<int[]>[] ioOps) {
        
        //stores cpu meterics
        int[] completion = new int[n];
        int[] waiting = new int[n];
        int[] segment = new int[n];

        //stores the last time a process enters the ready queue, will use again for computing waiting time (at least i think i haven't figured it out yet, delete if not)
        int[] lastReady = new int[n];

        int[] remaining = new int[n];

        //starts every process on it's first segment
        for (int i = 0; i < n; i++) {

            remaining[i] = ioOps[i].get(0)[0];
            segment[i] = 0;

            //process just arrives in the ready queue
            lastReady[i] = arrival[i];
        }

        
        PriorityQueue<int[]> events = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        
        //a ready queue for processes that will be sorted based on arrival time (shortest -> longest)
        PriorityQueue<Integer> ready = new PriorityQueue<>((a, b) -> Integer.compare(arrival[a], arrival[b]) );

        //checking if a processes burst time is zero
        int activeCount = 0;
        for (int i = 0; i < n; i++) {

            //no burst time means that as soon as the process arrives in the ready queue it is finished. So set arrival time = completion time
            if (burst[i] == 0) {

                //processes with no burst time complete as soon as they arrive (at least they should theoretically, idk how it works in the real world)
                completion[i] = arrival[i];

                //if a process finishes instantly, it also means that it doesn't wait at all
                waiting[i] = 0;
                continue;
            }

            //adds an arrival event for the processes that will need the CPU (they have a burst time > 0)
            events.add(new int[]{arrival[i], 0, i});
            lastReady[i] = arrival[i];
            activeCount++;
        }

        //a waiting queue to hold the processes waiting for I/O operations
        Queue<Integer> ioWait = new LinkedList<>();

        //only 1 process can execute I/O at a time 
        boolean ioBusy = false;

        //stores the PID of the process currently in the CPU (-1 if there is none)
        int cpu = -1;
        int time = 0;

        //number of completed processes
        int completed = 0;

        //will keep looping until all processes are completed
        while (completed < activeCount) {

            //get all of the events the occur at the current time
            while (!events.isEmpty() && events.peek()[0] == time) {
                int[] ev = events.poll();
                int pid = ev[2];

                //event 0: a process arrives in the ready queue
                if (ev[1] == 0) {

                    //add the process to the ready queue
                    lastReady[pid] = time;
                    ready.add(pid);

                //event 2: a process finishes I/O operations
                } else if (ev[1] == 2) {  

                    //the I/O is free since the process is done with it
                    ioBusy = false;

                    //the process moves on to it's next segment of execution
                    segment[pid]++;

                    //checks to see if the process has another segement
                    if (segment[pid] < ioOps[pid].size()) {

                        //if it needs to execute again it goes back into the ready queue
                        remaining[pid] = ioOps[pid].get(segment[pid])[0];
                        lastReady[pid] = time;
                        ready.add(pid);

                    } else {

                        //if it doesn't have another segment the process is finished execution
                        completion[pid] = time;
                        completed++;
                    }

                    //checks to see if there are any processes waiting for I/O operations currently
                    if (!ioWait.isEmpty()) {

                        //the next process in the waiting queue "starts" I/O operations
                        int nextPid = ioWait.poll();
                        int ioDur = ioOps[nextPid].get(segment[nextPid])[1];
                        ioBusy = true;

                        //adds a new I/O complete event
                        events.add(new int[]{time + ioDur, 2, nextPid});
                    }
                }
            }

            //if a process that arrives in the ready queue has an earlier (smaller) arrival time then we prempt the process currently in the CPU
            if (cpu != -1 && !ready.isEmpty() && arrival[ready.peek()] < arrival[cpu]) {
                
                //the preempted process goes back into the ready queue
                lastReady[cpu] = time;
                ready.add(cpu);
                cpu = -1;
            }

            //if the CPU is free and there is a process in the ready queue, then get the next process in the ready queue
            if (cpu == -1 && !ready.isEmpty()) {
                cpu = ready.poll();

                //waiting time in the ready queue [process] = (the current time, which is the time it leaves the ready queue) - (the time it last entered the ready queue)
                waiting[cpu] = waiting[cpu] + (time - lastReady[cpu] );
            }

            int nextTime = Integer.MAX_VALUE;
            //current CPU burst finishes
            if (cpu != -1) {

                nextTime = time + remaining[cpu];
            }

            //if there are more events to execute
            if (!events.isEmpty()) {

                //get the next event (either a new process arrives or an I/O completes)
                nextTime = Math.min(nextTime, events.peek()[0]);
            }

            //if there is nothing happends at the current time
            if (nextTime == Integer.MAX_VALUE) {

                if (cpu == -1 && ready.isEmpty()) {
                    break;
                }

                //move to the next second
                time++;
                continue;
            }

            int elapsed = nextTime - time;
            if (cpu != -1) {

                remaining[cpu] = remaining[cpu] - elapsed;
            }
            time = nextTime;

            //if the CPU process finishes executing its current segment
            if (cpu != -1 && remaining[cpu] == 0) {
                
                int ioDur = ioOps[cpu].get(segment[cpu])[1];

                //checks to see if there is an I/O operation left
                if (ioDur > 0) {

                    if (!ioBusy) {

                        //create a new I/O complete event
                        ioBusy = true;
                        events.add(new int[]{time + ioDur, 2, cpu});

                    } else {

                        //banishes the process to the waiting queue
                        ioWait.add(cpu);
                    }

                } else {

                    //since there is no I/O, move to next segment
                    segment[cpu]++;
                    if (segment[cpu] < ioOps[cpu].size()) {

                        remaining[cpu] = ioOps[cpu].get(segment[cpu])[0];
                        lastReady[cpu] = time;
                        ready.add(cpu);
                    } else {

                        //if there is no next segment, the process is donezo
                        completion[cpu] = time;
                        completed++;
                    }
                }

                cpu = -1;
            }
        }

        //printing CPU numbers and stuff idk my brain is tired and i wanna sleep
        System.out.println("\n========== FCFS SCHEDULING RESULTS ==========");
        System.out.println("PID\tArrival\tBurst\tCompletion\tWaiting\tTurnaround");
        System.out.println("----------------------------------------------------------");

        double totalWaiting = 0, totalTurnaround = 0;
        for (int i = 0; i < n; i++) {

            int tat = completion[i] - arrival[i];
            totalWaiting = totalWaiting + waiting[i];
            totalTurnaround = totalTurnaround + tat;

            System.out.printf("%d\t%d\t%d\t%d\t\t%d\t%d\n", (i + 1), arrival[i], burst[i], completion[i], waiting[i], tat);
        }

        System.out.println("----------------------------------------------------------");

        System.out.printf("Average Waiting Time: %.2f\n", totalWaiting / activeCount);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaround / activeCount);
    }
}

class Round_Robin_Scheduling {
    public static void main(String[] args) {

        Scanner keyboard = new Scanner(System.in);

        System.out.print("Enter the number of processes to simulate (minimum of 20):");
        int n = keyboard.nextInt();

        //checks to see if the user entered at least 20 processes
        while (n < 20) {
            System.out.print("\nError: Must add at least 20 processes. \nEnter number of processes: ");
            n = keyboard.nextInt();
        }

        int[] arrivalTimes = new int[n];
        int[] burstTimes = new int[n];
        List<int[]>[] IO_Operations = new List[n];

        //getting user input for each process
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

            //contains a list of I/O pairs (I/O start time, I/O duration)
            List<int[]> IO_Pair_List = new ArrayList<>();

            //if the I/O Start Time is longer than the processes' Burst Time
            if (IO_Start >= burstTimes[i]) {

                //add a I/O pair where there is only the CPU Burst Time and no I/O time
                IO_Pair_List.add(new int[]{burstTimes[i], 0});

            } else {

                IO_Pair_List.add(new int[]{IO_Start, IO_Duration});

                //after first I/O operation is finshed, the process just needs to execute its remaining Burst Time
                IO_Pair_List.add(new int[]{burstTimes[i] - IO_Start, 0});
            }

            IO_Operations[i] = IO_Pair_List;
        }

        //user enters the time quantum length
        System.out.print("\nEnter Time Quantum Length: ");
        int timeQuantum = keyboard.nextInt();

        //arrays to track each processes' status
        int[] remaining_CPU_Time = new int[n];
        int[] Current_IO_Pair = new int[n];
        int[] completionTime = new int[n];
        int[] waitingTime = new int[n];

        //stores the last time a process entered the ready queue 
        int[] Last_Ready_Time = new int[n];
        boolean[] finished = new boolean[n];

        //set every process to its first CPU time
        int activeCount = 0;
        for (int i = 0; i < n; i++) {

            //checking if a process's burst time is zero
            if (burstTimes[i] == 0) {

                //no burst time means that as soon as the process arrives in the ready queue it is finished. So set arrival time = completion time
                completionTime[i] = arrivalTimes[i];
                waitingTime[i] = 0;

                //since no CPU time is needed it is now finished execution
                finished[i] = true;
                continue;
            }

            remaining_CPU_Time[i] = IO_Operations[i].get(0)[0];
            Current_IO_Pair[i] = 0;
            activeCount++;
        }

        //a priority queue to store arrival times from shortest to longest
        PriorityQueue<int[]> Sorted_Arrival_Times = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        for (int i = 0; i < n; i++) {
            if (burstTimes[i] > 0) {

                //add the arrival times of each process to the sorted queue
                Sorted_Arrival_Times.add(new int[]{arrivalTimes[i], 0, i});
            }
        }

        //making the ready queue
        Queue<Integer> readyQueue = new LinkedList<>();

        //for when a process is executing its I/O operations
        boolean IO_Busy = false;

        //for when processes are waiting for I/O
        Queue<Integer> IO_Waiting_Queue = new LinkedList<>();

        //stores the PID of the process that is currently executing in the CPU (-1 if there is no process in the CPU )
        int Current_CPU_Pid = -1;

        //tracks the current time
        int currentTime = 0;

        //CPU Scheduling continues as long as there is a process: waiting to enter the system, in the ready queue, currently executing I/O operations, or in the waiting queue
        while (!Sorted_Arrival_Times.isEmpty() || !readyQueue.isEmpty() || Current_CPU_Pid != -1 || IO_Busy || !IO_Waiting_Queue.isEmpty()) {

            //will deal with whatever event is occuring at the current time
            while (!Sorted_Arrival_Times.isEmpty() && Sorted_Arrival_Times.peek()[0] <= currentTime) {
                
                //extracts current event
                int[] ev = Sorted_Arrival_Times.poll();
                
                //if it's an arrival event
                if (ev[1] == 0) {

                    int pid = ev[2];

                    //ignore any already finished processes
                    if (finished[pid]) {
                        continue;
                    }

                    //add process to the ready queue
                    Last_Ready_Time[pid] = currentTime;
                    readyQueue.add(pid);
                }
            }

            //runs the next process when there isn't a process in the CPU yet and there is a process in the ready queue
            if (Current_CPU_Pid == -1 && !readyQueue.isEmpty()) {

                Current_CPU_Pid = readyQueue.poll();
                waitingTime[Current_CPU_Pid] += currentTime - Last_Ready_Time[Current_CPU_Pid];
                
                //stores the amount of time the process executed for (either because it got preempted because it reached the time quantum or because it finished its burst time)
                int Current_CPU_Time_Slice = Math.min(timeQuantum, remaining_CPU_Time[Current_CPU_Pid]);
                
                //adds the process back into the queue
                Sorted_Arrival_Times.add(new int[]{currentTime + Current_CPU_Time_Slice, 1, Current_CPU_Pid});
            }


            int nextTime = Integer.MAX_VALUE;

            //if there is another event
            if (!Sorted_Arrival_Times.isEmpty()) {

                nextTime = Sorted_Arrival_Times.peek()[0];
            }

            if (Current_CPU_Pid != -1) {

                nextTime = Math.min(nextTime, currentTime + Math.min(timeQuantum, remaining_CPU_Time[Current_CPU_Pid]));
            }

            if (nextTime == Integer.MAX_VALUE) {

                break;
            }

            currentTime = nextTime;

            //will process events are occur currently
            while (!Sorted_Arrival_Times.isEmpty() && Sorted_Arrival_Times.peek()[0] == currentTime) {

                int[] nextEvent = Sorted_Arrival_Times.poll();
                int eventType = nextEvent[1];
                int nextPID = nextEvent[2];

                if (finished[nextPID]) {
                    continue;
                }

                //event 0: a process arrives
                if (eventType == 0) {

                    //add the process to the ready queue
                    Last_Ready_Time[nextPID] = currentTime;
                    readyQueue.add(nextPID);

                //event 1: current CPU segment is done or the burst time is finished
                } else if (eventType == 1) {

                    if (finished[nextPID]) {
                        continue;
                    }

                    //subtracy the remaining time from the duration the process just executed for
                    int used = Math.min(timeQuantum, remaining_CPU_Time[nextPID]);
                    remaining_CPU_Time[nextPID] = remaining_CPU_Time[nextPID] - used;

                    //if the current CPU burst is done
                    if (remaining_CPU_Time[nextPID] == 0) {

                        //get the I/O duration of the process
                        int Current_IO_Duration = IO_Operations[nextPID].get(Current_IO_Pair[nextPID])[1];

                        //checks to see if I/O operation is finished
                        if (Current_IO_Duration > 0) {

                            //checks to see if there is another process currently executing I/O operations
                            if (!IO_Busy) {

                                //set the I/O operator to be busy (only one process can execute I/O at a time)
                                IO_Busy = true;

                                //create a I/O done event
                                Sorted_Arrival_Times.add(new int[]{currentTime + Current_IO_Duration, 2, nextPID});
                            
                            } else {

                                //if I/O is busy (with another process), adds the process to the I/O waiting queue
                                IO_Waiting_Queue.add(nextPID);
                            }

                        //if the I/O operation is finished
                        } else {

                            //get the next I/O pair for the process
                            Current_IO_Pair[nextPID]++;

                            //checks to see if there is another I/O operation
                            if (Current_IO_Pair[nextPID] < IO_Operations[nextPID].size()) {
                                
                                remaining_CPU_Time[nextPID] = IO_Operations[nextPID].get(Current_IO_Pair[nextPID])[0];
                                Last_Ready_Time[nextPID] = currentTime;
                                
                                //add process back into ready queue
                                readyQueue.add(nextPID);

                            //if there are no more segments
                            } else {

                                //store completion time and set process to finished
                                completionTime[nextPID] = currentTime;
                                finished[nextPID] = true;
                            }
                        }

                    //if the CPU burst isn't done (meaning its time quantum ran out)
                    } else {

                        //store the time when the process goes back into the ready queue
                        Last_Ready_Time[nextPID] = currentTime;

                        //add process back into the ready queue
                        readyQueue.add(nextPID);
                    }

                    Current_CPU_Pid = -1;

                // I/O is done
                } else if (eventType == 2) {

                    IO_Busy = false;

                    Current_IO_Pair[nextPID]++;

                    //if there is another CPU burst segment to execute
                    if (Current_IO_Pair[nextPID] < IO_Operations[nextPID].size()) {

                        remaining_CPU_Time[nextPID] = IO_Operations[nextPID].get(Current_IO_Pair[nextPID])[0];

                        //adds the process back into the ready queue for continued later execution and stores the time in went in
                        Last_Ready_Time[nextPID] = currentTime;
                        readyQueue.add(nextPID);
                    
                    //if there are no more segments (the process is done)
                    } else {

                        //set process to finished
                        completionTime[nextPID] = currentTime;
                        finished[nextPID] = true;
                    }

                    //checks to see if there are any processes in the waiting queue (waiting for I/O)
                    if (!IO_Waiting_Queue.isEmpty()) {

                        //get the next process in the waiting queue
                        int nextIOPid = IO_Waiting_Queue.poll();

                        //get the I/O duration of the process
                        int io_Duration = IO_Operations[nextIOPid].get(Current_IO_Pair[nextIOPid])[1];
                        
                        IO_Busy = true;
                        
                        //add a new I/O done event
                        Sorted_Arrival_Times.add(new int[]{currentTime + io_Duration, 2, nextIOPid});
                    }
                }
            }
        }

        //printing CPU metrics
        System.out.println("\n***Round Robin Scheduling***");
        System.out.println("------------------------------------------------------------------------------");
        System.out.printf("%-8s %-15s %-18s %-15s %s\n", "PID", "Arrival Time", "Completion Time", "Waiting Time", "Turnaround Time");

        int totalWaitingTime = 0;
        int totalTurnaroundTime = 0;

        //a loop to calculate and display the waiting and turnarounf times
        for (int i = 0; i < n; i++) {

            int turnaround = completionTime[i] - arrivalTimes[i];
            
            totalWaitingTime = totalWaitingTime + waitingTime[i];
            totalTurnaroundTime = totalTurnaroundTime + turnaround;

            System.out.printf("%-8d %-15d %-18d %-15d %d\n", i + 1, arrivalTimes[i], completionTime[i], waitingTime[i], turnaround);
        }

        System.out.println("------------------------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", totalWaitingTime / (double) activeCount);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaroundTime / (double) activeCount);
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

        //stores the priority numbers for each process
        int[] priority = new int[n];

        List<int[]>[] ioOps = new List[n];

        //reading in inputs from user
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
        
        //arrays for tracking stuff
        int[] remaining = new int[n];
        int[] segment = new int[n];
        int[] completion = new int[n];
        int[] waiting = new int[n];
        int[] lastReady = new int[n];

        int activeCount = 0;

        //ignore processes with 0 for their burst time
        for (int i = 0; i < n; i++) {

            if (burst[i] == 0) {
                completion[i] = arrival[i];
                waiting[i] = 0;
                continue;
            }

            remaining[i] = ioOps[i].get(0)[0];
            segment[i] = 0;
            lastReady[i] = arrival[i];
            activeCount++;
        }

        PriorityQueue<int[]> events = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        
        //adding arrival events for active processes (processes with a burst time > 0 )
        for (int i = 0; i < n; i++) {

            if (burst[i] > 0) {
                events.add(new int[]{arrival[i], 0, i});
            }
        }

        //the ready queue is ordered by priority initially and then if the priorities are the same, then it orders them by arrival time (I think that's how you deal with ties in priority scheduling)
        PriorityQueue<Integer> ready = new PriorityQueue<>((a, b) -> {
            
            //priority ordering
            if (priority[a] != priority[b]) {
                return Integer.compare(priority[a], priority[b]);
            }
            
            //arrival time ordering
            return Integer.compare(arrival[a], arrival[b]);
        });

        //I/O wait queue
        Queue<Integer> ioWait = new LinkedList<>();

        boolean ioBusy = false;
        int cpu = -1;
        int time = 0;
        int completed = 0;

        while (completed < activeCount) {

            //only deal current arrival events
            while (!events.isEmpty() && events.peek()[0] <= time && events.peek()[1] == 0) {
                
                int[] ev = events.poll();
                int pid = ev[2];
                lastReady[pid] = time;
                ready.add(pid);
            }

            //checks to see if the process in the ready queue has a higher priority (lower number) than the process currently in the CPU
            if (cpu != -1 && !ready.isEmpty() && priority[ready.peek()] < priority[cpu]) {
                
                //preempt the current process and banish it back to the ready queue
                lastReady[cpu] = time;
                ready.add(cpu);
                cpu = -1;
            }

            //if the CPU is justing sitting there wasting time being a bum, then put it to work by giving it the highest priority process (can you tell im loosing my mind LMAO)
            if (cpu == -1 && !ready.isEmpty()) {

                cpu = ready.poll();
                waiting[cpu] = waiting[cpu] + (time - lastReady[cpu] );
            }

            int nextTime = Integer.MAX_VALUE;
            if (cpu != -1) {

                nextTime = time + remaining[cpu];
            }

            //as long as there are events, get the next event
            if (!events.isEmpty()) {

                nextTime = Math.min(nextTime, events.peek()[0]);
            }

            if (nextTime == Integer.MAX_VALUE) {

                if (!ready.isEmpty()) {
                    continue;
                }

                if (!events.isEmpty()) {

                    //update current time
                    time = events.peek()[0];
                    continue;
                }

                break;
            }

            int elapsed = nextTime - time;
            if (cpu != -1) {
                //reduce remaining time
                remaining[cpu] -= elapsed;
            }

            time = nextTime;

            //deal with new arrival evnets at the new current time
            while (!events.isEmpty() && events.peek()[0] == time && events.peek()[1] == 0) {
                
                int[] ev = events.poll();
                int pid = ev[2];
                lastReady[pid] = time;
                ready.add(pid);
            }

            //checks to see if the current segment for the process is done (remaining time is 0)
            if (cpu != -1 && remaining[cpu] == 0) {

                int ioDur = ioOps[cpu].get(segment[cpu])[1];

                //checks to see if the process needs I/O operation
                if (ioDur > 0) {

                    //if I/O is free then creates a new I/O complete event
                    if (!ioBusy) {
                        ioBusy = true;
                        events.add(new int[]{time + ioDur, 2, cpu});

                    } else {

                        //if I/O is not free, then exile the process to the waiting queue for all eternity
                        ioWait.add(cpu);
                    }

                //if it doesn't need I/O operating time, then just move on with the next segment
                } else {

                    segment[cpu]++;
                    //checks to see if there is a next segment
                    if (segment[cpu] < ioOps[cpu].size()) {

                        remaining[cpu] = ioOps[cpu].get(segment[cpu])[0];
                        lastReady[cpu] = time;
                        ready.add(cpu);

                    //if no next segment then the process is done
                    } else {

                        completion[cpu] = time;
                        completed++;
                    }
                }
                cpu = -1;
            }

            //handling I/O
            while (!events.isEmpty() && events.peek()[0] == time && events.peek()[1] == 2) {
                
                int[] ioEvent = events.poll();
                int pid = ioEvent[2];
                ioBusy = false;
                segment[pid]++;

                //checks to see if there is another segment after finishing I/O
                if (segment[pid] < ioOps[pid].size()) {

                    remaining[pid] = ioOps[pid].get(segment[pid])[0];
                    lastReady[pid] = time;
                    ready.add(pid);

                //if no next segment after completing I/O, the process if finished
                } else {

                    completion[pid] = time;
                    completed++;
                }

                //if the waiting queue is not empty, have the next waiting process get I/O time
                if (!ioWait.isEmpty()) {

                    int next = ioWait.poll();
                    int dur = ioOps[next].get(segment[next])[1];
                    ioBusy = true;
                    //I/O complete event
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
        System.out.printf("Average Waiting Time: %.2f\n", totalWaiting / activeCount);
        System.out.printf("Average Turnaround Time: %.2f\n", totalTurnaround / activeCount);
    }
    
}//finally done
