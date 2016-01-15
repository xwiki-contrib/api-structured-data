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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.structureddata.Application;

/**
 * AppWithinMinutes Application implementation.
 * 
 * @version $Id$
 */
public class AWMApplication implements Application
{
    private DocumentReferenceResolver<String> resolver;
    private EntityReferenceSerializer<String> serializer;
    private Logger logger;

    private BaseClass xClass;
    private DocumentReference xClassRef;
    private WikiReference wikiRef;
    private XWikiContext context;
    private XWiki xwiki;
    private String dataSpace;
    private String appName;

    public AWMApplication(XWikiContext context,
            DocumentReferenceResolver<String> resolver,
            EntityReferenceSerializer<String> serializer,
            Logger logger, 
            DocumentReference appWebHomeRef) throws XWikiException 
    {
        this.context = context;
        this.resolver = resolver;
        this.serializer = serializer;
        this.logger = logger;
        this.xwiki = context.getWiki();
        // Get the class reference from the AppWithinMinutes.LiveTableClass object
        BaseObject item;
        if(appWebHomeRef != null) {
            item = getAWMObject(context, serializer, appWebHomeRef);
        }
        else {
            item = getAWMObject(context, serializer);
        }
        this.wikiRef = context.getDoc().getDocumentReference().getWikiReference();
        DocumentReference classReference = resolver.resolve(item.getStringValue("class"), this.wikiRef);
        this.xClassRef = classReference;
        this.xClass = this.xwiki.getXClass(classReference, context);
        // Get data space
        if(appWebHomeRef == null) {
            List<SpaceReference> spacesRef = xClassRef.getSpaceReferences();
            SpaceReference lastAncestor = spacesRef.get(0);
            appWebHomeRef = new DocumentReference(context.getWikiId(), lastAncestor.getName(), "WebHome");
        }
        SpaceReference appWebHomeSpaceRef = appWebHomeRef.getLastSpaceReference();
        this.appName = serializer.serialize(appWebHomeSpaceRef, "local");
        this.dataSpace = this.appName+item.getStringValue("dataSpace");
    }

    @Override
    public Map<String, Object> getAppSchema() throws XWikiException 
    {
        return ApplicationSchema.getAppSchema(xClass, context, logger);
    }

    @Override
    public ItemMap getItem(String itemId) throws Exception {
        ItemMap value = new ItemMap();
        try {
            String objId = this.dataSpace+"."+itemId;
            BaseObject xObj = this.getObjectFromId(objId);
            ApplicationItem item = new ApplicationItem(itemId, 0, xObj, this.xClass, this.wikiRef, this.context, this.resolver, this.logger);
            value = item.getItemMap(false);
        } catch (Exception e) {
            logger.error("Unable to load the item.", e);
        }
        return value;
    }

    @Override
    public Map<String, Object> getItems() throws Exception 
    {
        Map<String, Object> options = new HashMap<>();
        return this.getItems(options);
    }

    @Override
    public Map<String, Object> getItems(Map<String, Object> options) throws Exception {
        Map<String, Object> value = new HashMap<>();
        String limitOpt = "limit";
        String offsetOpt = "offset";
        List<String> itemsList = xwiki.getSpaceDocsName(dataSpace, context);
        Collections.sort(itemsList);
        Integer startIndex = 0;
        Integer endIndex = itemsList.size();
        if (options.containsKey(offsetOpt)) {
            startIndex = (Integer) options.get(offsetOpt);
        }
        if (options.containsKey(limitOpt)) {
            endIndex = startIndex + (Integer) options.get(limitOpt);
        }
        List<String> finalList = itemsList.subList(startIndex, endIndex);
        Integer objCount = 0;
        for (int i=0; i<finalList.size(); ++i) {
            String docName = finalList.get(i);
            String docFullName = dataSpace+"."+docName;
            if(!isTemplate(docName)) {
                BaseObject xObj = this.getObjectFromId(docFullName);
                if (xObj != null) {
                    ApplicationItem item = new ApplicationItem(docName, 0, xObj, this.xClass, this.wikiRef, this.context, this.resolver, this.logger);
                    ItemMap map = item.getItemMap(true);
                    value.put("Item"+objCount, map);
                    objCount++;
                }
            }
        }
        return value;
    }

