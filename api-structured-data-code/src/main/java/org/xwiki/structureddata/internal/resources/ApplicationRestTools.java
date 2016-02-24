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
package org.xwiki.structureddata.internal.resources;

import com.xpn.xwiki.XWikiContext;
import java.util.Date;
import java.util.Map;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.structureddata.DataMap;

/**
 * Tools used by Application rest resources.
 * 
 * @version $Id$
 */
public class ApplicationRestTools {
    protected static DocumentReference getAWMRef(XWikiContext context, String wikiName, String appId)
    {
        if(wikiName != null) {
            return new DocumentReference(wikiName, appId, "WebHome");
        }
        return new DocumentReference(context.getWikiId(), appId, "WebHome");
    }

    protected static DocumentReference getClassRef(String wikiName, String appId, EntityReferenceResolver<String> resolver)
    {
        if(wikiName != null) {
            WikiReference wikiRef = new WikiReference(resolver.resolve(wikiName, EntityType.WIKI));
            return new DocumentReference(resolver.resolve(appId, EntityType.DOCUMENT, wikiRef));
        }
        return new DocumentReference(resolver.resolve(appId, EntityType.DOCUMENT));
    }

    protected static void updateMapFromJson(Map<String, Object> json, DataMap oldMapToUpdate) {
        for(Map.Entry<String, Object> e : json.entrySet()) {
            String key = e.getKey();
            try {
                Object newVal;
                Object oldVal = ((Map<String, Object>) oldMapToUpdate).get(key);
                String className = "";
                if(oldVal != null) {
                    className = oldVal.getClass().getSimpleName();
                }
                // Dates from JSON are represented as an Integer or a Long value, they have to be converted to a Date
                switch (className) {
                    case "Date":
                        String jsonType = json.get(key).getClass().getSimpleName();
                        if(jsonType.equals("Long")) {
                            newVal = new Date((Long) json.get(key));
                        }
                        else {
                            newVal = new Date(((Integer) json.get(key)).longValue());
                        }   break;
                    case "Float":
                        newVal = Float.parseFloat(json.get(key).toString());
                        break;
                    case "Double":
                        newVal = Double.parseDouble(json.get(key).toString());
                        break;
                    case "Long":
                        newVal = Long.parseLong(json.get(key).toString());
                        break;
                    default:
                        newVal = json.get(key);
                        break;
                }
                if(!newVal.equals(oldVal)) {
                    oldMapToUpdate.set(key, newVal);
                }
            } catch (Exception f) {
            }
        }
    }
}
