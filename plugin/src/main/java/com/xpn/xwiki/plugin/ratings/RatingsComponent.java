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
package com.xpn.xwiki.plugin.ratings;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * TODO DOCUMENT ME!
 * 
 * @version $Id:$
 */
@ComponentRole
public interface RatingsComponent
{
    RatingsManager getRatingsManager() throws RatingsException;

    List<Rating> getRatings(String documentName, int start, int count, boolean asc) throws RatingsException;

    Rating setRating(String documentName, String author, int vote) throws RatingsException;

    Rating getRating(String documentName, String author) throws RatingsException;

    AverageRating getAverageRating(String documentName, String method) throws RatingsException;

    AverageRating getAverageRating(String documentName) throws RatingsException;

    AverageRating getAverageRating(String fromsql, String wheresql, String method) throws RatingsException;

    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql) throws RatingsException;

    AverageRating getUserReputation(String username) throws RatingsException;
}
