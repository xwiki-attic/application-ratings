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
package org.xwiki.contrib.ratings.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.ratings.Rating;
import org.xwiki.contrib.ratings.RatingsException;
import org.xwiki.contrib.ratings.UpdateRatingEvent;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultRatingsManager extends AbstractRatingsManager
{
    /**
     * The logger to LOGGER.
     */
    @Inject
    private Logger LOGGER;
    
    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.contrib.ratings.RatingsManager#setRating(com.xpn.xwiki.plugin.comments.Container, String, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Rating setRating(String documentName, String author, int vote) throws RatingsException
    {
        Rating rating = getRating(documentName, author);
        int oldVote;
        if (rating == null) {
            oldVote = 0;
            rating = new DefaultRating(documentName, author, vote, getXWikiContext());
        } else {
            oldVote = rating.getVote();
            rating.setVote(vote);
            rating.setDate(new Date());
        }
        // update rating
        rating.save();
        
        // update average rating count
        updateAverageRatings(documentName, rating, oldVote);
        
        // update reputation
        observationManager.notify(new UpdateRatingEvent(documentName, rating, oldVote), null);
        return rating;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.contrib.ratings.RatingsManager#getRatings(com.xpn.xwiki.plugin.comments.Container, int, int,
     *      boolean, com.xpn.xwiki.XWikiContext)
     */
    public List<Rating> getRatings(String documentName, int start, int count, boolean asc)
        throws RatingsException
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calling default manager code for ratings");
        }
        try {
            int skipped = 0;
            int nb = 0;
            XWikiDocument doc = getXWiki().getDocument(documentName, getXWikiContext());
            List<BaseObject> bobjects = doc.getObjects(getRatingsClassName());
            if (bobjects != null) {
                List<Rating> ratings = new ArrayList<Rating>();
                for (BaseObject bobj : bobjects) {
                    if (bobj != null) {
                        if (skipped < start) {
                            skipped++;
                        } else {
                            ratings.add(getDefaultRating(documentName, bobj));
                            nb++;
                        }
                        if ((count != 0) && (nb == count)) {
                            break;
                        }
                    }
                }
                return ratings;
            }
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Rating getRating(String ratingId) throws RatingsException
    {
        try {
            int i1 = ratingId.indexOf(":");
            if (i1 == -1) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, cannot parse rating id");
            }

            String docName = ratingId.substring(0, i1);
            String sObjectNb = ratingId.substring(i1 + 1);
            int objectNb = Integer.parseInt(sObjectNb);
            XWikiDocument doc = getXWiki().getDocument(docName, getXWikiContext());

            if (doc.isNew()) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, document does not exist");
            }

            BaseObject object = doc.getObject(getRatingsClassName(), objectNb);

            if (object == null) {
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_INVALID_RATING_ID, "Invalid rating ID, could not find rating");
            }

            return new DefaultRating(docName, object, getXWikiContext());
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.contrib.ratings.RatingsManager#getRating(com.xpn.xwiki.plugin.comments.Container, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Rating getRating(String documentName, int id) throws RatingsException
    {
        try {
            int skipped = 0;
            XWikiDocument doc = getXWiki().getDocument(documentName, getXWikiContext());
            List<BaseObject> bobjects = doc.getObjects(getRatingsClassName());
            if (bobjects != null) {
                for (BaseObject bobj : bobjects) {
                    if (bobj != null) {
                        if (skipped < id) {
                            skipped++;
                        } else {
                            return getDefaultRating(documentName, bobj);
                        }
                    }
                }
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.contrib.ratings.RatingsManager#getRating(com.xpn.xwiki.plugin.comments.Container, String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Rating getRating(String documentName, String author) throws RatingsException
    {
        try {
            if (author == null) {
                return null;
            }
            List<Rating> ratings = getRatings(documentName, 0, 0, false);
            if (ratings == null) {
                return null;
            }
            for (Rating rating : ratings) {
                if (rating != null && author.equals(rating.getAuthor())) {
                    return rating;
                }
            }
        } catch (XWikiException e) {
            return null;
        }
        return null;
    }

    private DefaultRating getDefaultRating(String documentName, BaseObject bobj)
    {
        return new DefaultRating(documentName, bobj, getXWikiContext());
    }
}
