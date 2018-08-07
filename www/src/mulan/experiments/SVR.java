package mulan.experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libsvm.svm;
import libsvm.svm_print_interface;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluator;
import mulan.evaluation.MultipleEvaluation;
import mulan.regressor.MTRegressor;
import mulan.regressor.transformation.EnsembleOfRegressorChains;
import mulan.regressor.transformation.RegressorChain;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.meta.CVParameterSelection;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

public class SVR {

	/** the number of cross-validation folds to use for evaluation **/
	public static final int numFolds = 10;
	public static final EnsembleOfRegressorChains.SamplingMethod sampling = EnsembleOfRegressorChains.SamplingMethod.None;
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

		//String fileStem = "edm,flare1,flare2,water-quality,oes97,oes10,atp1d,atp7d...";
		String filename = args[0];
		System.out.println(args[0]);

		// parsing options related to multi-target methods being evaluated
		String mt = "SVR";

		numSlots = Runtime.getRuntime().availableProcessors();

		full = new MultiLabelInstances("data/" + filename + ".arff", "data/" + filename + ".xml");
		numTargets = full.getNumLabels();

		// Initialize output file

		BufferedWriter outResults = new BufferedWriter(new FileWriter("results_" + filename + "_" + mt + ".txt"));

		outResults.write("dataset\teval_type\tmt_method\tbase_learner\ttarget_index\ttarget_name\t");
		outResults.write("'" + "Average Relative RMSE" + "'\t");
		outResults.write("real_time\tcpu_time\n");
		outResults.flush();

		// NORMALIZATION
		Standardize std = new Standardize();
		std.setInputFormat(full.getDataSet());
		std.setIgnoreClass(true);
		Instances standarizedData = Filter.useFilter(full.getDataSet(), std);
		full = new MultiLabelInstances(standarizedData, full.getLabelsMetaData());
		
		LibSVM base = new LibSVM();
		base.setSVMType(new SelectedTag(3, LibSVM.TAGS_SVMTYPE));
		base.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_RBF, LibSVM.TAGS_KERNELTYPE));

		MTRegressor regressor = new MTRegressor(base);

		Evaluator eval = new Evaluator();
		eval.setSeed(1);
		MultipleEvaluation results = null;

		long start = System.currentTimeMillis();
		long startCPU = getCpuTime();
		results = eval.crossValidate(regressor, full, numFolds);
		long end = System.currentTimeMillis();
		long endCPU = getCpuTime();

		outResults.write(filename + "\t" + mt + "\t" + "\t0\tall\t");
		outResults.write(results.getMean("Average Relative RMSE") + "\t");
		outResults.write((end - start) + "\t" + (endCPU - startCPU) + "\n");

		for (int m = 0; m < numTargets; m++) {
			String targetName = full.getDataSet().attribute(full.getLabelIndices()[m]).name();
			outResults.write(filename + "\t" + mt + "\t" + "\t" + (m + 1) + "\t" + targetName + "\t");
			outResults.write(results.getMean("Average Relative RMSE", m) + "\t");
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
