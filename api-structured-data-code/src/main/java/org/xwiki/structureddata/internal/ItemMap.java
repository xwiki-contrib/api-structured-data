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
import java.util.HashMap;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Change the value of an item using set().
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ItemMap extends HashMap<String, Object> {

    protected final static String AUTHOR = "author";
    protected final static String CREATOR = "creator";
    protected final static String CREATION = "creationDate";
    protected final static String UPDATE = "updateDate";
    protected final static String PARENT = "parent";
    protected final static String HIDDEN = "hidden";
    protected final static String TITLE = "title";
    protected final static String CONTENT = "content";

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
    public void set(String key, Object value)
    {
        this.put(key, value);
    }

    protected void setXDoc(XWikiDocument xDoc, EntityReferenceSerializer<String> serializer) {
        DocumentMap docMapTmp = new DocumentMap();
        docMapTmp.put(AUTHOR, xDoc.getAuthorReference());
        docMapTmp.put(CREATOR, xDoc.getCreatorReference());
        docMapTmp.put(CREATION, xDoc.getCreationDate());
        docMapTmp.put(UPDATE, xDoc.getContentUpdateDate());
        docMapTmp.put(PARENT, xDoc.getParentReference());
        docMapTmp.put(HIDDEN, xDoc.isHidden());
        docMapTmp.put(TITLE, xDoc.getTitle());
        docMapTmp.put(CONTENT, xDoc.getContent());
        this.docMap = docMapTmp;
    }

    public DocumentMap getDocumentFields() {
        return docMap;
    }
}
