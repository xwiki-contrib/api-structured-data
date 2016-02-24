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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.structureddata.Application;
import org.xwiki.structureddata.internal.AWMApplication;
import org.xwiki.structureddata.internal.DocumentMap;
import org.xwiki.structureddata.internal.ItemMap;

/**
 * Rest ressource for Application in the current wiki.
 * 
 * @version $Id$
 */
@Component("org.xwiki.structureddata.internal.resources.CurrentApplicationResource")
@Path("/applications/current/{pageFullName}")
@Produces({ MediaType.APPLICATION_JSON })
public class CurrentApplicationResource extends XWikiResource
{
    @Inject
    private EntityReferenceResolver<String> resolver;

    @Inject
    private Logger appLogger;

    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> serializer;

    @Inject
    ContextualAuthorizationManager authorization;

    @GET
    public Map<String, Object> getCurrent(@PathParam("pageFullName") String pageFullName) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        return ApplicationResource.getResource(app);
    }

    @Path("/schema")
    @GET
    public Map<String, Object> getSchema(@PathParam("pageFullName") String pageFullName) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        return app.getSchema();
    }

    @Path("/items")
    @GET
    public Map<String, Object> getItems(@PathParam("pageFullName") String pageFullName,
                                        @QueryParam("limit") String limit,
                                        @QueryParam("offset") String offset,
                                        @QueryParam("query") String query,
                                        @QueryParam("hidden") String hidden) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        return ItemsResource.getResource(app, limit, offset, query, hidden);
    }

    @Path("/items/{itemId}")
    @GET
    public Map<String, Object> getItem(@PathParam("pageFullName") String pageFullName,
            @PathParam("itemId") String itemId) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        return app.getItem(itemId);
    }

    @Path("/items/{itemId}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    public Map<String, Object> storeItem(@PathParam("pageFullName") String pageFullName,
            @PathParam("itemId") String itemId,
            String jsonRequest) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        ItemMap item = app.getItem(itemId);
        ItemMap newItemData = new ObjectMapper().readValue(jsonRequest, ItemMap.class);
        ApplicationRestTools.updateMapFromJson(newItemData, item);
        return app.storeItem(item);
    }

    @Path("/items/{itemId}")
    @DELETE
    public Map<String, Object> deleteItem(@PathParam("pageFullName") String pageFullName,
            @PathParam("itemId") String itemId) throws Exception
    {
        Application app = getApplication(pageFullName);
        return app != null ? app.deleteItem(itemId) : null;
    }

    @Path("/items/{itemId}/document")
    @GET
    public Map<String, Object> getItemDocument(@PathParam("pageFullName") String pageFullName,
            @PathParam("itemId") String itemId) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        return app.getItem(itemId).getDocumentFields();
    }

    @Path("/items/{itemId}/document")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    public Map<String, Object> storeItemDocument(@PathParam("pageFullName") String pageFullName,
            @PathParam("itemId") String itemId,
            String jsonRequest) throws Exception
    {
        Application app = getApplication(pageFullName);
        if(app == null)
            return new HashMap<>();
        DocumentMap docData = new ObjectMapper().readValue(jsonRequest, DocumentMap.class);
        ItemMap item = app.getItem(itemId);
        DocumentMap oldDocData = item.getDocumentFields();
        ApplicationRestTools.updateMapFromJson(docData, oldDocData);
        return app.storeItem(item);
    }

    private Application getApplication(String pageFullName) throws XWikiException
    {
        XWikiContext context = xcontextProvider.get();
        DocumentReference pageRef = new DocumentReference(resolver.resolve(pageFullName, EntityType.DOCUMENT));
        XWikiDocument xDoc = context.getWiki().getDocument(pageRef, context);
        context.setDoc(xDoc);
        DocumentReference awmWebHomeRef = AWMApplication.isAWM(context, serializer);
        if(awmWebHomeRef != null) {
            return new AWMApplication(context, authorization, resolver, serializer, queryManager, appLogger, awmWebHomeRef);
        }
        return null;
    }
}
