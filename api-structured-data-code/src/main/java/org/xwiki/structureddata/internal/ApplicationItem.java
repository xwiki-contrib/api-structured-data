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
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
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
    private Logger logger;

    /**
     * Create an item.
     * @param itemId the document full name in which the item is located
     * @param objNumber the item number in the document
     * @param xDoc 
     * @param xObject the BaseObject representing the item in XWiki
     * @param xClass the BaseClass of the item
     * @param context the wiki context
     * @param resolver the document reference resolver
     * @param serializer 
     * @param logger 
     * @throws XWikiException
     */
    public ApplicationItem(String itemId,
            Integer objNumber,
            XWikiDocument xDoc,
            BaseObject xObject,
            BaseClass xClass,
            XWikiContext context,
            EntityReferenceResolver<String> resolver,
            EntityReferenceSerializer<String> serializer,
            Logger logger) throws XWikiException {
        this.xDoc = xDoc;
        this.xObject = xObject;
        this.context = context;
        this.xClass = xClass;
        this.itemId = itemId;
        this.objNumber = objNumber;
        this.serializer = serializer;
        this.resolver = resolver;
        this.logger = logger;
    }

    /**
     * Get the map representing the item.
     * @return the item map
     * @throws Exception
     */
    protected ItemMap getItemMap() throws Exception
    {
        ItemMap value = new ItemMap();
        String methodToSearch = "getValue";
        if (this.xObject == null) {
            this.xObject = this.xClass.newCustomClassInstance(this.context);
            this.xObject.setXClassReference(this.xClass.getReference());
        }
        // Get the properties map
        List<PropertyClass> propList = this.xClass.getEnabledProperties();
        for (int j = 0; j < propList.size(); ++j) {
            PropertyClass prop = propList.get(j);
            String key = prop.getName();
            Object propValue;
            if(!prop.getClassType().equals("Password")) {
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
                        //logger.info("Can't find the value of property [{}] in item [{}]", key, this.itemId);
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
    protected Map<String, Object> store(ItemMap item) throws Exception
    {
        Map<String, Object> result = new HashMap<>();
        try {
            if (this.xObject == null) {
                this.xObject = this.create();
            }
            Set<String> itemKeySet = item.keySet();
            Iterator<String> iter = itemKeySet.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = item.get(key);
                this.xObject.set(key, value, this.context);
            }
            this.xDoc.setAuthorReference(context.getUserReference());
            // Save the document fields if they have been changed. If the author has been changed in the item,
            // it will override the previous line which set the author as the current user
            this.updateDocumentFields(item);
            this.context.getWiki().saveDocument(this.xDoc, "Properties updated", this.context);
            result.put(ApplicationItem.SUCCESS, "1");
        } catch (Exception e) {
            result.put(ApplicationItem.ERROR, e);
        }
        return result;
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

    private void updateDocumentFields(ItemMap item) {
        if(item.getDocumentFields() != null && item.getDocumentFields().size() >= 8) {
            DocumentMap docMap = item.getDocumentFields();
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
