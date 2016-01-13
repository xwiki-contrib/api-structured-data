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
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiResource;
import org.xwiki.structureddata.internal.DefaultApplication;
import org.xwiki.structureddata.Application;
import org.xwiki.structureddata.internal.AWMApplication;

/**
 * Rest ressource for the schema of an Application.
 * 
 * @version $Id$
 */
@Component("org.xwiki.structureddata.internal.resources.SchemaResource")
@Path("/applications/{appName}/schema/")
@Produces({ MediaType.APPLICATION_JSON })
public class SchemaResource extends XWikiResource
{
    @Inject
    private DocumentReferenceResolver<String> resolver;
    
    @Inject
    private Logger logger;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> serializer;
    
    /**
     * Get the data structure of the application.
     * @param appId the full name of the application or the AWM application id
     * @return a map with the data structure
     * @throws Exception 
     */
    @GET
    public Map<String, Object> get(@PathParam("appName") String appId) throws Exception
    {
        XWikiContext context = xcontextProvider.get();
        Application newApp;
        DocumentReference awmWebHomeRef = new DocumentReference(context.getWikiId(), appId, "WebHome");
        if(AWMApplication.isAWM(context, serializer, awmWebHomeRef) != null) {
            newApp = new AWMApplication(context, resolver, serializer, logger, awmWebHomeRef);
        }
        else {
            DocumentReference classRef = resolver.resolve(appId);
            newApp = new DefaultApplication(context, resolver, serializer, queryManager, logger, classRef);
        }
        return newApp.getAppSchema();
    }
}
