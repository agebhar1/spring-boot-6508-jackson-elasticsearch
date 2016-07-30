package com.github.agebhar1.openweathermap;

import lombok.Value;

@Value(staticConstructor = "of")
public class Forecast {
	
	City city;
	long dt;
	double minTemperature;
	double maxTemperature;
	
}
