package com.mycompany.app.data;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.marklogic.client.pojo.annotation.Id;

import de.svenjacobs.loremipsum.LoremIpsum;

public class RandomDocGenerator {

	static final Map<String, List<String>> randomValuesByType = new HashMap<String, List<String>>();
	static final Map<String, List<Long>> randomLongByType = new HashMap<String, List<Long>>();
	static final LoremIpsum loremIpsum = new LoremIpsum();
	static final Pattern yearRegex = Pattern.compile("year", Pattern.CASE_INSENSITIVE);
	static final Pattern descriptionRegex = Pattern.compile("(description|outputJournal)", Pattern.CASE_INSENSITIVE);

	static {
		try {
			setupDataSelectionLists();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static <T> T newComplexRandomClass(Class<T> classy) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		T instance = null;
		if (classy.isEnum()) {
			T[] constants = classy.getEnumConstants();
			int indexNum = ThreadLocalRandom.current().nextInt(constants.length);
			instance = constants[indexNum];
		} else {
			instance = classy.newInstance();
		}
		Field[] fields = classy.getDeclaredFields();
		for (Method method : classy.getDeclaredMethods()) {
			String methodName = method.getName();
			if (methodName.startsWith("set")) {
				Method getMethod = null;
				try {
					getMethod = classy.getMethod("g" + methodName.substring(1));
				} catch (NoSuchMethodException e) {

				}
				String fieldName = methodName.substring(3).toLowerCase();
				String regex = "/" + fieldName + "/i";
				Field field = null;
				Field[] matchingFields = Arrays
						.stream(fields)
						.filter(
								f -> 
								Pattern.matches(regex, f.getName())
						)
						.toArray(Field[]::new);
				if (matchingFields.length > 0) {
					field = matchingFields[0];
				}
				if (method.getAnnotation(Id.class) != null
						|| (getMethod != null && getMethod.isAnnotationPresent(Id.class))
						|| (field != null && field.isAnnotationPresent(Id.class))) {
					method.invoke(instance, UUID.randomUUID().toString());
				} else {
					Class<?> argClass = method.getParameterTypes()[0];
					Field assocField = getFieldFromMethod(classy, method);
					Object randomValue = randomValueFromClass(argClass, assocField);
					if (randomValue == null) {
						randomValue = RandomDocGenerator.newComplexRandomClass(argClass);
					}
					method.invoke(instance, randomValue);
				}
			}
		}
		return instance;
	}

	public static <T> T newRandomClass(Class<T> classy) throws InstantiationException, IllegalAccessException, InstantiationException, IllegalAccessException,
	IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		return newRandomClass(classy, "");
	}

