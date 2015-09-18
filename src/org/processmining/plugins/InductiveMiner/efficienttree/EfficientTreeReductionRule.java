package org.processmining.plugins.InductiveMiner.efficienttree;




public interface EfficientTreeReductionRule {
	/**
	 * Apply the reduction rule on tree, on the node at position i.
	 * @param tree
	 * @param i
	 * @return the tree, changed or not
	 */
	public boolean apply(EfficientTree tree, int node);
}
