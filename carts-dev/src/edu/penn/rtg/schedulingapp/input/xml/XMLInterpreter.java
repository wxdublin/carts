package edu.penn.rtg.schedulingapp.input.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.penn.rtg.schedulingapp.TreeComponent;
import edu.penn.rtg.schedulingapp.SchComponent;
import edu.penn.rtg.schedulingapp.SchedulingComponent;
import edu.penn.rtg.schedulingapp.SchedulingTree;
import edu.penn.rtg.schedulingapp.Task;
import edu.penn.rtg.schedulingapp.TaskList;

/**
 * Interpret XML Document via parser
 */
public class XMLInterpreter {

	protected XMLParser parser;
	protected Document doc;
	protected SchedulingTree tree;
	private DocumentBuilder db;
	protected String fileName;
	private String errorMsg;

	/**
	 * XMLInterpreter Constructor
	 * @param file XML file
	 */

	public XMLInterpreter(String file)  {
		fileName = file;
	}

	/**
	 * Return tree which is interpreted by this class
	 * @return scheduling tree
	 */

	public SchedulingTree getScTree()  {
		return tree;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	
	/**
	 * Parse XML file
	 * @param filename specified filename
	 */

	public boolean readFile() {
		DocumentBuilderFactory dbf = null;
		Document docu = null;

		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			docu = db.parse(new File(fileName));

			Node root = docu.getDocumentElement();
			if (root.getNodeName().equalsIgnoreCase("system")) {
				readNode(root);
				
			} else {
				errorMsg="root node is not <system>";
				return false;
			}
		} catch (FactoryConfigurationError e) {
			errorMsg="DocumentBuilderFactory Configuration error:"+e.getMessage();
			//e.printStackTrace();
			return false;
		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
			errorMsg="Parser Configuration error:"+e.getMessage();
			return false;
		} catch (SAXException e) {
			errorMsg="XML error:"+e.getMessage();
			//e.printStackTrace();
			return false;
		} catch (IOException e) {
			if( e instanceof FileNotFoundException) {
				errorMsg="Input file "+fileName+" is not founded";
			}
			else
			{
				errorMsg="I/O error:"+e.getMessage();
			}
			//e.printStackTrace();
			return false;
		} catch (Exception e) {
			if(e instanceof NumberFormatException) {
				errorMsg="CARTS XML tag value is not number:"+e.getMessage();
			}
			else
			{
				errorMsg="CARTS XML structure error:"+e.getMessage();
			}
			//e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Read node to build scheduling tree
	 * @param node given node
	 * @throws Exception 
	 */

	private void readNode(Node node) throws Exception  {
		if (node.getNodeName().equalsIgnoreCase("system")) {
			TreeComponent root=readRootComp(node);
			tree = new SchedulingTree(root);
		}

		if (tree.getRoot() != null) {
			//System.out.println(node.getChildNodes().getLength());
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				readChildren(tree.getRoot(), node.getChildNodes().item(i));
			}
		} else {
			System.out.println("err in xml interpreter");
//			readChildren(node);
		}
	}
//	/**
//	 * Read child nodes of given node to build scheduling tree
//	 * @param node given node
//	 */

//	private void readChildren(Node node) throws Exception {
//		if (!node.hasChildNodes())
//			return;
//
//		NodeList childNodes = node.getChildNodes();
//		for (int i = 0; i < childNodes.getLength(); i++) {
//			Node child = (Node) childNodes.item(i);
//			if (child.getNodeType() == Node.ELEMENT_NODE) {
//				readNode(child);
//				continue;
//			}
//			if (child.getNodeType() == Node.TEXT_NODE) {
//				if (child.getNodeValue() != null)
//					// System.out.println(child.getNodeValue())
//					;
//			}
//		}
//	}
	
	/**
	 * Read node to add children of specified scheduling component
	 * @param parent parent scheduling component of given node
	 * @param node given node
	 * @throws Exception 
	 */

	private void readChildren(TreeComponent parent, Node child) throws Exception {
		if (child.getNodeName().equalsIgnoreCase("task")) {
			readTask(parent,child);
		} else if (child.getNodeName().equalsIgnoreCase("component")) {
			readComp(parent,child);
		}
	}
	private TreeComponent readRootComp(Node node)
	{

		if (!node.hasAttributes()) return null;
		
		NamedNodeMap attrList = node.getAttributes();
		TreeComponent root=readCompAttr(attrList,true);
		return root;
	}
	private void readComp(TreeComponent parent, Node child) throws Exception
	{
		NamedNodeMap attrList = child.getAttributes();
		if(attrList.getNamedItem("name")==null) {
			throw new Exception("The name of a component is missing.");
		}
		String name=attrList.getNamedItem("name").getNodeValue();
		if (tree.getMap().get(name) != null) {
			throw new Exception("Component name '"+name+"' is duplicated.");
		}
		TreeComponent comp=readCompAttr(attrList,false);
		tree.addComponent(comp, parent);

		for (int i = 0; i < child.getChildNodes().getLength(); i++) {
			readChildren(comp, child.getChildNodes().item(i));
		}
	}
	private TreeComponent readCompAttr(NamedNodeMap attrList,boolean isRoot) {
		TreeComponent c;
		SchComponent comp;
		if(isRoot) {
			c = new TreeComponent("OS Scheduler",null);
			comp=c.getSchCom();
			comp.setMinPeriod(1);
			comp.setMaxPeriod(1);
		}
		else
		{
			String name = attrList.getNamedItem("name").getNodeValue();
			c = new TreeComponent(name, tree.getRoot());
			c.setTaskList(new TaskList());
			comp=c.getSchCom();
			
		}

//		if (attrList.getNamedItem("subtype") != null) {
//			if (attrList.getNamedItem("subtype").getNodeValue()
//					.equalsIgnoreCase("tasks")) {
//				comp.setTaskList(new TaskList());
//			} else {
//				//comp.setSubUnit(new RBTTree());
//			}
//		}
		String scheduler;
		if (attrList.getNamedItem("scheduler") != null) {
			scheduler = attrList.getNamedItem("scheduler").getNodeValue();
		} else if (attrList.getNamedItem("os_scheduler") != null) {
			scheduler = attrList.getNamedItem("os_scheduler").getNodeValue();
		} else {
			scheduler = "edf";
		}
		comp.setAlgorithm(scheduler);

		if (attrList.getNamedItem("min_period") != null && attrList.getNamedItem("max_period") != null) {
			double minperiod = new Double(attrList.getNamedItem("min_period")
					.getNodeValue());
			double maxperiod = new Double(attrList.getNamedItem("max_period")
					.getNodeValue());
			comp.setMinPeriod(minperiod);
			comp.setMaxPeriod(maxperiod);
		} 

		if (attrList.getNamedItem("period") != null) {
			double period = new Double(attrList.getNamedItem("period").getNodeValue());
			comp.setPeriod(period);
			comp.setMaxPeriod(period);
			comp.setMinPeriod(period);
		}

		if (attrList.getNamedItem("criticality") != null) {
			String criticality = attrList.getNamedItem("criticality")
					.getNodeValue();
			comp.setCriticality(criticality);
		}

		if (attrList.getNamedItem("vmips") != null) {
			String vmips = attrList.getNamedItem("vmips").getNodeValue();
			comp.setVmipsStr(vmips);
		}

		if (attrList.getNamedItem("subtype") != null) {
			String subtype = attrList.getNamedItem("subtype").getNodeValue();
			comp.setSubType(subtype);
		}
		return c;
	}
	private void readTask(TreeComponent parent, Node child) throws Exception
	{
		NamedNodeMap attrList = child.getAttributes();
		
		String taskName="";
		if(attrList.getNamedItem("name")!=null)
			taskName = new String(child.getAttributes().getNamedItem("name").getNodeValue());
		Task task = new Task(taskName);

		if(attrList.getNamedItem("p")==null) {
			//System.out.println("period of a task is missing");
			throw new Exception("period of a task is missing");
		}
		double period = Double.parseDouble(attrList.getNamedItem("p").getNodeValue());
		task.setPeriod(period);

		if(attrList.getNamedItem("e")==null) {
			System.out.println("execution of a task is missing");
			throw new Exception("execution time of a task is missing");
		}
		double execution = Double.parseDouble(attrList.getNamedItem("e").getNodeValue());
		task.setExecution(execution);

		if(attrList.getNamedItem("d")==null) {
			System.out.println("deadline of a task is missing");
			throw new Exception("deadline of a task is missing");
		}
		double deadline =  Double.parseDouble(attrList.getNamedItem("d").getNodeValue());
		task.setDeadline(deadline);

		if(child.getAttributes().getNamedItem("cs")!=null)
		{
			int cs=Integer.parseInt(child.getAttributes().getNamedItem("cs").getNodeValue());
			task.setCriticalSection(cs);
		}
		

		if (child.getAttributes().getNamedItem("jitter") != null) {
			String jitter = new String(child.getAttributes().getNamedItem("jitter").getNodeValue());
			task.setJitter(Double.parseDouble(jitter));
		}
		if (child.getAttributes().getNamedItem("offset") != null) {
			String offset = new String(child.getAttributes().getNamedItem("offset").getNodeValue());
			task.setOffset(Double.parseDouble(offset));
		}
		if (child.getAttributes().getNamedItem("noninterrupt_fraction")!= null) {
			String noninterrupt_fraction = new String(child.getAttributes()
			.getNamedItem("noninterrupt_fraction").getNodeValue());
			task.setNonIntFunc(Double.parseDouble(noninterrupt_fraction));
		}
		
		if(child.getAttributes().getNamedItem("delta_rel") != null){
			String delta_rel_str = child.getAttributes().getNamedItem("delta_rel").getNodeValue();
			task.setDelta_rel(Double.parseDouble(delta_rel_str));
		}
		if(child.getAttributes().getNamedItem("delta_sch") != null){
			String delta_sch_str = child.getAttributes().getNamedItem("delta_sch").getNodeValue();
			task.setDelta_sch(Double.parseDouble(delta_sch_str));
		}
		if(child.getAttributes().getNamedItem("delta_cxs") != null){
			String delta_cxs_str = child.getAttributes().getNamedItem("delta_cxs").getNodeValue();
			task.setDelta_cxs(Double.parseDouble(delta_cxs_str));
		}
		if(child.getAttributes().getNamedItem("delta_crpmd") != null){
			String delta_crpmd_str = child.getAttributes().getNamedItem("delta_crpmd").getNodeValue();
			task.setDelta_crpmd(Double.parseDouble(delta_crpmd_str));
		}
		
		tree.addTask(parent, task);
		
	}

}
