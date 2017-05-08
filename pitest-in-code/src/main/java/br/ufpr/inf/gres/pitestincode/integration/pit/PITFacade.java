/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufpr.inf.gres.pitestincode.integration.pit;

import br.ufpr.inf.gres.pitestincode.integration.IntegrationFacade;
import br.ufpr.inf.gres.pitestincode.base.mutation.Mutant;
import br.ufpr.inf.gres.pitestincode.base.mutation.Operator;
import br.ufpr.inf.gres.pitestincode.base.mutation.Program;
import br.ufpr.inf.gres.pitestincode.base.mutation.TestCase;
import com.google.common.base.CharMatcher;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pitest.classpath.ClassPath;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.build.MutationAnalysisUnit;
import org.pitest.mutationtest.build.MutationTestUnit;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.config.SettingsFactory;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator;
import org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator;
import org.pitest.mutationtest.engine.gregor.mutators.ConstructorCallMutator;
import org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.InlineConstantMutator;
import org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.MathMutator;
import org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.NonVoidMethodCallMutator;
import org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator;
import org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator;
import org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator;
import org.pitest.testapi.TestGroupConfig;
import org.pitest.util.Glob;

/**
 *
 * @author Giovani Guizzo
 */
public class PITFacade extends IntegrationFacade {

    private static final ArrayList<Operator> ALL_OPERATORS = new ArrayList<>();
    private static final HashMap<String, Operator> ALL_OPERATORS_BY_CLASS = new HashMap<>();

    private final HashMap<Program, MutationTestUnit> mutationUnits = new HashMap<>();
    private final HashMap<Program, HashMap<Mutant, MutationDetails>> generatedMutants = new HashMap<>();
    private final HashMap<Program, EntryPointImpl> entryPoints = new HashMap<>();

    private final String trainingDirectory;

    static {
        Operator operator = new Operator("CONDITIONALS_BOUNDARY", "Conditionals");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(ConditionalsBoundaryMutator.CONDITIONALS_BOUNDARY_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("NEGATE_CONDITIONALS", "Conditionals");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(NegateConditionalsMutator.NEGATE_CONDITIONALS_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("REMOVE_CONDITIONALS_EQ_IF", "Conditionals");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new RemoveConditionalMutator(RemoveConditionalMutator.Choice.EQUAL, true).getGloballyUniqueId(), operator);

        operator = new Operator("REMOVE_CONDITIONALS_EQ_ELSE", "Conditionals");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new RemoveConditionalMutator(RemoveConditionalMutator.Choice.EQUAL, false).getGloballyUniqueId(), operator);

        operator = new Operator("REMOVE_CONDITIONALS_ORD_IF", "Conditionals");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new RemoveConditionalMutator(RemoveConditionalMutator.Choice.ORDER, true).getGloballyUniqueId(), operator);

