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
package de.hshannover.f4.trust.irondetectprocedures;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ibente
 */
public class TrendTest {
    
    double[] d1;
    double[] d2;
    int size = 10;
    
    public TrendTest() {
    }
    
    @Before
    public void setUp() {
        d1 = new double[size];
        d2 = new double[size];
        
        for (int i = 0; i < size; i++) {
            d1[i] = i;
            d2[i] = (3 + Math.random())* i - 3.5;
        }
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of test method, of class Trend.
     */
    @Test
    public void testTest() {
        System.out.println("test");
        Trend instance = new Trend();
        instance.setUp("23");
        double result = 0;
        
        for (int i = 0; i < size; i++) {
            result = instance.test(d1[i], d2[i]);
        }
        assertEquals(true, true);
    }
}
