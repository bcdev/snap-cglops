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



import org.esa.snap.binning.VariableContext;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.impl.DefaultNamespace;

import java.util.HashSet;
import java.util.Set;

/**
 * A namespace that is constructed from the variable context
 */
class VariableContextNamespace implements Namespace {

    private final DefaultNamespace defaultNamespace;
    private final Set<String> variableNameSet;
    private final String[] variableNames;

    public VariableContextNamespace(VariableContext varCtx) {
        final int variableCount = varCtx.getVariableCount();
        this.variableNameSet = new HashSet<>(variableCount);
        this.variableNames = new String[variableCount];
        for (int i = 0; i < variableCount; i++) {
            String name = varCtx.getVariableName(i);
            variableNames[i] = name;
            variableNameSet.add(name);
        }
        this.defaultNamespace = new DefaultNamespace();
    }

    public String[] getVariableNames() {
        return variableNames;
    }

    @Override
    public Function resolveFunction(String name, Term[] args) {
        return defaultNamespace.resolveFunction(name, args);
    }

    @Override
    public Symbol resolveSymbol(String name) {
        Symbol symbol = defaultNamespace.resolveSymbol(name);
        if (symbol != null) {
            return symbol;
        }
        if (variableNameSet.contains(name)) {
            symbol = new FeatureSymbol(name);
            defaultNamespace.registerSymbol(symbol);
            return symbol;
        }
        return null;
    }

}
