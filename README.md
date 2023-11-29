# BarleyNetBlotchModel

<img src="/images/barleynetblotch.jpg" description="Photo: E. Fløistad, NIBIO"/>
<h3>Description</h3>
<p>The disease is caused by <em>Pyrenophora teres</em> (spot form and net form).</p>
<p>The netblotch model is a regression model made by Dr. Oleif Elen based on Norwegian disease assessments, weather and barley variety data that calculates disease development continuously. The start value is put in 14 days after sowing and the disease development calculations start 28 days after sowing. Disease development calculations stop after 900 degree-days (around BBCH 75). The starting value is corrected for by previous crop and tillage. The disease development model runs parallel to a spray threshold model. The main factors driving the disease development model are the amount of precipitation and the number of days with precipitation over 0,2mm between 24 and 14 days before the day of disease development calculation. The threshold model is based on a constant amount of precipitation of 2 and number of days with precipitation of 3. Additionally, the user can enter observed disease severity to adjust the predicted values. Current and 5-day in advance predicted climate data is automatically downloaded from the meterological institute and used for the prediction models.</p>


### Development

Clone Gitlab repository

```
$ git clone https://gitlab.nibio.no/VIPS/models/java/Model_BARLEYNETB.git
$ cd Model_BARLEYNETB
```