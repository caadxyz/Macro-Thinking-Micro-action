package test_learning_java;

public class Pingpong implements Runnable{
	private String word; // what word to print
	private int delay;   // how long to pause
	public Pingpong (String whatToSay, int delayTime){
		this.word=whatToSay;
		this.delay=delayTime;
		}
	public void run(){
		try{
			for(;;){
				System.out.print(word+ " ");
				Thread.sleep(delay); // wait until next time
							}
		} catch (InterruptedException e){
			return; // end this thread
		}
	}
    public static void main (String[] args){
    	Runnable ping=new Pingpong("ping1",100);
    	Runnable pong=new Pingpong("pong1",300);
    	new Thread(ping).start();
    	new Thread(pong).start();
    	}
}
