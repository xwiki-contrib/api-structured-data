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
package org.xwiki.structureddata;

import java.util.Map;
import org.xwiki.structureddata.internal.ItemMap;

/**
 * Interface if the Applications.
 * 
 * @version $Id$
 */
public interface Application 
{
    /**
     * Get the application's data structure.
     * @return the schema map
     * @throws Exception 
     */
    Map<String, Object> getSchema() throws Exception;
    
    /**
     * Get an item of the application.
     * @param itemId the string id of the item
     * @return the map representing the item
     * @throws Exception 
     */
    ItemMap getItem(String itemId) throws Exception;
    
    /**
     * Get the items of the application.
     * @return a map with all items
     * @throws Exception 
     */
    Map<String, Object> getItems() throws Exception;
    
    /**
     * Get some items of the application.
     * @param options a map with query options (limit, offset, and query ("where" clause))
     * @return a map with all items
     * @throws Exception 
     */
    Map<String, Object> getItems(Map<String, Object> options) throws Exception;
    
    /**
     * Store an item of the application in the wiki.
     * @param itemData the data of the item
     * @return the state of the save (Success/Error)
     * @throws Exception 
     */
    Map<String, Object> storeItem(ItemMap itemData) throws Exception;
    
    /**
     * Delete an item of the application from the wiki.
     * @param itemId the string if of the item
     * @return the state of the deletion (Success/Error)
     * @throws Exception 
     */
    Map<String, Object> deleteItem(String itemId) throws Exception;

    /**
     * @return data about the application
     */
    String toString();

}
