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
package org.xwiki.structureddata.script;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.xwiki.structureddata.internal.DefaultApplication;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.QueryManager;
import org.xwiki.structureddata.internal.AWMApplication;
import org.xwiki.structureddata.Application;

/**
 * Make the Application API available to scripting.
 * 
 * @version $Id$
 */
@Component
@Named("xapp")
@Singleton
public class XAppScriptService implements ScriptService
{
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected QueryManager queryManager;

    @Inject
    protected DocumentReferenceResolver<String> resolver;
    
    @Inject
    @Named("local")
    protected EntityReferenceSerializer<String> serializer;
    
    @Inject
    private Logger logger;

    /**
     * Get an Application with the name of its class.
     * @param appId the id of the class (AWM id or class full name)
     * @return the Application
     * @throws XWikiException 
     */
    public Application getApp(String appId) throws XWikiException
    {
        XWikiContext context = this.xcontextProvider.get();
        
        Application newApp;
        DocumentReference awmWebHomeRef = new DocumentReference(context.getWikiId(), appId, "WebHome");
        if(AWMApplication.isAWM(context, serializer, awmWebHomeRef) != null) {
            newApp = new AWMApplication(context, resolver, serializer, logger, awmWebHomeRef);
        }
        else {
            newApp = new DefaultApplication(context, resolver, serializer, queryManager, logger, resolver.resolve(appId));
        }
        return newApp;
    }

    /**
     * Get an Application with the name of its class.
     * @param classReference the reference of the class
     * @return the Application
     * @throws XWikiException 
     */
    public Application getApp(DocumentReference classReference) throws XWikiException
    {
        XWikiContext context = this.xcontextProvider.get();
        
        Application newApp = new DefaultApplication(context, resolver, serializer, queryManager, logger, classReference);
        return newApp;
    }

    /**
     * Get the current AppWithinMinutes application.
     * @return the Application
     * @throws XWikiException 
     */
    public Application getCurrent() throws XWikiException
    {
        XWikiContext context = this.xcontextProvider.get();
        
        Application newApp = null;
        DocumentReference awmWebHomeRef = AWMApplication.isAWM(context, serializer);
        if(awmWebHomeRef != null) {
            newApp = new AWMApplication(context, resolver, serializer, logger, awmWebHomeRef);
        }

        return newApp;
    }

}
