package br.ufpr.inf.gres.pitestincode.integration.pit;

import java.util.Collection;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.MutationResultListener;

/**
 *
 * @author Giovani Guizzo
 */
public class MutantResultListenerImpl implements MutationResultListener {

    private ClassMutationResults results;

    @Override
    public void runStart() {
    }

    @Override
    public void handleMutationResult(ClassMutationResults results) {
        this.results = results;
    }

    @Override
    public void runEnd() {
    }

    public Collection<MutationResult> getResults() {
        return results.getMutations();
    }

}
