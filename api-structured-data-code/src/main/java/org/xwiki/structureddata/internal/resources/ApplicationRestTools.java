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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;

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
}