    /**
     * Check if the document is a template of the current class
     * @param docName
     * @return true if the document is a template
     */
    private Boolean isTemplate(String docName) {
        String className = this.xClassRef.getName();
        // Check for template following the model "Application" -> "ApplicationTemplate"
        // or the model "ApplicationClass" -> "ApplicationClassTemplate"
        if (docName.equals(className + "Template")) {
            return true;
        }
        // Check for template following the model "ApplicationClass" -> "ApplicationTemplate"
        if (className.length() > 5 && className.endsWith("Class")) {
            String shortClassName = className.substring(0, className.length()-5);
            if (docName.equals(shortClassName + "Template")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, Object> storeItem(String itemId, ItemMap itemData) throws Exception {
        String objName = dataSpace+"."+itemId; // The object name is the document full name
        BaseObject xObj = this.getObjectFromId(objName);
        ApplicationItem item = new ApplicationItem(objName, 0, xObj, this.xClass, this.wikiRef, this.context, this.resolver, this.logger);
        return item.store(itemData);
    }

    @Override
    public Map<String, Object> deleteItem(String itemId) throws Exception {
        String objName = dataSpace+"."+itemId; // The object name is the document full name
        BaseObject xObj = this.getObjectFromId(objName);
        ApplicationItem item = new ApplicationItem(objName, 0, xObj, this.xClass, this.wikiRef, this.context, this.resolver, this.logger);
        return item.delete();
    }

    /**
     * @param context the wiki context
     * @param serializer 
     * @return true if the current document is in an AWM app
     * @throws XWikiException 
     */
    public static DocumentReference isAWM(XWikiContext context, 
            EntityReferenceSerializer<String> serializer) throws XWikiException 
    {
        BaseObject awmObj = getAWMObject(context, serializer);
        if (awmObj == null) {
            return null;
        }
        return awmObj.getDocumentReference();
    }
    /**
     * @param context the wiki context
     * @param serializer 
     * @param appWebHomeRef the reference of the application's WebHome
     * @return true is the reference represent an AWM app
     * @throws XWikiException 
     */
    public static DocumentReference isAWM(XWikiContext context, 
            EntityReferenceSerializer<String> serializer, 
            DocumentReference appWebHomeRef) throws XWikiException 
    {
        BaseObject awmObj = getAWMObject(context, serializer, appWebHomeRef);
        if (awmObj == null) {
            return null;
        }
        return awmObj.getDocumentReference();
    }
    /**
     * Get the LiveTableClass object of the AWM app of the current document.
     * @param context the wiki context
     * @param serializer 
     * @return the AppWIthiMinutes.LiveTableClass object
     * @throws XWikiException 
     */
    public static BaseObject getAWMObject(XWikiContext context, 
            EntityReferenceSerializer<String> serializer) throws XWikiException 
    {
        // Check if the current document is in a subspace (Code/Data/etc.) of an AppWithinMinutes application
        DocumentReference docRef = context.getDoc().getDocumentReference();
        List<SpaceReference> spacesRef = docRef.getSpaceReferences();
        SpaceReference lastAncestor = spacesRef.get(0);
        // Top-level space's WebHome :
        String webHomeSpace = serializer.serialize(lastAncestor, "local");
        BaseObject awmObj = null;
        // Start compatibility code for XWiki < 7.2 (without Nested spaces)
        // --> AWM code space is a top-level space named "{ApplicationName}Code"
        if (webHomeSpace.endsWith("Code") && webHomeSpace.length() > 4) {
            String appNameTmp = webHomeSpace.substring(0, webHomeSpace.length()-4);
            DocumentReference appWebHomeRef = new DocumentReference(context.getWikiId(), 
                    appNameTmp, 
                    "WebHome");
            awmObj = getAWMObject(context, serializer, appWebHomeRef);
        }
        // End compatibility code
        if (awmObj == null) {
            DocumentReference appWebHomeRef = new DocumentReference(context.getWikiId(), webHomeSpace, "WebHome");
            awmObj = getAWMObject(context, serializer, appWebHomeRef);
        }
        return awmObj;
    }
    /**
     * Get the LiveTableClass object of the selected AWM app.
     * @param context the wiki context
     * @param serializer 
     * @param appWebHomeRef the reference of the application's WebHome
     * @return the AppWIthiMinutes.LiveTableClass object
     * @throws XWikiException 
     */
    public static BaseObject getAWMObject(XWikiContext context, 
            EntityReferenceSerializer<String> serializer, 
            DocumentReference appWebHomeRef) throws XWikiException 
    {
        DocumentReference awmClassRef = new DocumentReference(context.getWikiId(), "AppWithinMinutes", "LiveTableClass");
        return context.getWiki().getDocument(appWebHomeRef, context).getXObject(awmClassRef);
    }
    /**
     * @param itemId the id of an XWiki object
     * @return the XWikiDocument containing the object
     * @throws XWikiException 
     */
    private XWikiDocument getDocFromId(String itemId) throws XWikiException 
    {
        DocumentReference itemDocRef = this.resolver.resolve(itemId, this.wikiRef);
        return this.xwiki.getDocument(itemDocRef, this.context);
    }
    /**
     * @param objId the id of an XWiki object
     * @return the XWiki object represented by the id
     * @throws XWikiException 
     */
    private BaseObject getObjectFromId(String itemId) throws XWikiException 
    {
        XWikiDocument xDoc = this.getDocFromId(itemId);
        if (xDoc == null) {
            return null;
        }
        return xDoc.getXObject(this.xClassRef);
    }
    
    @Override
    public String toString() 
    {
        return "AppWithinMinutes application : " + this.appName;
    }
}
