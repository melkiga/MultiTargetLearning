package mulan.experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

import libsvm.svm;
import libsvm.svm_print_interface;
import mulan.classifier.MultiLabelLearnerBase;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluation;
import mulan.evaluation.Evaluator;
import mulan.evaluation.MultipleEvaluation;
import mulan.evaluation.measure.AverageRelativeRMSE;
import mulan.evaluation.measure.Measure;
import mulan.regressor.transformation.MyEnsembleOfRegressorChains;
import mulan.regressor.transformation.MyRegressorChain;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.meta.Bagging;
import weka.classifiers.meta.CVParameterSelection;
import weka.classifiers.trees.REPTree;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

public class SVRRC {

	/** the number of models in ensemble methods **/
	public static final int numEnsembleModels = 10;
	/** whether the base learner should output debug messages **/
	public static final boolean baseDebug = false;
	/** whether the multi-target methods should output debug messages **/
	public static final boolean mtDebug = true;
	/** the number of cross-validation folds to use for evaluation **/
	public static final int numFolds = 10;
	public static final MyEnsembleOfRegressorChains.SamplingMethod sampling = MyEnsembleOfRegressorChains.SamplingMethod.None;
	/** number of execution slots to use by Weka's algorithms which support this option **/
	private static int numSlots;
	/** number of targets **/
	private static int numTargets;
	/** the multi-target datasets. Train and test will be null when cv is performed and vise versa **/
	private static MultiLabelInstances full;
	private static MultiLabelInstances train;
	private static MultiLabelInstances test;
	
	private static svm_print_interface svm_print_null = new svm_print_interface()
    {
            public void print(String s) {}
    };

	public static void main(String[] args) throws Exception {
		
		svm.svm_set_print_string_function(svm_print_null);
		
		//String fileStem = "edm,flare1,flare2,water-quality,oes97,oes10,atp1d,atp7d";
		String filename = args[0];
		
		System.out.println(args[0]);

		// parsing options related to multi-target methods being evaluated
		String mt = "SVRRC";
		
		numSlots = Runtime.getRuntime().availableProcessors();

		full = new MultiLabelInstances("data/" + filename + ".arff", "data/" + filename + ".xml");
		
//		Normalize norm = new Normalize();
//	    norm.setInputFormat(full.getDataSet());
//	    norm.setIgnoreClass(true);
//	    Instances normalizedData = Filter.useFilter(full.getDataSet(), norm);
//	    full = new MultiLabelInstances(normalizedData, full.getLabelsMetaData());
		
		Standardize std = new Standardize();
		std.setInputFormat(full.getDataSet());
		std.setIgnoreClass(true);
		Instances standarizedData = Filter.useFilter(full.getDataSet(), std);
		full = new MultiLabelInstances(standarizedData, full.getLabelsMetaData());

//		Instances fullInstances = full.getDataSet();
//		
//		double[] means = new double[fullInstances.numAttributes()];
//		double[] stdDevs = new double[fullInstances.numAttributes()];
//		
//		for(int i = 0; i < fullInstances.numAttributes(); i++)
//		{
//			means[i] = fullInstances.attributeStats(i).numericStats.mean;
//			stdDevs[i] = fullInstances.attributeStats(i).numericStats.stdDev;
//			
//			for(int j = 0; j < fullInstances.size(); j++)
//			{
//				double value = (fullInstances.instance(j).value(i) - means[i]) / stdDevs[i];
//				fullInstances.instance(j).setValue(i, value);
//			}
//		}
		
		numTargets = full.getNumLabels();

		List<Measure> measures = new ArrayList<Measure>();
		measures.add(new AverageRelativeRMSE(numTargets, full, full));

		MultiLabelLearnerBase mtMethodPtr = null;

		String resultsFileName = "results_Ensemble_" + filename + "_" + mt + ".txt";
		BufferedWriter outResults = new BufferedWriter(new FileWriter(resultsFileName));

		// header
		outResults.write("dataset\teval_type\tmt_method\tbase_learner\ttarget_index\ttarget_name\t");
		// print the measures name
		for (Measure m : measures) {
			outResults.write("'" + m.getName() + "'\t");
		}
		outResults.write("real_time\tcpu_time\n");
		outResults.flush();
		
		LibSVM base = new LibSVM();
		base.setSVMType(new SelectedTag(3, LibSVM.TAGS_SVMTYPE));
		base.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_RBF, LibSVM.TAGS_KERNELTYPE));
		
