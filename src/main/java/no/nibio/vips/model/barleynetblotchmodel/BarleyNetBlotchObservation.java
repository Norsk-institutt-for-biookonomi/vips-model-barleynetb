/*
 * Copyright (c) 2016 NIBIO <http://www.nibio.no/>. 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package no.nibio.vips.model.barleynetblotchmodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import no.nibio.vips.observation.ObservationImpl;

/**
 * @copyright 2016 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
@JsonDeserialize(as = BarleyNetBlotchObservation.class)
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
