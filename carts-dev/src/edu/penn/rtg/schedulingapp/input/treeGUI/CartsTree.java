package edu.penn.rtg.schedulingapp.input.treeGUI;

import java.awt.Component;
import java.util.Collection;


import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;


import edu.penn.rtg.schedulingapp.Task;
import edu.penn.rtg.schedulingapp.input.treeGUI.dialog.AddComponentDialog;
import edu.penn.rtg.schedulingapp.input.treeGUI.dialog.AddTaskDialog;
import edu.penn.rtg.schedulingapp.input.treeGUI.dialog.EditComponentDialog;
import edu.penn.rtg.schedulingapp.input.treeGUI.dialog.EditTaskDialog;
import edu.penn.rtg.schedulingapp.input.treeGUI.translator.*;
import edu.penn.rtg.schedulingapp.input.treeGUI.*;

import edu.penn.rtg.schedulingapp.util.UserUtil;
import edu.penn.rtg.schedulingapp.output.OutputI;
import edu.penn.rtg.schedulingapp.*;

@SuppressWarnings("serial")
public class CartsTree extends JTree  {
	// Scheduling Tree object for which this class handles the GUI
	protected SchedulingTree schedulingTree;
	// Pop up menu for Component and Task
	protected JPopupMenu popupTask;
	protected ComponentMenu popupComponent; 
	private CartsTreeModel treeModel;
	// Mouse Listener(Reference to the selected component in the tree)
	private CartsMouseLis mouseLis;
	// Output object to which Analysis Result needs to be sent
	protected OutputI output;
	private TreeComponent lastComp;
	private CartsI main;
	public CartsTree(DefaultTreeModel treeModel, SchedulingTree scTree, OutputI out,CartsI treeUI)
	{
		super(treeModel);
		this.treeModel=new CartsTreeModel(treeModel,this);
		this.schedulingTree=scTree;
		this.output=out;
		this.main=treeUI;

		setEditable(false);
		getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		setShowsRootHandles(true);
		setCellRenderer(new CartsTableRenderer());
		mouseLis=new CartsMouseLis(this);
		addMouseListener(mouseLis);
		loadTree(scTree.getRoot());
	}
	///////////////////////////////////////////////////////////////////
	//
	// JTree Related
	//
	////////////////////////////////////////////////////////////////////


	/**
	 * Initializes the underlying JTree object with the Components/Tasks in the
	 * given Scheduling Tree
	 * 
	 * @param comp
	 *            The component which needs to be loaded
	 */
	protected void loadTree(TreeComponent comp) {
		Collection<TreeComponent> components = comp.getAllChildren();
		for (TreeComponent component : components) {
			treeModel.addObject(comp, component);
			loadTree(component);
		}

		Collection<Task> tasks = comp.getUnsortedTask(0);
		for (Task task : tasks) {
			treeModel.addObject(comp, task);
		}
	}



	///////////////////////////////////////////////////////////////////
	//
	// Graph /Algo
	//
	////////////////////////////////////////////////////////////////////

	/**
	 * Generate values to draw the SBF/DBF graphs
	 */
	protected void drawGraph() {
		TreeComponent comp = selCom();
		if (comp==null) return ;
		CartsAnalysisThread cat=new CartsAnalysisThread(comp);
		cat.start();
	}


	/**
	 *  process given Algorithms on the tree (Common for Algorithm Handler)
	 *  @param algo: Algorithms PRM, EDP, EQV, SIRAP, which are implemented before 1/1/2013
	 */

	protected void processAlgo(String algo)
	{
		TreeComponent comp = selCom();
		if (comp==null) return ;
		lastComp=comp;
		TreeComponent root=schedulingTree.getRoot();
		CartsAnalysisThread cat=new CartsAnalysisThread(algo,comp,root,output,this);
		cat.start();
	}
	

	/**
	 * process given Algorithms on the tree (Common for Algorithm Handler)
	 * @param algo: algorithms MPR, DMPR, CADMPR_TASKCENTRIC, CADMPR_MODELCENTRIC, CADMPR_HYBRID
	 * 				which are implemented after 1/1/2013
	 */
	protected void processAlgo2(String algo)
	{
		//TODO: new a thread to calculated components' interface and update the interface!	
		TreeComponent comp = selCom();
		if (comp==null) return ;
		lastComp=comp;

		TreeComponent root=schedulingTree.getRoot();
		CartsAnalysisThread cat=new CartsAnalysisThread(algo,comp,root,output,this);
		cat.start();		
	
	}
	
	

	///////////////////////////////////////////////////////////////////
	//
	// Menu Handler
	//
	////////////////////////////////////////////////////////////////////


	public TreeComponent selCom()
	{
		// Check whether the selected node in the tree UI is a component
		if (mouseLis.isSelNull()
				|| (!( mouseLis.getSelLastNode() instanceof TreeComponent))) {
			UserUtil.show("Select a Component");
			return null;
		}
		else {
			return  mouseLis.getSelLastCom();
		}
	}
	public Task selTask()
	{
		// Check whether the selected node in the tree is a Task
		if (mouseLis.isSelNull()
				|| (!( mouseLis.getSelLastNode() instanceof Task))) {
			UserUtil.show("Select a Task");
			return null;
		}
		else {
			return mouseLis.getSelLastTask();
		}
	}
	/**
	 * Handler for 'Add Component' clicked in the pop up menu
	 */
	public void addCompClicked() {
		TreeComponent comp = selCom();
		if (comp==null) return ;
		TreeComponent root=schedulingTree.getRoot();		
		JDialog d = new AddComponentDialog(this,root);
		d.setVisible(true);
	}

