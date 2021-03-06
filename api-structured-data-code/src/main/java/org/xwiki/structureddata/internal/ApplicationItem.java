/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.structureddata.internal;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import java.lang.reflect.Method;
import java.util.*;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * An item in an XWikiApplication.
 * 
 * @version $Id$
 */
public class ApplicationItem
{
    private static final String SUCCESS = "Success";
    private static final String ERROR = "Error";

    private BaseObject xObject;
    private XWikiContext context;
    private XWikiDocument xDoc;
    private BaseClass xClass;
    private String itemId;
    private Integer objNumber;
    private EntityReferenceSerializer<String> serializer;
    private EntityReferenceResolver<String> resolver;

    /**
     * Create an item.
     * @param itemId the document full name in which the item is located
     * @param objNumber the item number in the document
     * @param xDoc the document containing the item
     * @param xObject the BaseObject representing the item in XWiki
     * @param xClass the BaseClass of the item
     * @param context the wiki context
     * @param resolver the document reference resolver
     * @param serializer the document reference serializer
     * @throws XWikiException
     */
    public ApplicationItem(String itemId,
            Integer objNumber,
            XWikiDocument xDoc,
            BaseObject xObject,
            BaseClass xClass,
            XWikiContext context,
            EntityReferenceResolver<String> resolver,
            EntityReferenceSerializer<String> serializer) throws XWikiException {
        this.xDoc = xDoc;
        this.xObject = xObject;
        this.context = context;
        this.xClass = xClass;
        this.itemId = itemId;
        this.objNumber = objNumber;
        this.serializer = serializer;
        this.resolver = resolver;
    }

    /**
     * Get the map representing the item.
     * @return the item map
     * @throws Exception
     */
    protected ItemMap getItemMap(List<String> properties) throws Exception
    {
        ItemMap value = new ItemMap();
        String methodToSearch = "getValue";
        if (this.xObject == null) {
            this.xObject = this.xClass.newCustomClassInstance(this.context);
            this.xObject.setXClassReference(this.xClass.getReference());
        }
        // Get the properties map
        List<PropertyClass> propList = this.xClass.getEnabledProperties();
        for (PropertyClass prop : propList) {
            String key = prop.getName();
            if(properties == null || properties.size() == 0 || properties.contains(key)) {
                Object propValue;
                if (!prop.getClassType().equals("Password")) {
                    try {
                        Method methodToFind = this.xObject.getField(key).getClass().getMethod(methodToSearch);
                        propValue = methodToFind.invoke(this.xObject.getField(key));
                        value.put(key, propValue);
                    } catch (NullPointerException e) {
                        try {
                            // If value is not set, set an empty value and try again
                            this.xObject.set(key, "", this.context);
                            Method methodToFind = this.xObject.getField(key).getClass().getMethod(methodToSearch);
                            propValue = methodToFind.invoke(this.xObject.getField(key));
                            value.put(key, propValue);
                        } catch (NullPointerException | NoSuchMethodException f) {
                            //System.out.println("Can't find the value of property " + key + " in item " + this.itemId);
                        }
                    }
                }
            }
        }
        ItemMap objectMap;
        String id = this.itemId;
        if (this.objNumber > 0) {
            id = id + "|" + this.objNumber.toString();
        }
        objectMap = value;
        objectMap.setId(id);
        objectMap.setXDoc(this.xDoc, this.serializer);
        return objectMap;
    }

