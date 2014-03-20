package edu.penn.rtg.experiment.systemtransformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import edu.penn.rtg.common.Tool;
import edu.penn.rtg.csa.cadmpr.Component;
import edu.penn.rtg.csa.cadmpr.XMLInterpreter4CADMPR;
import edu.penn.rtg.csa.cadmpr.Task;

public class SystemTransformer {

	
	public SystemTransformer(String inputFilename){
		XMLInterpreter4CADMPR xmlInterpreter = new XMLInterpreter4CADMPR(inputFilename); //4 is short for "for"
		xmlInterpreter.parseFile();
		xmlInterpreter.getRootComponent();
	}
	
	public static DecimalFormat folderDF = new DecimalFormat("#.###");
	public static DecimalFormat fileDF = new DecimalFormat("#.###");
	public static DecimalFormat df = new DecimalFormat("#.###");
	
	public static double getRandom(){
		double x = Math.random();
		return x;
	}

	public static void modifyCostOfOverhead(Component component, double ohRatio){
		if(component.getChildComponents() == null || component.getChildComponents().isEmpty()){ //leaf component
			Vector<Task> taskset = component.getTaskset();
			if(taskset == null || taskset.isEmpty()){
				return; //no task to change
			}
			for(int i = 0; i < taskset.size(); i++){
				Task task = taskset.get(i);
				Double delta_crpmd_new = task.getExe() * ohRatio;
				task.setDelta_crpmd(delta_crpmd_new);
			}
		}else{
			for(int i=0; i < component.getChildComponents().size(); i++){
				Component childComponent = component.getChildComponents().get(i);
				modifyCostOfOverhead(childComponent, ohRatio);
			}	
		}
	}

	public static void varyCostOfOverhead(String rootPath, double tasksetUtil, double ohRatioMin, double ohRatioStep,
			double ohRatioMax, double tasksetNumPerOhRatio){
		
		String topFolder = rootPath + "/" + fileDF.format(ohRatioMin) + "-" + fileDF.format(ohRatioStep) + "-" + fileDF.format(ohRatioMax) + "-" + tasksetNumPerOhRatio;
		boolean isFolderCreated = (new File(topFolder)).mkdirs();
		if(!isFolderCreated){System.err.println("Create the folder" + topFolder + "for xml files fails!");}
		for(double ohRatio = ohRatioMin; ohRatio < ohRatioMax; ohRatio += ohRatioStep){
			//create the folder for this ohRatio output
			isFolderCreated = (new File(topFolder + "/" + folderDF.format(ohRatio))).mkdirs();
			if(!isFolderCreated){System.err.println("Create the folder for xml files fails!");}
			for(int tasksetIndex = 0; tasksetIndex < tasksetNumPerOhRatio; tasksetIndex++){
				String inputFilename = rootPath + "/" + "rawData" + "/" + tasksetIndex + "-" + fileDF.format(tasksetUtil) + "-in.xml";
				String outputFilename = topFolder + "/" + fileDF.format(ohRatio) + "/" 
						+ tasksetIndex + "-" + fileDF.format(ohRatio) + "-in.xml";
				
				XMLInterpreter4CADMPR xmlInterpreter = new XMLInterpreter4CADMPR(inputFilename); //4 is short for "for"
				xmlInterpreter.parseFile();
				Component rootComponent = xmlInterpreter.getRootComponent();
				
				modifyCostOfOverhead(rootComponent, ohRatio);
				
				writeXML(outputFilename, rootComponent);
			}
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		folderDF.setMaximumFractionDigits(2);
		folderDF.setMinimumFractionDigits(2);
		fileDF.setMaximumFractionDigits(2);
		fileDF.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		
		String rootPath_EffectOfOhCost = "workspace-rtsj/effect-costOfCacheOh/4.90";
		varyCostOfOverhead(rootPath_EffectOfOhCost, 4.90, 0.00, 0.10, 1.00, 25);
		
	}
	
	
	/**
	 * Only work for two level! For RT-Xen
	 * @param outputFilename
	 * @param rootComponent
	 */
	public static void writeXML(String outputFilename, Component rootComponent){
		String str = "";
		
		if(rootComponent == null){
			System.err.println("not valid rootComponent Exit(1)!");
			System.exit(1);
		}
		
		str += "<system os_scheduler=\"gEDF\" period=\"" + rootComponent.getdMPRInterface().getPi() +  "\"> \r\n";
		
		for(int i=0; i<rootComponent.getChildComponents().size(); i++){
			Component childComponent = rootComponent.getChildComponents().get(i);
			str += "\t<component name=\"C" + i + "\" scheduler=\"gEDF\" period=\""+df.format(childComponent.getdMPRInterface().getPi())+"\"> \r\n";
			for(int j=0; j<childComponent.getTaskset().size(); j++){
				Task task = childComponent.getTaskset().get(j);
				
				str+= "\t\t <task name=\"T" + task.getName() + "\" p=\"" + df.format(task.getPeriod()) + "\" d=\"" + df.format(task.getDeadline()) + 
						"\" e=\"" + df.format(task.getExe()) + "\" " + "delta_rel=\"" + df.format(task.getDelta_rel()) + "\" delta_sch=\"" + df.format(task.getDelta_sch()) + 
						"\" delta_cxs=\"" + df.format(task.getDelta_cxs()) + "\" delta_crpmd=\"" + df.format(task.getDelta_crpmd()) + "\" > </task>\r\n";
			}
			
			str += "\t</component>\r\n";
		}
		str += "</system>\r\n";
		
		try{
			
			BufferedWriter output_xmlFile = new BufferedWriter(new FileWriter(outputFilename,false));
			output_xmlFile.write(str);
			output_xmlFile.flush();
			Tool.debug(str);
			output_xmlFile.close();
		}catch (IOException e){
			System.err.println("GenerateTaskset: open output_xmlFile fails. file name: " + outputFilename + "\r\n");
			Tool.write2log("GenerateTaskset: open output_xmlFile fails. file name: " + outputFilename + "\r\n");
		}
	}

}
