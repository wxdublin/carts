package edu.penn.rtg.schedulingapp.basic;

public class IntPRM implements ResModel {
	int period;
	int exec;

	public IntPRM(int period, int exec) {
		super();
		this.period = period;
		this.exec = exec;
	}

	public IntPRM(PeriodicRM res) {
		this.period=res.period;
		this.exec=(int) Math.ceil(res.exec);
	}

	@Override
	public int getSupply(int t) {
		int y=(int) Math.floor((t-(period-exec))/period);
		int x= (int) (2*(period-exec));
		int rem=Math.max(0,t-x-y*period);
		int s=(int) (Math.floor(y*exec)+rem);
		s=Math.max(0,s);

		return s;
	}

	@Override
	public void display() {
		System.out.println("Res:"+period+","+exec);

	}

	@Override
	public double getUtil() {
		return  (double)exec/period;
	}

	@Override
	public PTask getTask() {
		return new PTask(period,exec);
	}
	
	//------------------------------------- Added by SC ---------------------------------//
	public int getPeriod(){
		return this.period;
	}
	
	public int getExe(){
		return this.exec;
	}
	//-----------------------------------------------------------------------------------//

	@Override
	public double getSupply(double t) {
		// TODO Auto-generated method stub
		System.out.println("XM-ATTENTION: getSupply(double) call getSupply(int), cast double to int ");
		return (double)this.getSupply((int) t) ;
	}

}
