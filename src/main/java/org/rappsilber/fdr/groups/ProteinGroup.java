/*
 * Copyright 2015 Lutz Fischer <lfischer at staffmail.ed.ac.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rappsilber.fdr.groups;

import org.rappsilber.fdr.entities.Protein;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.rappsilber.fdr.entities.Peptide;
import org.rappsilber.fdr.entities.PeptidePair;
import org.rappsilber.fdr.entities.ProteinGroupLink;
import org.rappsilber.fdr.utils.AbstractFDRElement;

/**
 *
 * @author lfischer
 */
public class ProteinGroup extends AbstractFDRElement<ProteinGroup> implements  Iterable<Protein> { //,Comparable<ProteinGroup> {

    public static int PROTEINGROUPCOUNT = 0;
    protected int ProteinGroupID = PROTEINGROUPCOUNT++;
    HashSet<Protein> groupproteins = new HashSet<Protein>(1);
    HashSet<PeptidePair> peppairs = new HashSet<PeptidePair>();
    private double score = Double.POSITIVE_INFINITY;
    private boolean hasInternalSupport = false;
    private boolean hasBetweenSupport = false;
    private boolean hasLinearSupport = false;
    int hashcode = 5;
    public double m_fdr = -1;
    public static final ProteinGroup NOPROTEINGROUP = Peptide.NOPEPTIDE.getProteinGroup();
    private static final int NOFDRGROUPDEFINED = -9999;
    private volatile int fdrgroup = NOFDRGROUPDEFINED;
    private boolean fdrgroupSet = false;
    private boolean isDecoy = true;

//    static {
//        NOPROTEINGROUP = Peptide.NOPEPTIDE.getProteinGroup();
////        ArrayList<Protein> p = new ArrayList<Protein>();
////        p.add(Protein.NOPROTEIN);
////        NOPROTEINGROUP = new ProteinGroup(p, new PeptidePair(new PSM("", Peptide.NOPEPTIDE, Peptide.NOPEPTIDE, 1, -1, false, false, 0, 0, 0)));
//    }

    public ProteinGroup(Collection<Protein> proteins, final PeptidePair pep) {
        this(proteins, new ArrayList<PeptidePair>() {
            {
                add(pep);
            }
        });
    }

    public ProteinGroup(Collection<Protein> proteins, Collection<PeptidePair> peps, boolean isDecoy) {
        this(proteins, peps);
        this.isDecoy = isDecoy;
    }
    
    public ProteinGroup(Collection<Protein> proteins, Collection<PeptidePair> peps) {
        for (Protein p : proteins) {
            groupproteins.add(p);
            hashcode+= p.hashCode();
            this.isDecoy &= p.isDecoy();
        }
        for (PeptidePair pp : peps) {
            addPeptidePair(pp);
        }
    }

    public Collection<Protein> getProteins() {
        return groupproteins;
    }

    public void addProtein(Protein p) {
        groupproteins.add(p);
        //score = Math.max(score, p.getScore());
        hashcode += p.hashCode();
        peppairs.addAll(p.getPeptidePairs());
        hasInternalSupport |= p.hasInternalSupport();
        hasLinearSupport |= p.hasInternalSupport();
        hasBetweenSupport |= p.hasBetweenSupport();
        isDecoy &= p.isDecoy();
        //setFDRGroup();
    }

    
    private void setSupport() {
        for (PeptidePair pp : peppairs) {
            hasInternalSupport |= pp.isInternal();
            hasLinearSupport |= pp.isLinear();
            hasBetweenSupport |= pp.isBetween();
            isDecoy &= pp.isDecoy();
        }
    }
    

    private void setFDRGroup() {
//        boolean internalSupport = false;
//        boolean linearSupport = false;
//        boolean betweenSupport = false;
//        int fdrgroup;
//        for (PeptidePair pp : peppairs) {
//            internalSupport |= pp.isInternal();
//            linearSupport |= pp.isLinear();
//            betweenSupport |= pp.isBetween();
//            isDecoy &= pp.isDecoy();
//        }
//
        fdrgroup = (hasInternalSupport ? 2:0) + (hasLinearSupport ?1 :0);
        if (fdrgroup == 0 && hasBetweenSupport)
            fdrgroup = 4;

        //this.fdrgroup=fdrgroup;
        fdrgroupSet = true;
    }

