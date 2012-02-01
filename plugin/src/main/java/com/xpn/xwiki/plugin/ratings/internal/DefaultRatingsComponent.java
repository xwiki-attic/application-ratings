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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.ratings.AverageRating;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingsComponent;
import com.xpn.xwiki.plugin.ratings.RatingsException;
import com.xpn.xwiki.plugin.ratings.RatingsManager;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultRatingsComponent implements RatingsComponent
{
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    private final static Object RATINGS_MANAGER_LOCK = new Object();

    private RatingsManager ratingsManager;

    @Override
    public RatingsManager getRatingsManager() throws RatingsException
    {
        synchronized (RATINGS_MANAGER_LOCK) {
            XWikiContext context = getXWikiContext();

            if (this.ratingsManager == null) {
                String ratingsManagerHint = context.getWiki().Param("xwiki.ratings.manager.component", "default");

                logger.debug("Initializing ratings manager with hint '{}'", ratingsManagerHint);

                try {
                    this.ratingsManager = componentManager.lookup(RatingsManager.class, ratingsManagerHint);
                } catch (ComponentLookupException e) {
                    logger.error("Could not initialize ratings manager for hint '{}'. Using default instead.",
                        ratingsManagerHint, e);

                    try {
                        this.ratingsManager = componentManager.lookup(RatingsManager.class);
                    } catch (ComponentLookupException e1) {
                        // Fatal, but unlikely since the default is bundled with the module itself.
                        throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                            RatingsException.ERROR_RATINGS_NO_RATINGS_MANAGER, "No ratings manager available.", e1);
                    }
                }
            }
        }

        return this.ratingsManager;
    }

    public void setRatingsManager(RatingsManager ratingsManager)
    {
        this.ratingsManager = ratingsManager;
    }

    @Override
    public List<Rating> getRatings(String documentName, int start, int count, boolean asc) throws RatingsException
    {
        return getRatingsManager().getRatings(documentName, start, count, asc);
    }

    @Override
    public Rating setRating(String documentName, String author, int vote) throws RatingsException
    {
        return getRatingsManager().setRating(documentName, author, vote);
    }

    @Override
    public Rating getRating(String documentName, String author) throws RatingsException
    {
        return getRatingsManager().getRating(documentName, author);
    }

    @Override
    public AverageRating getAverageRating(String documentName, String method) throws RatingsException
    {
        return getRatingsManager().getAverageRating(documentName, method);
    }

    @Override
    public AverageRating getAverageRating(String documentName) throws RatingsException
    {
        return getRatingsManager().getAverageRating(documentName);
    }

    @Override
    public AverageRating getAverageRating(String fromsql, String wheresql, String method) throws RatingsException
    {
        return getRatingsManager().getAverageRatingFromQuery(fromsql, wheresql, method);
    }

    @Override
    public AverageRating getAverageRatingFromQuery(String fromsql, String wheresql) throws RatingsException
    {
        return getRatingsManager().getAverageRatingFromQuery(fromsql, wheresql);
    }

    @Override
    public AverageRating getUserReputation(String username) throws RatingsException
    {
        return getRatingsManager().getUserReputation(username);
    }

    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
