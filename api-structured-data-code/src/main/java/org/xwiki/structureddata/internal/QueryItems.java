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
 *
 * @author Yann
 */
public class QueryItems {
    private static String queryOpt = "query";
    private static String limitOpt = "limit";
    private static String offsetOpt = "offset";
    protected static Query getQuery(QueryManager queryManager, String xClassFullName, Map<String, Object> options, String appWhereClause, String appSelectClause) throws QueryException
    {
        // Create a filter to remove class templates from the results
        String templateFilter = "and item.name <> '" +  xClassFullName + "Template' ";
        if (xClassFullName.length() > 5 && xClassFullName.endsWith("Class")) {
            String shortClassName = xClassFullName.substring(0, xClassFullName.length()-5);
            templateFilter += "and item.name <> '" + shortClassName + "Template' ";
        }
        // Search query
        String queryString = "select " + appSelectClause
                + " from Document doc, doc.object( " + xClassFullName + " ) as item "
                + "where " + appWhereClause + templateFilter;
        // Add the additional filter from the "options" parameter
        if (options.containsKey(queryOpt)) {
            String whereClause = (String) options.get(queryOpt);
            queryString += " and " + whereClause;
        }
        queryString += " order by " + appSelectClause;
        // Execute the query
        Query query = queryManager.createQuery(queryString, Query.XWQL);
        // Filter the results depending on optionnal parameters
        if (options.containsKey(limitOpt)) {
            query = query.setLimit((Integer) options.get(limitOpt));
        }
        if (options.containsKey(offsetOpt)) {
            query = query.setOffset((Integer) options.get(offsetOpt));
        }
        return query;
    }
}
