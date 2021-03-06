package com.al.json2model.model;


import java.util.AbstractMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.al.json2model.general.ClassFile;
import com.al.json2model.general.DataType;
import com.al.json2model.general.NameUtils;
import com.al.json2model.model.properties.Language;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Model class for Java. This class will be in charge to model the data and produce
 * a file.java class.
 * 
 * @author alfredo
 *
 */
public class ModelSwift extends ModelAbstract {
	
	/**
	 * Default constructor.
	 * @param name Name of the class to be created.
	 * @param json JSON file to use as a blueprint.
	 * @param language The language used for the class to be created.
	 * @param destFolder Destination folder where to put the file(s).
	 * @see ModelAbstract
	 */
	public ModelSwift(String name, String json, Language language, String destFolder) {
		super(name, json, language, destFolder);
	}
	
	
	@Override
	protected DataType getPrimitiveDataType(Map.Entry<String, JsonElement> entry) {
		
		JsonPrimitive primivitive = entry.getValue().getAsJsonPrimitive();
		
		if (primivitive.isBoolean()) {
			return new DataType(entry.getKey(), "Bool", false);
		} else if (primivitive.isNumber()) {
			
			if( isDouble(primivitive.getAsString())) {
				return new DataType(entry.getKey(), "Double", false);
			}else {
				return new DataType(entry.getKey(), "Int", false);
			}	
		} else if (primivitive.isString()) {
			return new DataType(entry.getKey(), "String", false);
		} else {
			return new DataType(entry.getKey(), "AnyObject", false);
		}
	}
	
	@Override
	protected DataType getArrayDataType(Map.Entry<String, JsonElement> entry) {
		
		final String format = "[%s]";

		String type = null;
		String name = entry.getKey();
		String nameClass = null;
		JsonElement testType = entry.getValue().getAsJsonArray().get(0);

		if (testType.isJsonObject() || testType.isJsonArray()) {

			nameClass = NameUtils.getCapitalized(NameUtils.getSingular(entry.getKey()));
			type = String.format(format, nameClass);

		} else {

			Map.Entry<String, JsonElement> pair = new AbstractMap.SimpleEntry<String, JsonElement>(name, testType);

			DataType base = getPrimitiveDataType(pair);
			nameClass = base.getType();
			type = String.format(format, nameClass);
		}

		return new DataType(name, type, false);
	}
	

	@Override
	protected void prepareFiles() {
		
		String extension = ".swift";
			
		ClassFile file = new ClassFile(modelName, extension, destFolder, getBody());
		files.add(file);
		
	}
	


	@Override
	protected String getBody() {
		
		// Prepare the body.
		String properties = getBodyProperties();
		String constructor = getBodyConstructor();
		String getterAndSetters = getBodyGettersAndSetters();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format(language.CLASS_DECLARATION_START, modelName));
		sb.append(properties);
		sb.append(constructor);
		sb.append(getLoadMethod());
		sb.append(getterAndSetters);
		sb.append(language.CLASS_DECLARATION_END);
		
		return sb.toString();
	}

	@Override
	protected String getBodyProperties(){
		
		StringBuilder sb = new StringBuilder();

		for (String propertyKey : properties.keySet()) {
			
			DataType t = properties.get(propertyKey);
			String type = t.isObject() ? NameUtils.getCapitalized(t.getName()) : t.getType();
			
			// If the object is an array then use var.
			String format = language.PROPERTY_DECLARATION;
			if (type.contains("[")) {
				format = language.PROPERTY_DECLARATION.replace("let", "var");
			}
			
			sb.append(String.format(format, t.getName(), type));
		}
		
		sb.append(language.NEW_LINE);
		
		return sb.toString();
	}
	
	@Override
	protected String getBodyConstructor(){
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format(language.CONSTRUCTOR_DECLARATION_START , getPropertiesAsArgument()));
		sb.append(language.CONTRUCTOR_SUPER);

		for (String propertyKey : properties.keySet()) {
			DataType t = properties.get(propertyKey);
			sb.append(String.format(language.CONTRUCTOR_PROPERTY_ASSIGNMENT, t.getName(), t.getName()));
		}
		
		sb.append(language.CONTRUCTOR_DECLARATION_END);
		
		return sb.toString();
	}
	
	@Override
	protected String getPropertiesAsArgument() {
		
		StringBuilder sb = new StringBuilder();

		for (String propertyKey : properties.keySet()) {
			
			DataType t = properties.get(propertyKey);
			String type = t.isObject() ? StringUtils.capitalize(t.getName()) : t.getType();
			sb.append(t.getName()).append(": ").append(type).append(", ");
		}
		
		// Remove the last ', ' characters added.
		return sb.substring(0, sb.length() - 2);
	}
	
	@Override
	protected String getBodyGettersAndSetters(){
		
		StringBuilder sb = new StringBuilder();

		for (String propertyKey : properties.keySet()) {
			
			DataType t = properties.get(propertyKey);
			String getterSuffix = t.getType().equals("boolean") ? language.GETTER_NAME_SUFFIX_BOOLEAN : language.GETTER_NAME_SUFFIX;
			
			String getterName = getterSuffix + StringUtils.capitalize(t.getName());
			String setterName = language.SETTER_NAME_SUFFIX + StringUtils.capitalize(t.getName());
		
			// Add all the elements together.
			sb.append(String.format(language.GETTER_DECLARATION_START,t.getType(), getterName));
			sb.append(String.format(language.GETTER_BODY, t.getName()));
			sb.append(language.SETTER_DECLARATION_END);
			sb.append(String.format(language.SETTER_DECLARATION_START, setterName, t.getType(), t.getName()));
			sb.append(String.format(language.SETTER_BODY, t.getName(), t.getName()));
			sb.append(language.GETTER_DECLARATION_END);
		}
		
		return sb.toString();
	}
	
	@Override
	protected String getLoadMethod() {

		if (!topObject) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(language.METHOD_LOAD_START);
		sb.append(language.METHOD_LOAD_BODY);
		sb.append(language.METHOD_LOAD_END);
		
		return sb.toString();
		
	}
}
