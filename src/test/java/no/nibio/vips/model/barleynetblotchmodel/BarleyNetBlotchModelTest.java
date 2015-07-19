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

import no.nibio.vips.model.barleynetblotchmodel.BarleyNetBlotchModel;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import static junit.framework.Assert.fail;
import no.nibio.vips.entity.ModelConfiguration;
import no.nibio.vips.entity.Result;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.model.ModelId;
import no.nibio.vips.observation.Observation;
import no.nibio.vips.observation.ObservationImpl;
import no.nibio.vips.pestmanagement.Spraying;
import no.nibio.vips.pestmanagement.SprayingImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author treinar
 */
public class BarleyNetBlotchModelTest {
    
    public BarleyNetBlotchModelTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getResult method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetResult() throws Exception {
        System.out.println("getResult");
        ModelConfiguration config = this.getConfiguration("/weatherdata_boe_2014.json");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        config.setConfigParameter("sowingDate", "2014-04-01");
        // Using "Tyra" as barley variant
        config.setConfigParameter("resistanceFactor", "5");
        config.setConfigParameter("conversionCorrectionFactor", 5);
        config.setConfigParameter("sameCropAsLastSeason", "true");
        ObservationImpl obs = new ObservationImpl();
        obs.setObservedValue(75.0);
        obs.setDenominator(100);
        obs.setName("Byggbrunflekk");
        TimeZone tz = TimeZone.getTimeZone("Europe/Oslo");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(tz);
        
        obs.setTimeOfObservation(format.parse("2014-06-10"));
        config.setConfigParameter("observation", obs);
        
        // Setting spraying
        SprayingImpl spraying = new SprayingImpl();
        spraying.setPesticideName("Killer XXX");
        spraying.setSprayingDate(format.parse("2014-06-05"));
        spraying.setSprayingEffectFactor(0.5);
        config.setConfigParameter("spraying", spraying);
        
        instance.setConfiguration(config);
        List<Result> result = instance.getResult();
        assertNotNull(result);
        
    }

    /**
     * Test of getModelId method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelId() {
        System.out.println("getModelId");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        ModelId expResult = new ModelId("BARLEYNETB");
        ModelId result = instance.getModelId();
        assertEquals(expResult.toString(), result.toString());
    }

    /**
     * Test of getModelName method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelName_0args() {
        System.out.println("getModelName");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getModelName();
        assertNotNull(result);
    }

    /**
     * Test of getModelName method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelName_String() {
        System.out.println("getModelName");
        String language = "";
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getModelName(language);
        assertNotNull(result);
    }

    /**
     * Test of getLicense method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetLicense() {
        System.out.println("getLicense");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getLicense();
        assertNotNull(result);
    }

    /**
     * Test of getCopyright method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetCopyright() {
        System.out.println("getCopyright");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getCopyright();
        assertNotNull(result);
    }

    /**
     * Test of getModelDescription method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelDescription_0args() {
        System.out.println("getModelDescription");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getModelDescription();
        assertNotNull(result);
    }

    /**
     * Test of getModelDescription method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelDescription_String() {
        System.out.println("getModelDescription");
        String language = "";
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getModelDescription(language);
        assertNotNull(result);
    }

    /**
     * Test of getWarningStatusInterpretation method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetWarningStatusInterpretation_0args() {
        System.out.println("getWarningStatusInterpretation");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getWarningStatusInterpretation();
        assertNotNull(result);
    }

    /**
     * Test of getWarningStatusInterpretation method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetWarningStatusInterpretation_String() {
        System.out.println("getWarningStatusInterpretation");
        String language = "";
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getWarningStatusInterpretation(language);
        assertNotNull(result);
    }

    /**
     * Test of getModelUsage method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelUsage_0args() {
        System.out.println("getModelUsage");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getModelUsage();
        assertNotNull(result);
    }

    /**
     * Test of getModelUsage method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetModelUsage_String() {
        System.out.println("getModelUsage");
        String language = "";
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getModelUsage(language);
        assertNotNull(result);
    }

    /**
     * Test of getSampleConfig method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testGetSampleConfig() {
        System.out.println("getSampleConfig");
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        String result = instance.getSampleConfig();
        assertNotNull(result);
    }

    /**
     * Test of setConfiguration method, of class BarleyNetBlotchModel.
     */
    @Test
    public void testSetConfiguration() throws Exception {
        System.out.println("setConfiguration");
        ModelConfiguration config = null;
        BarleyNetBlotchModel instance = new BarleyNetBlotchModel();
        //instance.setConfiguration(config);
        
    }

    
    private ModelConfiguration getConfiguration(String fileName)
    {
        try {
            ModelConfiguration config = new ModelConfiguration();
            config.setModelId(BarleyNetBlotchModel.MODEL_ID.toString());
            
            config.setConfigParameter("timeZone", "Europe/Oslo");
            BufferedInputStream inputStream = new BufferedInputStream(this.getClass().getResourceAsStream(fileName));
            JsonFactory f = new MappingJsonFactory();
            JsonParser jp = f.createParser(inputStream);
            JsonNode all = jp.readValueAsTree();
            //List<WeatherObservation> observations = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            List<WeatherObservation> observations = mapper.convertValue(all, new TypeReference<List<WeatherObservation>>(){});

            Date firstDate = null;
            Date lastDate = null;
            /*if(all.isArray())
            {
                for(JsonNode node : all){
                    Date timeMeasured = (Date)mapper.convertValue(node.get("timeMeasured").asText(), new TypeReference<Date>(){});
                    if(firstDate == null || firstDate.compareTo(timeMeasured) > 0)
                    {
                        firstDate = timeMeasured;
                    }
                    if(lastDate == null || lastDate.compareTo(timeMeasured) < 0)
                    {
                        lastDate = timeMeasured;
                    }
                    //System.out.println(node.toString());
                    WeatherObservation observation = new WeatherObservation();
                    observation.setTimeMeasured(timeMeasured);
                    observation.setLogIntervalId(node.get("logIntervalId").asInt());
                    observation.setElementMeasurementTypeId(node.get("elementMeasurementTypeId").asText());
                    observation.setValue(node.get("value").asDouble());
                    observations.add(observation);
                }

            }
            else
            {
                fail("Data input from file is not a JSON array");
            }
                    */
            config.setConfigParameter("observations", observations);

            
            return config;
        } catch (IOException ex) {
            return null;
        } 
    }
}
