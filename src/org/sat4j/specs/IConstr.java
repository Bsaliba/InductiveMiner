/*******************************************************************************
 * SAT4J: a SATisfiability library for Java Copyright (C) 2004, 2012 Artois University and CNRS
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU Lesser General Public License Version 2.1 or later (the
 * "LGPL"), in which case the provisions of the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL, and not to allow others to use your version of
 * this file under the terms of the EPL, indicate your decision by deleting
 * the provisions above and replace them with the notice and other provisions
 * required by the LGPL. If you do not delete the provisions above, a recipient
 * may use your version of this file under the terms of the EPL or the LGPL.
 *
 * Based on the original MiniSat specification from:
 *
 * An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
 * Sixth International Conference on Theory and Applications of Satisfiability
 * Testing, LNCS 2919, pp 502-518, 2003.
 *
 * See www.minisat.se for the original solver in C++.
 *
 * Contributors:
 *   CRIL - initial API and implementation
 *******************************************************************************/
package org.sat4j.specs;

/**
 * The most general abstraction for handling a constraint.
 * 
 * @author leberre
 * 
 */
public interface IConstr {

    /**
     * @return true iff the clause was learnt during the search
     */
    boolean learnt();

    /**
     * @return the number of literals in the constraint.
     */
    int size();

    /**
     * returns the ith literal in the constraint
     * 
     * @param i
     *            the index of the literal
     * @return a literal
     */
    int get(int i);

    /**
     * To obtain the activity of the constraint.
     * 
     * @return the activity of the clause.
     * @since 2.1
     */
    double getActivity();

    /**
     * Partition constraints into the ones that can only be found once on the
     * trail (e.g. clauses) and the ones that can be found several times (e.g.
     * cardinality constraints and pseudo-boolean constraints).
     * 
     * @return true if the constraint can be used several times as a reason to
     *         propagate a literal.
     * @since 2.3.1
     */
    boolean canBePropagatedMultipleTimes();
}
