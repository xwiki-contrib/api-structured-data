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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
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
    private DocumentReferenceResolver<String> resolver;
    private EntityReferenceSerializer<String> serializer;
    private Logger logger;

    private BaseClass xClass;
    private DocumentReference xClassRef;
    private String xClassFullName;
    private XWikiContext context;
    private XWiki xwiki;

    /**
     * Create a new object describing the application represented by a class reference.
     * @param context the context in the running wiki
     * @param resolver the Document reference resolver
     * @param serializer 
     * @param queryManager the XWiki query manager
     * @param logger 
     * @param classReference the reference of the class
     * @throws XWikiException 
     */
    public DefaultApplication(XWikiContext context, 
            DocumentReferenceResolver<String> resolver, 
            EntityReferenceSerializer<String> serializer,
            QueryManager queryManager,
            Logger logger, 
            DocumentReference classReference) throws XWikiException 
    {
        this.context = context;
        this.queryManager = queryManager;
        this.resolver = resolver;
        this.serializer = serializer;
        this.logger = logger;
        this.xwiki = this.context.getWiki();
        this.xClassRef = classReference;
        this.xClass = this.xwiki.getXClass(this.xClassRef, context);
        this.xClassFullName = serializer.serialize(xClassRef);
    }

    @Override
    public Map<String, Object> getAppSchema() throws XWikiException 
    {
        return ApplicationSchema.getAppSchema(xClass, context, logger);
    }

    
    @Override
    public ItemMap getItem(String objId) 
    {
        ItemMap value = new ItemMap();
        try {
            String objName = this.getDocNameFromId(objId);
            Integer objNumber = this.getObjNumberFromId(objId);
            BaseObject xObj = this.getObjectFromId(objId);
            ApplicationItem item = new ApplicationItem(objName, objNumber, xObj, this.xClass, this.context, this.resolver, this.logger);
            value = item.getItemMap(false);
        } catch (Exception e) {
            logger.error("Unable to load the item.", e);
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
        String queryOpt = "query";
        String limitOpt = "limit";
        String offsetOpt = "offset";
        try {
            // Create a filter to remove class templates from the results
            String templateFilter = "and item.name <> '" +  this.xClassFullName + "Template' ";
            if (this.xClassFullName.length() > 5 && this.xClassFullName.endsWith("Class")) {
                String shortClassName = this.xClassFullName.substring(0, this.xClassFullName.length()-5);
                templateFilter += "and item.name <> '" + shortClassName + "Template' ";
            }
            // Search query
            String queryString = "select item.name, item.number "
                    + "from Document doc, doc.object( " + this.xClassFullName + " ) as item "
                    + "where 1=1 " + templateFilter;
            // Add the additional filter from the "options" parameter
            if (options.containsKey(queryOpt)) {
                String whereClause = (String) options.get(queryOpt);
                queryString += "and " + whereClause;
            }
            queryString += " order by item.name, item.number";
            // Execute the query
            Query query = this.queryManager.createQuery(queryString, Query.XWQL);
            // Filter the results depending on optionnal parameters
            if (options.containsKey(limitOpt)) {
                query = query.setLimit((Integer) options.get(limitOpt));
            }
            if (options.containsKey(offsetOpt)) {
                query = query.setOffset((Integer) options.get(offsetOpt));
            }
            List<Object[]> objDocList = query.execute();
            for (int i = 0; i < objDocList.size(); ++i) {
                // Get all instances of the class in the document
                String objName = (String) objDocList.get(i)[0];
                Integer objNum = (Integer) objDocList.get(i)[1];
                DocumentReference docRef = this.resolver.resolve(objName);
                try {
                    BaseObject xObj = this.xwiki.getDocument(docRef, this.context).getXObject(this.xClassRef, objNum);
                    if (xObj != null) {
                        ApplicationItem item = new ApplicationItem(objName, objNum, xObj, this.xClass, this.context, this.resolver, this.logger);
                        ItemMap properties = item.getItemMap(true);
                        value.put("Item" + i, properties);
                    }
                } catch (Exception e) {
                    logger.warn("Unable to load the item number [{}]", i, e);
                }
            }
        } catch (QueryException e) {
            logger.error("Unable to get the list of items", e);
        }
        return value;
    }

    @Override
    public Map<String, Object> storeItem(String itemId, ItemMap itemData) throws Exception {
        String objName = this.getDocNameFromId(itemId);
        Integer objNumber = this.getObjNumberFromId(itemId);
        BaseObject xObj = this.getObjectFromId(itemId);
        ApplicationItem item = new ApplicationItem(objName, objNumber, xObj, this.xClass, this.context, this.resolver, this.logger);
        return item.store(itemData);
    }

    @Override
    public Map<String, Object> deleteItem(String itemId) throws Exception {
        String objName = this.getDocNameFromId(itemId);
        Integer objNumber = this.getObjNumberFromId(itemId);
        BaseObject xObj = this.getObjectFromId(itemId);
        ApplicationItem item = new ApplicationItem(objName, objNumber, xObj, this.xClass, this.context, this.resolver, this.logger);
        return item.delete();
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
        DocumentReference itemDocRef = this.resolver.resolve(docName);
        return this.xwiki.getDocument(itemDocRef, this.context);
    }
    protected BaseObject getObjectFromId(String objId) throws XWikiException {
        Integer objNumber = this.getObjNumberFromId(objId);
        XWikiDocument xDoc = this.getDocFromId(objId);
        if (xDoc == null) {
            return null;
        }
        return xDoc.getXObject(this.xClassRef, objNumber);
    }
    
    @Override
    public String toString() {
        return "XWiki class : " + this.xClassFullName;
    }
}
