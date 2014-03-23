package edu.penn.rtg.experiment.rtxen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.Vector;

import edu.penn.rtg.common.Tool;
import edu.penn.rtg.csa.cadmpr.Component;
import edu.penn.rtg.csa.cadmpr.XMLInterpreter4CADMPR;
import edu.penn.rtg.csa.cadmpr.Task;

public class RTXenTaskScriptGenerator {

	
	public RTXenTaskScriptGenerator(String inputFilename){
		XMLInterpreter4CADMPR xmlInterpreter = new XMLInterpreter4CADMPR(inputFilename); //4 is short for "for"
		xmlInterpreter.parseFile();
		xmlInterpreter.getRootComponent();
	}
	

	public static int CACHE_SIZE_KB = 256;
	public static int DURATION  = 60; //second
	public static int DURATION_AFTER_FINISH = 20; //second
	
	public static void generateTaskScript4RTXen(String rootPath, String inputFilename){
		String tasksetFilepath = rootPath + "/" + inputFilename;
		XMLInterpreter4CADMPR xmlInterpreter = new XMLInterpreter4CADMPR(tasksetFilepath); //4 is short for "for"
		xmlInterpreter.parseFile();
		Component rootComponent = xmlInterpreter.getRootComponent();
		
		for(int i=0; i<rootComponent.getChildComponents().size(); i++){
			Component childComponent = rootComponent.getChildComponents().get(i);
			String scriptPerComponent = "";
			scriptPerComponent += "chmod a+x ./st_trace\r\n"
								+ "./st_trace " + inputFilename + "_Dom" + i + " &\r\n" 
								+ "sleep 5\r\n"
								+ "./cpu_busy &\r\n"; //cpu_busy to get periodic server
			
			for(int j=0; j < childComponent.getTaskset().size(); j++){
				Task task = childComponent.getTaskset().get(j);
				scriptPerComponent += "./base_task_cache " + task.getPeriod() + " " + task.getExe() + " " 
							+ CACHE_SIZE_KB + " " + DURATION + "&\r\n"; 
			}
			scriptPerComponent += "sleep " + DURATION + "\r\n"
								+ "sleep " + DURATION_AFTER_FINISH + "\r\n"
								+ "mv * /root/rtsj14/\r\n"
								+ "echo -n \"Dom_" + i + " finish at \"\r\n"
								+ "date";
			String scriptOutputFilename = rootPath + "/" + inputFilename + "_Dom" + i +".sh";
			try{
				BufferedWriter scriptOutputFile = new BufferedWriter(new FileWriter(scriptOutputFilename, false));
				scriptOutputFile.write(scriptPerComponent);
				scriptOutputFile.flush();
				System.out.println(scriptPerComponent);
			}catch (IOException e){
				System.err.println("GenerateTaskset: open output_xmlFile fails. file name: " + scriptOutputFilename + "\r\n");
				Tool.write2log("GenerateTaskset: open output_xmlFile fails. file name: " + scriptOutputFilename + "\r\n");
			}
			
		}
		
		
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String rootPath = args[0];
		String inputFilename = args[1];
		
		generateTaskScript4RTXen(rootPath, inputFilename);
		

		
	}
	
	

}