	public static <T> T newRandomClass(Class<T> classy, String fieldName) throws InstantiationException, IllegalAccessException, InstantiationException, IllegalAccessException,
	IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		T randomVal = randomValueFromSimpleClass(classy, fieldName);
		if (randomVal == null) {
			randomVal = newComplexRandomClass(classy);
		}
		return randomVal;
	}

	private static void setupDataSelectionLists() throws IOException {
		ClassLoader classLoader = RandomDocGenerator.class.getClassLoader();
		File csvFile = new File(classLoader.getResource("MOCK_DATA.csv").getFile());
		CSVParser parser = CSVParser.parse(csvFile, Charset.defaultCharset(), CSVFormat.DEFAULT.withHeader());
		// Get a list of CSV file records
		List<CSVRecord> csvRecords = parser.getRecords();
		// Read the CSV file records starting from the second record to skip the
		// header
		for (int i = 1; i < csvRecords.size(); i++) {
			Map<String, String> record = csvRecords.get(i).toMap();
			for (String key : record.keySet()) {
				if (!(record.get(key) == null || record.get(key).equals(""))) {
					if (randomValuesByType.containsKey(key)) {
						randomValuesByType.get(key).add(record.get(key));
					} else {
						randomValuesByType.put(key, new ArrayList<String>(Arrays.asList(record.get(key))));
					}
				}
			}
		}
		parser.close();
	}

	public static <T> Field getFieldFromMethod(Class<T> classy, Method method)
			throws NoSuchFieldException, SecurityException {
		String methodName = method.getName();
		Field assocField = null;
		try {
			assocField = classy.getDeclaredField(methodName.substring(3));
		} catch (NoSuchFieldException e) {
			assocField = classy.getDeclaredField(methodName.substring(3, 4).toLowerCase() + methodName.substring(4));
		}
		return assocField;
	}

	@SuppressWarnings("rawtypes")
	public static Class getTypeClass(Field associatedField) {
		Type type = associatedField.getGenericType();
		Class subClass = null;
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			subClass = (Class) pt.getActualTypeArguments()[0];
		}
		return subClass;
	}

	@SuppressWarnings("unchecked")
	private static <T> T randomValueFromClass(Class<T> classy, Field associatedField)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		String simpleName = classy.getSimpleName();
		String fieldName = associatedField.getName();
		T randomVal = null;
		@SuppressWarnings("rawtypes")
		Class subClass = getTypeClass(associatedField);
		if(subClass instanceof Class && ((Class<?>)subClass).isEnum()) {
			Object[] values = subClass.getEnumConstants();
			int selectionIndex = ThreadLocalRandom.current().nextInt(values.length);
			randomVal = (T) values[selectionIndex];
		} else {
			switch (simpleName) {
			case "List":
				if (subClass != null && !subClass.getName().equals(Object.class.getName())) {
					randomVal = classy.cast(getListOfRandomClasses(subClass, fieldName));
				}
				break;
			default:
				randomVal = randomValueFromSimpleClass(classy, associatedField.getName());
				break;
			}
		}
		return randomVal;
	}

	private static <T> T randomValueFromSimpleClass(Class<T> classy, String fieldName) {
		String simpleName = classy.getSimpleName();
		T randomVal = null;
		switch (simpleName) {
		case "Double":
			randomVal = classy.cast(new Double(Math.round(ThreadLocalRandom.current().nextDouble() * 10000) / 100.0));
			break;
		case "Long":
			randomVal = classy.cast(getSemiRandomLong(fieldName));
			break;
		case "Integer":
			randomVal = classy.cast(new Integer(ThreadLocalRandom.current().nextInt()));
			break;
		case "Boolean":
			randomVal = classy.cast(new Boolean(ThreadLocalRandom.current().nextInt(2) == 1));
			break;
		case "String":
			randomVal = classy.cast(getSemiRandomString(fieldName));
			break;
		case "GregorianCalendar":
			GregorianCalendar gc = new GregorianCalendar();
			int year = randBetween(1980, 2015);
			gc.set(Calendar.YEAR, year);
			int dayOfYear = randBetween(1, gc.getActualMaximum(Calendar.DAY_OF_YEAR));
			gc.set(Calendar.DAY_OF_YEAR, dayOfYear);
			randomVal = classy.cast(gc);
			break;
		default:
			break;
		}
		return randomVal;
	}
	
	private static int randBetween(int start, int end) {
		return start + (int) Math.round(Math.random() * (end - start));
	}

	private static <T> List<T> getListOfRandomClasses(Class<T> classy, String fieldName)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchFieldException, SecurityException {
		int size = ThreadLocalRandom.current().nextInt(4) + 1;
		List<T> subClasses = new ArrayList<T>();
		for (int i = 0; i < size; i++) {
			subClasses.add(newRandomClass(classy, fieldName));
		}
		return subClasses;
	}

	private static String getSemiRandomString(String fieldName) {
		List<String> selectionList = randomValuesByType.get(fieldName);
		if (selectionList != null) {
			int selectionIndex = ThreadLocalRandom.current().nextInt(selectionList.size());
			return selectionList.get(selectionIndex);
		} else if (descriptionRegex.matcher(fieldName).find()) {
			return loremIpsum.getWords(ThreadLocalRandom.current().nextInt(3) + 2) + " "
					+ randomStringValueByType("tags") + " "
					+ loremIpsum.getWords(ThreadLocalRandom.current().nextInt(3) + 2,
							ThreadLocalRandom.current().nextInt(3) + 10)
					+ " " + randomStringValueByType("tags");
		} else {
			return loremIpsum.getWords(ThreadLocalRandom.current().nextInt(3) + 2);
		}
	}

	private static Long getSemiRandomLong(String fieldName) {
		if (randomLongByType.containsKey(fieldName)) {
			List<Long> longList = randomLongByType.get(fieldName);
			return longList.get(ThreadLocalRandom.current().nextInt(longList.size()));
		} else if (yearRegex.matcher(fieldName).find()) {
			return new Long(randBetween(1970, 2015));
		} else {
			return new Long(ThreadLocalRandom.current().nextLong(300));
		}
	}

	private static String randomStringValueByType(String type) {
		return randomStringValueByType(type, ThreadLocalRandom.current().nextInt(randomValuesByType.get(type).size()));
	}

	private static String randomStringValueByType(String type, int index) {
		return randomValuesByType.get(type).get(index);
	}
}
