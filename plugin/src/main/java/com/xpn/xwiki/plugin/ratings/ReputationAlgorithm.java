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

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;

/**
 * @version $Id$
 */
@ComponentRole
public interface ReputationAlgorithm
{
    /**
     * @return the current ratings manager.
     */
    public RatingsManager getRatingsManager();

    /**
     * Gets or calculates the user reputation.
     * 
     * @param username Person to calculate the reputation for
     * @return AverageRating of the voter
     */
    AverageRating getUserReputation(String username) throws ReputationException;

    /**
     * Recalculates the contributor reputation. Only the creator of the document will have it's reputation updated
     * 
     * @param voter Voter that will have it's reputation updated
     * @param documentName Elements that was rated
     * @param rating New rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @return AverageRating of the voter
     */
    AverageRating calcNewVoterReputation(String voter, String documentName, Rating rating, int oldVote)
        throws ReputationException;

    /**
     * Recalculates the contributors reputation
     * 
     * @param documentName Elements that was rated
     * @param rating New rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @return Map of AverageRating of each contributor of the page that has an updated reputation
     */
    Map<String, AverageRating> calcNewAuthorsReputation(String documentName, Rating rating, int oldVote)
        throws ReputationException;

    /**
     * Recalculates the contributor reputation. Only the creator of the document will have it's reputation updated
     * 
     * @param contributor Contributor that will have it's reputation updated
     * @param documentName Elements that was rated
     * @param rating New rating of the element, including who did the rating
     * @param oldVote previous vote of the user
     * @return AverageRating of the contributor
     */
    AverageRating calcNewContributorReputation(String contributor, String documentName, Rating rating, int oldVote)
        throws ReputationException;

    /**
     * Recalculated all reputation of the wiki The result is given as a set of AverageRating objects That can be saved
     * to the user page
     * 
     * @return Map of AverageRating of each user of the wiki
     */
    Map<String, AverageRating> recalcAllReputation() throws ReputationException;
}
