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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

/**
 * Rest ressource for the list of classes in the wiki.
 * 
 * @version $Id$
 */
@Component("org.xwiki.structureddata.internal.resources.ApplicationsResource")
@Path("/applications/")
@Produces({ MediaType.APPLICATION_JSON })
public class ApplicationsResource extends XWikiResource 
{
    /**
     * Get a list of the classes/applications in the wiki.
     * @return a map containing the list of classes
     * @throws XWikiException 
     */
    @GET
    public Map<String, Object> get() throws XWikiException
    {
        Map<String, Object> result = new HashMap<>();
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        List<String> classList = xwiki.getClassList(context);
        result.put("Applications list", classList);
        return result;
    }
}
