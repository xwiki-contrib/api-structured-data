/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xwiki.structureddata.internal;

import java.util.Map;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Get the query returning the list of items for a given Application.
 *
 * @version $Id$
 */
public class QueryItems {
    protected static Query getQuery(QueryManager queryManager, String xClassFullName, Map<String, Object> options, String appWhereClause, String appSelectClause) throws QueryException
    {
        String queryOpt = "query";
        String hiddenOpt = "hidden";
        String limitOpt = "limit";
        String offsetOpt = "offset";
        // Create a filter to remove class templates from the results
        // /!\ Templates can be named ApplicationClassTemplate or ApplicationTemplate
        String templateFilter = "and item.name <> '" +  xClassFullName + "Template' ";
        if (xClassFullName.length() > 5 && xClassFullName.endsWith("Class")) {
            String shortClassName = xClassFullName.substring(0, xClassFullName.length()-5);
            templateFilter += "and item.name <> '" + shortClassName + "Template' ";
        }
        // Main search query
        String queryString = "select " + appSelectClause
                + " from Document doc, doc.object( '" + xClassFullName + "' ) as item "
                + "where " + appWhereClause + templateFilter;
        // Add the additional filter from the "options" parameter
        if (options.containsKey(queryOpt)) {
            String whereClause = (String) options.get(queryOpt);
            queryString += " and " + whereClause;
        }
        // Hide the hidden documents except if it is explicitly requested to display them
        if (options.containsKey(hiddenOpt)) {
            if(options.get(hiddenOpt) != "true" && options.get(hiddenOpt) != "1") {
                queryString += " and (doc.hidden <> true or doc.hidden is null)";
            }
        }
        else {
            queryString += " and (doc.hidden <> true or doc.hidden is null)";
        }
        queryString += " order by " + appSelectClause;
        // Execute the query
        Query query = queryManager.createQuery(queryString, Query.XWQL);
        // Filter the results depending on optional parameters
        if (options.containsKey(limitOpt)) {
            query = query.setLimit((Integer) options.get(limitOpt));
        }
        if (options.containsKey(offsetOpt)) {
            query = query.setOffset((Integer) options.get(offsetOpt));
        }

        return query;
    }
}
