package bPrime.batch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.pdf.PDFGraphics2D;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.log.OpenLogFilePlugin;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGen;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;

import bPrime.ProcessTreeModelParameters;
import bPrime.ThreadPool;
import bPrime.mining.MiningPlugin;
import bPrime.model.ProcessTreeModel;
import bPrime.model.conversion.ProcessTreeModel2PetriNet.WorkflowNet;

@Plugin(name = "Batch mine Process Trees using B'", returnLabels = { "Process Trees" }, returnTypes = { ProcessTrees.class }, parameterLabels = {
		"Log", "Parameters" }, userAccessible = true)
public class BatchMiningPlugin {
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "S.J.J. Leemans", email = "s.j.j.leemans@tue.nl")
	@PluginVariant(variantLabel = "Mine Process Trees, default", requiredParameterLabels = { })
	public ProcessTrees mineDefault(PluginContext context) {
		BatchParameters parameters = new BatchParameters();
		
		//ask the user for the folder to be batch processed
		boolean repeat = true;
		while (repeat) {
			String folder = (String) JOptionPane.showInputDialog(null,
					"What is the folder to batch process?",
					"Provide Folder",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					parameters.getFolder());
			if (folder == null) {
				return null;
			} else {
				File x = new File(folder);
				if (x.exists() && x.isDirectory()) {
					repeat = false;
					parameters.setFolder(folder);
				}
			}
		}
		
		return this.mineParameters(context, parameters);
	}
	
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "S.J.J. Leemans", email = "s.j.j.leemans@tue.nl")
	@PluginVariant(variantLabel = "Mine Process Trees, parameterized", requiredParameterLabels = { 1 })
	public ProcessTrees mineParameters(final PluginContext context, final BatchParameters parameters) {
		
		//initialise for thread splitting
		ThreadPool pool = new ThreadPool(parameters.getNumberOfConcurrentFiles());
		File folder = new File(parameters.getFolder());
		List<String> files = getListOfFiles(folder, parameters.getExtensions());
		final ProcessTrees result = new ProcessTrees();
		final MiningPlugin miningPlugin = new MiningPlugin();
		final PNLogReplayer replayer = new PNLogReplayer();
		final boolean measurePrecision = parameters.getMeasurePrecision();
		
		for (String file2 : files) {
		
			final String file = file2;
			final int index = result.add();
			
			pool.addJob(
					new Runnable() {
			            public void run() {
			            	runJob(result, 
			            			index, 
			            			context, 
			            			file, 
			            			miningPlugin, 
			            			replayer, 
			            			measurePrecision,
			            			parameters);
			            }
					}
				);
		}
		
		try {
			pool.join();
		} catch (ExecutionException e) {
			//debug("something failed (thread join)");
			e.printStackTrace();
			return null;
		}
		
		//write the result to an HTML file
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter( new FileWriter( "D:\\output\\index.html"));
			writer.write(result.toHTMLString(true));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
		//debug("finished batch");
    	return result;
	}
	
	private void runJob(ProcessTrees result, 
			int index,
			PluginContext context,
			String fileName,
			MiningPlugin miningPlugin,
			PNLogReplayer replayer,
			boolean measurePrecision,
			BatchParameters batchParameters) {
		//perform the computations, store the result in result[index]
		
		//import the log
		//debug(fileName);
		XLog log;
		try {
			OpenLogFilePlugin logImporter = new OpenLogFilePlugin();
			log = (XLog) logImporter.importFile(context, fileName);
		} catch (Exception e) {
			//debug("error encountered (log import)");
			e.printStackTrace();
			return;
		}
		
		//mine the Petri net
		ProcessTreeModelParameters mineParameters = new ProcessTreeModelParameters();
		Object[] arr = miningPlugin.mineParametersPetrinetWithoutConnections(context, log, mineParameters);
		ProcessTreeModel model = (ProcessTreeModel) arr[0];
		WorkflowNet workflowNet = (WorkflowNet) arr[1];
		Petrinet petrinet = workflowNet.petrinet;
		Marking initialMarking = workflowNet.initialMarking;
		Marking finalMarking = workflowNet.finalMarking;
		TransEvClassMapping mapping = (TransEvClassMapping) arr[2];
		XEventClass dummy = mapping.getDummyEventClass();
    	
		//Visualise the Petri net
		File outputFilePDF;
		File outputFilePNG;
		if (batchParameters.getPetrinetOutputFolder() != null) {
			String x = new File(fileName).getName();
			if (x.indexOf(".") > 0) {
			    x = x.substring(0, x.lastIndexOf("."));
			}
			outputFilePDF = new File(batchParameters.getPetrinetOutputFolder(), x + ".pdf");
			outputFilePNG = new File(batchParameters.getPetrinetOutputFolder(), x + ".png");
		} else {
			outputFilePDF = new File(fileName + ".pdf");
			outputFilePNG = new File(fileName + ".png");
		}
		Dimension dimension = batchParameters.getPetrinetOutputDimension();
		ProMJGraphPanel graphPanel = ProMJGraphVisualizer.instance().visualizeGraphWithoutRememberingLayout(petrinet);
		graphPanel.setSize(dimension);
		
		ProMJGraph graph = (ProMJGraph) graphPanel.getComponent();
		graph.setSize(dimension);
		
		//output pdf
		try {
			VectorGraphics g = new PDFGraphics2D(outputFilePDF, dimension);
			g.setProperties(new Properties());
			g.startExport();
			graph.print(g);
			g.endExport();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//output png
		Color bg = null;
		bg = graph.getBackground();
		int inset = 2;
		BufferedImage img = graph.getImage(bg, inset);
		try {
			ImageIO.write(img, "png", outputFilePNG);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		img.flush();
		
		/*PetriNetVisualization visualisation = new PetriNetVisualization(); 
		JComponent visualisationComponent = visualisation.visualize(context, petrinet);
		visualisationComponent.doLayout();
		debug(String.valueOf(visualisationComponent.getHeight()));
		Graphics visualisationGraphics = visualisationComponent.getGraphics();
		debug(visualisationGraphics.toString());
		Image visualisedPetrinet = visualisationComponent.createImage(1000, 1000);
		
		try {
			ImageIO.write((RenderedImage) visualisedPetrinet, "png", imageFile);
		} catch (Exception e1) {
			debug("Image generation failed.");
			e1.printStackTrace();
		}*/
		
		String comment = "";
		if (measurePrecision) {
		
	    	//replay the log
			XLogInfo info = XLogInfoFactory.createLogInfo(log, mineParameters.getClassifier());
			Collection<XEventClass> activities = info.getEventClasses().getClasses();
			
			PetrinetReplayerWithILP algorithm = new PetrinetReplayerWithILP();
			CostBasedCompleteParam replayParameters = new CostBasedCompleteParam(activities, dummy, petrinet.getTransitions(), 1, 1);
			replayParameters.setInitialMarking(initialMarking);
			replayParameters.setFinalMarkings(new Marking[] {finalMarking});
			replayParameters.setCreateConn(false);
			replayParameters.setGUIMode(false);
			//replayParameters.setUseLogWeight(false);
			//Map<XEventClass, Integer> weightMap = new HashMap<XEventClass, Integer>();
			//weightMap.put(dummy, 0);
			//for (XEventClass activity : activities) {
			//	weightMap.put(activity, 1);
			//}
			//replayParameters.setxEventClassWeightMap(weightMap);
			PNRepResult replayed = null;
			try {
				replayed = replayer.replayLog(context, petrinet, log, mapping, algorithm, replayParameters);
			} catch (Exception e) {
				//debug("error encountered (replay algorithm)");
				e.printStackTrace();
				return;
			}
	    	
	    	//measure precision/generalisation
	    	AlignmentPrecGen precisionMeasurer = new AlignmentPrecGen();
	    	AlignmentPrecGenRes precisionGeneralisation = precisionMeasurer.measureConformanceAssumingCorrectAlignment(context, mapping, replayed, petrinet, initialMarking, true);
	    	
	    	comment = "<br>precision " + precisionGeneralisation.getPrecision() +
	    			"<br>generalisation " + precisionGeneralisation.getGeneralization();
		}
		
		comment = "mined process tree " + model.toHTMLString(false) + comment + "<br><img src='"+outputFilePNG.getName()+"'>";
    	
    	result.set(index, fileName, comment);
	}
	
	private List<String> getListOfFiles(File file, Set<String> extensions) {
		List<String> result = new LinkedList<String>();
		if (file.isFile()) {
			String name = file.getName();
			if (extensions.contains(name.substring(name.length()-4, name.length()))) {
            	result.add(file.toString());
            }
		} else if (file.isDirectory()) {
			File[] listOfFiles = file.listFiles();
			if (listOfFiles != null) {
				for (int i = 0; i < listOfFiles.length; i++) {
					result.addAll(getListOfFiles(listOfFiles[i], extensions));
				}
			}
		}
		return result;
    }
	
	private void debug(String s) {
		System.out.println(s);
	}
}
