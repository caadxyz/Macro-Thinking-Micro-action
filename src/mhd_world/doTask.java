package mhd_world;

import java.util.*;
import java.io.*;

public class doTask {
  private java.util.Timer timer;
  private java.util.TimerTask task;
  
  public doTask(java.util.TimerTask task) {
    this.timer = new Timer();
    this.task = task;
  }
  
  
  public void start(int delay, int internal) {
    timer.schedule(task, delay * 1000, internal * 1000);//利用timer.schedule方法
  }

  public static void main(String[] args) {
    java.util.TimerTask task1 = new MyTask("     Job 1");
    java.util.TimerTask task2= new MyTask("Job 2");
    doTask pt = new doTask(task1);
    pt.start(1,3);
    doTask pt2 = new doTask(task2);
    pt2.start(1,1);
  }

}