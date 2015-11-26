package com.spbsu.jstacs;

import de.jstacs.classifiers.differentiableSequenceScoreBased.OptimizableFunction;
import de.jstacs.classifiers.differentiableSequenceScoreBased.gendismix.GenDisMixClassifierParameterSet;
import de.jstacs.classifiers.differentiableSequenceScoreBased.msp.MSPClassifier;
import de.jstacs.data.*;
import de.jstacs.data.sequences.Sequence;
import de.jstacs.sequenceScores.differentiable.DifferentiableSequenceScore;
import de.jstacs.sequenceScores.statisticalModels.differentiable.DifferentiableStatisticalModel;
import de.jstacs.sequenceScores.statisticalModels.differentiable.DifferentiableStatisticalModelFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class AppTest {

  @Test
  public void testApp() throws Exception {
    List<DataSet> dataSets = new ArrayList<DataSet>();
    for (int i = 0; i < 10; i++) {
      DataSet dataSet = new DNADataSet(String.format("src/test/data/%s.fa", i + 1));
      dataSet = getDataSetWithSameLength(dataSet);
      if (dataSet.getElementLength() == 101) {
        printDataSet(dataSet);
        dataSets.add(dataSet);
      }
    }
    System.out.println("DataSets: " + dataSets.size());
    DataSet[] data = dataSets.toArray(new DataSet[] {});

//    TrainableStatisticalModel[] models = new TrainableStatisticalModel[data.length];
//    for (int i = 0; i < data.length; i++) {
//      data[i] = getDataSetWithSameLength(data[i]);
//      printDataSet(data[i]);
//      TrainableStatisticalModel hmm = TrainableStatisticalModelFactory.createHomogeneousMarkovModel(data[i].getAlphabetContainer(), 0.04, (byte) 1);
//      models[i] = hmm;
//    }
//
//    TrainSMBasedClassifier cl = new TrainSMBasedClassifier(models);
//    cl.train(data);


    AlphabetContainer con = data[0].getAlphabetContainer();
    DifferentiableStatisticalModel[] models = new DifferentiableStatisticalModel[data.length];
    for (int i = 0; i < data.length; i++) {
      models[i] = DifferentiableStatisticalModelFactory.createPWM(con, data[i].getElementLength(), 4);
    }


    GenDisMixClassifierParameterSet pars = new GenDisMixClassifierParameterSet(con,101,(byte)10,1E-6,1E-1,1, false, OptimizableFunction.KindOfParameter.PLUGIN,true,1);

    MSPClassifier cl = new MSPClassifier(pars, models);
    cl.train(data);

    System.out.println("classes: " + cl.getNumberOfClasses());
    for (int i = 0; i < models.length; i++) {
      double positives = 0;
      for (Sequence sequence: data[i].getAllElements()) {
        byte res = cl.classify(sequence);
        if (res == i)
          positives++;
      }
      int length = data[i].getAllElements().length;
      System.out.println(String.format("[%s] positives: %s, all: %s, value: %s", data[i].getAnnotation(),
          positives, length, positives / length));
    }


    double llSum = 0;
    int count = 0;
    for (int i = 0; i < data.length; i++) {
      DataSet dataSet = data[i];
      count += dataSet.getAllElements().length;
      for (int j = 0; j < dataSet.getAllElements().length; j++) {
        Sequence sequence = dataSet.getAllElements()[j];
        double sum = 1;
        for (int k = 0; k < data.length; k++) {
          sum += cl.getScore(sequence, k);
        }
        llSum += cl.getScore(sequence, i) / sum;
      }
    }
    System.out.println(Math.exp(llSum / count));
  }

  private void printDataSet(DataSet dataSet) {
    System.out.println(String.format("[%s] all: %s, length: %s", dataSet.getAnnotation(),
        dataSet.getAllElements().length, dataSet.getElementLength()));
  }

  private DataSet getDataSetWithSameLength(DataSet dataSet) throws EmptyDataSetException, WrongAlphabetException {
    Map<Integer, Integer> map = new HashMap<Integer, Integer>();
    for (Sequence sequence : dataSet.getAllElements()) {
      int length = sequence.getLength();
      Integer integer = map.get(length) == null ? 0 : map.get(length);
      map.put(length, integer + 1);
    }
    System.out.println(map);

    int max = 0;
    int index = map.keySet().iterator().next();
    for (Integer integer : map.keySet()) {
      if (map.get(integer) > max) {
        max = map.get(integer);
        index = integer;
      }
    }
    System.out.println("Max: " + index);

    List<Sequence> list = new ArrayList<Sequence>();
    for (Sequence sequence : dataSet.getAllElements()) {
      if (sequence.getLength() == index) {
        list.add(sequence);
      }
    }
    return new DataSet(dataSet.getAnnotation(), list.toArray(new Sequence[]{}));
  }
}
