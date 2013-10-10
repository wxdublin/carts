package edu.penn.rtg.schedulingapp.output.graph;

import java.awt.Color;

import ptolemy.plot.Plot;
/**
 * It extends ptolemy.plot.Plot for sbf/dbf graph
 */

@SuppressWarnings("serial")
public class CartsPlot extends Plot {
	double maxX = 0;
	double maxY = 0;
	double lasty=0;
	double lastx=0;
	static protected Color[] _colors = {
		new Color(0x00aaaa), // cyan-ish
		new Color(0xff0000), // red
		new Color(0x0000ff), // blue
		new Color(0x000000), // black
		new Color(0xffa500), // orange
		new Color(0x53868b), // cadetblue4
		new Color(0xff7f50), // coral
		new Color(0x45ab1f), // dark green-ish
		new Color(0x90422d), // sienna-ish
		new Color(0xa0a0a0), // grey-ish
		new Color(0x14ff14), // green-ish
		};
	static protected Color[] _colors2 = {
		new Color(0xff0000), // red
		new Color(0x0000ff), // blue
		new Color(0x00aaaa), // cyan-ish
		new Color(0x000000), // black
		new Color(0xffa500), // orange
		new Color(0x53868b), // cadetblue4
		new Color(0xff7f50), // coral
		new Color(0x45ab1f), // dark green-ish
		new Color(0x90422d), // sienna-ish
		new Color(0xa0a0a0), // grey-ish
		new Color(0x14ff14), // green-ish
		};

	/**
	 * Start to draw Supply Bound Function 
	 * 
	 */
	public CartsPlot(boolean RBF)
	{
		if(RBF)
			setColors(_colors);
		else
			setColors(_colors2);
				
	}
	public void startSBF() {
		read("Dataset: Supply");
		read("Color:red");
		read("0,0");
		lasty=0;
	}
	/**
	 * Start to draw Demand Bound Function 
	 * 
	 */

	public void startDBF() {
		read("Dataset: Demand");
		read("Color: blue");
		read("0,0");
		lasty=0;
	}
	
	public void addDem(double x,double y){
		add(x,lasty);
		add(x,y);
		lasty=y;
	}
	public void addStarvation(double x){
		add(x,0);
		lastx=x;
		lasty=0;
	}
	public void drawUntil(double x1,double x2,double endx){
		while(true)
		{
			if(lastx+x1>endx){
				lasty=lasty+(endx-lastx);
				lastx=endx;
			}
			else
			{
				lastx=lastx+x1;
				lasty=lasty+x1;
			}
			add(lastx,lasty);
			if(lastx==endx) break;
			lastx=lastx+x2;
			if(lastx>endx) lastx=endx;
			add(lastx,lasty);
			if(lastx==endx) break;
		}
	}
	public void drawDPRM(int p1,double e1,double p2,double e2,int endx){
		int val[]=new int[endx];
		int val1[]=new int[endx];
		int val2[]=new int[endx];
		double left=2*(p1-e1);
		double e_left=e1;
		int sup=0;
		for(int i=0;i<endx;i++) {
			val1[i]=sup;
			if(left==0) {
				e_left--;
				sup+=1;
				if(e_left==0){
					left=(p1-e1);
					e_left=e1;
				}
			}
			else {
				left--;
			}
		}
		left=2*(p2-e2);
		e_left=e2;
		sup=0;
		for(int i=0;i<endx;i++) {
			val2[i]=sup;
			if(left==0) {
				e_left--;
				sup+=1;
				if(e_left==0){
					left=(p2-e2);
					e_left=e2;
				}
			}
			else {
				left--;
			}
		}
		left=0;
		int old_val=-1;
		for(int i=endx-1;i>=0;i--) {
			val[i]=val1[i]+val2[i];
			//System.out.println(i+","+val[i]+","+old_val);
			if(old_val==-1) {
				old_val=val[i];
			} else if(old_val-val[i]>1) {
				old_val--;
				val[i]=old_val;
			} else {
				old_val=val[i];
			}
				
		}

		old_val=0;
		for(int i=0;i<endx;i++) {
			if(val[i]!=old_val) {
				if(lastx!=i-1)
					add(i-1,old_val);
				add(i,val[i]);
				lastx=i;
				//System.out.println(i+","+val[i]);
				old_val=val[i];
			}
		}
		
	}
	public void addSup(double x1,double x2){
		add(x1,lasty);
		lasty=lasty+x2-x1;
		add(x2,lasty);
	}

	/**
	 * Draw from current location to specific point
	 * 
	 * @param x x-axis value of point
	 * @param y y-axis value of point
	 */

	public void add(double x, double y) {
		//System.out.println(x + "," + y);
		read(x + "," + y);
		if (x > maxX)
			maxX = x;
		if (y > maxY)
			maxY = y;
	}
	public void circle(double x,double y){
		read("Dataset: sbf >= rbf");
		read("Impulses: on");
		read("Marks: dots");
		read(x + "," + y);
	}

	/**
	 * End to draw graph
	 * 
	 */

	public void endAll() {
		read("XRange: 0," + maxX);
		read("YRange: 0," + maxY);
	}
	public void endAll(double x,double y)
	{
		read("XRange: 0," + x);
		read("YRange: 0," + y);
	}
}
