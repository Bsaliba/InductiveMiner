package org.processmining.plugins.InductiveMiner.mining.cuts.ExhaustiveKSuccessor;

import org.processmining.plugins.InductiveMiner.mining.IMLog;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.cuts.Cut;
import org.processmining.plugins.InductiveMiner.mining.cuts.CutFinder;
import org.processmining.plugins.InductiveMiner.mining.cuts.ExhaustiveKSuccessor.Exhaustive.Result;

public class CutFinderEKS implements CutFinder {

	public Cut findCut(IMLog log, IMLogInfo logInfo, MiningParameters parameters) {
		
		UpToKSuccessorMatrix kSuccessor = UpToKSuccessor.fromLog(log);
		Exhaustive exhaustive = new Exhaustive(log, logInfo, kSuccessor, parameters);
		Result r = exhaustive.tryAll();
		
		return r.cut;
	}
	
}
