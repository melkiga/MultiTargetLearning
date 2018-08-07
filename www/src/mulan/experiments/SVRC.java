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
import mulan.regressor.transformation.EnsembleOfRegressorChains;
import mulan.regressor.transformation.MyRegressorChain;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;
import weka.filters.unsupervised.attribute.Standardize;

public class SVRC {

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

		//String fileStem = "edm,flare1,flare2,water-quality,oes97,oes10,atp1d,atp7d";
		String filename = args[0];
		System.out.println(args[0]);

		// parsing options related to multi-target methods being evaluated
		String mt = "SVRC";

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

//		Normalize norm = new Normalize();
//		norm.setInputFormat(full.getDataSet());
//		norm.setIgnoreClass(true);
//		Instances normalizedData = Filter.useFilter(full.getDataSet(), norm);
//		full = new MultiLabelInstances(normalizedData, full.getLabelsMetaData());
		
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

		LibSVM base = new LibSVM();
		base.setSVMType(new SelectedTag(3, LibSVM.TAGS_SVMTYPE));
		base.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_RBF, LibSVM.TAGS_KERNELTYPE));
		base.setNormalize(false);

		// CORRELATIONS AND CHAIN

		double[][] corr = new double[numTargets][numTargets];
		double[] corrSum = new double[numTargets];
		int numberInstances = full.getDataSet().size();

		for(int j = 0; j < numTargets; j++)
			for(int k = 0; k < numTargets; k++)
			{
				corr[j][k] = Utils.correlation(full.getDataSet().attributeToDoubleArray(j), full.getDataSet().attributeToDoubleArray(k), numberInstances);
				corrSum[j] += corr[j][k];
			}

		HashMap<Integer,Double> map = new HashMap<Integer,Double>();

		for(int j = 0; j < numTargets; j++)
			map.put(j, corrSum[j]);

		int[] sortedIndexes = sortByComparator(map, false);
		int[] chain = new int[numTargets];

		for (int j = 0; j < numTargets; j++)
			chain[j] = full.getLabelIndices()[sortedIndexes[j]];

		// BUILD REGRESSOR

		MyRegressorChain regressor = new MyRegressorChain(base, chain);
		regressor.setMeta(MyRegressorChain.metaType.TRUE);
		regressor.setNumFolds(numFolds);
		regressor.setDebug(true);

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

	private static int[] sortByComparator(Map<Integer,Double> unsortMap, final boolean order)
	{
		List<Entry<Integer,Double>> list = new LinkedList<Entry<Integer,Double>>(unsortMap.entrySet());

		Collections.sort(list, new Comparator<Entry<Integer,Double>>()
				{
			public int compare(Entry<Integer,Double> o1,
					Entry<Integer,Double> o2)
			{
				if (order)
				{
					return o1.getValue().compareTo(o2.getValue());
				}
				else
				{
					return o2.getValue().compareTo(o1.getValue());

				}
			}
				});

		int[] sortedIndexes = new int[list.size()];

		int i = 0;
		for (Entry<Integer,Double> entry : list)
		{
			sortedIndexes[i] = entry.getKey();
			i++;
		}

		return sortedIndexes;
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
