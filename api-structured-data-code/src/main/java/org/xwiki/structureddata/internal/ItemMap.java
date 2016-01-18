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
package org.xwiki.structureddata.internal;

import java.util.HashMap;

/**
 * Change the value of an item using set().
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ItemMap extends HashMap<String, Object> {

    private String apiId;

    public String getId() {
        return apiId;
    }

    public void setId(String apiId) {
        this.apiId = apiId;
    }
    /**
     * Change the value associated to a key in the map.
     * @param key the String key
     * @param value the Object value
     */
    public void set(String key, Object value)
    {
        this.put(key, value);
    }
}
