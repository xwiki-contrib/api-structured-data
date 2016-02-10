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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.structureddata.Application;

/**
 * AppWithinMinutes Application implementation.
 * 
 * @version $Id$
 */
public class AWMApplication implements Application
{
    private EntityReferenceResolver<String> resolver;
    private EntityReferenceSerializer<String> serializer;
    private Logger logger;

    private QueryManager queryManager;
    private BaseClass xClass;
    private DocumentReference xClassRef;
    private WikiReference wikiRef;
    private XWikiContext context;
    private XWiki xwiki;
    private String dataSpace;
    private String appName;
    private ContextualAuthorizationManager authorization;

    public AWMApplication(XWikiContext context,
            ContextualAuthorizationManager authorizationManager,
            EntityReferenceResolver<String> resolver,
            EntityReferenceSerializer<String> serializer,
            QueryManager queryManager,
            Logger logger,
            DocumentReference appWebHomeRef) throws XWikiException
    {
        this.context = context;
        this.queryManager = queryManager;
        this.resolver = resolver;
        this.serializer = serializer;
        this.logger = logger;
        this.xwiki = context.getWiki();
        this.authorization = authorizationManager;

        // Get the class reference from the AppWithinMinutes.LiveTableClass object
        BaseObject item;
        if(appWebHomeRef != null) {
            item = getAWMObject(context, appWebHomeRef);
        }
        else {
            item = getAWMObject(context, serializer);
        }
        if(appWebHomeRef != null)
            this.wikiRef = appWebHomeRef.getWikiReference();
        else
            this.wikiRef = context.getDoc().getDocumentReference().getWikiReference();
        DocumentReference classReference = new DocumentReference(resolver.resolve(item.getStringValue("class"), EntityType.DOCUMENT, this.wikiRef));
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
    public Map<String, Object> getSchema() throws XWikiException
    {
        if(!this.authorization.hasAccess(Right.VIEW, xClass.getReference())) {
            return new HashMap<>();
        }
        return ApplicationSchema.getAppSchema(xClass, context, logger);
    }

    @Override
    public ItemMap getItem(String itemId) throws Exception {
        ItemMap value = new ItemMap();
        try {
            String objId = this.dataSpace+"."+itemId;
            XWikiDocument xDoc = this.getDocFromId(objId);
            BaseObject xObj = this.getObjectFromId(objId);
            ApplicationItem item = this.getApplicationItem(itemId, 0, xObj, xDoc);
            value = item.getItemMap();
        } catch (AccessDeniedException e) {
        } catch (Exception e) {
            logger.error("Unable to load the item [{}] : [{}]", itemId, e.toString());
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
        try {
            String xClassFullName = serializer.serialize(xClassRef);
            String awmWhereClause = "doc.space = '" + this.dataSpace + "'";
            Query query = QueryItems.getQuery(queryManager, xClassFullName, options, awmWhereClause, "doc.name");
            List<String> objDocList = query.setWiki(this.wikiRef.getName()).execute();
            for (int i = 0; i < objDocList.size(); ++i) {
                // Get all instances of the class in the document
                String docName = objDocList.get(i);
                String docFullName = dataSpace+"."+docName;
                try {
                    XWikiDocument xDoc = this.getDocFromId(docFullName);
                    BaseObject xObj = this.getObjectFromId(docFullName);
                    if (xObj != null) {
                        ApplicationItem item = this.getApplicationItem(docName, 0, xObj, xDoc);
                        ItemMap map = item.getItemMap();
                        value.put(map.getId(), map);
                    }
                } catch (AccessDeniedException e) {
                } catch (Exception e) {
                    logger.error("Unable to load the item [{}] : [{}]", docName, e.toString());
                }
            }
        } catch (QueryException e) {
            logger.error("Unable to get the list of items", e);
        }
        return value;
    }

    @Override
    public Map<String, Object> storeItem(ItemMap itemData) throws Exception {
        String itemId = itemData.getId();
        String objName = dataSpace+"."+itemId; // The XWiki object name is the document full name
        DocumentReference itemDocRef = new DocumentReference(resolver.resolve(objName, EntityType.DOCUMENT, this.wikiRef));
        try {
            this.authorization.checkAccess(Right.EDIT, itemDocRef);
            XWikiDocument xDoc = this.getDocFromId(objName);
            BaseObject xObj = this.getObjectFromId(objName);
            ApplicationItem item = this.getApplicationItem(objName, 0, xObj, xDoc);
            return item.store(itemData);
        } catch(AccessDeniedException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("Error", e.getMessage());
            return errorMap;
        }
    }

    @Override
    public Map<String, Object> deleteItem(String itemId) throws Exception {
        String objName = dataSpace+"."+itemId; // The XWiki object name is the document full name
        DocumentReference itemDocRef = new DocumentReference(resolver.resolve(objName, EntityType.DOCUMENT, this.wikiRef));
        try {
            this.authorization.checkAccess(Right.EDIT, itemDocRef);
            XWikiDocument xDoc = this.getDocFromId(objName);
            BaseObject xObj = this.getObjectFromId(objName);
            ApplicationItem item = this.getApplicationItem(objName, 0, xObj, xDoc);
            return item.delete();
        } catch(AccessDeniedException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("Error", e.getMessage());
            return errorMap;
        }
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
     * @param appWebHomeRef the reference of the application's WebHome
     * @return true is the reference represent an AWM app
     * @throws XWikiException
     */
    public static DocumentReference isAWM(XWikiContext context,
            DocumentReference appWebHomeRef) throws XWikiException
    {
        BaseObject awmObj = getAWMObject(context, appWebHomeRef);
        if (awmObj == null) {
            return null;
        }
        return awmObj.getDocumentReference();
    }

    /**
     * Get the LiveTableClass object of the AWM app of the current document.
     * @param context the wiki context
     * @param serializer the serializer of References into String
     * @return the AppWIthiMinutes.LiveTableClass object
     * @throws XWikiException
     */
    public static BaseObject getAWMObject(XWikiContext context,
            EntityReferenceSerializer<String> serializer) throws XWikiException
    {
        // Check if the current document is in a subspace (Code/Data/etc.) of an AppWithinMinutes application
        DocumentReference docRef = context.getDoc().getDocumentReference();
        String wikiId = docRef.getWikiReference().getName();
        List<SpaceReference> spacesRef = docRef.getSpaceReferences();
        SpaceReference lastAncestor = spacesRef.get(0);
        // Top-level space's WebHome :
        String webHomeSpace = serializer.serialize(lastAncestor, "local");
        BaseObject awmObj = null;
        // Start compatibility code for XWiki < 7.2 (without Nested spaces)
        // --> AWM code space is a top-level space named "{ApplicationName}Code"
        if (webHomeSpace.endsWith("Code") && webHomeSpace.length() > 4) {
            String appNameTmp = webHomeSpace.substring(0, webHomeSpace.length()-4);
            DocumentReference appWebHomeRef = new DocumentReference(wikiId,
                    appNameTmp,
                    "WebHome");
            awmObj = getAWMObject(context, appWebHomeRef);
        }
        // End compatibility code
        if (awmObj == null) {
            DocumentReference appWebHomeRef = new DocumentReference(wikiId, webHomeSpace, "WebHome");
            awmObj = getAWMObject(context, appWebHomeRef);
        }
        return awmObj;
    }
    /**
     * Get the LiveTableClass object of the selected AWM app.
     * @param context the wiki context
     * @param appWebHomeRef the reference of the application's WebHome
     * @return the AppWIthiMinutes.LiveTableClass object
     * @throws XWikiException
     */
    public static BaseObject getAWMObject(XWikiContext context,
            DocumentReference appWebHomeRef) throws XWikiException
    {
        WikiReference wikiRef = appWebHomeRef.getWikiReference();
        DocumentReference awmClassRef = new DocumentReference(wikiRef.getName(), "AppWithinMinutes", "LiveTableClass");
        return context.getWiki().getDocument(appWebHomeRef, context).getXObject(awmClassRef);
    }

    /**
     * @param itemId the id of an XWiki object
     * @return the XWikiDocument containing the object
     * @throws XWikiException
     */
    public XWikiDocument getDocFromId(String itemId) throws XWikiException
    {
        DocumentReference itemDocRef = new DocumentReference(this.resolver.resolve(itemId, EntityType.DOCUMENT, this.wikiRef));
        return this.xwiki.getDocument(itemDocRef, this.context);
    }
    /**
     * @param itemId the id of an XWiki object
     * @return the XWiki object represented by the id
     * @throws XWikiException
     */
    private BaseObject getObjectFromId(String itemId) throws XWikiException, AccessDeniedException
    {
        XWikiDocument xDoc = this.getDocFromId(itemId);
        if (xDoc == null) {
            return null;
        }
        this.authorization.checkAccess(Right.VIEW, xDoc.getDocumentReference());
        return xDoc.getXObject(this.xClassRef);
    }

    private ApplicationItem getApplicationItem(String objName, Integer objNumber, BaseObject xObj, XWikiDocument xDoc) throws XWikiException {
        return new ApplicationItem(objName, objNumber, xDoc, xObj, this.xClass, this.context, this.resolver, this.serializer, this.logger);
    }

    @Override
    public String toString()
    {
        return "AppWithinMinutes application : " + this.appName;
    }
}
