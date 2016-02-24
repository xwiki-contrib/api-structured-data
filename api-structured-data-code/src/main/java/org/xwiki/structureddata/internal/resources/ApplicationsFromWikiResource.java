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
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import java.util.HashMap;
import java.util.List;
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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.structureddata.Application;
import org.xwiki.structureddata.internal.AWMApplication;
import org.xwiki.structureddata.internal.DefaultApplication;
import org.xwiki.structureddata.internal.DocumentMap;
import org.xwiki.structureddata.internal.ItemMap;

/**
 * Rest ressource for Application in the selected wiki.
 * 
 * @version $Id$
 */
@Component("org.xwiki.structureddata.internal.resources.ApplicationsFromWikiResource")
@Path("/wikis/{wikiName}/applications/")
@Produces({ MediaType.APPLICATION_JSON })
public class ApplicationsFromWikiResource extends XWikiResource
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

    /**
     * Get a list of the classes/applications in the wiki.
     * @param wikiName the name of the selected wiki
     * @return a map containing the list of classes
     * @throws XWikiException
     */
    @GET
    public Map<String, Object> getAppList(@PathParam("wikiName") String wikiName) throws Exception
    {
        XWikiContext context = xcontextProvider.get();
        Map<String, Object> result = new HashMap<>();
        String oldWikiId = context.getWikiId();
        context.setWikiId(wikiName);
        XWiki xwiki = context.getWiki();
        List<String> classList = xwiki.getClassList(context);
        String queryString = "select doc.space"
                + " from Document doc, doc.object(AppWithinMinutes.LiveTableClass) as item"
                + " where doc.fullName <> 'AppWithinMinutes.LiveTableTemplate'"
                + " order by doc.space";
        try {
            Query query = queryManager.createQuery(queryString, Query.XWQL);
            List<String> awmList = query.setWiki(wikiName).execute();
            result.put("AWM Applications", awmList);
        } catch(QueryException e) {
            result.put("Error while searching AWM applications list", "Error: "+e);
        }
        result.put("XWiki Classes", classList);
        context.setWikiId(oldWikiId);
        return result;
    }

    @Path("{appName}")
    @GET
    public Map<String, Object> get(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        return ApplicationResource.getResource(app);
    }

    @Path("{appName}/schema")
    @GET
    public Map<String, Object> getSchema(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        return app.getSchema();
    }

    @Path("{appName}/items")
    @GET
    public Map<String, Object> getItems(@PathParam("wikiName") String wikiName,
                                        @PathParam("appName") String appId,
                                        @QueryParam("limit") String limit,
                                        @QueryParam("offset") String offset,
                                        @QueryParam("query") String query,
                                        @QueryParam("hidden") String hidden) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        return ItemsResource.getResource(app, limit, offset, query, hidden);
    }

    @Path("{appName}/items/{itemId}")
    @GET
    public Map<String, Object> getItem(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId,
            @PathParam("itemId") String itemId) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        return app.getItem(itemId);
    }

    @Path("{appName}/items/{itemId}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    public Map<String, Object> storeItem(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId,
            @PathParam("itemId") String itemId,
            String jsonRequest) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        ItemMap item = app.getItem(itemId);
        ItemMap newItemData = new ObjectMapper().readValue(jsonRequest, ItemMap.class);
        ApplicationRestTools.updateMapFromJson(newItemData, item);
        return app.storeItem(item);
    }

    @Path("{appName}/items/{itemId}")
    @DELETE
    public Map<String, Object> deleteItem(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId,
            @PathParam("itemId") String itemId) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        return app.deleteItem(itemId);
    }

    @Path("{appName}/items/{itemId}/document")
    @GET
    public Map<String, Object> getItemDocument(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId,
            @PathParam("itemId") String itemId) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        return app.getItem(itemId).getDocumentFields();
    }

    @Path("{appName}/items/{itemId}/document")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON })
    public Map<String, Object> storeItemDocument(@PathParam("wikiName") String wikiName,
            @PathParam("appName") String appId,
            @PathParam("itemId") String itemId,
            String jsonRequest) throws Exception
    {
        Application app = getApplication(wikiName, appId);
        DocumentMap docData = new ObjectMapper().readValue(jsonRequest, DocumentMap.class);
        ItemMap item = app.getItem(itemId);
        DocumentMap oldDocData = item.getDocumentFields();
        ApplicationRestTools.updateMapFromJson(docData, oldDocData);
        return app.storeItem(item);
    }

    private Application getApplication(String wikiName, String appId) throws Exception
    {
        XWikiContext context = xcontextProvider.get();
        Application newApp;
        DocumentReference awmWebHomeRef = ApplicationRestTools.getAWMRef(context, wikiName, appId);
        if(AWMApplication.isAWM(context, awmWebHomeRef) != null) {
            newApp = new AWMApplication(context, authorization, resolver, serializer, queryManager, appLogger, awmWebHomeRef);
        }
        else {
            DocumentReference classRef = ApplicationRestTools.getClassRef(wikiName, appId, resolver);
            newApp = new DefaultApplication(context, authorization, resolver, serializer, queryManager, appLogger, classRef);
        }
        return newApp;
    }
}
