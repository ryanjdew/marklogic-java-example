package com.mycompany.app.custompojodao;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.pojo.PojoRepository;
import com.marklogic.client.pojo.annotation.Id;
import com.mycompany.app.Configuration;

public class DAOFactory {
	
	private HashMap<String, Method> idMethods = new HashMap<String, Method>();
	private HashMap<String, Field> idProperties = new HashMap<String, Field>();

	private DatabaseClient client = Configuration.mlClient();
	// Return a POJO Repository for a given class
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable> PojoRepository<T, ID> getPojoRepository(Class<T> classy) {
		Class<ID> Id =  getIdClass(classy);
		@SuppressWarnings("rawtypes")
		PojoRepository<T, ID> pojoRepository = new CustomPojoRepositoryImpl(client, classy, Id);
		return pojoRepository;
	}
	
	// Get the class of the POJO's ID
	@SuppressWarnings("unchecked")
	private <T, ID extends Serializable> Class<ID> getIdClass(Class<T> entityClass) {
		findId(entityClass);
        
        String className = entityClass.getName();
        Method idMethod = idMethods.get(className);
        Field idProperty = idProperties.get(className);
        if ( idMethod != null ) {
            try {
                return (Class<ID>) idMethod.getReturnType();
            } catch (Exception e) {
                throw new IllegalStateException("Error invoking " + entityClass.getName() + " method " +
                    idMethod.getName(), e);
            }
        } else if ( idProperty != null ) {
            try {
                return (Class<ID>) idProperty.getType();
            } catch (Exception e) {
                throw new IllegalStateException("Error retrieving " + entityClass.getName() + " field " +
                    idProperty.getName(), e);
            }
        } else {
            throw new IllegalArgumentException("Your class " + entityClass.getName() +
                " does not have a method or field annotated with com.marklogic.client.pojo.annotation.Id");
        }
    }

	
	// Use reflection to find the Id annotation
	private <T> void findId(Class<T> classy) {
		String className = classy.getName();
		if (idMethods.get(className) == null && idProperties.get(className) == null) {
			for (Field property : classy.getDeclaredFields()) {
				if (property.getAnnotation(Id.class) != null) {
					idProperties.put(className, property);
					break;
				}
			}
			if (idProperties.get(className) == null) {
				for (Method property : classy.getDeclaredMethods()) {
					if (property.getAnnotation(Id.class) != null) {
						idMethods.put(className, property);
						break;
					}
				}
			}
		}
	}
}