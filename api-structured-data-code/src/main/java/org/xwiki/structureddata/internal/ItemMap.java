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

import com.xpn.xwiki.doc.XWikiDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.structureddata.DataMap;

/**
 * Change the value of an item using set().
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ItemMap extends HashMap<String, Object> implements DataMap {

    public static final String AUTHOR = "author";
    public final static String CREATOR = "creator";
    public final static String CREATION = "creationDate";
    public final static String UPDATE = "updateDate";
    public final static String PARENT = "parent";
    public final static String HIDDEN = "hidden";
    public final static String TITLE = "title";
    public final static String CONTENT = "content";

    private String apiId;
    private DocumentMap docMap;

    public String getId() {
        return apiId;
    }

    public void setId(String apiId) {
        this.apiId = apiId;
    }
    /**
     * Change the value associated to a key in the map.
     * @param key the String key
     * @param value the Object value
     */
    @Override
    public void set(String key, Object value)
    {
        this.put(key, value);
    }

    protected void setXDoc(XWikiDocument xDoc, EntityReferenceSerializer<String> serializer) {
        DocumentMap docMapTmp = new DocumentMap();
        String xwikiId;
        try {
            xwikiId = xDoc.getDocumentReference().getWikiReference().getName() + ":";
        } catch(Exception e) {
            xwikiId = "";
        }
        docMapTmp.put(AUTHOR, xwikiId + serializer.serialize(xDoc.getAuthorReference()));
        docMapTmp.put(CREATOR, xwikiId + serializer.serialize(xDoc.getCreatorReference()));
        docMapTmp.put(CREATION, xDoc.getCreationDate());
        docMapTmp.put(UPDATE, xDoc.getContentUpdateDate());
        docMapTmp.put(PARENT, xwikiId + serializer.serialize(xDoc.getParentReference()));
        docMapTmp.put(HIDDEN, xDoc.isHidden());
        docMapTmp.put(TITLE, xDoc.getTitle());
        docMapTmp.put(CONTENT, xDoc.getContent());
        this.docMap = docMapTmp;
    }

    /**
     * Get some properties of the document containing the item : author, creator, creationDate, updateDate, parent,
     * hidden, title, content
     * @return the document map
     */
    public DocumentMap getDocumentFields() {
        return docMap;
    }

    /**
     * Get the selected document properties. Possible values are : author, creator, creationDate, updateDate, parent,
     * hidden, title, content
     * @param properties the list of properties to display in the result
     * @return the document map
     */
    public DocumentMap getDocumentFields(List<String> properties) {
        DocumentMap docMapFiltered = new DocumentMap();
        if(properties == null || properties.size() == 0) {
            return docMap;
        }
        for(String property : properties) {
            if(docMap.containsKey(property)) {
                docMapFiltered.put(property, docMap.get(property));
            }
        }
        return docMapFiltered;
    }
}
