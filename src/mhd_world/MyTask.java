package mhd_world;

public class MyTask
extends java.util.TimerTask {
String jobName;
private int i;
public void run() { //run in interface Runnabl
    System.out.print(i);
	System.out.println(jobName);
	i++;
}

public MyTask(String jobName) {
this.jobName = jobName;
}
}