        operator = new Operator("REMOVE_CONDITIONALS_ORD_ELSE", "Conditionals");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new RemoveConditionalMutator(RemoveConditionalMutator.Choice.ORDER, false).getGloballyUniqueId(), operator);

        operator = new Operator("MATH", "Variables");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(MathMutator.MATH_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("INCREMENTS", "Variables");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(IncrementsMutator.INCREMENTS_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("INVERT_NEGS", "Variables");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(InvertNegsMutator.INVERT_NEGS_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("INLINE_CONSTS", "Variables");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new InlineConstantMutator().getGloballyUniqueId(), operator);

        operator = new Operator("EXPERIMENTAL_MEMBER_VARIABLE", "Variables");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new org.pitest.mutationtest.engine.gregor.mutators.experimental.MemberVariableMutator().getGloballyUniqueId(), operator);

        operator = new Operator("RETURN_VALS", "Method");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(ReturnValsMutator.RETURN_VALS_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("VOID_METHOD_CALLS", "Method");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(VoidMethodCallMutator.VOID_METHOD_CALL_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("NON_VOID_METHOD_CALLS", "Method");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(NonVoidMethodCallMutator.NON_VOID_METHOD_CALL_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("CONSTRUCTOR_CALLS", "Method");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(ConstructorCallMutator.CONSTRUCTOR_CALL_MUTATOR.getGloballyUniqueId(), operator);

        operator = new Operator("EXPERIMENTAL_SWITCH", "Method");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(new org.pitest.mutationtest.engine.gregor.mutators.experimental.SwitchMutator().getGloballyUniqueId(), operator);

        operator = new Operator("EXPERIMENTAL_ARGUMENT_PROPAGATION", "Method");
        ALL_OPERATORS.add(operator);
        ALL_OPERATORS_BY_CLASS.put(ArgumentPropagationMutator.ARGUMENT_PROPAGATION_MUTATOR.getGloballyUniqueId(), operator);
    }

    public PITFacade(String trainingDirectory) {
        this.trainingDirectory = trainingDirectory;
    }

    @Override
    public List<Operator> getAllOperators() {
        ArrayList<Operator> operators = new ArrayList<>();
        for (Operator operator : ALL_OPERATORS) {
            operators.add(new Operator(operator));
        }
        return operators;
    }

    @Override
    public List<Mutant> executeOperator(Operator operator) {
        return executeOperators(Lists.newArrayList(operator));
    }

    @Override
    public List<Mutant> executeOperators(List<Operator> operators) {
        List<Mutant> mutants = new ArrayList<>();
        if (operators != null && !operators.isEmpty()) {
            PluginServices plugins = PluginServices.makeForContextLoader();

            ReportOptions reportOptions = createDefaultReportOptions();
            reportOptions.setMutators(operators.stream().map(Operator::getName).collect(Collectors.toList()));

            try {
                final Program programUnderTest = IntegrationFacade.getProgramUnderTest();
                EntryPointImpl entryPoint = getOrCreateEntryPoint();
                MutationAnalysisUnit unit = entryPoint.generateMutants(new File(trainingDirectory), reportOptions, new SettingsFactory(reportOptions, plugins), new HashMap<>());
                if (unit != null) {
                    if (unit instanceof MutationTestUnit) {
                        MutationTestUnit testUnit = (MutationTestUnit) unit;
                        Field field = testUnit.getClass().getDeclaredField("availableMutations");
                        field.setAccessible(true);
                        Collection<MutationDetails> fieldValue = (Collection<MutationDetails>) field.get(testUnit);
                        mutants.addAll(fieldValue.stream().map((mutationDetails) -> {
                            Mutant mutant = new Mutant(mutationDetails.getId().toString(), null, programUnderTest);
                            Operator mappedOperator = ALL_OPERATORS_BY_CLASS.get(mutationDetails.getMutator());
                            Operator operator = Iterables.find(operators, (tempOperator) -> tempOperator.equals(mappedOperator));
                            mutant.getOperators().add(operator);
                            operator.getGeneratedMutants().add(mutant);
                            HashMap<Mutant, MutationDetails> programMutants = generatedMutants.computeIfAbsent(programUnderTest, (t) -> new HashMap<>());
                            programMutants.putIfAbsent(mutant, mutationDetails);
                            return mutant;
                        }).collect(Collectors.toList()));
                        mutationUnits.putIfAbsent(programUnderTest, testUnit);
                    } else {
                        throw new IllegalArgumentException("This should not be happening!");
                    }
                }
            } catch (IOException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(PITFacade.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return mutants;
    }

    @Override
    public void executeMutant(Mutant mutantToExecute) {
        if (mutantToExecute != null) {
            executeMutants(Lists.newArrayList(mutantToExecute));
        }
    }

    @Override
    public void executeMutants(List<Mutant> mutantsToExecute) {
        if (mutantsToExecute != null && !mutantsToExecute.isEmpty()) {
            PluginServices plugins = PluginServices.makeForContextLoader();

            ReportOptions reportOptions = createDefaultReportOptions();

            try {
                final Program programUnderTest = IntegrationFacade.getProgramUnderTest();
                MutationTestUnit unit = mutationUnits.get(programUnderTest);
                if (unit != null) {
                    Field field = unit.getClass().getDeclaredField("availableMutations");
                    field.setAccessible(true);
                    Collection<MutationDetails> fieldValue = (Collection<MutationDetails>) field.get(unit);
                    fieldValue.clear();
                    HashMap<Mutant, MutationDetails> descriptions = generatedMutants.get(programUnderTest);
                    for (Mutant mutant : mutantsToExecute) {
                        MutationDetails description = descriptions.get(mutant);
                        fieldValue.add(description);
                    }
                    EntryPointImpl entryPoint = getOrCreateEntryPoint();
                    Collection<MutationResult> result = entryPoint.executeMutants(new File(trainingDirectory), reportOptions, new SettingsFactory(reportOptions, plugins), new HashMap<>(), unit);

                    for (Mutant mutant : mutantsToExecute) {
                        MutationDetails description = descriptions.get(mutant);
                        MutationResult specificResult = Iterables.find(result, (tempItem) -> {
                            return tempItem.getDetails().equals(description);
                        });
                        mutant.getKillingTestCases().addAll(specificResult.getKillingTest().map((testCase) -> {
                            TestCase sentinelTestCase = new TestCase(testCase);
                            sentinelTestCase.getKillingMutants().add(mutant);
                            return sentinelTestCase;
                        }));
                    }
                } else {
                    throw new IOException("Something went wrong. I could not find the mutation unit for the mutation procedure. This has something to do to the program under test. Maybe it is non-existent?");
                }
            } catch (IOException | SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException ex) {
                Logger.getLogger(PITFacade.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Mutant combineMutants(List<Mutant> mutantsToCombine) {
        throw new UnsupportedOperationException("Sorry, PIT test is not adapted to work with HOMs. Please, select a grammar file without HOMs.");
    }

    @Override
    public Program instantiateProgram(String programName) {
        String replace = programName.replace(".java", "");
        replace = CharMatcher.anyOf("\\/.").replaceFrom(replace, File.separator);
        return new Program(programName, new File(trainingDirectory + File.separator + replace + ".java"));
    }

    private ReportOptions createDefaultReportOptions() {
        ReportOptions reportOptions = new ReportOptions();
        String absolutePath = new File(trainingDirectory).getAbsolutePath();
        reportOptions.setReportDir(absolutePath + File.separator + "result");
        reportOptions.setGroupConfig(new TestGroupConfig());
        reportOptions.setSourceDirs(Lists.newArrayList(new File(absolutePath)));
        reportOptions.setCodePaths(Lists.newArrayList(absolutePath));
        reportOptions.setTargetClasses(Lists.newArrayList(new Glob(IntegrationFacade.getProgramUnderTest().getFullName())));
        reportOptions.setTargetTests(Lists.newArrayList(new Glob(IntegrationFacade.getProgramUnderTest().getFullName() + "Test")));
        reportOptions.setClassPathElements(Lists.newArrayList(ClassPath.getClassPathElementsAsPaths()));
        reportOptions.getClassPathElements().add(absolutePath);
        reportOptions.setMutateStaticInitializers(true);
        reportOptions.setDetectInlinedCode(true);
        reportOptions.setShouldCreateTimestampedReports(false);
        reportOptions.setIncludeLaunchClasspath(true);
        reportOptions.setVerbose(false);
        return reportOptions;
    }

    private EntryPointImpl getOrCreateEntryPoint() {
        return entryPoints.computeIfAbsent(IntegrationFacade.getProgramUnderTest(), (program) -> {
            return new EntryPointImpl();
        });
    }

    @Override
    public void executeMutantsAgainstAllTestCases(List<Mutant> mutantsToExecute) {
        //TODO
    }

    @Override
    public void tearDown() {
        for (EntryPointImpl entry : entryPoints.values()) {
            entry.close();
        }
        entryPoints.clear();
        mutationUnits.clear();
        generatedMutants.clear();
    }

}
