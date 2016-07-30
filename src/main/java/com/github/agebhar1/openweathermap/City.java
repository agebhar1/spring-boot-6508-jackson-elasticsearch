package com.github.agebhar1.openweathermap;

import lombok.Value;

@Value(staticConstructor = "of") 
public class City {
	
	int id;
	String name;

}
