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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.structureddata.Application;

/**
 * Rest ressource for the list of items in an Application.
 * 
 * @version $Id$
 */
public class ItemsResource 
{
    /**
     * Get the list of items of an application.
     * @param app the application object
     * @param limit the maximum number of results to display
     * @param offset the offset for the results to display
     * @param query a query filter for the result (HQL "where" clause)
     * @return a map with the items
     * @throws Exception 
     */
    protected static Map<String, Object> getResource(Application app, String limit, String offset, String query) throws Exception
    {
        Map<String, Object> options = new HashMap<>();
        if (limit != null) {
            options.put("limit", Integer.parseInt(limit));
        }
        if (offset != null) {
            options.put("offset", Integer.parseInt(offset));
        }
        if (query != null) {
            options.put("query", query);
        }
        return app.getItems(options);
    }
}