	/**
	 * Handler for 'Edit Component' clicked in the pop up menu
	 */
	public void editCompClicked() {
		TreeComponent comp = selCom();
		if (comp==null) return ;
		TreeComponent root=schedulingTree.getRoot();		
		JDialog d = new EditComponentDialog(this,comp,root);
		d.setVisible(true);
	}

	private void postCmd(TreeComponent comp,boolean isUpdated)
	{
		if(isUpdated){
			comp.setProcessed(false);
			comp.setProcessedRecursive(false);
			if(lastComp!=null && !lastComp.isProcessed()) {
				output.displayEmpty();
				TreeComponent root=schedulingTree.getRoot();
				root.setProcessedRecursive(false);
			}
			this.updateUI();
		}
		main.convertToXML();

	}

	/**
	 * Handler for 'Remove Component' clicked in the pop up menu
	 */
	public void removeCompClicked() {
		TreeComponent comp = selCom();
		if (comp==null) return ;
		// Check whether the selected component is the root itseld. if yes,
		// show warning
		if (mouseLis.getSelLen() == 1) {
			UserUtil.showErr("Root component cannot be removed");
		} else {
			TreeComponent parent =  mouseLis.getSelLastLastCom();
			TreeComponent child = comp;
			treeModel.removeObject(child);
			schedulingTree.removeComponent(parent, child);
			postCmd(parent,true);
		}
		this.updateUI();
	}

	/**
	 * Handler for 'Add Task' clicked in the pop up menu
	 */
	public void addTaskClicked() {
		TreeComponent comp = selCom();
		if (comp==null) return ;
		JDialog d =  new AddTaskDialog(this);
		d.setVisible(true);
	}

	/**
	 * Handler for 'Edit Task' clicked in the pop up menu
	 */
	public void editTaskClicked() {
		Task task = selTask();
		if (task==null) return ;
		JDialog d =  new EditTaskDialog(this,task);
		d.setVisible(true);
	}

	/**
	 * Handler for 'Remove Task' clicked in the pop up menu
	 */
	public void removeTaskClicked() {
		Task task = selTask();
		if (task==null) return ;
		TreeComponent parent =  mouseLis.getSelLastLastCom();
		treeModel.removeObject(task);
		parent.getTaskList().removeFromList(task);
		postCmd(parent,true);
	}

	///////////////////////////////////////////////////////////////////
	//
	// Respond with Dialog
	//
	////////////////////////////////////////////////////////////////////

	/**
	 * Takes the fields provided in the Add Component Dialog and calls the
	 * Scheduling Tree to add the new Component
	 * 
	 * @param c
	 *            new SchedulingComponent 
	 */
	public void replyFromAddCompDialog(TreeComponent c) {
		TreeComponent root=schedulingTree.getRoot();		
		if(!root.checkNameRecursive(c.getCompName()) ){
			UserUtil.showErr("Given component name is already existed");
			return;
		}
		TreeComponent parent =    mouseLis.getSelLastCom();
		c.setParentComp(parent);
		//c.printDebug();
		schedulingTree.addComponent(c, parent);
		treeModel.addObject(parent, c);
		postCmd(parent,true);
	}

	/**
	 * Takes the fields provided in the Edit Component Dialog and calls the
	 * Scheduling Tree to update the selected component
	 * 
	 * @param c
	 *            updated SchedulingComponent
	 */
	public void replyFromEditCompDialog(TreeComponent c) {
		TreeComponent comp =  mouseLis.getSelLastCom();
		boolean isUpdated=comp.update(c);
		postCmd(comp,isUpdated);
	}

	/**
	 * Takes the fields provided in the Add Task Dialog and calls the Scheduling
	 * Tree to add the new task to the selected component
	 * 
	 * @param task
	 *            task
	 */
	public void replyFromAddTaskDialog(Task task) {
		TreeComponent comp =   mouseLis.getSelLastCom();
		//task.printDebug();
		schedulingTree.addTask(comp, task);
		treeModel.addObject(comp, task);
		postCmd(comp,true);
	}

	/**
	 * Takes the fields provided in the Edit Component Dialog and calls the
	 * Scheduling Tree to update the selected task
	 * 
	 * @param task
	 *            task
	 */
	public void replyFromEditTaskDialog(Task task) {
		TreeComponent comp =  mouseLis.getSelLastLastCom();
		Task selectedTask =   mouseLis.getSelLastTask();
		boolean isUpdated=selectedTask.update(task);
		postCmd(comp,isUpdated);
		
	}

	///////////////////////////////////////////////////////////////////
	//
	// Popup Menu
	//
	////////////////////////////////////////////////////////////////////

	/**
	 * Build and display the popup menu for Scheduling Component
	 * 
	 * @param comp
	 *            Component on which mouse has been clicked
	 */
	private void createPopupComponentMenu(TreeComponent comp) {
		if(popupComponent==null)
			popupComponent = new ComponentMenu(this);
		popupComponent.setCom(comp);
	}
	/**
	 * Build the popup menu for Task
	 * 
	 * @param task
	 *            Task on which mouse has been clicked
	 */
	protected void createPopupTaskMenu(Task task) {
		if(popupTask==null)
			popupTask = new TaskMenu(this);
	}


	public void showPopupComponent(Component sourceCom,int x, int y, TreeComponent comp) {
		createPopupComponentMenu(comp);
		popupComponent.show(sourceCom, x, y);
	}

	public void showPopupTask(Component sourceCom,int x, int y,  Task task) {
		createPopupTaskMenu(task);
		popupTask.show(sourceCom, x, y);
	}

	///////////////////////////////////////////////////////////////////
	//
	// Inner Interaction
	//
	////////////////////////////////////////////////////////////////////





}