//		RBFRegressor learner = new RBFRegressor();
//		//learner.setNumFunctions(10);
//		learner.setNumThreads(4);
//		learner.setPoolSize(4);
//		learner.setUseNormalizedBasisFunctions(true);
		
//		int numBags = 100;
//		Bagging bagging = new Bagging();
//		bagging.setNumIterations(numBags);
//		bagging.setNumExecutionSlots(numSlots);
//		bagging.setClassifier(learner);
		
		MyEnsembleOfRegressorChains ERCC = new MyEnsembleOfRegressorChains(base, numEnsembleModels, sampling);
		ERCC.setMeta(MyRegressorChain.metaType.TRUE);
		ERCC.setNumFolds(10);
		mtMethodPtr = ERCC;

		mtMethodPtr.setDebug(mtDebug);

		Evaluator eval = new Evaluator();
		eval.setSeed(1);
		MultipleEvaluation results = null;

		long start = System.currentTimeMillis();
		long startCPU = getCpuTime();
		results = eval.crossValidate(mtMethodPtr, full, numFolds);
		long end = System.currentTimeMillis();
		long endCPU = getCpuTime();

		ArrayList<Evaluation> evals = results.getEvaluations();
		double[][] totalSEs = new double[numTargets][numFolds]; // a_i
		double[][] trainMeanTotalSEs = new double[numTargets][numFolds]; // b_i_us
		int[][] nonMissingInstances = new int[numTargets][numFolds];

		for (int t = 0; t < evals.size(); t++) { // for each fold!
			AverageRelativeRMSE arrmse = ((AverageRelativeRMSE) evals.get(t).getMeasures()
					.get(1));
			for (int r = 0; r < numTargets; r++) {
				totalSEs[r][t] = arrmse.getTotalSE(r);
				trainMeanTotalSEs[r][t] = arrmse.getTrainMeanTotalSE(r);
				// either measure can be used for getting the num non-missing
				nonMissingInstances[r][t] = arrmse.getNumNonMissing(r);
			}
		}

		// calculating rrmse
		double[] rrmse_us = new double[numTargets];
		for (int r = 0; r < numTargets; r++) {
			for (int t = 0; t < numFolds; t++) {
				rrmse_us[r] += Math.sqrt(totalSEs[r][t])
						/ Math.sqrt(trainMeanTotalSEs[r][t]);
			}
			rrmse_us[r] /= numFolds;
		}

		// print static information
		outResults.write(filename + "\t" + mt + "\t" + "\t0\tall\t");
		for (Measure m : measures) {
			outResults.write(results.getMean(m.getName()) + "\t");
		}
		outResults.write((end - start) + "\t" + (endCPU - startCPU) + "\n");

		for (int m = 0; m < numTargets; m++) {
			String targetName = full.getDataSet().attribute(full.getLabelIndices()[m]).name();
			outResults.write(filename + "\t" + mt + "\t" + "\t" + (m + 1) + "\t" + targetName + "\t");
			for (Measure me : measures) {
				outResults.write(results.getMean(me.getName(), m) + "\t");
			}
			outResults.write((end - start) + "\t" + (endCPU - startCPU) + "\n");
		}
		outResults.flush();
		outResults.close();
	}

	/** 
	 * Get CPU time in milliseconds.
	 * @return the CPU time in ms
	 */
	public static long getCpuTime() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		return bean.isCurrentThreadCpuTimeSupported() ? (long) ((double) bean.getCurrentThreadCpuTime() / 1000000.0) : 0L;
	}
}
