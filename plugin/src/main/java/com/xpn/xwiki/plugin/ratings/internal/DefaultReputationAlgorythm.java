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
package com.xpn.xwiki.plugin.ratings.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.plugin.ratings.AverageRating;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingsComponent;
import com.xpn.xwiki.plugin.ratings.RatingsException;
import com.xpn.xwiki.plugin.ratings.RatingsManager;
import com.xpn.xwiki.plugin.ratings.ReputationAlgorithm;
import com.xpn.xwiki.plugin.ratings.ReputationException;

/**
 * Default very simple reputation algorithm. It won't include recalculation put only flow level reputation
 * 
 * @version $Id$
 * @see ReputationAlgorithm
 */
@Component
@Singleton
public class DefaultReputationAlgorythm implements ReputationAlgorithm
{
    @Inject
    private Logger logger;

    @Inject
    private RatingsComponent ratingsComponent;

    @Override
    public RatingsManager getRatingsManager()
    {
        try {
            return ratingsComponent.getRatingsManager();
        } catch (Exception e) {
            logger.error("Failed to get the ratings manager.", e);
            return null;
        }
    }

    /**
     * Gets or calculates the user reputation.
     * 
     * @param username Person to calculate the reputation for
     * @param context context of the request
     * @return AverageRating of the voter
     */
    @Override
    public AverageRating getUserReputation(String username) throws ReputationException
    {
        try {
            return getRatingsManager().getAverageRating(username, RatingsManager.RATING_REPUTATION_METHOD_AVERAGE);
        } catch (RatingsException e) {
            throw new ReputationException(e);
        }
    }

    /**
     * Not implemented. Voters don't receive reputation
     */
    @Override
    public AverageRating calcNewVoterReputation(String voter, String documentName, Rating rating, int oldVote)
        throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Implemented. Authors will receive a simple reputation.
     */
    @Override
    public AverageRating calcNewContributorReputation(String contributor, String documentName, Rating rating,
        int oldVote) throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Not implemented
     */
    @Override
    public Map<String, AverageRating> calcNewAuthorsReputation(String documentName, Rating rating, int oldVote)
        throws ReputationException
    {
        notimplemented();
        return null;
    }

    /**
     * Not implemented
     */
    @Override
    public Map<String, AverageRating> recalcAllReputation() throws ReputationException
    {
        notimplemented();
        return null;
    }

    protected void notimplemented() throws ReputationException
    {
        throw new ReputationException(ReputationException.MODULE_PLUGIN_RATINGS_REPUTATION,
            ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED, "Not implemented");
    }
}