    public double getScore() {
        if (score != Double.POSITIVE_INFINITY) {
            return score;
        }
        for (Protein p : groupproteins) {
            if (score > p.getScore()) {
                score = p.getScore();
            }
        }
        return score;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public boolean equals(Object pg) {
        HashSet<Protein> cproteinpairs = (HashSet<Protein>) groupproteins.clone();
        if (pg instanceof ProteinGroup) {
            // if it is actaually the same object
            if (pg == this) {
                return true; // no need to do any comparison
                // no need to do any comparison
                // no need to do any comparison
            }
            // if both groups don't contain the same numbers of links
            // then they are not the same
            if (((ProteinGroup) pg).groupproteins.size() != groupproteins.size()) {
                return false;
            }
            // if one contains a protein, the other does not, then it's not the same group
            for (Protein pc : ((ProteinGroup) pg).groupproteins) {
                if (!cproteinpairs.contains(pc)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean hasOverlap(ProteinGroup pg) {
        for (Protein pc : pg.groupproteins) {
            if (groupproteins.contains(pc)) {
                return true;
            }
            if (groupproteins.contains(pc.decoyComplement())) {
                return true;
            }
        }
        return false;
    }

    public  ProteinGroup decoyComplement() {
        ArrayList<Protein> dcProts = new ArrayList<Protein>(this.size());
        for (Protein pc : this) {
            dcProts.add(pc.decoyComplement());
        }
        
        return new ProteinGroup(dcProts, peppairs);
    }
    
    
    /**
     * If the ProteinPairs within this group have different scores, than this
     * function returns a list of ProteinPairGroups that contain the
     * ProteinPairs for higher scores. It gets the scores for all ProteinPairs
     * and for each seen score that is larger then the smallest score, it
     * returns a ProteinPairGroup containing all ProteinPairs with this or
     * larger score. The smallest score is not returned, as this would be a copy
     * of the current ProteinPairGroup
     *
     * @return
     */
    public ArrayList<ProteinGroup> getSubGroups() {
        ArrayList<ProteinGroup> ret = new ArrayList<ProteinGroup>();
        // first get the scores
        HashSet<Double> scores = new HashSet<Double>();
        for (Protein pp : groupproteins) {
            if (score > pp.getScore()) {
                score = pp.getScore();
            }
            scores.add(pp.getScore());
        }
        // the lowest score is ignored, as that would be the current group
        scores.remove(score);
        for (Double s : scores) {
            ArrayList<Protein> al = new ArrayList<Protein>();
            for (Protein pp : groupproteins) {
                if (pp.getScore() >= s) {
                    al.add(pp);
                }
            }
            ret.add(new ProteinGroup(al, peppairs));
        }
        return ret;
    }

    public int compareTo(ProteinGroup o) {
        return Double.compare(o.getScore(), getScore());
    }

//    public void add(ProteinPairGroup o) {
//    }
//    public int compareTo(ProteinGroup o) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
    public void add(ProteinGroup o) {
        //throw new UnsupportedOperationException("Not supported yet.");
        if (o == this) {
            return;
        }
        //this.score = Math.max(score ,o.getScore());
        this.groupproteins.addAll(o.groupproteins);
        peppairs.addAll(o.peppairs);
        hasInternalSupport |= o.hasInternalSupport();
        hasLinearSupport |= o.hasLinearSupport();
        hasBetweenSupport |= o.hasBetweenSupport();
        isDecoy &= o.isDecoy();
        
    }

    public int getFDRGroup() {
        if (!fdrgroupSet)
            setFDRGroup();
        return fdrgroup;
    }

    public boolean isTT() {
//        if (!fdrgroupSet)
//            setFDRGroup();
        return !isDecoy;
    }

    public boolean isTD() {
//        if (!fdrgroupSet)
//            setFDRGroup();
        return isDecoy;
    }

    public boolean isDD() {
        return false;
    }

    public Collection<PeptidePair> getPeptidePairs() {
        HashSet<PeptidePair> ret = new HashSet<PeptidePair>();
        for (Protein p : getProteins()) {
            ret.addAll(p.getPeptidePairs());
        }
        return ret;
    }

    public Collection<ProteinGroupLink> getLinks() {
        HashSet<ProteinGroupLink> ret = new HashSet<ProteinGroupLink>();
        for (PeptidePair pp : getPeptidePairs()) {
            ret.addAll(pp.getLinks());
        }
        return ret;
    }

    public Collection<Peptide> getPeptides() {
        HashSet<Peptide> ret = new HashSet<Peptide>();
        for (Protein p : getProteins()) {
            ret.addAll(p.getPeptides());
        }
        return ret;
    }

    public void addPeptidePair(PeptidePair pp) {
        for (Protein p : groupproteins) {
            double s = pp.getProteinScore(p);
            p.add(pp, s);
        }
        peppairs.add(pp);
        hasInternalSupport |= pp.isInternal();
        hasLinearSupport |= pp.isLinear();
        hasBetweenSupport |= pp.isBetween();
        isDecoy &= pp.isDecoy();

    }

    public String acessions() {
        StringBuffer sb = new StringBuffer();

        if (isDecoy()) {
            sb.append("DECOY:");
        }

        for (Protein p : groupproteins) {
            sb.append(p.getAccession());
            sb.append(";");
        }
        return sb.substring(0,sb.length() - 1);
    }

    
    public String descriptions() {
        StringBuffer sb = new StringBuffer();
        for (Protein p : groupproteins) {
            sb.append(";");
            sb.append(p.getDescription());
        }
        return sb.substring(1);
    }

    public String ids() {
        StringBuffer sb = new StringBuffer();
        for (Protein p : groupproteins) {
            sb.append(";");
            sb.append(p.getId());
        }
        return sb.substring(1);
    }

    public boolean isSubgroupOff(ProteinGroup pg) {
        for (Protein p : groupproteins) {
            if (!pg.containsProtein(p)) {
                return false;
            }
        }
        return true;

    }

    public boolean containsProtein(Protein p) {
        return groupproteins.contains(p);
    }

    public String toString() {
        if (isTD()) {
            return "D - " + acessions();
        }
        return "T - " + acessions();
    }

    public boolean isDecoy() {
//        if (!fdrgroupSet)
//            setFDRGroup();
        return isDecoy;
    }

    public String getFDRGroupName() {
        return Protein.getFDRGroupName(fdrgroup);
    }

    public static String getFDRGroupName(int fdrGroup) {
        return Protein.getFDRGroupName(fdrGroup);
    }

    public int proteinCount() {
        return groupproteins.size();
    }

    public void setFDR(double fdr) {
        m_fdr = fdr;
        for (Protein p : getProteins()) {
            if (Double.isNaN(p.getFDR()) || p.getFDR() >= fdr) {
                p.setFDR(fdr);
            }
        }

        for (PeptidePair pp : peppairs) {
            pp.setFdrProteinGroup(this);
        }
    }

    public double getFDR() {
        return m_fdr;
    }

    public int getPeptidePairCount() {
        return peppairs.size();
    }

    public int size() {
        return groupproteins.size();
    }

    public ProteinGroup getNonDecoyGroup() {
        if (!isDecoy()) {
            return this;
        }

        ProteinGroup ret = new ProteinGroup(new ArrayList<Protein>(), peppairs);
        for (Protein p : groupproteins) {
            if (p.isDecoy()) {
                Protein p2 = new Protein(p.getId(), p.getAccession(), p.getDescription(), false, p.hasLinearSupport(), p.hasInternalSupport(), true);
                ret.addProtein(p2);
            } else {
                ret.addProtein(p);
            }
        }
        return ret;
    }

    public Iterator<Protein> iterator() {
        return groupproteins.iterator();
    }

    /**
     * @return the internalSupport
     */
    public boolean hasInternalSupport() {
        return hasInternalSupport;
    }

    /**
     * @param internalSupport the internalSupport to set
     */
    public void setInternalSupport(boolean hasInternalSupport) {
        this.hasInternalSupport = hasInternalSupport;
    }

    /**
     * @return the linearSupport
     */
    public boolean hasLinearSupport() {
        return hasLinearSupport;
    }

    /**
     * @param linearSupport the linearSupport to set
     */
    public void setLinearSupport(boolean hasLinearSupport) {
        this.hasLinearSupport = hasLinearSupport;
    }

    /**
     * @return the betweenSupport
     */
    public boolean hasBetweenSupport() {
        return hasBetweenSupport;
    }

    /**
     * @param betweenSupport the betweenSupport to set
     */
    public void setBetweenSupport(boolean hasBetweenSupport) {
        this.hasBetweenSupport = hasBetweenSupport;
    }


    public Object getSite(int n) {
        return this;
    }

    public int getSites() {
        return 1;
    }    

    public boolean isLinear() {
        return true;
    }

    public boolean isInternal() {
        return false;
    }

    public boolean isBetween() {
        return false;
    }
    
}