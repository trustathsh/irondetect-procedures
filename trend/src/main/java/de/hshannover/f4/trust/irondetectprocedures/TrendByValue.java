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
 * This file is part of trend, version 0.0.4, 
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;

import de.hshannover.f4.trust.irondetect.model.Context;
import de.hshannover.f4.trust.irondetect.model.ContextParamType;
import de.hshannover.f4.trust.irondetect.model.ContextParameter;
import de.hshannover.f4.trust.irondetect.model.Feature;
import de.hshannover.f4.trust.irondetect.model.ProcedureResult;
import de.hshannover.f4.trust.irondetect.procedure.ProcedureResultMapper;
import de.hshannover.f4.trust.irondetect.procedure.Procedureable;
import de.hshannover.f4.trust.irondetect.util.Helper;

/*
 * Copyright 2012 Trust@FHH.
 *
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
 */
/**
 *
 * @author ibente
 */
public class TrendByValue implements Procedureable {

    private static Logger logger = Logger.getLogger(TrendByValue.class);
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
    /**
     *
     */
    SimpleRegression simpleRegression;
    /**
     * IO
     */
    FileWriter fw;
    private double slope, intercept, slopeStdErr, interceptStdErr, x, y, yPredicted;
    private int index = 0;
    private int predictionRange = 10;
    private ArrayList<Double> predictionResult = new ArrayList<Double>();
    private ArrayList<Double> predictionTime = new ArrayList<Double>();
    private long startTimeStamp;
    private boolean startTimeStampAvailable;

    private void setStartTimeStamp(long ts) {
        if (!this.startTimeStampAvailable) {
            this.startTimeStamp = ts;
            this.startTimeStampAvailable = true;
        }
    }

    private long getDeltaTime(long currentTime) {
        return currentTime - this.startTimeStamp;
    }

    public TrendByValue() {
    }

    @Override
    public void setUp(String config) {
        logger.trace("setUp(" + config + ")");
        this.expectedByPolicy = Double.parseDouble(config);
        this.trainingDone = false;
        this.simpleRegression = new SimpleRegression();
        this.slope = 0;
        this.intercept = 0;
        this.slopeStdErr = 0;
        this.interceptStdErr = 0;
        this.yPredicted = 0;

        try {
            fw = new FileWriter(new File("trendResult.txt"), false);
            fw.write("##;##" + System.getProperty("line.separator"));
            fw.write("idx;x;y;slope;intercept;slopeStdErr;interceptStdErr;yPredicted" + System.getProperty("line.separator"));
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(TrendByValue.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public ProcedureResult calculate(List<Feature> featureSet, List<Context> contextSet) {
        Feature feature;
        double timestamp = 0;

        logger.trace("calculate ...");
        // sort features by context parameter timestamp
        Collections.sort(featureSet, new FeatureComparatorForCtxp(ContextParamType.DATETIME));

        if (featureSet.isEmpty()) {
            return new ProcedureResult(0);
        }

        feature = featureSet.get(featureSet.size() - 1);

        // get timestamp in UNIX time
        ContextParameter contextParam = feature.getContextParameterByType(new ContextParamType(ContextParamType.DATETIME));
        Calendar cal = Helper.getXsdStringAsCalendar(contextParam.getValue());
        timestamp = cal.getTime().getTime() / 1000L;

        if (this.startTimeStampAvailable == false) {
            setStartTimeStamp((long) timestamp);
        }


        this.x = getDeltaTime((long) timestamp);
        this.y = new Double(feature.getValue()).doubleValue() / 1000;

        // update the regression calculation
        this.simpleRegression.addData(this.x, this.y);
        this.intercept = this.simpleRegression.getIntercept();
        this.slope = this.simpleRegression.getSlope();
        this.interceptStdErr = this.simpleRegression.getInterceptStdErr();
        this.slopeStdErr = this.simpleRegression.getSlopeStdErr();
        double predTime = this.x+this.predictionRange;
        this.yPredicted = this.simpleRegression.predict(predTime);
        this.predictionResult.add(this.yPredicted);
        this.predictionTime.add(predTime);
        // log the results for gnuplot
        logResults();

        // dispatch between policy defined and trained mean, prefer trained mean
        double expected = this.trainingDone ? trained : expectedByPolicy;

        /**
         * Associate the calculated value with the expected value and return a
         * value within the range -1, +1. This is done as follows: - if in is at
         * most 50 percent greater or lesser, return -1 (hint is not fulfilled)
         * - if in is at most 100 percent greater or lesser, return 0 -
         * otherwise, return 1
         */
        return ProcedureResultMapper.map(this.simpleRegression.getSlope(), expected, ProcedureResultMapper.DistanceType.percent, ProcedureResultMapper.Boundary.high, 50, 100);
    }

    public double test(double d1, double d2) {
        logger.trace("test ...");
        // sort features by context parameter timestamp
        // update the regression calculation
        this.simpleRegression.addData(d1, d2);

        this.x = d1;
        this.y = d2;
        this.intercept = this.simpleRegression.getIntercept();
        this.slope = this.simpleRegression.getSlope();
        this.interceptStdErr = this.simpleRegression.getInterceptStdErr();
        this.slopeStdErr = this.simpleRegression.getSlopeStdErr();
        this.yPredicted = this.simpleRegression.predict(this.index+this.predictionRange);

        logResults();

        return this.simpleRegression.getSlope();
    }

    /**
     *
     * @param featureSet
     * @param contextSet
     */
    @Override
    public void train(List<Feature> featureSet, List<Context> contextSet, Calendar startOfTraining, Calendar endOfTraining) {
        logger.trace("Training not supported for this procedure");
    }

    private void logResults() {
        try {
            if (this.predictionResult.size() < this.predictionRange) {
                fw.write(this.index++ + ";" + this.x + ";" + this.y + ";" + this.slope + ";" + this.intercept + ";" + this.slopeStdErr + ";" + this.interceptStdErr + ";" + "0" + System.getProperty("line.separator"));
            } else {
                fw.write(this.index++ + ";" + this.x + ";" + this.y + ";" + this.slope + ";" + this.intercept + ";" + this.slopeStdErr + ";" + this.interceptStdErr +  ";" + this.predictionResult.get(this.predictionResult.size()-this.predictionRange) +  System.getProperty("line.separator"));
            }
            fw.flush();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(TrendByValue.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Calculates the duration in days from start to end. Example: 1.1.2011 to
     * 5.1.2011 would return 5.
     *
     * @param start
     * @param end
     * @return
     */
    private int durationInDays(Calendar start, Calendar end) {
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
     * Checks if the two calendar objects refer to the same day. ignores
     * timezones
     *
     * @param c1
     * @param c2
     * @return
     */
    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void tearDown(String config) {
        logger.trace("tearDown()");
        try {
            fw.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(TrendByValue.class.getName()).log(Level.SEVERE, null, ex);
        }
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
