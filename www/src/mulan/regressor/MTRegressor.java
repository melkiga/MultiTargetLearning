/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package mulan.regressor;

import java.util.ArrayList;
import java.util.Random;

import mulan.classifier.MultiLabelOutput;
import mulan.data.DataUtils;
import mulan.data.MultiLabelInstances;
import mulan.regressor.transformation.TransformationBasedMultiTargetRegressor;
import mulan.transformations.regression.ChainTransformation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.meta.CVParameterSelection;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * This class implements the Regressor Chain (RC) method.<br>
 * <br>
 * For more information, see:<br>
 * <em>E. Spyromitros-Xioufis, G. Tsoumakas, W. Groves, I. Vlahavas. 2014. Multi-label Classification Methods for
 * Multi-target Regression. <a href="http://arxiv.org/abs/1211.6581">arXiv e-prints</a></em>.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * @version 2014.04.01
 */
public class MTRegressor extends TransformationBasedMultiTargetRegressor {

    private static final long serialVersionUID = 1L;

    protected Classifier[] regressors;
    
    protected Instances[] trainCopy;

    /**
     * Creates a new instance with the given base regressor. If {@link #chainSeed} == 0, the default
     * chain is used. Otherwise, a random chain is created using the given seed.
     * 
     * @param regressor the base regression algorithm that will be used
     */
    public MTRegressor(Classifier regressor) {
        super(regressor);
    }

    protected void buildInternal(MultiLabelInstances train) throws Exception {
        regressors = new Classifier[numLabels];
        trainCopy = new Instances[numLabels];

        for (int i = 0; i < numLabels; i++) {
        	trainCopy[i] = new Instances(train.getDataSet());
        	
        	ArrayList<Integer> idx = new ArrayList<Integer>();
            for (int j = 0; j < numLabels; j++)
            	idx.add(train.getDataSet().numAttributes() - numLabels + j);
            idx.remove(i);
            int[] indicesToRemove = convertIntegers(idx);
            
            Remove remove = new Remove();
            remove.setAttributeIndicesArray(indicesToRemove);
            remove.setInvertSelection(false);
            remove.setInputFormat(trainCopy[i]);
            trainCopy[i] = Filter.useFilter(trainCopy[i], remove);
            trainCopy[i].setClassIndex(trainCopy[i].numAttributes()-1);
            
            //trainCopy[i].randomize(new Random(i)); //////////////////////////////////////////////////////////////////////////////////!!!!!!!!!!!!!!!!!!!!
            
			CVParameterSelection learner = new CVParameterSelection();
			learner.setClassifier(AbstractClassifier.makeCopy(baseRegressor));
			learner.setNumFolds(5);
			learner.addCVParameter("VALUES C 1 10 100");
			learner.addCVParameter("VALUES P 0.01 0.1 0.2");
			learner.addCVParameter("VALUES G 1e-9 1e-7 1e-5 1e-3 1e-1 1 5 10");
			
			System.out.println("Bulding model " + (i + 1) + "/" + numLabels);
			learner.buildClassifier(trainCopy[i]);

            regressors[i] = learner.getClassifier();
        }
    }

    protected MultiLabelOutput makePredictionInternal(Instance instance) throws Exception {
    	double[] scores = new double[numLabels];

        Instance copyOfInstance = DataUtils.createInstance(instance, instance.weight(), instance.toDoubleArray());
        copyOfInstance.setDataset(instance.dataset());
        
        int[] chain = new int[numLabels];
        
        for (int counter = 0; counter < numLabels; counter++)
        	chain[counter] = instance.numAttributes() - numLabels + counter;

        for (int counter = 0; counter < numLabels; counter++) {
        	
        	ArrayList<Integer> idx = new ArrayList<Integer>();
            for (int j = 0; j < numLabels; j++)
            	idx.add(instance.numAttributes() - numLabels + j);
            idx.remove(counter);
            int[] indicesToRemove = convertIntegers(idx);
            
        	Remove remove = new Remove();
            remove.setAttributeIndicesArray(indicesToRemove);
            remove.setInputFormat(instance.dataset());
            remove.input(instance);
            remove.batchFinished();
            Instance transformed = remove.output();
            
            transformed.setDataset(trainCopy[counter]);

            double score = regressors[counter].classifyInstance(transformed);

            // find the appropriate position for that score in the scores array
            // i.e. which is the corresponding target
            int pos = 0;
            for (int i = 0; i < numLabels; i++) {
                if (chain[counter] == labelIndices[i]) {
                    pos = i;
                }
            }
            scores[pos] = score;
            copyOfInstance.setValue(chain[counter], score);
        }

        MultiLabelOutput mlo = new MultiLabelOutput(scores, true);
        return mlo;
    }

    @Override
    protected String getModelForTarget(int targetIndex) {
        try {
            regressors[targetIndex].getClass()
                    .getMethod("toString", (Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            return "A string representation for this base algorithm is not provided!";
        }
        return regressors[targetIndex].toString();
    }

    public int[] convertIntegers(ArrayList<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }
}
