/*
 * Copyright (c) 2019 NIBIO <http://www.nibio.no/>. 
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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import no.nibio.vips.entity.ModelConfiguration;
import no.nibio.vips.entity.Result;
import no.nibio.vips.entity.ResultImpl;
import no.nibio.vips.entity.WeatherObservation;
import no.nibio.vips.i18n.I18nImpl;
import no.nibio.vips.model.ConfigValidationException;
import no.nibio.vips.model.Model;
import no.nibio.vips.model.ModelExcecutionException;
import no.nibio.vips.model.ModelId;
import no.nibio.vips.pestmanagement.Spraying;
import no.nibio.vips.pestmanagement.SprayingImpl;
import no.nibio.vips.util.CommonNamespaces;
import no.nibio.vips.util.InvalidAggregationTypeException;
import no.nibio.vips.util.ModelUtil;
import no.nibio.vips.util.WeatherElements;
import no.nibio.vips.util.WeatherObservationListException;
import no.nibio.vips.util.WeatherUtil;

/**
 * @copyright 2019 <a href="http://www.nibio.no/">NIBIO</a>
 * @author Tor-Einar Skog <tor-einar.skog@nibio.no>
 */
public class BarleyNetBlotchModel extends I18nImpl implements Model{
    
    public final static ModelId MODEL_ID = new ModelId("BARLEYNETB");
    
    private TimeZone timeZone;
    private Date sowingDate;
    private BarleyNetBlotchModelDataMatrix dataMatrix;
    
    public final static Double DEFAULT_INITIAL_DISEASE_VALUE = 0.3;
    private Double initialDiseaseValue;
    public final static Double DEFAULT_RESISTANCE_FACTOR = 5.0;
    private Double resistanceFactor;
    public final static Double DEFAULT_CONVERSION_CORRECTION_FACTOR = 5.0;
    private Double conversionCorrectionFactor;
    private BarleyNetBlotchObservation observation;
    private Boolean sameCropAsLastSeason;
    private Boolean plowed;
    private Spraying spraying;
    
    private final Integer sprayEffectDurationDays = 21;
    private final Double initialThresholdValue = 0.5;
    private final Double RR28Threshold = 21.0; // Spec says 2.0, but old VIPS impl states 21.0
    private final Integer DG2410Threshold = 3;
    // We stop disease calculation when we pass this day degree limit
    private final Double dayDegreeLimit = 1500.0;
    
    private final ModelUtil modelUtil;
    
    public BarleyNetBlotchModel()
    {
        // Telling super class where to find resource files
        super("no.nibio.vips.model.barleynetblotchmodel.texts");
        
        this.modelUtil = new ModelUtil();
    }

