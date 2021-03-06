package edu.penn.rtg.schedulingapp.basic;
import java.util.Vector;


import edu.penn.rtg.schedulingapp.basic.ResModel;
import edu.penn.rtg.schedulingapp.nalgo.PeriodicRmEDF;
import edu.penn.rtg.schedulingapp.nalgo.PrmAlgo;
public class SComponent {
	public final int NONE=0;
	public final int EDF=1;
	public final int RM=2;
	public String name;
	ResModel res;
	Workload w;
	
	//------------------- These two fields are added by SC, with separate setter methods --------//
	//------------------- No change to other part of this class ---------------------------------//
	public IntPRM initRes = new IntPRM(0,0);
	public EqvPrmModel eqvRes = new EqvPrmModel();
	
	public void setInitRes(IntPRM initRes){
		this.initRes = initRes;
	}
	
	public void setEqvRes(EqvPrmModel eqvRes){
		this.eqvRes = eqvRes;
	}
	//-------------------------------------------------------------------------------------------//
	
	Vector<SComponent> comList;
	
	int period;
	int periodLimit;
	int algo=0;
	public SComponent() {
		// root component
		comList=new Vector<SComponent>();
	}
	public SComponent(Workload w) {
		this.w = w;
	}
	public SComponent(ResModel res, Workload w) {
		this.res = res;
		this.w = w;
	}
	public void addComp(SComponent c) {
		comList.add(c);
	}
	
	public boolean checkSchedulability() throws Exception {
		boolean bSch=false;
		if(algo==EDF) {
			bSch=checkSchedulabilityEDF();
			if(!bSch) {
				if(res instanceof IntPrmPlus) {
					((IntPrmPlus)res).exec+=0.00000000001;
				}
				if(res instanceof PeriodicRM) {
					((PeriodicRM)res).exec+=0.00000000001;
				}
				bSch=checkSchedulabilityEDF();
			}
			return bSch;
		}
		else if(algo==RM)
		{
			return checkSchedulabilityRM ();
		}
		return false;
	}
	private boolean checkSchedulabilityRM() {
		// TODO Auto-generated method stub
		return false;
	}
	private boolean checkSchedulabilityEDF() throws Exception {
		// TODO Auto-generated method stub
		boolean bSch=true;
		int sup;
		double dem;
		for(int t=0;t<w.computeLCM();t++)
		{
			dem=w.demand_EDF(t);
			sup=res.getSupply(t);
			if(dem>sup) {
				bSch=false;
				//System.out.println(t+", d:"+dem+", s:"+sup);
				break;
			}
		}
		return bSch;
	}
	public void display() {
		System.out.println("Workload");
		w.display();
		System.out.println("Resource Model");
		res.display();
	}
	
	public void eqvDisplay() {
	    if(this.getComList() != null) {
	        for(SComponent c:this.getComList()) {
	            c.eqvDisplay();
	        }
	        this.getResModel().display();
	    }else {
	        this.getResModel().display();
	    }
	}

	// get/set field 
	public void setPeriod(int i) {
		this.period=i;
	}
	
	public void setPeriodLimit(int i) {
            this.periodLimit=i;
	}
	
	public Vector<SComponent> getComList() {
		return comList;
	}
	public Workload getWorkload() {
		return w;
	}
	public void setWorkload(Workload w) {
		this.w=w;
	}
	
	public int getPeriod() {
		return period;
	}
	
	public int getPeriodLimit() {
            return periodLimit;
	}
	
	public void setResModel(ResModel rm) {
		this.res=rm;
	}
	public ResModel getResModel() {
		return res;
	}
	public void setAlgoEDF() {
		algo=EDF;
	}
	public void setAlgoRM() {
		algo=RM;
	}
	public int getAlgo() {
		return algo;
	}
}