    /**
     * Store the item in the wiki.
     * @param item the item data to store
     * @return the state of the save
     * @throws Exception
     */
    protected Map<String, Object> store(ItemMap item, DocumentMap itemDocData) throws Exception
    {
        Map<String, Object> result = new HashMap<>();
        try {
            if (this.xObject == null) {
                this.xObject = this.create();
            }
            Set<String> itemKeySet = item.keySet();
            for (String key : itemKeySet) {
                String className = this.xObject.get(key).getClass().getSimpleName();
                Object value = item.get(key);
                Object newValue = convertTo(value, className);
                this.xObject.set(key, newValue, this.context);
            }
            this.xDoc.setAuthorReference(context.getUserReference());
            // Save the document fields if they have been changed. If the author has been changed in the item,
            // it will override the previous line which set the author as the current user
            if(itemDocData != null) {
                this.updateDocumentFields(itemDocData);
            }
            this.context.getWiki().saveDocument(this.xDoc, "Properties updated", this.context);
            result.put(ApplicationItem.SUCCESS, "1");
        } catch (Exception e) {
            result.put(ApplicationItem.ERROR, e);
            e.printStackTrace();
        }
        return result;
    }

    private Object convertTo(Object value, String type) {
        if(value == null)
            return null;
        try {
            switch (type) {
                case "LongProperty":
                    return Long.parseLong(value.toString());
                case "FloatProperty":
                    return Float.parseFloat(value.toString());
                case "DoubleProperty":
                    return Double.parseDouble(value.toString());
                case "IntegerProperty":
                    return Integer.parseInt(value.toString());
                default:
                    return value;
            }
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Delete the item from the wiki.
     * @return the state of the deletion
     * @throws Exception
     */
    protected Map<String, Object> delete() throws Exception
    {
        Map<String, Object> result = new HashMap<>();
        try {
            this.xDoc.removeXObject(this.xObject);
            result.put(ApplicationItem.SUCCESS, "1");
        } catch (Exception e) {
            result.put(ApplicationItem.ERROR, e);
        }
        this.context.getWiki().saveDocument(this.xDoc, this.context);
        return result;
    }

    /**
     * Create the item in the wiki.
     * @return theaseObject created
     * @throws XWikiException
     */
    private BaseObject create() throws XWikiException
    {
        BaseObject newObj = this.xDoc.newXObject(this.xClass.getReference(), this.context);
        try {
            newObj.setNumber(this.objNumber);
        } catch (Exception e) {
            this.objNumber = newObj.getNumber();
        }
        if(this.xDoc.isNew()) {
            this.xDoc.setCreatorReference(context.getUserReference());
        }
        this.context.getWiki().saveDocument(this.xDoc, this.context);
        return newObj;
    }

    private void updateDocumentFields(DocumentMap docMap) {
        if(docMap.size() >= 0 && docMap.changes.size() >= 0) {
            if(docMap.changes.contains(ItemMap.AUTHOR))
                this.xDoc.setAuthorReference(new DocumentReference(resolver.resolve((String) docMap.get(ItemMap.AUTHOR), EntityType.DOCUMENT)));
            if(docMap.changes.contains(ItemMap.CREATOR))
                this.xDoc.setCreatorReference(new DocumentReference(resolver.resolve((String) docMap.get(ItemMap.CREATOR), EntityType.DOCUMENT)));
            if(docMap.changes.contains(ItemMap.CREATION))
                this.xDoc.setCreationDate((Date) docMap.get(ItemMap.CREATION));
            if(docMap.changes.contains(ItemMap.UPDATE))
                this.xDoc.setContentUpdateDate((Date) docMap.get(ItemMap.UPDATE));
            if(docMap.changes.contains(ItemMap.PARENT))
                this.xDoc.setParentReference(new DocumentReference(resolver.resolve((String) docMap.get(ItemMap.PARENT), EntityType.DOCUMENT)));
            if(docMap.changes.contains(ItemMap.HIDDEN))
                this.xDoc.setHidden((Boolean) docMap.get(ItemMap.HIDDEN));
            if(docMap.changes.contains(ItemMap.TITLE))
                this.xDoc.setTitle((String) docMap.get(ItemMap.TITLE));
            if(docMap.changes.contains(ItemMap.CONTENT))
                this.xDoc.setContent((String) docMap.get(ItemMap.CONTENT));
            docMap.changes.clear();
        }
    }
}
