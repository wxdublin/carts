package edu.penn.rtg.schedulingapp.basic;

import java.util.ArrayList;
import edu.penn.rtg.schedulingapp.basic.*;
import edu.penn.rtg.schedulingapp.nalgo.PeriodSet;


/**
 * This class implements the bandwidth-periods representation of equivalent PRM interface
 * @author Sanjian Chen
 *
 */
public class EqvPrmModel implements ResModel{
    public PeriodSet periodSet;
    public double bandwidth;


    /**
     * Construct an empty EqvPrmModel
     */
    public EqvPrmModel() {
        this.bandwidth = 0;
        this.periodSet = new PeriodSet();
    }


    /**
     * Given a single period, construct the equivalent set defined in RTAS'11 Chen's paper
     * @param orgPeriod
     * @param bandwidth
     */
    public EqvPrmModel(long orgPeriod, double bandwidth) {
        this.bandwidth = bandwidth;
        this.periodSet = new PeriodSet(orgPeriod);
    }

    /**
     * Construct the period set by given continuous bound (contBound) and the set of discrete period (discSet)
     * @param contBound
     * @param discSet
     * @param bandwidth
     */
    public EqvPrmModel(PeriodSet periodSet, double bandwidth) {
        this.bandwidth = bandwidth;
        this.periodSet = periodSet;
    }

    @Override
    public void display() {
        System.out.println("Bandwidth : " + this.bandwidth);
        System.out.println("Continuous bound: " + this.periodSet.contBound);
        if(this.periodSet.discSet.size() > 0) {
            for(int i = 0; i < this.periodSet.discSet.size(); i++) {
                System.out.println("Discret period candidate No." + i + " : " + this.periodSet.discSet.get(i));
            }
        }
    }

    @Override
    public int getSupply(int t) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getUtil() {
        // TODO Auto-generated method stub
        return this.bandwidth;
    }

    public static EqvPrmModel compose(EqvPrmModel model1, EqvPrmModel model2) {
        if(model1.periodSet.contBound < 1E-20) {
            return model2;
        }else if(model2.periodSet.contBound < 1E-20) {
            return model1;
        }else {
            EqvPrmModel composed = new EqvPrmModel(PeriodSet.intersect(model1.periodSet, model2.periodSet), 
                    model1.bandwidth + model2.bandwidth);
            return composed;
        }
    }

    @Override
    public PTask getTask() {
        // TODO Auto-generated method stub
        return null;
    }


	public String getStr() {
		return null;
	}


	@Override
	public double getSupply(double t) {
		// TODO Auto-generated method stub
		return (double) this.getSupply((int)t);
	}

}
