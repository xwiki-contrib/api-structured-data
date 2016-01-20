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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.structureddata.Application;

/**
 * Default Application implementation.
 * 
 * @version $Id$
 */
public class DefaultApplication implements Application 
{

    private static final String PATTERN_ITEM_ID_SEPARATOR = "\\|";
    private static final String PATTERN_ITEM_ID_NUMBER = "\\|[0-9]+";

    private QueryManager queryManager;
    private ContextualAuthorizationManager authorization;
    private EntityReferenceResolver<String> resolver;
    private EntityReferenceSerializer<String> serializer;
    private Logger logger;

    private BaseClass xClass;
    private DocumentReference xClassRef;
    private WikiReference wikiRef;
    private String xClassFullName;
    private XWikiContext context;
    private XWiki xwiki;

    /**
     * Create a new object describing the application represented by a class reference.
     * @param context the context in the running wiki
     * @param authorizationManager 
     * @param resolver the Document reference resolver
     * @param serializer 
     * @param queryManager the XWiki query manager
     * @param logger 
     * @param classReference the reference of the class
     * @throws XWikiException 
     */
    public DefaultApplication(XWikiContext context,
            ContextualAuthorizationManager authorizationManager,
            EntityReferenceResolver<String> resolver, 
            EntityReferenceSerializer<String> serializer,
            QueryManager queryManager,
            Logger logger, 
            DocumentReference classReference) throws XWikiException 
    {
        this.context = context;
        this.queryManager = queryManager;
        this.authorization = authorizationManager;
        this.resolver = resolver;
        this.serializer = serializer;
        this.logger = logger;
        this.xwiki = this.context.getWiki();
        this.xClassRef = classReference;
        this.wikiRef = classReference.getWikiReference();
        this.xClass = this.xwiki.getXClass(this.xClassRef, context);
        this.xClassFullName = serializer.serialize(xClassRef);
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
    public ItemMap getItem(String itemId) 
    {
        ItemMap value = new ItemMap();
        try {
            String objName = this.getDocNameFromId(itemId);
            Integer objNumber = this.getObjNumberFromId(itemId);
            XWikiDocument xDoc = this.getDocFromId(itemId);
            BaseObject xObj = this.getObjectFromId(itemId);
            ApplicationItem item = this.getApplicationItem(objName, objNumber, xObj, xDoc);
            value = item.getItemMap();
        } catch (AccessDeniedException e) {
        } catch (Exception e) {
            logger.error("Unable to load the item [{}] : [{}]", itemId, e.toString());
        }
        return value;
    }
    
    @Override
    public Map<String, Object> getItems() throws XWikiException, QueryException 
    {
        Map<String, Object> options = new HashMap<>();
        return this.getItems(options);
    }
    
    @Override
    public Map<String, Object> getItems(Map<String, Object> options) throws QueryException, XWikiException {
        Map<String, Object> value = new HashMap<>();
        try {
            Query query = QueryItems.getQuery(queryManager, xClassFullName, options, "1=1", "item.name, item.number");
            List<Object[]> objDocList = query.setWiki(this.wikiRef.getName()).execute();
            for (int i = 0; i < objDocList.size(); ++i) {
                // Get all instances of the class in the document
                String objName = (String) objDocList.get(i)[0];
                Integer objNumber = (Integer) objDocList.get(i)[1];
                DocumentReference docRef = new DocumentReference(this.resolver.resolve(objName, EntityType.DOCUMENT, this.wikiRef));
                try {
                    this.authorization.checkAccess(Right.VIEW, docRef);
                    XWikiDocument xDoc = this.xwiki.getDocument(docRef, this.context);
                    BaseObject xObj = xDoc.getXObject(this.xClassRef, objNumber);
                    if (xObj != null) {
                        ApplicationItem item = this.getApplicationItem(objName, objNumber, xObj, xDoc);
                        ItemMap properties = item.getItemMap();
                        value.put(properties.getId(), properties);
                    }
                } catch (AccessDeniedException e) {
                } catch (Exception e) {
                    logger.error("Unable to load the item [{}] : [{}]", objName, e.toString());
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
        String objName = this.getDocNameFromId(itemId);
        DocumentReference itemDocRef = new DocumentReference(resolver.resolve(objName, EntityType.DOCUMENT, this.wikiRef));
        try {
            this.authorization.checkAccess(Right.EDIT, itemDocRef);
            Integer objNumber = this.getObjNumberFromId(itemId);
            XWikiDocument xDoc = this.getDocFromId(itemId);
            BaseObject xObj = this.getObjectFromId(itemId);
            ApplicationItem item = this.getApplicationItem(objName, objNumber, xObj, xDoc);
            return item.store(itemData);
        } catch(AccessDeniedException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("Error", e.getMessage());
            return errorMap;
        }
    }

    @Override
    public Map<String, Object> deleteItem(String itemId) throws Exception {
        String objName = this.getDocNameFromId(itemId);
        DocumentReference itemDocRef = new DocumentReference(resolver.resolve(objName, EntityType.DOCUMENT, this.wikiRef));
        try {
            this.authorization.checkAccess(Right.EDIT, itemDocRef);
            Integer objNumber = this.getObjNumberFromId(itemId);
            XWikiDocument xDoc = this.getDocFromId(itemId);
            BaseObject xObj = this.getObjectFromId(itemId);
            ApplicationItem item = this.getApplicationItem(objName, objNumber, xObj, xDoc);
            return item.delete();
        } catch(AccessDeniedException e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("Error", e.getMessage());
            return errorMap;
        }
    }
    
    protected String getDocNameFromId(String objId) {
        Pattern pattern = Pattern.compile(PATTERN_ITEM_ID_NUMBER);
        Matcher matcher = pattern.matcher(objId);
        while (matcher.find()) {
            return objId.split(PATTERN_ITEM_ID_SEPARATOR)[0];
        }
        return objId;
    }
    protected Integer getObjNumberFromId(String objId) {
        Pattern pattern = Pattern.compile(PATTERN_ITEM_ID_NUMBER);
        Matcher matcher = pattern.matcher(objId);
        while (matcher.find()) {
            return Integer.parseInt(objId.split(PATTERN_ITEM_ID_SEPARATOR)[1]);
        }
        return 0;
    }
    protected XWikiDocument getDocFromId(String objId) throws XWikiException {
        String docName = this.getDocNameFromId(objId);
        DocumentReference itemDocRef = new DocumentReference(this.resolver.resolve(docName, EntityType.DOCUMENT, this.wikiRef));
        return this.xwiki.getDocument(itemDocRef, this.context);
    }
    protected BaseObject getObjectFromId(String objId) throws XWikiException, AccessDeniedException {
        Integer objNumber = this.getObjNumberFromId(objId);
        XWikiDocument xDoc = this.getDocFromId(objId);
        if (xDoc == null) {
            return null;
        }
        this.authorization.checkAccess(Right.VIEW, xDoc.getDocumentReference());
        return xDoc.getXObject(this.xClassRef, objNumber);
    }

    private ApplicationItem getApplicationItem(String objName, Integer objNumber, BaseObject xObj, XWikiDocument xDoc) throws XWikiException {
        return new ApplicationItem(objName, objNumber, xDoc, xObj, this.xClass, this.context, this.resolver, this.serializer, this.logger);
    }
    
    @Override
    public String toString() {
        return "XWiki class : " + this.xClassFullName;
    }
}
