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
 * This file is part of meanDaily, version 0.0.4, 
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

import de.hshannover.f4.trust.ContextParamType;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.log4j.Logger;

import de.hshannover.f4.trust.Feature;
import de.hshannover.f4.trust.FeatureType;
import de.hshannover.f4.trust.irondetect.model.Context;
import de.hshannover.f4.trust.irondetect.model.ProcedureResult;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper.Boundary;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper.DistanceType;
import de.hshannover.f4.trust.irondetect.procedure.Procedureable;
import de.hshannover.f4.trust.irondetect.util.Helper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import sun.misc.Compare;

/**
 * A simple mean procedure based on Apache Commons Math. This procedure is able
 * to reason about Context instances of type datetime. That is during training,
 * the procedure aims to calculate the mean on a daily basis rather than just
 * summing up all feature values.
 *
 * @author ib
 *
 */
public class MeanDaily implements Procedureable {

    private static Logger logger = Logger.getLogger(MeanDaily.class);
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

    public MeanDaily() {
    }

    @Override
    public void setUp(String config) {
        logger.trace("setUp(" + config + ")");
        this.expectedByPolicy = Double.parseDouble(config);
        this.trainingDone = false;
    }

    @Override
    public ProcedureResult calculate(List<Feature> featureSet, List<Context> contextSet) {
        logger.trace("calculate ...");
        // sort features by context parameter timestamp
        Collections.sort(featureSet, new FeatureComparatorForCtxp(ContextParamType.DATETIME));
        
        // count features that were measured today
        int count = 0;
        Calendar today = Calendar.getInstance();
        for (Feature f : featureSet) {
            Calendar featureDate = Helper.getXsdStringAsCalendar(f.getContextParameterByType(new ContextParamType(ContextParamType.DATETIME)).getValue());
            if (isSameDay(featureDate, today)) {
                count++;
            }
        }

        // dispatch between policy defined and trained mean, prefer trained mean
        double expected = this.trainingDone ? trained : expectedByPolicy;

        /**
         * Associate the calculated value with the expected value and return a
         * value within the range -1, +1. This is done as follows: - if in is at
         * most 25 percent greater or lesser, return -1 (hint is not fulfilled)
         * - if in is at most 50 percent greater or lesser, return 0 -
         * otherwise, return 1
         */
        return ProcedureResultMapper.map(count, expected, DistanceType.percent, Boundary.high, 25, 50);
    }

    /**
     *
     * @param featureSet
     * @param contextSet
     */
    @Override
    public void train(List<Feature> featureSet, List<Context> contextSet, Calendar startOfTraining, Calendar endOfTraining) {
        // sort features by context parameter timestamp
        Collections.sort(featureSet, new FeatureComparatorForCtxp(ContextParamType.DATETIME));
        // get the training time
        int trainingTime = durationInDays(startOfTraining, endOfTraining);

        logger.debug("start training on data from  " + trainingTime + " days");

        // how many days actually (when features were measured)?
        int days = 0;
        List<List<Feature>> featuresPerDay = new ArrayList<List<Feature>>();

        ArrayList<Feature> oneDay = new ArrayList<Feature>();
        featuresPerDay.add(oneDay);
        oneDay.add(featureSet.get(0));

        // FIXME may crash when less than 2 features are present
        for (int i = 1; i < featureSet.size(); i++) {
            Calendar prev = Helper.getXsdStringAsCalendar(featureSet.get(i - 1).getContextParameterByType(new ContextParamType(ContextParamType.DATETIME)).getValue());
            Calendar now = Helper.getXsdStringAsCalendar(featureSet.get(i).getContextParameterByType(new ContextParamType(ContextParamType.DATETIME)).getValue());
            
            if (isSameDay(prev, now)) {
                featuresPerDay.get(days).add(featureSet.get(i));
            } else {
                days++;
                oneDay = new ArrayList<Feature>();
                featuresPerDay.add(oneDay);
                oneDay.add(featureSet.get(i));
            }
        }

        // calculate mean based on training duration
        double[] values = new double[trainingTime];
        
        
        // count events
        for (int i = 0; i < featuresPerDay.size(); i++) {
            values[i] = featuresPerDay.get(i).size();
        }

        // store as trained mean value
        this.trained = StatUtils.mean(values);
        this.trainingDone = true;
        logger.trace("training was done. value is " + this.trained);
    }
    
    /**
     * Calculates the duration in days from start to end. Example:
     * 1.1.2011 to 5.1.2011 would return 5.
     * @param start
     * @param end
     * @return 
     */
    private int durationInDays(Calendar start, Calendar end){
        int duration = 0;
        if (start.after(end)) {
            logger.error("can not determine duration of training data. start date is after end date.");
            return duration;
        }
        // check years (leap years are ignored)
        duration += (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 365;
        // check days
        duration += end.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR) + 1;
        return duration;
    }
    
    /**
     * Checks if the two calendar objects refer to the same day. ignores timezones
     * @param c1
     * @param c2
     * @return 
     */
    private boolean isSameDay(Calendar c1, Calendar c2){
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void tearDown(String config) {
        logger.trace("tearDown()");
    }

    private class FeatureComparatorForCtxp implements Comparator<Feature> {

        /**
         * Indicates the context parameter type that is used for sorting
         */
        private final int ctxParameterType;

        /**
         *
         * @param ctxp TODO at this moment ignored
         */
        public FeatureComparatorForCtxp(int ctxParamType) {
            this.ctxParameterType = ContextParamType.DATETIME;
        }

        @Override
        public int compare(Feature f1, Feature f2) {
            switch (this.ctxParameterType) {
                case ContextParamType.DATETIME:
                    Calendar contextF1 = Helper.getXsdStringAsCalendar(f1.getContextParameterByType(new ContextParamType(ContextParamType.DATETIME)).getValue());
                    Calendar contextF2 = Helper.getXsdStringAsCalendar(f2.getContextParameterByType(new ContextParamType(ContextParamType.DATETIME)).getValue());

                    if (contextF1.before(contextF2)) {
                        return -1;
                    } else if (contextF1.after(contextF2)) {
                        return 1;
                    } else {
                        return 0;
                    }
                default:
                    throw new UnsupportedOperationException("Not supported yet.");
            }
        }
    }
}
