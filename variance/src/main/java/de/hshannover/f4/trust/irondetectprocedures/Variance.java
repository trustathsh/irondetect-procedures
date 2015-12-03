/*
 * #%L
 * =====================================================
 *    _____                _     ____  _   _       _   _
 *   |_   _|_ __ _   _ ___| |_  / __ \| | | | ___ | | | |
 *     | | | '__| | | / __| __|/ / _` | |_| |/ __|| |_| |
 *     | | | |  | |_| \__ \ |_| | (_| |  _  |\__ \|  _  |
 *     |_| |_|   \__,_|___/\__|\ \__,_|_| |_||___/|_| |_|
 *                              \____/
 *  
 *  =====================================================
 * 
 * Hochschule Hannover 
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.f4.hs-hannover.de/
 * 
 * This file is part of variance, version 0.0.5, 
 * implemented by the Trust@HsH research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2011 - 2013 Trust@HsH
 * %%
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
 * #L%
 */
package de.hshannover.f4.trust.irondetectprocedures;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.log4j.Logger;

import de.hshannover.f4.trust.irondetect.model.Context;
import de.hshannover.f4.trust.irondetect.model.Feature;
import de.hshannover.f4.trust.irondetect.model.FeatureType;
import de.hshannover.f4.trust.irondetect.model.ProcedureResult;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper.Boundary;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper.DistanceType;
import de.hshannover.f4.trust.irondetect.procedure.Procedureable;


/**
 * A simple variance procedure based on Apache Commons Math
 * 
 * @author ib
 * 
 */
public class Variance implements Procedureable {

	private static Logger logger = Logger.getLogger(Variance.class);
	
	/**
	 * As specified in the policy
	 */
	private double expectedByPolicy;
	
	/**
	 * As trained by the training data
	 */
	private double trained;
	
	/**
	 * Flag that indicates if training was done.
	 */
	boolean trainingDone;
	
	public Variance() {
	}

	@Override
	public void setUp(String config) {
		logger.trace("setUp(" + config + ")");
		this.expectedByPolicy = Double.parseDouble(config);
		this.trainingDone = false;
	}

	@Override
	public ProcedureResult calculate(List<Feature> featureSet, List<Context> contextSet) {
		logger.trace("calculate");
		// get values from features
		double[] values = new double[featureSet.size()];
		for (int i = 0; i < featureSet.size(); i++) {
			if(featureSet.get(i).getType().getTypeId() == FeatureType.QUANTITIVE){
				values[i] = Double.valueOf(featureSet.get(i).getValue());
			}
		}
		// calculate variance
		double variance = StatUtils.variance(values);
		// dispatch between policy defined and trained mean, prefer trained mean
		double expected = this.trainingDone ? trained : expectedByPolicy;
		
		return ProcedureResultMapper.map(variance, expected, DistanceType.percent, Boundary.high, 25, 50);
	}
	
	@Override
	public void tearDown(String config) {
		logger.trace("tearDown()");
	}

	@Override
	public void train(List<Feature> featureSet, List<Context> contextSet, Calendar start, Calendar end) {
		logger.trace("start training ...");
		// get values from features
		// FIXME what if different types if features are used?
		double[] values = new double[featureSet.size()];
		for (int i = 0; i < featureSet.size(); i++) {
			if(featureSet.get(i).getType().getTypeId() == FeatureType.QUANTITIVE){
				values[i] = Double.valueOf(featureSet.get(i).getValue());
			}
		}
		
		// store as trained value
		this.trained = StatUtils.variance(values); 
		this.trainingDone = true;	
		logger.trace("training was done. value is " + this.trained);
	}
	
}

