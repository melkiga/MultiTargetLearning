package mulan.regressor.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import mulan.classifier.InvalidDataException;
import mulan.classifier.MultiLabelOutput;
import mulan.data.MultiLabelInstances;
import mulan.regressor.transformation.MyRegressorChain.metaType;
import weka.classifiers.Classifier;
import weka.classifiers.meta.CVParameterSelection;
import weka.classifiers.trees.REPTree;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Resample;
import weka.classifiers.functions.LibSVM;

/**
 * This class implements the Ensemble of Regressor Chains (ERC) method.<br>
 * For more information, see:<br>
 * <em>E. Spyromitros-Xioufis, G. Tsoumakas, W. Groves, I. Vlahavas. 2014. Multi-label Classification Methods for
 * Multi-target Regression. <a href="http://arxiv.org/abs/1211.6581">arXiv e-prints</a></em>.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * @version 2014.04.01
 */
public class MyEnsembleOfRegressorChains extends TransformationBasedMultiTargetRegressor {

    private static final long serialVersionUID = 1L;

    /**
     * The number of RC models to be created.
     */
    private int numOfModels;

    /**
     * Stores the RC models.
     */
    private MyRegressorChain[] ensemble;

    /** The seed to use in random number generators. Default = 1. **/
    private int seed = 1;

    /**
     * Three types of sampling.
     */
    public enum SamplingMethod {
        None, WithReplacement, WithoutReplacement,
    };

    /**
     * The method used to obtain the values of the meta features. TRUE is used by default.
     */
    private metaType meta = MyRegressorChain.metaType.TRUE;

    /**
     * The type of sampling to be used. None is used by default.
     */
    private SamplingMethod sampling = SamplingMethod.None;

    /**
     * The size of each sample (as a percentage of the training set size) when sampling with replacement is
     * performed. Default is 100.
     */
    private double sampleWithReplacementPercent = 100;

    /**
     * The size of each sample (as a percentage of the training set size) when sampling without replacement is
     * performed. Default is 67.
     */
    private double sampleWithoutReplacementPercent = 67;

    /**
     * The number of folds to use in RegressorChainCorrected when CV is selected for obtaining the values of
     * the meta-features.
     */
    private int numFolds = 3;

	private double[] means;

	private double[] stdDevs;

    /**
     * Default constructor.
     * 
     * @throws Exception Potential exception thrown. To be handled in an upper level.
     */
    public MyEnsembleOfRegressorChains() throws Exception {
        this(new REPTree(), 10, SamplingMethod.WithReplacement);
    }

    /**
     * Constructor.
     * 
     * @param baseRegressor the base regression algorithm that will be used
     * @param numOfModels the number of models in the ensemble
     * @param sampling the sampling method
     * @throws Exception Potential exception thrown. To be handled in an upper level.
     */
    public MyEnsembleOfRegressorChains(Classifier baseRegressor, int numOfModels,
            SamplingMethod sampling) throws Exception {
        super(baseRegressor);
        this.numOfModels = numOfModels;
        this.sampling = sampling;
        ensemble = new MyRegressorChain[numOfModels];
//        this.means = means;
//        this.stdDevs = stdDevs;
    }

    @Override
    protected void buildInternal(MultiLabelInstances mlTrainSet) throws Exception {
        // calculate the number of models
        int numDistinctChains = 1;
        for (int i = 1; i <= numLabels; i++) {
            numDistinctChains *= i;
            if (numDistinctChains > numOfModels) {
                numDistinctChains = numOfModels;
                break;
            }
        }
        numOfModels = numDistinctChains;

        // will hold the distinct chains created so far
        HashSet<String> distinctChains = new HashSet<String>(numOfModels);

        // this random number generator will be used for taking random samples
        // and creating random chains
        Random rand = new Random(seed);

        for (int i = 0; i < numOfModels; i++) {
            debug("ERC Building Model:" + (i + 1) + "/" + numOfModels);
            MultiLabelInstances sampledTrainingSet = null;
            if (sampling != SamplingMethod.None) {
                // initialize a Resample filter using a different seed each time
                Resample rsmp = new Resample();
                rsmp.setRandomSeed(rand.nextInt());
                if (sampling == SamplingMethod.WithoutReplacement) {
                    rsmp.setNoReplacement(true);
                    rsmp.setSampleSizePercent(sampleWithoutReplacementPercent);
                } else {
                    rsmp.setNoReplacement(false);
                    rsmp.setSampleSizePercent(sampleWithReplacementPercent);
                }
                Instances trainSet = new Instances(mlTrainSet.getDataSet());
                rsmp.setInputFormat(trainSet);
                Instances sampled = Filter.useFilter(trainSet, rsmp);
                sampledTrainingSet = new MultiLabelInstances(sampled,
                        mlTrainSet.getLabelsMetaData());
            }

            // create a distinct chain
            int[] chain = new int[numLabels];
            while (true) {
                for (int j = 0; j < numLabels; j++) { // the default chain
                    chain[j] = labelIndices[j];
                }
                ArrayList<Integer> chainAsList = new ArrayList<Integer>(numLabels);
                for (int j = 0; j < numLabels; j++) {
                    chainAsList.add(chain[j]);
                }
                Collections.shuffle(chainAsList, rand);
                for (int j = 0; j < numLabels; j++) {
                    chain[j] = chainAsList.get(j);
                }
                String chainString = chainAsList.toString();
                if (distinctChains.add(chainString)) {
                    // the chain is not in the set so we can break the loop
                    break;
                }
            }
            
//            double[][] corr = new double[numLabels][numLabels];
//            double[] corrSum = new double[numLabels];
//            int numberInstances = mlTrainSet.getDataSet().size();
//            
//            for(int j = 0; j < numLabels; j++)
//            	for(int k = 0; k < numLabels; k++)
//            	{
//            		corr[j][k] = Utils.correlation(mlTrainSet.getDataSet().attributeToDoubleArray(j), mlTrainSet.getDataSet().attributeToDoubleArray(k), numberInstances);
//            		corrSum[j] += corr[j][k];
//            	}
//            
//            HashMap<Integer,Double> map = new HashMap<Integer,Double>();
//            
//            for(int j = 0; j < numLabels; j++)
//            {
////            	System.out.println(Arrays.toString(corr[j]));
//            	map.put(j, corrSum[j]);
//            }
//            
////            System.out.println(Arrays.toString(corrSum));
////            System.out.println(map);
//            
//            int[] sortedIndexes = sortByComparator(map,false);
//            
//            for (int j = 0; j < numLabels; j++) { // the default chain
//              chain[j] = labelIndices[sortedIndexes[j]];
//            }
            
            ensemble[i] = new MyRegressorChain(baseRegressor, chain, i);
            ensemble[i].setNumFolds(numFolds);
            ensemble[i].setMeta(meta);
            ensemble[i].setDebug(getDebug());
            if (sampling == SamplingMethod.None) {
                ensemble[i].build(mlTrainSet);
            } else {
                ensemble[i].build(sampledTrainingSet);
            }
        }
    }
    
