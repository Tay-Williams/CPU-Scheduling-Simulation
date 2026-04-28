import java.util.*;

public class Round_Robin_Scheduling {

    public static void main(String[] args) {
        
        Scanner keyboard = new Scanner (System.in);

        System.out.print("Enter the number of processes to simulate (minimum of 20):");
        int n = keyboard.nextInt();

        //checks to see if the user entered at least 20 processes
        if (n < 20) {
            System.out.print("\nError: Must add at least 20 processes. \nEnter number of processes: ");
            n = keyboard.nextInt();
        }
        
        int arrivalTimes [] = new int [n];

        List<int[]> [] IO_Operations = new List[n];

        for (int i = 0; i < n; i++) {

            //getting user input for each process
            System.out.println("\n\nInput for Process " + (i + 1) + ":" );
            System.out.print("Arrival Time: ");
            arrivalTimes[i] = keyboard.nextInt();

            System.out.print("Burst Time: ");
            int burstTime = keyboard.nextInt();

            System.out.print("I/O Start Time: ");
            int IO_Start = keyboard.nextInt();

            System.out.print("I/O Duration: ");
            int IO_Duration = keyboard.nextInt();

            //contains a list of I/O pairs (I/O start time, I/O duration)
            List<int[]> IO_Pair_List = new ArrayList<> ();

            //if the I/O Start Time is longer than the processes' Burst Time
            if (IO_Start >= burstTime) {

                //add a I/O pair where there is only the CPU Burst Time and no I/O time
                IO_Pair_List.add(new int []{burstTime, 0});

            } else {

                IO_Pair_List.add(new int []{IO_Start, IO_Duration});

                //after first I/O operation is finshed, the process just needs to execute its remaining Burst Time
                IO_Pair_List.add(new int[]{burstTime - IO_Start, 0});
            }

            IO_Operations[i] = IO_Pair_List;
        }

        //user enters the time quantum length
        System.out.print("\nEnter Time Quantum Length: ");
        int timeQuantum = keyboard.nextInt();

        //arrays to track each processes' status
        int[] remaining_CPU_Time = new int [n];
        int[] Current_IO_Pair = new int [n];
        int[] completionTime = new int[n];
        int[] waitingTime = new int [n];
        
        //stores the last time a process entered the ready queue 
        int[] Last_Ready_Time = new int[n];
        boolean[] finished = new boolean[n];

        //set every process to its first CPU time
        for (int i = 0; i < n; i++) {
            remaining_CPU_Time[i] = IO_Operations[i].get(0)[0];
            Current_IO_Pair[i] = 0;
        }

        //a priority queue to store arrival times from shortest to longest
        PriorityQueue<int[]> Sorted_Arrival_Times = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));

        for (int i = 0; i < n ; i++) {

            //add the arrival times of each process to the sorted queue
            Sorted_Arrival_Times.add(new int[]{arrivalTimes[i], 0, i});
        }

        Queue<Integer> readyQueue = new LinkedList<>();

        //for when a process is executing its I/O operations
        boolean IO_Busy = false;

        //for when processes are waiting for I/O
        Queue<Integer> IO_Waiting_Queue = new LinkedList<>();

        //stores the PID of the process that is currently executing in the CPU
        int Current_CPU_Pid = -1;

        //tracks when a process' current CPU Burst is done
        int Current_CPU_Burst_End = -1;

        int currentTime = 0;

        //CPU Scgeduling continues as long as there is a process: waiting to enter the system, in the ready queue, currently executing I/O operations, or in the waiting queue
        while (!Sorted_Arrival_Times.isEmpty() || !readyQueue.isEmpty() || IO_Busy == false || !IO_Waiting_Queue.isEmpty() ) {

            //runs the next process when there isn't a process in the CPU yet and there is a process in the ready queue
            if (Current_CPU_Pid == -1 && !readyQueue.isEmpty() ) {

                Current_CPU_Pid = readyQueue.poll();
                waitingTime[Current_CPU_Pid] = waitingTime[Current_CPU_Pid] + (currentTime - Last_Ready_Time[Current_CPU_Pid]);

                //stores the amount of time the process executed for (either because it got preempted because it reached the time quantum or because it finished its burst time)
                int Current_CPU_Time_Slice = Math.min(timeQuantum, remaining_CPU_Time[Current_CPU_Pid]);

                //calculates end of the current burst
                Current_CPU_Burst_End = currentTime + Current_CPU_Time_Slice;
                
                //adds the process back into the queue
                Sorted_Arrival_Times.add(new int[]{Current_CPU_Burst_End, 1, Current_CPU_Pid});
            }

            //gets the next arriving process
            int[] nextEvent = Sorted_Arrival_Times.poll();

            //if there are no more events, exit
            if (nextEvent == null) {
                break;
            }

            currentTime = nextEvent[0];
            int eventType = nextEvent[1];
            int nextPID = nextEvent[2];

            switch (eventType) {

                //a process arrives in the ready queue
                case 0:
                    //set the last time the proces entered the ready queue as the current time
                    Last_Ready_Time[nextPID] = currentTime;
                    readyQueue.add(nextPID);
                    break;

                //the process's time ran out in the CPU (either time quantum ran out or the process's current burst segment is done)
                case 1:
                    int Time_Slice_Used = Math.min(timeQuantum, remaining_CPU_Time[nextPID]);

                    //store the process's remaining CPU burst time after substracting the time it just executed for
                    remaining_CPU_Time[nextPID] = remaining_CPU_Time[nextPID] - Time_Slice_Used;

                    //if the current CPU burst is done
                    if (remaining_CPU_Time[nextPID] == 0) {

                        //get the I/O duration of the process
                        int[] currentSegment = IO_Operations[nextPID].get(Current_IO_Pair[nextPID]);
                        int Current_IO_Duration = currentSegment[1];

                        //checks to see if I/O operation is finished
                        if (Current_IO_Duration > 0) {

                            //checks to see if there is another process currently executing I/O operations
                            if (IO_Busy == false) {

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
                            if (Current_IO_Pair[nextPID] < IO_Operations[nextPID].size() ) {

                                int[] nextSegment = IO_Operations[nextPID].get(Current_IO_Pair[nextPID]);

                                remaining_CPU_Time[nextPID] = nextSegment[0];
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

                        //if the CPU burst isn't done (its time quantum ran out)
                    } else {

                        //store the time when the process goes back into the ready queue
                        Last_Ready_Time[nextPID] = currentTime;

                        //add process back into the ready queue
                        readyQueue.add(nextPID);
                    }

                    Current_CPU_Pid = -1;
                    break;

                // I/O is done
                case 2:
                    IO_Busy = false;

                    Current_IO_Pair[nextPID]++;

                    //if there is another CPU burst segment to execute
                    if (Current_IO_Pair[nextPID] < IO_Operations[nextPID].size() ) {

                        int [] nextSegment = IO_Operations[nextPID].get(Current_IO_Pair[nextPID]);

                        remaining_CPU_Time[nextPID] = nextSegment[0];
                        Last_Ready_Time[nextPID] = currentTime;
                        readyQueue.add(nextPID);

                        //if there are no more segments
                    } else {

                        //set process to finished
                        completionTime[nextPID] = currentTime;
                        finished[nextPID] = true;
                    }

                    //checks to see if there are any processes in the waiting queue (waiting for I/O)
                    if (IO_Waiting_Queue.isEmpty() == false) {

                        //get the next process in the waiting queue
                        int next_IO_Pid = IO_Waiting_Queue.poll();

                        //get the I/O duration of the process
                        int io_Duration = IO_Operations[next_IO_Pid].get(Current_IO_Pair[next_IO_Pid])[1];

                        IO_Busy = true;

                        //add a new I/O done event
                        Sorted_Arrival_Times.add(new int[]{currentTime + io_Duration, 2, next_IO_Pid});
                    }

                    break;
            }

        }

        //printing CPU metrics

        System.out.println("\n***Round Robin Scheduling***");
        System.out.println("------------------------------------------------------------------------------");
        System.out.println("PID\t  Arrival Time\t  Completion Time  Waiting Time   Turnaround Time");

        int totalWaitingTime = 0;
        int totalTurnaroundTime = 0;

        //a loop to calculate and display the waiting and turnarounf times
        for (int i = 0; i < n; i++) {

            int turnaround = completionTime[i] - arrivalTimes[i];

            totalWaitingTime = totalWaitingTime + waitingTime[i];
            totalTurnaroundTime = totalTurnaroundTime + turnaround;

            System.out.printf("%d\t\t%d\t\t%d\t\t%d\t\t%d\n", i + 1, arrivalTimes[i], completionTime[i], waitingTime[i], turnaround);
        }
        System.out.println("------------------------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %f\n", totalWaitingTime / (double) n);
        System.out.printf("Average Turnaround Time: %f\n", totalTurnaroundTime / (double) n);

    }//end main

}//end class
