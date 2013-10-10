package edu.penn.rtg.experiment.tasksetgenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import edu.penn.rtg.common.Tool;

/**
 * Class GenerateTaskset 
 * This class uniformly generate random taskset for Num_domain domains, the time unit is millisecond! Because RT-Xen only support millisecond granularity
 * All component's scheduling are gEDF because MPR only supports gEDF.
 * Hypervisor's scheduling are hEDF because the full utilized task cannot be scheduled by gEDF. 
 * @author panda
 *
 */
public class TasksetGenerator {
	//public static final double MS_TO_NS = 1000000;
	public static final double MS_TO_US = 1000;
	private static int whichTasksetUtilDistr;
	
	private final double delta_rel = 0;
	private final double delta_sch = 0;
	private final double delta_cxs  = 0;
	private final double delta_crpmd = 86/TasksetGenerator.MS_TO_US;
	
	private final int NO_OVERHEAD = 0;
	private final int CAMPR = 1;
	private final int BASELINE = 2;
	
	private final double systemPeriod = 16; //top component's period
	
	//Generated task set's requirement
	private double task_util_min;
	private double task_util_max;
	private double task_period_min;
	private double task_period_max;
	private double domain_num;
	private Vector<Double> domainPeriods;
	private double taskset_util;
	private double taskset_size;
	
	
	
	//store the generated taskset
	private Vector<Double> task_period_harmonic_values;
	private Vector<Task> taskset;
	private Vector<Vector<Task>> domainTasks;	
	
	
	private DecimalFormat df;
	
	//TODO Generate taskset with different number of domains and different domain period, because they matters in your theory.
	public static void main(String[] args){
		if(args.length < 2){
			TasksetGenerator.printUsage();
			System.exit(0);
		}
		int whichGenerateApproach = Integer.parseInt(args[0]);
		String[] args_new = new String[args.length -1];
		
		for(int i=0; i<args_new.length;i++){
			args_new[i] = args[i+1];
		}
		
		switch(whichGenerateApproach){
		case 0: TasksetGenerator.run_generateTaskset4DifferentTasksetUtil(args_new); break;
		case 1: TasksetGenerator.run_generateOneTaskset(args_new); break;
		case 2: TasksetGenerator.run_generateTaskset4DifferentTasksetSize(args_new); break;
		case 3: TasksetGenerator.run_generateTaskset4DifferentDeltaCRPMD(args_new);break;
		default: TasksetGenerator.printUsage();
		}
		
		// generate k taskset for each taskset's utilization
		
		
	}
	
