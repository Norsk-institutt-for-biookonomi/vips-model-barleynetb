/*
 * Copyright (c) 2015 NIBIO <http://www.nibio.no/>. 
 * 
 * This file is part of BarleyNetBlotchModel.
 * BarleyNetBlotchModel is free software: you can redistribute it and/or modify
 * it under the terms of the NIBIO Open Source License as published by 
 * NIBIO, either version 1 of the License, or (at your option) any
 * later version.
 * 
 * BarleyNetBlotchModel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * NIBIO Open Source License for more details.
 * 
 * You should have received a copy of the NIBIO Open Source License
 * along with BarleyNetBlotchModel.  If not, see <http://www.nibio.no/licenses/>.
 * 
 */

package no.nibio.vips.model.barleynetblotchmodel;

import no.nibio.vips.util.DateMap;

/**
 * @copyright 2015 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class BarleyNetBlotchModelDataMatrix extends DateMap{
    
    // Daily precipitation
    public final static String RRD = "RRD";
    // Sum RRD for 14 days, starting 28 days ago
    public final static String RR28 = "RR28";
    // Number of days with RRD > 0.2 mm for 10 days, starting 24 days ago
    public final static String DG2410 = "DG2410";
    // Daily temperature
    public final static String TMD = "TMD";
    // Temperature sum, base temp = 0 Deg C
    public final static String TMDD0C = "TMDD0C";
    // Total calculated disease, not adjusted for observation
    public final static String X_UNADJUSTED = "X_UNADJUSTED";
    // Total disease, adjusted for observation
    public final static String X_ADJUSTED = "X_ADJUSTED";
    // Daily increment of X
    public final static String DELTA_X = "DELTA_X";
    // Threshold for disease
    public final static String Y = "Y";
    // Daily increment of Y
    public final static String DELTA_Y = "DELTA_Y";
    
    public BarleyNetBlotchModelDataMatrix()
    {
        super();
    }
}
