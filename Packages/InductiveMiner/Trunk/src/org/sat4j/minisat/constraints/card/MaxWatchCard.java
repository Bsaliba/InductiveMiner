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
package org.sat4j.minisat.constraints.card;

import java.io.Serializable;
import java.math.BigInteger;

import org.sat4j.minisat.constraints.cnf.Lits;
import org.sat4j.minisat.constraints.cnf.UnitClauses;
import org.sat4j.minisat.core.Constr;
import org.sat4j.minisat.core.ILits;
import org.sat4j.minisat.core.Propagatable;
import org.sat4j.minisat.core.Undoable;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.UnitPropagationListener;

public final class MaxWatchCard implements Propagatable, Constr, Undoable,
        Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Degr? de la contrainte de cardinalit?
     */
    private int degree;

    /**
     * Liste des litt?raux de la contrainte
     */
    private final int[] lits;

    /**
     * D?termine si c'est une in?galit? sup?rieure ou ?gale
     */
    private boolean moreThan;

    /**
     * Somme des coefficients des litt?raux observ?s
     */
    private int watchCumul;

    /**
     * Vocabulaire de la contrainte
     */
    private final ILits voc;

    /**
     * Constructeur de base cr?ant des contraintes vides
     * 
     * @param size
     *            nombre de litt?raux de la contrainte
     * @param learnt
     *            indique si la contrainte est apprise
     */
    public MaxWatchCard(ILits voc, IVecInt ps, boolean moreThan, int degree) {

        // On met en place les valeurs
        this.voc = voc;
        this.degree = degree;
        this.moreThan = moreThan;

        // On simplifie ps
        int[] index = new int[voc.nVars() * 2 + 2];
        for (int i = 0; i < index.length; i++) {
            index[i] = 0;
        }
        // On repertorie les litt?raux utiles
        for (int i = 0; i < ps.size(); i++) {
            if (index[ps.get(i) ^ 1] == 0) {
                index[ps.get(i)]++;
            } else {
                index[ps.get(i) ^ 1]--;
            }
        }
        // On supprime les litt?raux inutiles
        int ind = 0;
        while (ind < ps.size()) {
            if (index[ps.get(ind)] > 0) {
                index[ps.get(ind)]--;
                ind++;
            } else {
                if ((ps.get(ind) & 1) != 0) {
                    this.degree--;
                }
                ps.set(ind, ps.last());
                ps.pop();
            }
        }

        // On copie les litt?raux de la contrainte
        this.lits = new int[ps.size()];
        ps.moveTo(this.lits);

        // On normalise la contrainte au sens de Barth
        normalize();

        // Mise en place de l'observation maximale
        this.watchCumul = 0;

        // On observe les litt?raux non falsifi?
        for (int i = 0; i < this.lits.length; i++) {
            // Rappel: les ?l?ments falsifi?s ne seront jamais d?pil?s
            if (!voc.isFalsified(this.lits[i])) {
                this.watchCumul++;
                voc.watch(this.lits[i] ^ 1, this);
            }
        }
    }

    /**
     * Calcule la cause de l'affection d'un litt?ral
     * 
     * @param p
     *            un litt?ral falsifi? (ou Lit.UNDEFINED)
     * @param outReason
     *            vecteur de litt?raux ? remplir
     * @see Constr#calcReason(int p, IVecInt outReason)
     */
    public void calcReason(int p, IVecInt outReason) {
        for (int lit : this.lits) {
            if (this.voc.isFalsified(lit)) {
                outReason.push(lit ^ 1);
            }
        }
    }

    /**
     * Obtenir la valeur de l'activit? de la contrainte
     * 
     * @return la valeur de l'activit? de la contrainte
     * @see Constr#getActivity()
     */
    public double getActivity() {
        // TODO getActivity
        return 0;
    }

    /**
     * Incr?mente la valeur de l'activit? de la contrainte
     * 
     * @param claInc
     *            incr?ment de l'activit? de la contrainte
     * @see Constr#incActivity(double claInc)
     */
    public void incActivity(double claInc) {
        // TODO incActivity
    }

    public void setActivity(double d) {
    }

    /**
     * D?termine si la contrainte est apprise
     * 
     * @return true si la contrainte est apprise, false sinon
     * @see Constr#learnt()
     */
    public boolean learnt() {
        // TODO learnt
        return false;
    }

    /**
     * La contrainte est la cause d'une propagation unitaire
     * 
     * @return true si c'est le cas, false sinon
     * @see Constr#locked()
     */
    public boolean locked() {
        // TODO locked
        return true;
    }

    /**
     * Permet la cr?ation de contrainte de cardinalit? ? observation minimale
     * 
     * @param s
     *            outil pour la propagation des litt?raux
     * @param voc
     *            vocabulaire utilis? par la contrainte
     * @param ps
     *            liste des litt?raux de la nouvelle contrainte
     * @param moreThan
     *            d?termine si c'est une sup?rieure ou ?gal ? l'origine
     * @param degree
     *            fournit le degr? de la contrainte
     * @return une nouvelle clause si tout va bien, null sinon
     * @throws ContradictionException
     */
    public static Constr maxWatchCardNew(UnitPropagationListener s, ILits voc,
            IVecInt ps, boolean moreThan, int degree)
            throws ContradictionException {

        MaxWatchCard outclause = null;

        // La contrainte ne doit pas ?tre vide
        if (ps.size() < degree) {
            throw new ContradictionException(
                    "Creating trivially inconsistent constraint"); //$NON-NLS-1$
        } else if (ps.size() == degree) {
            for (int i = 0; i < ps.size(); i++) {
                if (!s.enqueue(ps.get(i))) {
                    throw new ContradictionException(
                            "Contradiction with implied literal"); //$NON-NLS-1$
                }
            }
            return new UnitClauses(ps);
        }

        // On cree la contrainte
        outclause = new MaxWatchCard(voc, ps, moreThan, degree);

        // Si le degr? est insufisant
        if (outclause.degree <= 0) {
            return null;
        }

        // Si il n'y a aucune chance de satisfaire la contrainte
        if (outclause.watchCumul < outclause.degree) {
            throw new ContradictionException();
        }

        // Si les litt?raux observ?s sont impliqu?s
        if (outclause.watchCumul == outclause.degree) {
            for (int i = 0; i < outclause.lits.length; i++) {
                if (!s.enqueue(outclause.lits[i])) {
                    throw new ContradictionException(
                            "Contradiction with implied literal"); //$NON-NLS-1$
                }
            }
            return null;
        }

        return outclause;
    }

    /**
     * On normalise la contrainte au sens de Barth
     */
    public void normalize() {
        // Gestion du signe
        if (!this.moreThan) {
            // On multiplie le degr? par -1
            this.degree = 0 - this.degree;
            // On r?vise chaque litt?ral
            for (int indLit = 0; indLit < this.lits.length; indLit++) {
                this.lits[indLit] = this.lits[indLit] ^ 1;
                this.degree++;
            }
            this.moreThan = true;
        }
    }

    /**
     * Propagation de la valeur de v?rit? d'un litt?ral falsifi?
     * 
     * @param s
     *            objet utilis? pour la propagation
     * @param p
     *            le litt?ral propag? (il doit etre falsifie)
     * @return false ssi une inconsistance est d?t?ct?e
     */
    public boolean propagate(UnitPropagationListener s, int p) {

        // On observe toujours tous les litt?raux
        this.voc.watch(p, this);
        assert !this.voc.isFalsified(p);

        // Si le litt?ral p est impliqu?
        if (this.watchCumul == this.degree) {
            return false;
        }

        // On met en place la mise ? jour du compteur
        this.voc.undos(p).push(this);
        this.watchCumul--;

        // Si les litt?raux restant sont impliqu?s
        if (this.watchCumul == this.degree) {
            for (int q : this.lits) {
                if (this.voc.isUnassigned(q) && !s.enqueue(q, this)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @since 2.1
     */
    public void remove(UnitPropagationListener upl) {
        for (int q : this.lits) {
            this.voc.watches(q ^ 1).remove(this);
        }
    }

    /**
     * Permet le r??chantillonage de l'activit? de la contrainte
     * 
     * @param d
     *            facteur d'ajustement
     */
    public void rescaleBy(double d) {
    }

    /**
     * Simplifie la contrainte(l'all?ge)
     * 
     * @return true si la contrainte est satisfaite, false sinon
     */
    public boolean simplify() {

        int i = 0;

        // On esp?re le maximum de la somme
        int curr = this.watchCumul;

        // Pour chaque litt?ral
        while (i < this.lits.length) {
            // On d?cr?mente si l'espoir n'est pas fond?
            if (this.voc.isUnassigned(this.lits[i++])) {
                curr--;
                if (curr < this.degree) {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Cha?ne repr?sentant la contrainte
     * 
     * @return Cha?ne repr?sentant la contrainte
     */
    @Override
    public String toString() {
        StringBuffer stb = new StringBuffer();

        if (this.lits.length > 0) {
            if (this.voc.isUnassigned(this.lits[0])) {
                stb.append(Lits.toString(this.lits[0]));
                stb.append(" "); //$NON-NLS-1$
            }
            for (int i = 1; i < this.lits.length; i++) {
                if (this.voc.isUnassigned(this.lits[i])) {
                    stb.append(" + "); //$NON-NLS-1$
                    stb.append(Lits.toString(this.lits[i]));
                    stb.append(" "); //$NON-NLS-1$
                }
            }
            stb.append(">= "); //$NON-NLS-1$
            stb.append(this.degree);
        }
        return stb.toString();
    }

    /**
     * M?thode appel?e lors du backtrack
     * 
     * @param p
     *            le litt?ral d?saffect?
     */
    public void undo(int p) {
        this.watchCumul++;
    }

    public void setLearnt() {
        throw new UnsupportedOperationException();
    }

    public void register() {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return this.lits.length;
    }

    public int get(int i) {
        return this.lits[i];
    }

    public void assertConstraint(UnitPropagationListener s) {
        throw new UnsupportedOperationException();
    }

    public void assertConstraintIfNeeded(UnitPropagationListener s) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.minisat.constraints.pb.PBConstr#getCoefficient(int)
     */
    public BigInteger getCoef(int literal) {
        return BigInteger.ONE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.sat4j.minisat.constraints.pb.PBConstr#getDegree()
     */
    public BigInteger getDegree() {
        return BigInteger.valueOf(this.degree);
    }

    public ILits getVocabulary() {
        return this.voc;
    }

    /**
     * @since 2.1
     */
    public void forwardActivity(double claInc) {
        // TODO Auto-generated method stub

    }

    public boolean canBePropagatedMultipleTimes() {
        return true;
    }

    public Constr toConstraint() {
        return this;
    }

    public void calcReasonOnTheFly(int p, IVecInt trail, IVecInt outReason) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}