	public static void run_generateTaskset4DifferentDeltaCRPMD(String[] args){
		//taskset utilization range and taskset_num per taskset utilization
		double task_util_min = 0;//0.002; //same with Jaewoo's RTAS12 paper
		double task_util_max = 0;
		if(args[0].equalsIgnoreCase("Uniform_Light")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_LIGHT;
			task_util_min = 0.001;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.1;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Medium")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_MEDIUM;
			task_util_min = 0.1;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.4;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_HEAVY;
			task_util_min = 0.5;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.9;//0.05;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Light")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_LIGHT;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Medium")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_MEDIUM;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_HEAVY;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		
		double delta_crpmd_min = Double.parseDouble(args[1]);//0.1;
		double delta_crpmd_step = Double.parseDouble(args[2]);//0.1;
		double delta_crpmd_max = Double.parseDouble(args[3]);//6;
		int workloadNum_per_util = Integer.parseInt(args[4]);//25;
		//task's parameter range //task period (550, 650); (350,850), (100,1000) 
		double task_period_min = Double.parseDouble(args[5]);//350;
		double task_period_max = Double.parseDouble(args[6]); //850;//1024;
		int domain_num = Integer.parseInt(args[7]);
		//is task's period harmonic
		boolean isPeriodHarmonic = false;
		Vector<Double> domainPeriods = new Vector<Double>();
		
		if(args[8].equalsIgnoreCase("Yes") || args[8].equalsIgnoreCase("Y")){
			double domain_period_min = Double.parseDouble(args[9]);
			double domain_period_max = Double.parseDouble(args[10]);
			generateDomainPeriod(domain_period_min, domain_period_max, domainPeriods, domain_num);
		}else{
			domainPeriods.add(256.0);
			domainPeriods.add(128.0);
			domainPeriods.add(64.0);
			domainPeriods.add(32.0);
			while(domainPeriods.size() > domain_num){
				domainPeriods.remove(domainPeriods.size()-1);
			}
		}
		
		
		
		TasksetGenerator generateTaskset4DeltaCRPMD = new TasksetGenerator(task_util_min,task_util_max, 
				task_period_min, task_period_max, domain_num, domainPeriods);
		
		System.out.println("============= Start generating taskset================\r\n");
		
		generateTaskset4DeltaCRPMD.generateTaskset4DifferentDeltaCRPMD(delta_crpmd_min, delta_crpmd_step, delta_crpmd_max, workloadNum_per_util, isPeriodHarmonic);
		
		System.out.println("============= Finish generating taskset==============\r\n");
	
	}
	
	
	public static void run_generateOneTaskset(String[] args){
		//taskset utilization range and taskset_num per taskset utilization
		double task_util_min = 0;//0.002; //same with Jaewoo's RTAS12 paper
		double task_util_max = 0;
		if(args[0].equalsIgnoreCase("Uniform_Light")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_LIGHT;
			task_util_min = 0.001;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.1;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Medium")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_MEDIUM;
			task_util_min = 0.1;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.4;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_HEAVY;
			task_util_min = 0.5;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.9;//0.05;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Light")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_LIGHT;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Medium")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_MEDIUM;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_HEAVY;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}

		double taskset_util_min = Double.parseDouble(args[1]);//0.1;
		double taskset_util_step = Double.parseDouble(args[2]);//0.1;
		double taskset_util_max = Double.parseDouble(args[3]);//6;
		int workloadNum_per_util = Integer.parseInt(args[4]);//25;
		//task's parameter range //task period (550, 650); (350,850), (100,1000) 
		double task_period_min = Double.parseDouble(args[5]);//350;
		double task_period_max = Double.parseDouble(args[6]); //850;//1024;
		int domain_num = Integer.parseInt(args[7]);
		//is task's period harmonic
		boolean isPeriodHarmonic = false;
		Vector<Double> domainPeriods = new Vector<Double>();

		int cursor = 8;
		if(args[8].equalsIgnoreCase("Yes") || args[8].equalsIgnoreCase("Y")){
			double domain_period_min = Double.parseDouble(args[9]);
			double domain_period_max = Double.parseDouble(args[10]);
			generateDomainPeriod(domain_period_min, domain_period_max, domainPeriods, domain_num);
			cursor = 11;
		}else{
			domainPeriods.add(256.0);
			domainPeriods.add(128.0);
			domainPeriods.add(64.0);
			domainPeriods.add(32.0);
			while(domainPeriods.size() > domain_num){
				domainPeriods.remove(domainPeriods.size()-1);
			}
			cursor = 9;
		}
		double tasksetUtil = Double.parseDouble(args[cursor]);
		cursor++;
		int tasksetIndex = Integer.parseInt(args[cursor]);
		
		TasksetGenerator generateOneTaskset = new TasksetGenerator(task_util_min,task_util_max, 
				task_period_min, task_period_max, domain_num, domainPeriods);
		
		System.out.println("============= Start generating ONE taskset================\r\n");
		
		generateOneTaskset.generateOneTasksetOnce(taskset_util_min, taskset_util_step, taskset_util_max, workloadNum_per_util, isPeriodHarmonic,tasksetUtil, tasksetIndex);
		
		System.out.println("============= Finish generating taskset==============\r\n");
	}
	

	
	/**
	 * Function run_generateTaskset4DifferentTasksetUtil
	 * Generate taskset 4 different taskset util
	 */
	public static void run_generateTaskset4DifferentTasksetUtil(String[] args){
		//taskset utilization range and taskset_num per taskset utilization
		double task_util_min = 0;//0.002; //same with Jaewoo's RTAS12 paper
		double task_util_max = 0;
		if(args[0].equalsIgnoreCase("Uniform_Light")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_LIGHT;
			task_util_min = 0.001;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.1;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Medium")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_MEDIUM;
			task_util_min = 0.1;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.4;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_HEAVY;
			task_util_min = 0.5;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.9;//0.05;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Light")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_LIGHT;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Medium")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_MEDIUM;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_HEAVY;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		
		double taskset_util_min = Double.parseDouble(args[1]);//0.1;
		double taskset_util_step = Double.parseDouble(args[2]);//0.1;
		double taskset_util_max = Double.parseDouble(args[3]);//6;
		int workloadNum_per_util = Integer.parseInt(args[4]);//25;
		//task's parameter range //task period (550, 650); (350,850), (100,1000) 
		double task_period_min = Double.parseDouble(args[5]);//350;
		double task_period_max = Double.parseDouble(args[6]); //850;//1024;
		int domain_num = Integer.parseInt(args[7]);
		//is task's period harmonic
		boolean isPeriodHarmonic = false;
		Vector<Double> domainPeriods = new Vector<Double>();
		
		if(args[8].equalsIgnoreCase("Yes") || args[8].equalsIgnoreCase("Y")){
			double domain_period_min = Double.parseDouble(args[9]);
			double domain_period_max = Double.parseDouble(args[10]);
			generateDomainPeriod(domain_period_min, domain_period_max, domainPeriods, domain_num);
		}else{
			domainPeriods.add(256.0);
			domainPeriods.add(128.0);
			domainPeriods.add(64.0);
			domainPeriods.add(32.0);
			while(domainPeriods.size() > domain_num){
				domainPeriods.remove(domainPeriods.size()-1);
			}
		}
		
		
		
		TasksetGenerator generateTaskset4TasksetUtil = new TasksetGenerator(task_util_min,task_util_max, 
				task_period_min, task_period_max, domain_num, domainPeriods);
		
		System.out.println("============= Start generating taskset================\r\n");
		
		generateTaskset4TasksetUtil.generateTaskset4DifferentTasksetUtil(taskset_util_min, taskset_util_step, taskset_util_max, workloadNum_per_util, isPeriodHarmonic);
		
		System.out.println("============= Finish generating taskset==============\r\n");
	
	}
	
	
	
	public static void run_generateTaskset4DifferentTasksetSize(String[] args){
		//taskset utilization range and taskset_num per taskset utilization
		double task_util_min = 0;//0.002; //same with Jaewoo's RTAS12 paper
		double task_util_max = 0;
		if(args[0].equalsIgnoreCase("Uniform_Light")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_LIGHT;
			task_util_min = 0.001;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.1;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Medium")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_MEDIUM;
			task_util_min = 0.1;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.4;//0.05;
		}
		if(args[0].equalsIgnoreCase("Uniform_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.UNIFROM_HEAVY;
			task_util_min = 0.5;//0.002; //same with Jaewoo's RTAS12 paper
			task_util_max = 0.9;//0.05;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Light")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_LIGHT;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Medium")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_MEDIUM;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		if(args[0].equalsIgnoreCase("BIMODAL_Heavy")){
			whichTasksetUtilDistr = GlobalVariable.BIMODAL_HEAVY;
			task_util_min = 0.001; // [0.001,0.5] [0.5,0.9]
			task_util_max = 0.9;
		}
		
		double taskset_size_min = Double.parseDouble(args[1]);//4;
		double taskset_size_step = Double.parseDouble(args[2]);//20;
		double taskset_size_max = Double.parseDouble(args[3]);//500;
		int workloadNum_per_tasksetSize = Integer.parseInt(args[4]);//25;
		//task's parameter range //task period (550, 650); (350,850), (100,1000) 
		double task_period_min = Double.parseDouble(args[5]);//350;
		double task_period_max = Double.parseDouble(args[6]); //850;//1024;
		int domain_num = Integer.parseInt(args[7]);
		//is task's period harmonic
		boolean isPeriodHarmonic = false;
		Vector<Double> domainPeriods = new Vector<Double>();
		
		if(args[8].equalsIgnoreCase("Yes") || args[8].equalsIgnoreCase("Y")){
			double domain_period_min = Double.parseDouble(args[9]);
			double domain_period_max = Double.parseDouble(args[10]);
			generateDomainPeriod(domain_period_min, domain_period_max, domainPeriods, domain_num);
		}else{
			domainPeriods.add(256.0);
			domainPeriods.add(128.0);
			domainPeriods.add(64.0);
			domainPeriods.add(32.0);
			while(domainPeriods.size() > domain_num){
				domainPeriods.remove(domainPeriods.size()-1);
			}
		}
		
		
		
		TasksetGenerator generateTaskset4TasksetUtil = new TasksetGenerator(task_util_min,task_util_max, 
				task_period_min, task_period_max, domain_num, domainPeriods);
		
		System.out.println("============= Start generating taskset================\r\n");
		
		generateTaskset4TasksetUtil.generateTaskset4DifferentTasksetSize(taskset_size_min, taskset_size_step, taskset_size_max, workloadNum_per_tasksetSize, isPeriodHarmonic);
		
		System.out.println("============= Finish generating taskset==============\r\n");
	
	}
	
	public static void printUsage(){
		String str = "===========================\r\n";
		str += "TasksetGenerator generateApproach Uniform_[Light/Medium/Heavy]/Bimodal_[Light/Medium/Heavy], tasksetUtil(min, step, max) tasksetNum_perUtil TaskPeriodRange(min, max) domainNum isDomainPeriodChange DomainPeriod(min, max) tasksetUtil tasksetIndex\r\n";		str += "=================================\r\n";
		str += "Reminder: generateApproach: \r\n0-generate task set in a utilization range \r\n" +
				"1-generate only ONE task set with assigned util and index \r\n" +
				"2-generate task set for different task set size\r\n" + 
				"task's period is (550, 650); (350,850), (100,1000) \r\n";
		
		System.out.println(str);
		
	}
	
	public static void generateDomainPeriod(double domain_period_min, double domain_period_max, Vector<Double> domainPeriods, int domainNum){
		for(int i=0; i<domainNum; i++){
			double currentPeriod;
			while(true){
				currentPeriod = (int) (Math.random() * (domain_period_max - domain_period_min) + domain_period_min);
				if(currentPeriod < domain_period_min || currentPeriod > domain_period_max) {System.out.println("Regenerate domain period"); continue;}
				else break;
			}
			
			domainPeriods.addElement(currentPeriod);
		}
		
	}
	
	/**
	 * function generateTaskset4DifferentDeltaCRPMD
	 * Generate taskset for different delta_cprmd value for a fix number of tasks.
	 */
	public void generateTaskset4DifferentDeltaCRPMD(double delta_crpmd_min,double delta_crpmd_step,
			double delta_crpmd_max, int workloadNum_per_util,boolean isPeriodHarmonic){
		//generate a system with 64 tasks in total; 
		//each taskset generated is same except for the \delat_crpmd.
		int tasknum_per_taskset = 64;
		int scale = 100; //the delta_crpmd is scaled by 100X in input args, now it's scaled back by /100.
		this.taskset_size = tasknum_per_taskset;
	
		
		//modify 
		DecimalFormat folderDF = new DecimalFormat("#.###");
		folderDF.setMaximumFractionDigits(2);
		folderDF.setMinimumFractionDigits(2);
		String topFolder = "" + folderDF.format(delta_crpmd_min) + "-" + folderDF.format(delta_crpmd_step) + "-" + folderDF.format(delta_crpmd_max) + "-" + workloadNum_per_util;
		for(int i=0; i<workloadNum_per_util; i++){
			this.generateTaskset_perTasksetSize(isPeriodHarmonic);
			this.distribute2Domains();
			for(double current_delta_crpmd = delta_crpmd_min; current_delta_crpmd < delta_crpmd_max; current_delta_crpmd += delta_crpmd_step){
					System.out.println("Generate taskset with delta_crpmd :" + current_delta_crpmd + "\t workload index: " + i + "\r\n");
					boolean isFolderCreated = (new File(topFolder + "/" + folderDF.format(current_delta_crpmd))).mkdirs();
					if(!isFolderCreated){System.err.println("Create the folder for xml files fails!");}
					String output_tasksetFilename = topFolder + "/" + folderDF.format(current_delta_crpmd) + "/" + i + "-" + folderDF.format(current_delta_crpmd) + ".txt";
					String output_xmlFilename = topFolder + "/" + folderDF.format(current_delta_crpmd) + "/" + i + "-" + folderDF.format(current_delta_crpmd) + "-in.xml";
					
					this.modifyDeltaCRPMD(current_delta_crpmd/scale); 
					
					this.writeTasks2File(output_tasksetFilename);
					this.writeXML(output_xmlFilename);							
			}
			this.taskset = new Vector<Task>();
			this.domainTasks = new Vector<Vector<Task>>();
			for(int j=0; j < domain_num; j++){
				domainTasks.add(new Vector<Task>());
			}
		}
		
	}
	
	public void generateTaskset4DifferentTasksetUtil(double taskset_util_min, double taskset_util_step, double taskset_util_max, int workloadNum_per_util, boolean isPeriodHarmonic){
		DecimalFormat folderDF = new DecimalFormat("#.###");
		folderDF.setMaximumFractionDigits(2);
		folderDF.setMinimumFractionDigits(2);
		String topFolder = "" + folderDF.format(taskset_util_min) + "-" + folderDF.format(taskset_util_step) + "-" + folderDF.format(taskset_util_max) + "-" + workloadNum_per_util;
		for(double current_taskset_util = taskset_util_min; current_taskset_util < taskset_util_max; current_taskset_util += taskset_util_step){
			this.taskset_util = current_taskset_util;
			for(int i=0; i<workloadNum_per_util; i++){
				System.out.println("Generate task set utilization :" + current_taskset_util + "\t workload index: " + i + "\r\n");
				boolean isFolderCreated = (new File(topFolder + "/" + folderDF.format(current_taskset_util))).mkdirs();
				if(!isFolderCreated){System.err.println("Create the folder for xml files fails!");}
				String output_tasksetFilename = topFolder + "/" + folderDF.format(current_taskset_util) + "/" + i + "-" + folderDF.format(current_taskset_util) + ".txt";
				String output_xmlFilename = topFolder + "/" + folderDF.format(current_taskset_util) + "/" + i + "-" + folderDF.format(current_taskset_util) + "-in.xml";
				this.generateTaskset(isPeriodHarmonic);
				
				this.distribute2Domains();
				this.writeTasks2File(output_tasksetFilename);
				this.writeXML(output_xmlFilename);
				//clear the generated tasks, prepare for the next task set.
				this.taskset = new Vector<Task>();
				this.domainTasks = new Vector<Vector<Task>>();
				for(int j=0; j < domain_num; j++){
					domainTasks.add(new Vector<Task>());
				}
			}
			
		}
	}
	

	public void generateTaskset4DifferentTasksetSize(double taskset_size_min, double taskset_size_step, double taskset_size_max, int workloadNum_per_tasksetSize, boolean isPeriodHarmonic){
		DecimalFormat folderDF = new DecimalFormat("#.###");
		folderDF.setMaximumFractionDigits(2);
		folderDF.setMinimumFractionDigits(2);
		String topFolder = "" + folderDF.format(taskset_size_min) + "-" + folderDF.format(taskset_size_step) + "-" + folderDF.format(taskset_size_max) + "-" + workloadNum_per_tasksetSize;
		for(double current_taskset_size = taskset_size_min; current_taskset_size < taskset_size_max; current_taskset_size += taskset_size_step){
			this.taskset_size = current_taskset_size;
			for(int i=0; i<workloadNum_per_tasksetSize; i++){
				System.out.println("Generate task set size :" + current_taskset_size + "\t workload index: " + i + "\r\n");
				boolean isFolderCreated = (new File(topFolder + "/" + folderDF.format(current_taskset_size))).mkdirs();
				if(!isFolderCreated){System.err.println("Create the folder for xml files fails!");}
				String output_tasksetFilename = topFolder + "/" + folderDF.format(current_taskset_size) + "/" + i + "-" + folderDF.format(current_taskset_size) + ".txt";
				String output_xmlFilename = topFolder + "/" + folderDF.format(current_taskset_size) + "/" + i + "-" + folderDF.format(current_taskset_size) + "-in.xml";
				this.generateTaskset_perTasksetSize(isPeriodHarmonic);
				
				this.distribute2Domains();
				this.writeTasks2File(output_tasksetFilename);
				this.writeXML(output_xmlFilename);
				//clear the generated tasks, prepare for the next task set.
				this.taskset = new Vector<Task>();
				this.domainTasks = new Vector<Vector<Task>>();
				for(int j=0; j < domain_num; j++){
					domainTasks.add(new Vector<Task>());
				}
			}
			
		}
	}
	
	
	public void generateOneTasksetOnce(double taskset_util_min, double taskset_util_step, double taskset_util_max, int workloadNum_per_util, boolean isPeriodHarmonic, double tasksetUtil, int tasksetIndex){
		DecimalFormat folderDF = new DecimalFormat("#.###");
		folderDF.setMaximumFractionDigits(2);
		folderDF.setMinimumFractionDigits(2);
		String topFolder = "" + folderDF.format(taskset_util_min) + "-" + folderDF.format(taskset_util_step) + "-" + folderDF.format(taskset_util_max) + "-" + workloadNum_per_util;
		double current_taskset_util  = tasksetUtil;
		int i = tasksetIndex;
		this.taskset_util = current_taskset_util;
		System.out.println("Generate task set utilization :" + current_taskset_util + "\t workload index: " + i + "\r\n");
		boolean isFolderCreated = (new File(topFolder + "/" + folderDF.format(current_taskset_util))).mkdirs();
		if(!isFolderCreated){System.err.println("Create the folder for xml files fails!");}
		String output_tasksetFilename = topFolder + "/" + folderDF.format(current_taskset_util) + "/" + i + "-" + folderDF.format(current_taskset_util) + ".txt";
		String output_xmlFilename = topFolder + "/" + folderDF.format(current_taskset_util) + "/" + i + "-" + folderDF.format(current_taskset_util) + "-in.xml";
		this.generateTaskset(isPeriodHarmonic);

		this.distribute2Domains();
		this.writeTasks2File(output_tasksetFilename);
		this.writeXML(output_xmlFilename);
		//clear the generated tasks, prepare for the next task set.
		this.taskset = new Vector<Task>();
		this.domainTasks = new Vector<Vector<Task>>();
		for(int j=0; j < domain_num; j++){
			domainTasks.add(new Vector<Task>());
		}
	}

		
	
	
	
	public TasksetGenerator(){
		this.df = new DecimalFormat("#.##");
		this.task_period_harmonic_values = new Vector<Double>();
		this.taskset = new Vector<Task>();
		this.domainTasks = new Vector<Vector<Task>>();
		this.domainPeriods = new Vector<Double>();

	}
	
	public TasksetGenerator(double task_util_min, double task_util_max, double task_period_min, double task_period_max,
			int domain_num, Vector<Double> domainPeriods){
		this.df = new DecimalFormat("#.##");
		this.task_util_min = task_util_min;
		this.task_util_max = task_util_max;
		this.task_period_min = task_period_min;
		this.task_period_max = task_period_max;
		this.domain_num = domain_num;
		this.domainPeriods = domainPeriods;
		this.task_period_harmonic_values = new Vector<Double>();
		this.taskset = new Vector<Task>();
		
		this.domainTasks = new Vector<Vector<Task>>();
		for(int i=0; i<domain_num; i++){
			domainTasks.add(new Vector<Task>());
		}
		
	}
	
	
	
	public void clearAllFields(){
		this.task_util_min = 0;
		this.task_util_max = 0;
		this.task_period_min = 0;
		this.task_period_max = 0;
		this.domain_num = 0;
		this.domainPeriods = null;
		this.taskset_util = 0;
		this.task_period_harmonic_values = null;
		this.taskset = null;
		this.domainTasks = null;	
	}
	
	/**
	 * Function getRandom
	 * Return a random value between [0,1)
	 * 
	 */
	public double getRandom(){
		double x = Math.random();
		return x;
	}
	
	
	/** 
	 * Function getPeriod
	 * @param isHarmonic; if true, return the harmonic period, which is the power of 2.
	 * @return return the period of the task. if isharmonic is true, the returned period is the power of 2.
	 */
	public int getRandomTaskPeriod(boolean isHarmonic){
		if(isHarmonic != true){
			return (int)(this.getRandom() * (this.task_period_max - this.task_period_min) + this.task_period_min);
		}else{
			//when firstly called, generate a harmonic period vector, i.e., 2^i, to speed up the generation process.
			if(this.task_period_harmonic_values == null || this.task_period_harmonic_values.isEmpty() ||
					this.task_period_harmonic_values.get(0) < this.task_util_min || 
					this.task_period_harmonic_values.get(this.task_period_harmonic_values.size()-1) > this.task_util_max){
				this.task_period_harmonic_values = new Vector<Double>();
				double currentPeriod = 0;
				int pow_index = 0;
				while(currentPeriod < this.task_period_min){
					currentPeriod = Math.pow(2, pow_index);
					pow_index++;
					
				}
				while(currentPeriod <= this.task_period_max){
					currentPeriod = Math.pow(2, pow_index);
					this.task_period_harmonic_values.add(currentPeriod);
					pow_index++;
				}
			}
			
			return (int)this.task_period_harmonic_values.get((int)Math.floor(this.getRandom() * this.task_period_harmonic_values.size())).doubleValue();

		}
	
	}
	
	public void modifyDeltaCRPMD(double new_delta_crpmd){
		for(int i=0; i<this.taskset.size();i++){
			taskset.get(i).setDelta_crpmd(new_delta_crpmd);
		}
		for(int i=0; i<this.domainTasks.size(); i++){
			for(int j=0; j<this.domainTasks.get(i).size();j++){
				this.domainTasks.get(i).get(j).setDelta_crpmd(new_delta_crpmd);
			}
		}
	}
	
	/**
	 * function generateTaskset
	 * Generate a taskset with the utilization equal to this.taskset_util
	 * Set the this.taskset of the object
	 * @return none.
	 */
	public void generateTaskset(boolean isHarmonic){
		double currentTotalUtil = 0;
		long task_index_i = 0;
		
		if(this.taskset_util <=0 ){
			Tool.write2log("ATTENTION: GenerateTaskset: taskset_util <=0.");
			System.err.println("ATTENTION: GenerateTaskset: taskset_util <=0");
		}
		
		while(currentTotalUtil <= this.taskset_util - this.task_util_max){
			double currentTaskUtil = 0;
			if(whichTasksetUtilDistr == GlobalVariable.UNIFROM_LIGHT || whichTasksetUtilDistr == GlobalVariable.UNIFROM_MEDIUM 
					|| whichTasksetUtilDistr == GlobalVariable.UNIFROM_HEAVY){
				 currentTaskUtil = this.getRandom() * (this.task_util_max - this.task_util_min) + this.task_util_min;
			}
			//Under Bimodel, tasksetUtil Min and Max are the 0.001 to 0.9 which is the range the bimodal can generate
			if(whichTasksetUtilDistr == GlobalVariable.BIMODAL_LIGHT){ //8/9  1/9 [0.001, 0.5) or [0.5, 0.9] 
				double chooser = this.getRandom() * (9  - 0) + 0; 
				if(chooser <= 8) currentTaskUtil = this.getRandom()*(0.5 - 0.001) + 0.001;
				if(chooser > 8 && chooser <=9 ) currentTaskUtil = this.getRandom() * (0.9 - 0.5) + 0.5;
			}
			
			if(whichTasksetUtilDistr == GlobalVariable.BIMODAL_MEDIUM){ // 6/9, 3/9 [0.001, 0.5) or [0.5, 0.9] 
				double chooser = this.getRandom() * (9  - 0) + 0; 
				if(chooser <= 6) currentTaskUtil = this.getRandom()*(0.5 - 0.001) + 0.001;
				if(chooser > 6 && chooser <=9 ) currentTaskUtil = this.getRandom() * (0.9 - 0.5) + 0.5;
			}
			
			if(whichTasksetUtilDistr == GlobalVariable.BIMODAL_HEAVY){ // 4/9, 5/9 [0.001, 0.5) or [0.5, 0.9] 
				double chooser = this.getRandom() * (9  - 0) + 0; 
				if(chooser <= 4) currentTaskUtil = this.getRandom()*(0.5 - 0.001) + 0.001;
				if(chooser > 5 && chooser <=9 ) currentTaskUtil = this.getRandom() * (0.9 - 0.5) + 0.5;
			}
			
			int currentTaskPeriod = this.getRandomTaskPeriod(isHarmonic);
			int currentTaskExe = (int)(currentTaskPeriod * currentTaskUtil);
			currentTaskUtil = currentTaskExe*1.0/currentTaskPeriod;
			if(currentTaskUtil < this.task_util_min || currentTaskUtil > this.task_util_max) {Tool.debug("generated task util is wrong, re-generate!"); continue;}
			if(currentTaskPeriod < this.task_period_min || currentTaskPeriod > this.task_period_max) {Tool.debug("generated task period is wrong, re-generate!"); continue;}
			
			Task task = new Task();
			task.setPeriod(currentTaskPeriod);
			task.setExe(currentTaskExe);
			task.setDeadline(currentTaskPeriod);
			task.setName(Long.toString(task_index_i));
			task.setDelta_rel(this.delta_rel);
			task.setDelta_sch(this.delta_sch);
			task.setDelta_cxs(this.delta_cxs);
			task.setDelta_crpmd(this.delta_crpmd);
			this.taskset.add(task);
			currentTotalUtil += task.getExe() / task.getPeriod();
			
			task_index_i++;			
			
		}
		if(this.taskset_util - currentTotalUtil > this.task_util_min){
			double currentTaskUtil = this.taskset_util - currentTotalUtil;
			int currentTaskPeriod = this.getRandomTaskPeriod(isHarmonic);
			int currentTaskExe = (int)(currentTaskPeriod * currentTaskUtil);
			Task task = new Task();
			task.setPeriod(currentTaskPeriod);
			task.setExe(currentTaskExe);
			task.setDeadline(currentTaskPeriod);
			task.setName(Long.toString(task_index_i));
			task.setDelta_rel(this.delta_rel);
			task.setDelta_sch(this.delta_sch);
			task.setDelta_cxs(this.delta_cxs);
			task.setDelta_crpmd(this.delta_crpmd);
			this.taskset.add(task);
		}
		
		
	}
	
	
	/**
	 * function generateTaskset_perTasksetSize
	 * Generate a taskset with the taskset size equal to this.taskset_size
	 * Set the this.taskset of the object
	 * @return none.
	 */
	public void generateTaskset_perTasksetSize(boolean isHarmonic){
		long task_index_i = 0;
		
		if(this.taskset_size <=0 ){
			Tool.write2log("ATTENTION: GenerateTaskset: taskset_size <=0.");
			System.err.println("ATTENTION: GenerateTaskset: taskset_size <=0");
		}
		
		while(task_index_i < this.taskset_size){
			double currentTaskUtil = 0;
			if(whichTasksetUtilDistr == GlobalVariable.UNIFROM_LIGHT || whichTasksetUtilDistr == GlobalVariable.UNIFROM_MEDIUM 
					|| whichTasksetUtilDistr == GlobalVariable.UNIFROM_HEAVY){
				 currentTaskUtil = this.getRandom() * (this.task_util_max - this.task_util_min) + this.task_util_min;
			}
			//Under Bimodel, tasksetUtil Min and Max are the 0.001 to 0.9 which is the range the bimodal can generate
			if(whichTasksetUtilDistr == GlobalVariable.BIMODAL_LIGHT){ //8/9  1/9 [0.001, 0.5) or [0.5, 0.9] 
				double chooser = this.getRandom() * (9  - 0) + 0; 
				if(chooser <= 8) currentTaskUtil = this.getRandom()*(0.5 - 0.001) + 0.001;
				if(chooser > 8 && chooser <=9 ) currentTaskUtil = this.getRandom() * (0.9 - 0.5) + 0.5;
			}
			
			if(whichTasksetUtilDistr == GlobalVariable.BIMODAL_MEDIUM){ // 6/9, 3/9 [0.001, 0.5) or [0.5, 0.9] 
				double chooser = this.getRandom() * (9  - 0) + 0; 
				if(chooser <= 6) currentTaskUtil = this.getRandom()*(0.5 - 0.001) + 0.001;
				if(chooser > 6 && chooser <=9 ) currentTaskUtil = this.getRandom() * (0.9 - 0.5) + 0.5;
			}
			
			if(whichTasksetUtilDistr == GlobalVariable.BIMODAL_HEAVY){ // 4/9, 5/9 [0.001, 0.5) or [0.5, 0.9] 
				double chooser = this.getRandom() * (9  - 0) + 0; 
				if(chooser <= 4) currentTaskUtil = this.getRandom()*(0.5 - 0.001) + 0.001;
				if(chooser > 5 && chooser <=9 ) currentTaskUtil = this.getRandom() * (0.9 - 0.5) + 0.5;
			}
			
			int currentTaskPeriod = this.getRandomTaskPeriod(isHarmonic);
			int currentTaskExe = (int)(currentTaskPeriod * currentTaskUtil);
			currentTaskUtil = currentTaskExe*1.0/currentTaskPeriod;
			if(currentTaskUtil < this.task_util_min || currentTaskUtil > this.task_util_max) {Tool.debug("generated task util is wrong, re-generate!"); continue;}
			if(currentTaskPeriod < this.task_period_min || currentTaskPeriod > this.task_period_max) {Tool.debug("generated task period is wrong, re-generate!"); continue;}
			
			Task task = new Task();
			task.setPeriod(currentTaskPeriod);
			task.setExe(currentTaskExe);
			task.setDeadline(currentTaskPeriod);
			task.setName(Long.toString(task_index_i));
			task.setDelta_rel(this.delta_rel);
			task.setDelta_sch(this.delta_sch);
			task.setDelta_cxs(this.delta_cxs);
			task.setDelta_crpmd(this.delta_crpmd);
			this.taskset.add(task);
			
			task_index_i++;			
			
		}
		
		
	}
	/**
	 * Function roundns2us
	 * Round each task's period, deadline and execution time from ns to microsecond. 
	 * Because the granularity of RT-Xen is microsecond and overhead's time unit is microsecond. 
	 */
	public void roundns2us(){
		if(this.taskset == null || this.taskset.isEmpty()) return;
		for(int i=0; i<this.taskset.size(); i++){
			Task task = this.taskset.get(i);
			task.setPeriod(task.getPeriod()/1000);
			task.setDeadline(task.getDeadline()/1000);
			task.setExe(task.getExe()/1000);
			task.setDelta_rel(task.getDelta_rel());
			task.setDelta_sch(task.getDelta_sch());
			task.setDelta_cxs(task.getDelta_cxs());
			task.setDelta_crpmd(task.getDelta_crpmd());
		}
		
	}
	
	/**
	 * Function round2ms
	 * Round each task's period, deadline and execution time from ns to millisecond.
	 */
	public void roundns2ms(){
		this.roundns2us();
		this.roundns2us();
	}
	
	/**
	 * Function distribute2Domains
	 * Distribute the taskset into the number of domains, uniformly.
	 */
	public void distribute2Domains(){
		Tool.debug("Distribute tasks to " + this.domain_num + " domains now \r\n");
		if(this.domainTasks == null) {System.err.println("this.domainTasks is null");System.exit(1);}
		for(int i=0; i<this.taskset.size(); i++){
			this.domainTasks.get((int)(i%this.domain_num)).add(this.taskset.get(i));
		}
	}
	
	
	public void writeTasks2File(String output_tasksetFilename){
		
		if(this.taskset == null || this.taskset.isEmpty()) return;
		String str = "";
		str += "name\t period\t exe\t deadline\t delta_rel\t delta_sch\t delta_cxs\t delta_crpmd\r\n";
		for(int i=0; i<this.taskset.size(); i++){
			Task task = this.taskset.get(i);
			str += task.getName() + "\t" + task.getPeriod() + "\t" + task.getExe() + "\t" + task.getDeadline() +
					task.getDelta_rel() + "\t" + task.getDelta_sch() + "\t" + task.getDelta_cxs() + "\t" + task.getDelta_crpmd() + "\r\n";
			
		}
		
		try{
			BufferedWriter output_tasksetFile = new BufferedWriter(new FileWriter(output_tasksetFilename,false));
			output_tasksetFile.write(str);
			output_tasksetFile.flush();
			Tool.debug(str);
			output_tasksetFile.close();
		}catch (IOException e){
			System.err.println("GenerateTaskset: taskset File open failed. Filename:" + output_tasksetFilename);
			Tool.write2log("GenerateTaskset: taskset File open failed. Filename:" + output_tasksetFilename);
		}
		
	}
	
	/**
	 * Function write2XML
	 * Write each domain's taskset into the input xml file
	 */
	public void writeXML(String output_xmlFilename){
		String str = "";
		if(this.domainPeriods == null || this.domainPeriods.isEmpty()){
			System.err.println("GenerateTaskset: domainPeriods field is null or empty. Should initialize this field first! Exit(1)!");
			System.exit(1);
		}
		
		str += "<system os_scheduler=\"gEDF\" period=\"" + this.systemPeriod +  "\"> \r\n";
		for(int i=0; i<this.domain_num; i++){
			str += "\t<component name=\"C" + i + "\" scheduler=\"gEDF\" period=\""+df.format(this.domainPeriods.get(i))+"\"> \r\n";
			for(int j=0; j<this.domainTasks.get(i).size(); j++){
				Task task = this.domainTasks.get(i).get(j);
				
				str+= "\t\t <task name=\"T" + task.getName() + "\" p=\"" + df.format(task.getPeriod()) + "\" d=\"" + df.format(task.getDeadline()) + 
						"\" e=\"" + df.format(task.getExe()) + "\" " + "delta_rel=\"" + df.format(task.getDelta_rel()) + "\" delta_sch=\"" + df.format(task.getDelta_sch()) + 
						"\" delta_cxs=\"" + df.format(task.getDelta_cxs()) + "\" delta_crpmd=\"" + df.format(task.getDelta_crpmd()) + "\" > </task>\r\n";
			}
			
			str += "\t</component>\r\n";
		}
		str += "</system>\r\n";
		
		try{
			
			BufferedWriter output_xmlFile = new BufferedWriter(new FileWriter(output_xmlFilename,false));
			output_xmlFile.write(str);
			output_xmlFile.flush();
			Tool.debug(str);
			output_xmlFile.close();
		}catch (IOException e){
			System.err.println("GenerateTaskset: open output_xmlFile fails. file name: " + output_xmlFilename + "\r\n");
			Tool.write2log("GenerateTaskset: open output_xmlFile fails. file name: " + output_xmlFilename + "\r\n");
		}
	}
	
	
	
	
	
	///////////////Get and Set Function/////////////////////////////////
	public double getTask_util_min() {
		return task_util_min;
	}









	public void setTask_util_min(double task_util_min) {
		this.task_util_min = task_util_min;
	}









	public double getTask_util_max() {
		return task_util_max;
	}









	public void setTask_util_max(double task_util_max) {
		this.task_util_max = task_util_max;
	}









	public double getTask_period_min() {
		return task_period_min;
	}









	public void setTask_period_min(double task_period_min) {
		this.task_period_min = task_period_min;
	}









	public double getTask_period_max() {
		return task_period_max;
	}









	public void setTask_period_max(double task_period_max) {
		this.task_period_max = task_period_max;
	}









	public double getDomain_num() {
		return domain_num;
	}









	public void setDomain_num(double domain_num) {
		this.domain_num = domain_num;
	}









	public double getTaskset_util() {
		return taskset_util;
	}









	public void setTaskset_util(double taskset_util) {
		this.taskset_util = taskset_util;
	}









	public double getDelta_rel() {
		return delta_rel;
	}









	public double getDelta_sch() {
		return delta_sch;
	}









	public double getDelta_cxs() {
		return delta_cxs;
	}









	public double getDelta_crpmd() {
		return delta_crpmd;
	}









	








	public int getNO_OVERHEAD() {
		return NO_OVERHEAD;
	}









	public int getCAMPR() {
		return CAMPR;
	}









	public int getBASELINE() {
		return BASELINE;
	}









	

}
