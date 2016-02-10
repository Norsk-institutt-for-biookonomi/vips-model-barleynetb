/*
 * Copyright (c) 2016 NIBIO <http://www.nibio.no/>. 
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import no.nibio.vips.observation.ObservationImpl;

/**
 * @copyright 2016 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class BarleyNetBlotchObservation extends ObservationImpl{

    public Double getPercentInfectedLeaves()
    {
        try
        {
            ObjectMapper m = new ObjectMapper();
            JsonNode node = m.readTree(this.getObservationData());
            return node.get("percentInfectedLeaves").asDouble();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            return null;
        }
        
    }
    
    public void setPercentInfectedLeaves(Double percentInfectedLeaves){
        this.setObservationData("{\"percentInfectedLeaves\": " + percentInfectedLeaves+ "}");
    }
}
