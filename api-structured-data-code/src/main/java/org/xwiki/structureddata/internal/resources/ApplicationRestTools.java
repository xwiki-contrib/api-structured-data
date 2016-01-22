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
import org.xwiki.structureddata.internal.DocumentMap;

/**
 * Tools used by Application rest resources.
 * 
 * @version $Id$
 */
public class ApplicationRestTools {
    protected static final DocumentReference getAWMRef(XWikiContext context, String wikiName, String appId)
    {
        if(wikiName != null) {
            return new DocumentReference(wikiName, appId, "WebHome");
        }
        return new DocumentReference(context.getWikiId(), appId, "WebHome");
    }

    protected static final DocumentReference getClassRef(XWikiContext context, String wikiName, String appId, EntityReferenceResolver<String> resolver)
    {
        if(wikiName != null) {
            WikiReference wikiRef = new WikiReference(resolver.resolve(wikiName, EntityType.WIKI));
            return new DocumentReference(resolver.resolve(appId, EntityType.DOCUMENT, wikiRef));
        }
        return new DocumentReference(resolver.resolve(appId, EntityType.DOCUMENT));
    }

    protected static final void updateMapFromJson(Map<String, Object> json, Map<String, Object> toUpdate) {
        for(Map.Entry<String, Object> e : json.entrySet()) {
            String key = e.getKey();
            try {
                Object newVal;
                Object oldVal = toUpdate.get(key);
                String className = oldVal.getClass().getSimpleName();
                // Dates from JSON are represented as an Integer or a Long value, they have to be converted to a Date
                if(className.equals("Date")) {
                    String jsonType = json.get(key).getClass().getSimpleName();
                    if(jsonType.equals("Long")) {
                        newVal = new Date((Long) json.get(key));
                    }
                    else {
                        newVal = new Date(((Integer) json.get(key)).longValue());
                    }
                }
                // Get the string value of numbers in order to be able to convert them to the right type
                // e.g : 2.0 (Float) is converted to 2 (Integer) in JSON and should then be converted back to Float
                else if(className.equals("Float")) {
                    newVal = Float.parseFloat(json.get(key).toString());
                }
                else if(className.equals("Double")) {
                    newVal = Double.parseDouble(json.get(key).toString());
                }
                else if(className.equals("Long")) {
                    newVal = Long.parseLong(json.get(key).toString());
                }
                else {
                    newVal = json.get(key);
                }
                if(!newVal.equals(oldVal)) {
                    if(toUpdate.getClass().getSimpleName().equals("DocumentMap")) {
                        ((DocumentMap) toUpdate).set(key, newVal);
                    }
                    else {
                        toUpdate.put(key, newVal);
                    }
                }
            } catch (Exception f) {
            }
        }
    }
}
