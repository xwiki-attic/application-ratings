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
import org.xwiki.component.phase.Initializable;

@ComponentRole
public interface RatingsManager extends Initializable
{
    String RATING_CLASS_FIELDNAME_DATE = "date";

    String RATING_CLASS_FIELDNAME_AUTHOR = "author";

    String RATING_CLASS_FIELDNAME_VOTE = "vote";

    String RATING_CLASS_FIELDNAME_PARENT = "parent";

    String AVERAGERATING_CLASS_FIELDNAME_NBVOTES = "nbvotes";

    String AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE = "averagevote";

    String AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD = "method";

    String RATING_REPUTATION_METHOD_BALANCED = "balanced";

    String RATING_REPUTATION_METHOD_AVERAGE = "average";

    String RATING_REPUTATION_METHOD_DEFAULT = "average";

    String getRatingsClassName();

    List<Rating> getRatings(String documentName, int start, int count, boolean asc) throws RatingsException;

    Rating getRating(String ratingId) throws RatingsException;

    Rating getRating(String documentName, int id) throws RatingsException;

    Rating getRating(String documentName, String user) throws RatingsException;

    Rating setRating(String documentName, String author, int vote) throws RatingsException;

    boolean removeRating(Rating rating) throws RatingsException;

    // average rating and reputation

    boolean isAverageRatingStored();

    boolean isReputationStored();

    boolean hasReputation();

    String[] getDefaultReputationMethods();

    AverageRating getAverageRating(String documentName) throws RatingsException;

    AverageRating getAverageRating(String documentName, String method) throws RatingsException;

    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql) throws RatingsException;

    AverageRating getAverageRatingFromQuery(String fromsql, String wheresql, String method) throws RatingsException;

    AverageRating getAverageRating(String documentName, String method, boolean create) throws RatingsException;

    AverageRating calcAverageRating(String documentName, String method) throws RatingsException;

    void updateAverageRating(String documentName, Rating rating, int oldVote, String method) throws RatingsException;

    void updateReputation(String documentName, Rating rating, int oldVote) throws RatingsException;

    void updateAverageRatings(String documentName, Rating rating, int oldVote) throws RatingsException;

    AverageRating getUserReputation(String username) throws RatingsException;

    ReputationAlgorithm getReputationAlgorythm() throws RatingsException;
}
