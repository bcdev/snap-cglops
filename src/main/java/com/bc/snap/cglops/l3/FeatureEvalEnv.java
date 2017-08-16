/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.snap.cglops.l3;


import org.esa.snap.binning.Vector;
import org.esa.snap.core.jexp.EvalEnv;

import java.util.HashMap;

/**
 * Environment for evaluating expressions that use a record's attribute values.
 *
 * @author Norman Fomferra
 */
class FeatureEvalEnv implements EvalEnv {

    private final HashMap<String, Object> env;
    private final String[] variableNames;

    public FeatureEvalEnv(String[] variableNames) {
        this.variableNames = variableNames;
        this.env = new HashMap<>(31);
    }

    public void setContext(Vector inputVector) {
        for (int i = 0; i < variableNames.length; i++) {
            String name = variableNames[i];
            Object value = inputVector.get(i);
            env.put(name, value);
        }
    }

    public Object getValue(String name) {
        return env.get(name);
    }
}
