package br.ufpr.inf.gres.pitestincode.base.mutation;

import org.apache.commons.collections4.list.SetUniqueList;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * @author Giovani Guizzo
 */
public class Mutant extends Program {

    protected Program originalProgram;
    protected boolean equivalent = false;
    protected SetUniqueList<Operator> operators;
    protected SetUniqueList<Mutant> constituentMutants;
    protected SetUniqueList<TestCase> killingTestCases;

    public Mutant(String name, File sourceFile, Program originalProgram) {
        super(name, sourceFile);
        this.originalProgram = originalProgram;
        this.constituentMutants = SetUniqueList.setUniqueList(new ArrayList<>());
        this.killingTestCases = SetUniqueList.setUniqueList(new ArrayList<>());
        this.operators = SetUniqueList.setUniqueList(new ArrayList<>());
    }

    public Mutant(Mutant mutant) {
        this(mutant.fullName, mutant.sourceFile, mutant.originalProgram);
        this.equivalent = mutant.equivalent;
        this.constituentMutants.addAll(mutant.constituentMutants);
        this.killingTestCases.addAll(mutant.killingTestCases);
        this.operators.addAll(mutant.operators);
    }

    public Program getOriginalProgram() {
        return originalProgram;
    }

    public void setOriginalProgram(Program originalProgram) {
        this.originalProgram = originalProgram;
    }

    public SetUniqueList<Operator> getOperators() {
        return operators;
    }

    public void setOperators(SetUniqueList<Operator> operators) {
        this.operators = operators;
    }

    public boolean isEquivalent() {
        return equivalent;
    }

    public void setEquivalent(boolean equivalent) {
        this.equivalent = equivalent;
    }

    public SetUniqueList<Mutant> getConstituentMutants() {
        return constituentMutants;
    }

    public void setConstituentMutants(SetUniqueList<Mutant> constituentMutants) {
        this.constituentMutants = constituentMutants;
    }

    public SetUniqueList<TestCase> getKillingTestCases() {
        return killingTestCases;
    }

    public void setKillingTestCases(SetUniqueList<TestCase> killingTestCases) {
        this.killingTestCases = killingTestCases;
    }

    public boolean isAlive() {
        return killingTestCases.isEmpty();
    }

    public int getOrder() {
        int order = getConstituentMutants().size();
        return order == 0 ? 1 : order;
    }

    public boolean isHigherOrder() {
        return getOrder() > 1;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.originalProgram);
        hash = 11 * hash + Objects.hashCode(this.fullName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Mutant other = (Mutant) obj;
        return Objects.equals(this.originalProgram, other.originalProgram) && Objects.equals(this.fullName, other.fullName);
    }

    @Override
    public String toString() {
        return fullName;
    }

    public boolean isDead() {
        return !isAlive();
    }
}
