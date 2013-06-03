package edu.penn.rtg.schedulingapp.algo;

import edu.penn.rtg.schedulingapp.TreeComponent;
import edu.penn.rtg.schedulingapp.ResourceModel;
import edu.penn.rtg.schedulingapp.ResourceModelList;
import edu.penn.rtg.schedulingapp.SchedulingComponent;
import edu.penn.rtg.schedulingapp.Task;
import edu.penn.rtg.schedulingapp.TaskList;
import edu.penn.rtg.schedulingapp.util.CartsProgress;
import edu.penn.rtg.schedulingapp.util.SchException;
import edu.penn.rtg.schedulingapp.util.UserUtil;

public class DPRM {
	CartsProgress cp;
	public void start(TreeComponent comp){
		int size=comp.getChildSize()+2;
		cp=new CartsProgress(size);
		cp.setVisible(true);
	}
	public void end(){
		cp.end();
	}

	public static boolean check(TreeComponent comp)
	{
		if (comp.getAllChildren().size() != 0) {
			for (TreeComponent c : comp.getAllChildren()) {
				if(!DPRM.check(c)) 
					return false;
			}
		}
		if(comp.getSchCom().isEDF())
			return true;
		else
			return false;
	}
	
	protected ResourceModel generateInterface(TreeComponent C)
			throws Exception {
		if (C.getSchCom().isEDF()) {
			ResourceModel rm=getEDF((SchedulingComponent)C.getSchCom());
			//rm.printDPRM();

			/*
			EDFSchedulability_DPRM sch = new EDFSchedulability_DPRM(C);
			int period=sch.getPeriod1();
			int exec=sch.getExec1();
			ResourceModel rm=new ResourceModel(period,exec,0);
			rm.period2=sch.getPeriod2();
			rm.exec2=sch.getExec1();
			rm.type=ResourceModel.DPRM;
			*/
			
			return rm;
		} 
		else
		{
			//System.out.println("TODO");
			UserUtil.show("Computing DPRM interface only works with EDF. Cannot continue");
			throw new SchException();
		}
	}
	private ResourceModel getEDF(SchedulingComponent comp) throws Exception
	{
		EDFSchedulability_DPRM sch = new EDFSchedulability_DPRM(comp);
		int lcm=comp.computeLCM_DPRM();
		int e=(int)Math.ceil((double)sch.getExec(lcm));
		//comp.showPeriods();
		//System.out.println(lcm);
		double kappa1=(double)e/lcm;
		double kappa2=kappa1;
		//System.out.println(kappa1+" "+lcm+","+e);
		if(kappa1==1){
			return new ResourceModel(1,1,0);
		}
		if(kappa1>1){
			e=(int)Math.ceil((double)sch.getExec(10));
			return new ResourceModel(10,e,0);
		}
		int max_p1=sch.getMaxPeriod(kappa1);
		double kc;
		int max_e1,max_e2;
		int p2;
		int ce2,cp2;
		int fp1=0,fp2=0,fe1=0,fe2=0;
		for(int p1=1;p1<=max_p1;p1++) {
			max_e1=sch.getMaxExec(p1);
			//System.out.println(p1+","+max_e1);
			for(int e1=0;e1<=max_e1;e1++) {
				ResourceModel rm=new ResourceModel(p1,(float)e1/p1,p1);
				max_e2=sch.getMaxPeriod(kappa2);
				//System.out.println(max_e2);
				for(int e2=1;e2<=max_e2;e2++) {
				
					p2=sch.getPeriod(rm,e2);
					//System.out.println(e2+":"+p2);
					if(p2==-1) {
						cp2=0;
						ce2=0;
					}
					else {
						cp2=p2;
						ce2=e2;
					}
					kc=0;
					if(e1!=0)
						kc+=(float)e1/p1;
					if(ce2!=0)
						kc+=(float)ce2/cp2;
					//System.out.println(p1+","+e1+","+cp2+","+ce2+","+kc);
					//System.out.println(kc);
					if(kc<=kappa2&&kc>0) {
						kappa2=kc;
						max_e2=sch.getMaxExecution(kappa2);
						//rm.printDebug();
						//System.out.println(p1+","+e1+","+cp2+","+ce2);
						fp1=p1;fp2=cp2;
						fe1=e1;fe2=ce2;
					}
				}
			}
			if(kappa2<kappa1) {
				kappa1=kappa2;
				max_p1=sch.getMaxPeriod(kappa1);
			}
			
		}
		ResourceModel minrm;
		if(fe1==0) {
			minrm=new ResourceModel(fp2,fe2,0);
			minrm.period2=0;
			minrm.exec2=0;
			
		} else {
			minrm=new ResourceModel(fp1,fe1,0);
			minrm.period2=fp2;
			minrm.exec2=fe2;
		}
		if(fe2==0){
			minrm.period2=0;
		}
		return minrm;
	
	}

	protected Task transformInterface1(ResourceModel rm)
			throws Exception {
		double p=rm.getPeriod();
		return new Task(p,rm.getBandwidth(),p);
	}
	protected Task transformInterface2(ResourceModel rm)
			throws Exception {
		double p=rm.period2;
		return new Task(p,rm.exec2,p);
	}
	public void abstractionProcedure(TreeComponent c) throws Exception {
		if (c.getAllChildren().size() != 0) {
			for (TreeComponent comp : c.getAllChildren()) {
				abstractionProcedure(comp);
			}
		}
		cp.increment();
		ResourceModelList list = new ResourceModelList();
		TaskList processed = new TaskList();
		ResourceModel rm = generateInterface(c);
		rm.type=ResourceModel.DPRM;
		ResourceModel rm2=new ResourceModel(rm.period2,rm.exec2,0,0,0,0);
		rm2.type=ResourceModel.DPRM;
		//rm2.printDebug();
		list.getResourceModelArray().add(rm);
		processed.getTasks().add(transformInterface1(rm));
		if(rm2.getPeriod()!=0) {
			list.getResourceModelArray().add(rm2);
			processed.getTasks().add(transformInterface2(rm));
		}
		
		c.setProcessedTaskList(processed);
		c.setResourceModelList(list);
		if (c.hasParentComp()) {
			//System.out.println("Yes");
			c.getParentComp().getChildrenToTaskTable().put(c, processed);
			//System.out.println(tl.size());
			
		}
		c.setProcessed(true);
	}
}