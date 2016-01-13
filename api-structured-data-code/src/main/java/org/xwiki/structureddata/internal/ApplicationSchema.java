/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xwiki.structureddata.internal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Get the schema of an Application.
 * 
 * @version $Id$
 */
public class ApplicationSchema {
    
    /**
     * @param xClass the BaseClass representing the application
     * @param context the wiki context
     * @param logger 
     * @return the schema of the application
     */
    protected static Map<String, Object> getAppSchema(BaseClass xClass, XWikiContext context, Logger logger) {
        Map<String, Object> value = new HashMap<>();
        List<PropertyClass> propList = xClass.getEnabledProperties();
        for (int i = 0; i < propList.size(); ++i) {
            PropertyClass property = propList.get(i);
            String key = property.getName();
            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put("Type", property.getClassType());
            // Add possible values for static and database lists
            addListPropertyValues(propertyMap, property, context, logger);
            value.put(key, propertyMap);
        }
        return value;
    }
    
    /**
     * Add the posible values for DB and Static lists to the schema.
     * @param propertyMap the map representing the property where to put the list of values
     * @param property the property to check
     * @param context the wiki context
     * @param logger 
     */
    private static void addListPropertyValues(Map<String, Object> propertyMap, PropertyClass property, XWikiContext context, Logger logger) {
        String valueKey = "Values";
        try {
            if ("StaticList".equals(property.getClassType())) {
                String staticListValues = ((StaticListClass) property).getValues();
                propertyMap.put(valueKey, staticListValues);
            } else if ("DBList".equals(property.getClassType())) {
                List<ListItem> dbList = ((DBListClass) property).getDBList(context);
                List<String> dBListValues = new ArrayList<>();
                for (int j = 0; j < dbList.size(); ++j) {
                    String dbValue = dbList.get(j).getValue();
                    dBListValues.add(dbValue);
                }
                propertyMap.put(valueKey, dBListValues);
            }
        } catch (Exception e) {
            logger.warn("Unable to load the possible values of list property [{}]", property, e);
        }
    }
}