    private int[] sortByComparator(Map<Integer,Double> unsortMap, final boolean order)
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

    @Override
    protected MultiLabelOutput makePredictionInternal(Instance instance) throws Exception,
            InvalidDataException {
        double[] scores = new double[numLabels];

        for (int i = 0; i < numOfModels; i++) {
            MultiLabelOutput ensembleMLO = ensemble[i].makePrediction(instance);
            double[] score = ensembleMLO.getPvalues();
            for (int j = 0; j < numLabels; j++) {
                scores[j] += score[j];
            }
        }

        for (int j = 0; j < numLabels; j++) {
            scores[j] /= numOfModels;
        }
        
        //Rescaling
//        for (int j = 0; j < numLabels; j++) {
//            scores[j] = scores[j]*stdDevs[instance.numAttributes()-numLabels + j] + means[instance.numAttributes()-numLabels + j];
//        }

        MultiLabelOutput mlo = new MultiLabelOutput(scores, true);
        return mlo;
    }
    
//    @Override
//    protected MultiLabelOutput makePredictionInternal(Instance instance) throws Exception,
//            InvalidDataException {
//        double[][] scores = new double[numOfModels][numLabels];
//
//        for (int i = 0; i < numOfModels; i++) {
//            MultiLabelOutput ensembleMLO = ensemble[i].makePrediction(instance);
//            scores[i] = ensembleMLO.getPvalues();
//        }
//
//        double[] avg = new double[numLabels];
//        double[] var = new double[numLabels];
//        double[] output = new double[numLabels];
//        
//        for (int i = 0; i < numLabels; i++) {
//        	for (int j = 0; j < numOfModels; j++) {
//        		avg[i] += scores[j][i]; 
//        	}
//        	avg[i] /= numOfModels;
//        }
//
//        for (int i = 0; i < numLabels; i++) {
//        	for (int j = 0; j < numOfModels; j++) {
//        		var[i] += (scores[j][i] - avg[i]) * (scores[j][i] - avg[i]); 
//        	}
//        	var[i] /= numOfModels;
//        	var[i] = Math.sqrt(var[i]);
//        }
//        
//        System.out.println(instance);
//        System.out.println("");
//        
//        for (int i = 0; i < numOfModels; i++)
//        System.out.println(Arrays.toString(scores[i]));
//        
//        System.out.println("");
//        System.out.println(Arrays.toString(avg));
//        System.out.println(Arrays.toString(var));
//        
//        for (int i = 0; i < numLabels; i++) {
//        	ArrayList<Double> involved = new ArrayList<Double>();
//        	
//        	for (int j = 0; j < numOfModels; j++) {
//        		if(scores[j][i] < avg[i]-var[i] || scores[j][i] > avg[i]+var[i])
//        		{
//        			// do nothing
//        		}
//        		else
//        		{
//        			involved.add(scores[j][i]);
//        		}
//        	}
//        	
//        	System.out.println(involved);
//
//        	for(double d : involved)
//        		output[i] += d;
//        	output[i] /= involved.size();
//        }
//
//        MultiLabelOutput mlo = new MultiLabelOutput(output, true);
//        return mlo;
//    }

    @Override
    protected String getModelForTarget(int targetIndex) {
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < numOfModels; i++) {
            output.append("Ensemble member: " + (i + 1) + "\n");
            output.append(ensemble[i].getModelForTarget(targetIndex) + "\n");
        }
        return output.toString();
    }

    public void setSampleWithReplacementPercent(int sampleWithReplacementPercent) {
        this.sampleWithReplacementPercent = sampleWithReplacementPercent;
    }

    public void setSampleWithoutReplacementPercent(double sampleWithoutReplacementPercent) {
        this.sampleWithoutReplacementPercent = sampleWithoutReplacementPercent;
    }

    public void setNumFolds(int numFolds) {
        this.numFolds = numFolds;
    }

    public void setMeta(metaType meta) {
        this.meta = meta;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

}