package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.marklogic.client.pojo.PojoRepository;
import com.mycompany.app.data.RandomDocGenerator;
import com.mycompany.app.pojos.Person;
import com.mycompany.app.simplepojodao.DAOFactory;

/**
 * Unit test for simple App.
 */
public class SimplePOJODAOTest {
	private DAOFactory daoFactory = new DAOFactory();

	@Test
	/**
	 * Tests the insert and retrieval of coverage object
	 * 
	 * @throws Exception
	 */
	public void testPersonWriteAndRead() throws Exception {
		PojoRepository<Person, Serializable> pojoRepo = daoFactory.getPojoRepository(Person.class);

		Person instance1 = RandomDocGenerator.newRandomClass(Person.class);

		assertNotNull(instance1);

		pojoRepo.write(instance1);

		Person instance2 = pojoRepo.read(instance1.get_id());

		assertNotNull(instance2);

		checkValues(Person.class, instance1, instance2);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> void checkValues(Class<T> classy, T instance1, T instance2) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException {
		for (Method method : classy.getDeclaredMethods()) {
			if (method.getName().startsWith("get")) {
				String simpleClassName = method.getReturnType().getSimpleName();
				Object result1 = method.invoke(instance1);
				Object result2 = method.invoke(instance2);
				if (simpleClassName.equals("List")) {
					Field assocField = RandomDocGenerator.getFieldFromMethod(classy, method);
					Class subClass = RandomDocGenerator.getTypeClass(assocField);
					if (subClass != null && !subClass.getName().equals(Object.class.getName())) {
						List list1 = (List) result1;
						List list2 = (List) result2;
						assertEquals(list1.size(), list2.size());
						for (int i = 0; i < list1.size(); i++) {
							if (subClass.getSimpleName().equals("String")) {
								assertEquals(list1.get(i), list2.get(i));
							} else {
								checkValues(subClass, list1.get(i), list2.get(i));
							}
						}
					}
				} else if (simpleClassName.equals("GregorianCalendar")) {
					assertEquals(((GregorianCalendar) result1).getTime(), ((GregorianCalendar) result2).getTime());
				} else {
					assertEquals(result1, result2);
				}
			}
		}
	}
}