    @Override
    public List<Result> getResult() throws ModelExcecutionException {

        this.calculateWeatherData();
        //System.out.println(this.dataMatrix.toCSV());
        this.calculateInitialDiseaseValue();
        this.calculateDisease(this.initialDiseaseValue, BarleyNetBlotchModelDataMatrix.X_ADJUSTED);
        this.calculateThreshold();
        //System.out.println(this.dataMatrix.toCSV());
        
        // Infection assessment starts 28 days after 
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(this.sowingDate);
        cal.add(Calendar.DATE, 28);
        Date currentDate = cal.getTime();
        
        Double TMDD0C;
        List<Result> results = new ArrayList<>();
        Double xAdjusted = 0.0;
        do
        {
            TMDD0C = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMDD0C);
            
            xAdjusted = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.X_ADJUSTED);
            Double y = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.Y);
            Integer warningStatus = Result.WARNING_STATUS_NO_WARNING;
            if(xAdjusted != null && y != null)
            {
                if(xAdjusted < y)
                {
                    warningStatus = Result.WARNING_STATUS_NO_RISK;
                }
                if(xAdjusted.equals(y))
                {
                    warningStatus = Result.WARNING_STATUS_MINOR_RISK;
                }
                if(xAdjusted > y)
                {
                    warningStatus = Result.WARNING_STATUS_HIGH_RISK;
                }
                
                Result result = new ResultImpl();
                result.setValidTimeStart(currentDate);
                result.setWarningStatus(warningStatus);
                result.setValue(this.getModelId().toString(), BarleyNetBlotchModelDataMatrix.X_ADJUSTED, String.valueOf(xAdjusted));
                result.setValue(this.getModelId().toString(), BarleyNetBlotchModelDataMatrix.Y, String.valueOf(y));
                // Adding weather data
                result.setValue(CommonNamespaces.NS_WEATHER, BarleyNetBlotchModelDataMatrix.TMD, String.valueOf(this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMD)));
                result.setValue(CommonNamespaces.NS_WEATHER, BarleyNetBlotchModelDataMatrix.TMDD0C, String.valueOf(this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMDD0C)));
                result.setValue(CommonNamespaces.NS_WEATHER, BarleyNetBlotchModelDataMatrix.RRD, String.valueOf(this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.RRD)));
                result.setValue(CommonNamespaces.NS_WEATHER, BarleyNetBlotchModelDataMatrix.RR28, String.valueOf(this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.RR28)));
                result.setValue(this.getModelId().toString(), BarleyNetBlotchModelDataMatrix.DG2410, String.valueOf(this.dataMatrix.getParamIntValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.DG2410)));
                results.add(result);
            }
            
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        while(TMDD0C != null && TMDD0C < this.dayDegreeLimit && xAdjusted >= 0.0);
        
        return results;
    }

    @Override
    public ModelId getModelId() {
        return BarleyNetBlotchModel.MODEL_ID;
    }

    @Override
    public String getModelName() {
        return this.getModelName(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getModelName(String language) {
        return this.getText("name", language);
    }

    @Override
    public String getLicense() {
        return "Copyright (c) 2015 NIBIO <http://www.nibio.no/>. \n" +
            "\n" +
            "This file is part of BarleyNetBlotchModel. \n" + 
            "BarleyNetBlotchModel is free software: you can redistribute it and/or modify \n" + 
            "it under the terms of the NIBIO Open Source License as published by \n" + 
            "NIBIO, either version 1 of the License, or (at your option) any \n" +
            "later version. \n" +
            "\n" +
            "BarleyNetBlotchModel is distributed in the hope that it will be useful, \n" +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of \n" +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the \n" +
            "NIBIO Open Source License for more details. \n" +
            "" +
            "You should have received a copy of the NIBIO Open Source License \n" +
            "along with BarleyNetBlotchModel.  If not, see <http://www.nibio.no/licenses/>. \n";
    }

    @Override
    public String getCopyright() {
        return "(c) 2015 NIBIO (http://www.nibio.no/). Contact: post@nibio.no";
    }

    @Override
    public String getModelDescription() {
        return this.getModelDescription(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getModelDescription(String language) {
        try
        {
            return this.modelUtil.getTextWithBase64EncodedImages(this.getText("description", language), this.getClass());
        }
        catch(IOException ex)
        {
            return this.getText("description", language);
        }
    }

    @Override
    public String getWarningStatusInterpretation() {
        return this.getWarningStatusInterpretation(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getWarningStatusInterpretation(String language) {
        return this.getText("warningStatusInterpretation", language);
    }

    @Override
    public String getModelUsage() {
        return this.getModelUsage(Model.DEFAULT_LANGUAGE);
    }

    @Override
    public String getModelUsage(String language) {
        return this.getText("modelUsage", language);
    }

    @Override
    public String getSampleConfig() {
        return  "{\n" +
                "\t\"loginInfo\":{\n" +
                "\t\t\"username\":\"example\",\n" +
                "\t\t\"password\":\"example\"\n" +
                "\t},\n" +
                "\t\"modelId\":\"" + MODEL_ID.toString() + "\",\n" +
                "\t\"configParameters\":{\n" +
                "\t\t\"timeZone\":\"Europe/Oslo\",\n" +
                "\t\t\"sowingDate\":\"2012-04-15\",\n" +
                "\t\t\"sameCropAsLastSeason\":\"true\",\n" +
                "\t\t\"plowed\":\"false\",\n" +
                "\t\t\"resistanceFactor\":\"5.0\",\n" +
                "\t\t\"conversionCorrectionFactor\":\"5.0\",\n" +
                "\t\t\"observation\":{\n" +
                "\t\t\t\"timeOfObservation\": \"2012-05-20T12:00:00+02:00\",\n" +
                "\t\t\t\"latitude\": \"71.0\",\n" +
                "\t\t\t\"longitude\": \"10.0\",\n" +
                "\t\t\t\"observedValue\": \"25.0\",\n" +
                "\t\t\t\"denominator\": \"100.0\",\n" +
                "\t\t\t\"name\": \"Pyrenophora teres\"\n" +
                "\t\t},\n" +
                "\t\t\"spraying\":{\n" +
                "\t\t\t\"sprayingDate\": \"2012-06-01\",\n" +
                "\t\t\t\"pesticideName\": \"ACME\",\n" +
                "\t\t\t\"sprayingEffectFactor\": \"0.5\"\n" +
                "\t\t},\n" +
                "\t\t\"observations\":[\n" +
                "\t\t{\n" +
                "\t\t\t\t\"timeMeasured\": \"2012-03-25T00:00:00+02:00\",\n" +
                "\t\t\t\t\"elementMeasurementTypeId\":\"TM\",\n" +
                "\t\t\t\t\"logIntervalId\":1,\n" +
                "\t\t\t\t\"value\":2.937\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\t\"timeMeasured\": \"2012-03-25T00:00:00+02:00\",\n" +
                "\t\t\t\t\"elementMeasurementTypeId\":\"RR\",\n" +
                "\t\t\t\t\"logIntervalId\":1,\n" +
                "\t\t\t\t\"value\":0\n" +
                "\t\t}\n" +
                "\t\t]\n" +
                "\t}\n" +
                "}\n";
    }

    @Override
    public void setConfiguration(ModelConfiguration config) throws ConfigValidationException {
        // Init weather data collections
        // Hourly temperatures
        List<WeatherObservation> TM  = new ArrayList<>();
        // Hourly precipitation
        List<WeatherObservation> RR  = new ArrayList<>();
        
        // Object deserialization tool
        ObjectMapper mapper = new ObjectMapper();
        
        // Set time zone
        this.timeZone = TimeZone.getTimeZone((String) config.getConfigParameter("timeZone"));
        // Set sowing date
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(this.timeZone);
        mapper.setDateFormat(dateFormat);
        this.sowingDate = mapper.convertValue(config.getConfigParameter("sowingDate"), Date.class);
                
        // Set spraying (if any)
        this.spraying = mapper.convertValue(config.getConfigParameter("spraying"), SprayingImpl.class);
        // Do we have same crop this year as last season? Affects initial value.
        this.sameCropAsLastSeason = mapper.convertValue(config.getConfigParameter("sameCropAsLastSeason"), Boolean.class);
        // Did we plough?
        this.plowed = config.getConfigParameter("plowed") == null ? 
                false
                : mapper.convertValue(config.getConfigParameter("plowed"), Boolean.class);
        
        if(config.getConfigParameter("resistanceFactor") != null)
        {
            this.resistanceFactor = mapper.convertValue(config.getConfigParameter("resistanceFactor"), Double.class);
        }
        else
        {
            this.resistanceFactor = BarleyNetBlotchModel.DEFAULT_RESISTANCE_FACTOR;
        }
        
        if(config.getConfigParameter("conversionCorrectionFactor") != null)
        {
            this.conversionCorrectionFactor = mapper.convertValue(config.getConfigParameter("conversionCorrectionFactor"), Double.class);
        }
        else
        {
            this.conversionCorrectionFactor = BarleyNetBlotchModel.DEFAULT_CONVERSION_CORRECTION_FACTOR;
        }
        
        // Back to parsing dates as timestamps
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
        
        // Set observation (if any)
        this.observation = mapper.convertValue(config.getConfigParameter("observation"), BarleyNetBlotchObservation.class);
        
        // Importing weather data, creating collections
        // Can accept both hourly and daily data
        WeatherUtil wUtil = new WeatherUtil();
        this.dataMatrix = new BarleyNetBlotchModelDataMatrix();
        
        List<WeatherObservation> observations = this.modelUtil.extractWeatherObservationList(config.getConfigParameter("observations"));
        
        for(WeatherObservation o:observations)
        {
            //System.out.println(o);
            switch(o.getElementMeasurementTypeId())
            {
                case WeatherElements.TEMPERATURE_MEAN:
                    if(o.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_1H))
                    {
                        TM.add(o);
                    }else {
                        o.setTimeMeasured(wUtil.pragmaticAdjustmentToMidnight(o.getTimeMeasured(), timeZone));
                        this.dataMatrix.setParamDoubleValueForDate(o.getTimeMeasured(), BarleyNetBlotchModelDataMatrix.TMD, o.getValue());
                    }break;
                case WeatherElements.PRECIPITATION:
                    if(o.getLogIntervalId().equals(WeatherObservation.LOG_INTERVAL_ID_1H))
                    {
                        RR.add(o);
                    }else {
                        o.setTimeMeasured(wUtil.pragmaticAdjustmentToMidnight(o.getTimeMeasured(), timeZone));
                        this.dataMatrix.setParamDoubleValueForDate(o.getTimeMeasured(), BarleyNetBlotchModelDataMatrix.RRD, o.getValue());
                    }break;
                default:
                    // Keep calm and continue importing data
                    break;
            }
        }
        
        // If we've received hourly weather data, create and store daily values
        // Air temperature
        if(dataMatrix.getFirstDateWithParameterValue(BarleyNetBlotchModelDataMatrix.TMD) == null)
        {
            try {
                List<WeatherObservation> dailyTemperatures = new WeatherUtil().getAggregatedDailyValues(TM,
                        this.timeZone,
                        15,
                        WeatherUtil.AGGREGATION_TYPE_AVERAGE, 0);
                if(dailyTemperatures != null)
                {
                    for(WeatherObservation obs:dailyTemperatures)
                    {
                        this.dataMatrix.setParamDoubleValueForDate(obs.getTimeMeasured(), BarleyNetBlotchModelDataMatrix.TMD, obs.getValue());
                    }
                }
            } catch (WeatherObservationListException | InvalidAggregationTypeException ex) {
                throw new ConfigValidationException(ex.getMessage());
            }
        }
        // Precipitation
        if(dataMatrix.getFirstDateWithParameterValue(BarleyNetBlotchModelDataMatrix.RRD) == null)
        {
            try {
                List<WeatherObservation> dailyTemperatures = new WeatherUtil().getAggregatedDailyValues(RR,
                        this.timeZone,
                        15,
                        WeatherUtil.AGGREGATION_TYPE_SUM, 0);
                if(dailyTemperatures != null)
                {
                    for(WeatherObservation obs:dailyTemperatures)
                    {
                        this.dataMatrix.setParamDoubleValueForDate(obs.getTimeMeasured(), BarleyNetBlotchModelDataMatrix.RRD, obs.getValue());
                    }
                }
            } catch (WeatherObservationListException | InvalidAggregationTypeException ex) {
                throw new ConfigValidationException(ex.getMessage());
            }
        }
        
        Date lastWeatherDataDate = dataMatrix.getLastDateWithParameterValue(BarleyNetBlotchModelDataMatrix.TMD);
        if(lastWeatherDataDate == null)
        {
            throw new ConfigValidationException("Apparently missing all daily data for mean temperature");
        }
        else
        {
            if(lastWeatherDataDate.getTime() - sowingDate.getTime() < (18 * 24 * 60 * 60 * 1000))
            {
                throw new ConfigValidationException("Must have at least 18 days of weather data after sowing date");
            }
        }
    }

    private void calculateWeatherData() {
        // Calculate RR28
        // Calculate DG2410
        // Calculate Day Degrees (base temp=0)
        
        Date currentDate = this.sowingDate;
        Calendar cal = Calendar.getInstance(this.timeZone);
        Double TMDD0C = 0.0;
        int counter = 0;
        Double RR28 = 0.0;
        Integer DG2410 = 0;
        while(TMDD0C <= this.dayDegreeLimit)
        {
            counter++;
            Double TMD = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMD);
            Double RRD = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.RRD);
            
            // If we're out of weather data before we reach a day degree sum of 900, we stop
            if(TMD == null || RRD == null)
            {
                break;
            }
            
            if(TMD > 0)
            {
                TMDD0C += TMD;
            }
            this.dataMatrix.setParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMDD0C, TMDD0C);
            
            // RR28
            RR28 += this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.RRD);
            if(counter > 14)
            {
                // Substract the last one
                cal.setTime(currentDate);
                cal.add(Calendar.DATE, -14);
                RR28 -= this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), BarleyNetBlotchModelDataMatrix.RRD);
            }
            if(counter >= 14)
            {
                cal.setTime(currentDate);
                cal.add(Calendar.DATE, 15);
                this.dataMatrix.setParamDoubleValueForDate(cal.getTime(), BarleyNetBlotchModelDataMatrix.RR28, RR28);
            }
            
            // DG2410
            DG2410 += this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.RRD) > 0.2 ?
                    1 : 0;
            if(counter > 10)
            {
                // Substract the last one
                cal.setTime(currentDate);
                cal.add(Calendar.DATE, -10);
                if(this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), BarleyNetBlotchModelDataMatrix.RRD) > 0.2)
                {
                    DG2410--;
                }
            }
            if(counter >= 10)
            {
                cal.setTime(currentDate);
                cal.add(Calendar.DATE, 11);
                this.dataMatrix.setParamIntValueForDate(cal.getTime(), BarleyNetBlotchModelDataMatrix.DG2410, DG2410);
            }
            
            //System.out.println(this.dataMatrix.toCSV());
            // Moving on
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        //System.out.println(this.dataMatrix.toCSV());
    }

    /**
     * If no observation made, don't change the default value
     * If the observation is made AFTER spraying, don't change the default value
     * Otherwise, deduct by (see DOCS)
     */
    private void calculateInitialDiseaseValue() throws ModelExcecutionException
    {
        if(     this.observation == null 
                || this.observation.getPercentInfectedLeaves() < 0
                || (this.spraying != null && this.observation.getTimeOfObservation().compareTo(this.spraying.getSprayingDate()) > 0)
        )
        {
            this.initialDiseaseValue = this.getUnadjustedInitialDiseaseValue();
        }
        else
        {
            
            // We have to calculate total unadjusted disease first
            this.calculateDisease(this.getUnadjustedInitialDiseaseValue(),BarleyNetBlotchModelDataMatrix.X_UNADJUSTED);

            //System.out.println(this.dataMatrix.toCSV());
            // Then check the unadjusted disease at observation date,
            // compare to observation, find correction value and then 
            // Adjust the initial disease value based on the relative difference 
            // between calculation and observation
            WeatherUtil wUtil = new WeatherUtil();
            Date observationDate = wUtil.normalizeToExactDate(this.observation.getTimeOfObservation(), this.timeZone);
            Double calculatedX = this.dataMatrix.getParamDoubleValueForDate(observationDate, BarleyNetBlotchModelDataMatrix.X_UNADJUSTED);
            
            // If calculatedX is null, this might be because the day degree limit has passed
            // We check for this and issue error message if necessary
            if(calculatedX == null)
            {
                Date lastDayOfModelCalculation = this.getLastDateOfModelCalculation();
                if(lastDayOfModelCalculation.compareTo(observationDate) <= 0)
                {
                    throw new ModelExcecutionException("Observation date is past period of model development calculation (day degrees exceeding " + this.dayDegreeLimit + ", ending at " + lastDayOfModelCalculation + ")");
                }
            }
            
            Double infectedLeaves = this.observation.getPercentInfectedLeaves();
            Integer countedLeaves = 100;
            Double observedInfectionLeafAreaPercent = this.getInfectionLeafAreaPercent(infectedLeaves, countedLeaves);
            Double adjustedInitialDiseaseValue = null;
            if(calculatedX != null && calculatedX > 0.0)
            {
                adjustedInitialDiseaseValue = this.getUnadjustedInitialDiseaseValue() * (observedInfectionLeafAreaPercent / calculatedX);
            }
            this.initialDiseaseValue = adjustedInitialDiseaseValue != null ? 
                    adjustedInitialDiseaseValue
                    : this.getUnadjustedInitialDiseaseValue();
        }
    }
    
    private Double getUnadjustedInitialDiseaseValue()
    {

        Double retVal = BarleyNetBlotchModel.DEFAULT_INITIAL_DISEASE_VALUE;
        
        // We adjust for resistance:
        retVal = retVal * (10-this.resistanceFactor) / 4;
        
        // Then based on crop change
        if(this.sameCropAsLastSeason.equals(Boolean.FALSE))
        {
            retVal = retVal / 2;
        }
        // And lastly (possibly) on plowing
        else if(this.plowed.equals(Boolean.FALSE))
        {
            retVal = retVal * 2;
        }
        
        
        
        return retVal;
    }

    private void calculateDisease(Double initialDiseaseValue, String diseaseParameter) {
        //System.out.println("calculateDisease(" + initialDiseaseValue + ", " + diseaseParameter + ")");
        // The initial value is inserted from 14 to 28 days after sowing date
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(this.sowingDate);
        cal.add(Calendar.DATE, 14);
        Date twoWeeksAfterSowing = cal.getTime();
        cal.add(Calendar.DATE, 14);
        Date fourWeeksAfterSowing = cal.getTime();
        Date currentDate = twoWeeksAfterSowing;
        while(currentDate.compareTo(fourWeeksAfterSowing) < 0)
        {
            this.dataMatrix.setParamDoubleValueForDate(currentDate, diseaseParameter, initialDiseaseValue);
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        
        Double TMDD0C;
        do
        {
            TMDD0C = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMDD0C);
            Double deltaX = this.getDeltaX(currentDate, diseaseParameter);
            this.dataMatrix.setParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.DELTA_X, deltaX);
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, -1);
            Double lastX = this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), diseaseParameter);
            
            Double newX = lastX + deltaX;
            // We adjust for spraying (if applicable) and/or observation (if made after spraying)
            if(diseaseParameter.equals(BarleyNetBlotchModelDataMatrix.X_ADJUSTED) && this.spraying != null)
            {
                if(this.cropWasSprayedToday(currentDate))
                {
                    newX = this.adjustForSpraying(newX);
                }
                // If observation succeeds spraying
                else if(this.observation != null && this.observation.getTimeOfObservation().compareTo(this.spraying.getSprayingDate()) > 0)
                {
                    // If observation was made today
                    if(new WeatherUtil().normalizeToExactDate(this.observation.getTimeOfObservation(), this.timeZone).compareTo(currentDate) == 0)
                    {
                        // Adjust to correct value
                        newX = this.getInfectionLeafAreaPercent(this.observation.getPercentInfectedLeaves(), 100);
                        
                    }
                    // If today is within spray effect period: Leave value unchanged
                    else if(this.todayIsWithinEffectPeriodOfSpraying(currentDate))
                    {
                        newX = lastX;
                    }
                }
                else if(this.todayIsWithinEffectPeriodOfSpraying(currentDate))
                {
                    newX = lastX;
                }
            }
            
            this.dataMatrix.setParamDoubleValueForDate(currentDate, diseaseParameter, newX);
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        while(TMDD0C != null && TMDD0C < this.dayDegreeLimit);
        
    }
    
    private Double getDeltaX(Date currentDate, String diseaseParameter)
    {
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(currentDate);
        cal.add(Calendar.DATE, -14);
        
        // Effect of number of days with rain (DG2410)
        Integer DG2410 = this.dataMatrix.getParamIntValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.DG2410);
        if(DG2410 == null)
        {
            System.out.println("DG2410 == null at" + currentDate);
        }
        Double ed = (0.639 * DG2410) - (0.0465 * Math.pow(DG2410, 2)) - 0.502 * Math.sqrt(DG2410);
        //System.out.println("ed=" + ed + "=(0.639 * " + DG2410 + ") - (0.0465 * Math.pow(" + DG2410 +", 2)) - 0.502 * Math.sqrt(" + DG2410 + ")");
        // Rainfall sum four to two weeks ago
        Double RR28 = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.RR28);
        Double disease14DaysAgo = this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), diseaseParameter);
        //System.out.println("getDeltaX(" + currentDate + ", " + diseaseParameter + "): disease14daysago [" + cal.getTime() + "] = " + disease14DaysAgo);
        Double deltaX = (disease14DaysAgo / 100) * (1 - disease14DaysAgo / 100) * (ed * RR28 * 0.015) * 100;
        //System.out.println("Deltax= " + deltaX + "=(" + disease14DaysAgo + " / 100) * (1 - " + disease14DaysAgo + " / 100) * (" + ed + "* RR28 * 0.015) * 100");
        return  deltaX;
    }

    /**
     * Convert the input from observation to the value in the disease function
     * @param infectedLeaves
     * @param totalLeaves
     * @return 
     */
    private Double getInfectionLeafAreaPercent(Double infectedLeaves, Integer totalLeaves)
    {
        Double infectionLeavesPercent = infectedLeaves * 100 / totalLeaves;
        Double infectionLeafAreaPercent = 0.0;
        if(infectionLeavesPercent > 0.0)
        {
            infectionLeafAreaPercent = (0.0081 * Math.pow(infectionLeavesPercent, 2) + 0.1205 * infectionLeavesPercent) / this.conversionCorrectionFactor;
        }
        
        // Making sure the return value is within acceptable bounds
        if(infectionLeafAreaPercent < 0.0)
        {
            infectionLeafAreaPercent = 0.0;
        }
        else if(infectionLeafAreaPercent > 100.0)
        {
            infectionLeafAreaPercent = 100.0;
        }
        return infectionLeafAreaPercent;
    }

    /**
     * 
     */
    private void calculateThreshold() {
        // The initial value is inserted from 14 to 28 days after sowing date
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(this.sowingDate);
        cal.add(Calendar.DATE, 14);
        Date twoWeeksAfterSowing = cal.getTime();
        cal.add(Calendar.DATE, 14);
        Date fourWeeksAfterSowing = cal.getTime();
        Date currentDate = twoWeeksAfterSowing;
        while(currentDate.compareTo(fourWeeksAfterSowing) < 0)
        {
            this.dataMatrix.setParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.Y, this.initialThresholdValue);
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        
        Double TMDD0C;
        do
        {
            TMDD0C = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMDD0C);
            Double deltaY = this.getDeltaY(currentDate);
            this.dataMatrix.setParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.DELTA_Y, deltaY);
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, -1);
            Double lastY = this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), BarleyNetBlotchModelDataMatrix.Y);
            Double newY = lastY + deltaY;
            this.dataMatrix.setParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.Y, newY);
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        while(TMDD0C != null && TMDD0C < this.dayDegreeLimit);
    }
    
    public Double getDeltaY(Date currentDate)
    {
        
        Double edThreshold = (0.639 * this.DG2410Threshold) - (0.0465 * Math.pow(this.DG2410Threshold,2)) - (0.502 * Math.sqrt(this.DG2410Threshold));
        
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(currentDate);
        cal.add(Calendar.DATE, -14);
        Double threshold14DaysAgo = this.dataMatrix.getParamDoubleValueForDate(cal.getTime(), BarleyNetBlotchModelDataMatrix.Y);
        //System.out.println(currentDate + " threshold14DaysAgo= " + threshold14DaysAgo );
        Double deltaY = (threshold14DaysAgo / 100) * (1 - threshold14DaysAgo / 100) * (edThreshold * this.RR28Threshold * 0.015) * 100;
        //System.out.println(deltaY + " = (" + threshold14DaysAgo + " / 100) * (1 - " + threshold14DaysAgo + " / 100) * ("+edThreshold+" * " + this.RR28Threshold + "* 0.015) * 100");
        return deltaY;
    }

    private boolean cropWasSprayedToday(Date currentDate) {
        if(this.spraying == null)
        {
            return false;
        }
        return this.spraying.getSprayingDate().compareTo(currentDate) == 0;
    }
    
    private boolean todayIsWithinEffectPeriodOfSpraying(Date currentDate) {
        // No spraying?
        if(this.spraying == null)
        {
            return false;
        }
        // Today is before spraying?
        if(currentDate.compareTo(this.spraying.getSprayingDate()) < 0)
        {
            return false;
        }
        // Today is less than [sprayEffectDurationDays] days after spraying??
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(this.spraying.getSprayingDate());
        cal.add(Calendar.DATE, this.sprayEffectDurationDays);
        return cal.getTime().compareTo(currentDate) >= 0;
    }

    private Double adjustForSpraying(Double originalX) {
        //System.out.println(originalX + " * ( 1 - " + this.spraying.getSprayingEffectFactor() + "=" + (originalX * ( 1 - this.spraying.getSprayingEffectFactor())));
        return originalX * ( 1 - this.spraying.getSprayingEffectFactor() );
    }

    private Date getLastDateOfModelCalculation() {
        // The initial value is inserted from 14 to 28 days after sowing date
        Calendar cal = Calendar.getInstance(this.timeZone);
        cal.setTime(this.sowingDate);
        cal.add(Calendar.DATE, 14);
        Date currentDate = cal.getTime();
        
        
        Double TMDD0C;
        do
        {
            TMDD0C = this.dataMatrix.getParamDoubleValueForDate(currentDate, BarleyNetBlotchModelDataMatrix.TMDD0C);
            
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, 1);
            currentDate = cal.getTime();
        }
        while(TMDD0C != null && TMDD0C < this.dayDegreeLimit);
        return currentDate;
    }

    
}
