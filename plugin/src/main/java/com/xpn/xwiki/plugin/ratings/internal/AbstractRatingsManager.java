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

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.ratings.AverageRating;
import com.xpn.xwiki.plugin.ratings.Rating;
import com.xpn.xwiki.plugin.ratings.RatingsException;
import com.xpn.xwiki.plugin.ratings.RatingsManager;
import com.xpn.xwiki.plugin.ratings.ReputationAlgorithm;
import com.xpn.xwiki.plugin.ratings.ReputationException;

/**
 * @version $Id$
 * @see RatingsManager
 */
public abstract class AbstractRatingsManager implements RatingsManager
{
    private static String ratingsClassName = "XWiki.RatingsClass";

    private static String averageRatingsClassName = "XWiki.AverageRatingsClass";

    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    @Inject
    protected Execution execution;

    @Inject
    protected ComponentManager componentManager;

    /** The event stream used for storing events. */
    @Inject
    protected EventStream eventStream;

    /** The default factory for creating event objects. */
    @Inject
    protected EventFactory factory;

    @Inject
    protected DocumentReferenceResolver<String> resolver;

    @Inject
    protected EntityReferenceSerializer<String> serializer;

    protected ReputationAlgorithm reputationAlgorithm;

    protected String reputationAlgorithmVersion;

    public String getRatingsClassName()
    {
        return ratingsClassName;
    }

    public String getAverageRatingsClassName()
    {
        return averageRatingsClassName;
    }

    @Override
    public void initialize() throws InitializationException
    {
        try {
            initRatingsClass();
            initAverageRatingsClass();
        } catch (Exception e) {
            // FIXME: any backup plan? (Eduard)
            logger.error("Component initialization failed", e);
        }
    }

    public BaseClass initAverageRatingsClass() throws XWikiException
    {
        XWikiDocument doc;
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;
        String averageRatingsClassName = getAverageRatingsClassName();

        doc = xwiki.getDocument(averageRatingsClassName, context);
        BaseClass bclass = doc.getXClass();
        bclass.setName(averageRatingsClassName);
        if (context.get("initdone") != null && !doc.isNew()) {
            return bclass;
        }

        needsUpdate |= bclass.addNumberField(AVERAGERATING_CLASS_FIELDNAME_NBVOTES, "Number of Votes", 5, "integer");
        needsUpdate |= bclass.addNumberField(AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE, "Average Vote", 5, "float");
        needsUpdate |= bclass.addTextField(AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, "Average Vote method", 10);

        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor("XWiki.Admin");
        }
        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator("XWiki.Admin");
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Average Ratings Class");
        }

        if (needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
        return bclass;
    }

    public BaseClass initRatingsClass() throws XWikiException
    {
        XWikiDocument doc;
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;
        String ratingsClassName = getRatingsClassName();

        doc = xwiki.getDocument(ratingsClassName, context);
        BaseClass bclass = doc.getXClass();
        bclass.setName(ratingsClassName);
        if (context.get("initdone") != null && !doc.isNew()) {
            return bclass;
        }

        needsUpdate |= bclass.addTextField(RATING_CLASS_FIELDNAME_AUTHOR, "Author", 30);
        needsUpdate |= bclass.addNumberField(RATING_CLASS_FIELDNAME_VOTE, "Vote", 5, "integer");
        needsUpdate |= bclass.addDateField(RATING_CLASS_FIELDNAME_DATE, "Date");
        needsUpdate |= bclass.addTextField(RATING_CLASS_FIELDNAME_PARENT, "Parent", 30);

        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor("XWiki.Admin");
        }
        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator("XWiki.Admin");
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Ratings Class");
        }

        if (needsUpdate) {
            xwiki.saveDocument(doc, context);
        }
        return bclass;
    }

    public boolean hasRatings()
    {
        XWikiContext context = getXWikiContext();
        int result = (int) context.getWiki().ParamAsLong("xwiki.ratings", 0);
        return (context.getWiki().getXWikiPreferenceAsInt("ratings", result, context) == 1);
    }

    @Override
    public boolean isAverageRatingStored()
    {
        XWikiContext context = getXWikiContext();
        int result = (int) context.getWiki().ParamAsLong("xwiki.ratings.averagerating.stored", 1);
        return (context.getWiki().getXWikiPreferenceAsInt("ratings_averagerating_stored", result, context) == 1);
    }

    @Override
    public boolean isReputationStored()
    {
        XWikiContext context = getXWikiContext();
        int result = (int) context.getWiki().ParamAsLong("xwiki.ratings.reputation.stored", 0);
        return (context.getWiki().getXWikiPreferenceAsInt("ratings_reputation_stored", result, context) == 1);
    }

    @Override
    public boolean hasReputation()
    {
        XWikiContext context = getXWikiContext();
        int result = (int) context.getWiki().ParamAsLong("xwiki.ratings.reputation", 0);
        return (context.getWiki().getXWikiPreferenceAsInt("ratings_reputation", result, context) == 1);
    }

    @Override
    public String[] getDefaultReputationMethods()
    {
        XWikiContext context = getXWikiContext();
        String method =
            context.getWiki().Param("xwiki.ratings.reputation.defaultmethod", RATING_REPUTATION_METHOD_DEFAULT);
        method = context.getWiki().getXWikiPreference("ratings_reputation_defaultmethod", method, context);
        return method.split(",");
    }

    @Override
    public void updateAverageRatings(String documentName, Rating rating, int oldVote) throws RatingsException
    {
        String[] methods = getDefaultReputationMethods();
        for (int i = 0; i < methods.length; i++) {
            updateAverageRating(documentName, rating, oldVote, methods[i]);
        }
    }

    @Override
    public AverageRating getAverageRatingFromQuery(String fromsql, String wheresql) throws RatingsException
    {
        return getAverageRatingFromQuery(fromsql, wheresql, RATING_REPUTATION_METHOD_AVERAGE);
    }

    @Override
    public AverageRating getAverageRating(String documentName) throws RatingsException
    {
        return getAverageRating(documentName, RATING_REPUTATION_METHOD_AVERAGE);
    }

    @Override
    public AverageRating getAverageRatingFromQuery(String fromsql, String wheresql, String method)
        throws RatingsException
    {
        try {
            XWikiContext context = getXWikiContext();

            String fromsql2 =
                fromsql + ", BaseObject as avgobj, FloatProperty as avgvote, StringProperty as avgmethod ";
            String wheresql2 =
                (wheresql.equals("") ? "where " : wheresql + " and ")
                    + "doc.fullName=avgobj.name and avgobj.className='" + getAverageRatingsClassName()
                    + "' and avgobj.id=avgvote.id.id and avgvote.id.name='" + AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE
                    + "' and avgobj.id=avgmethod.id.id and avgmethod.id.name='"
                    + AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD + "' and avgmethod.value='" + method + "'";
            String sql =
                "select sum(avgvote.value) as vote, count(avgvote.value) as nbvotes from XWikiDocument as doc "
                    + fromsql2 + wheresql2;

            if (logger.isDebugEnabled()) {
                logger.debug("Running average rating with sql " + sql);
            }
            context.put("lastsql", sql);

            List result = context.getWiki().getStore().search(sql, 0, 0, context);
            float vote = ((Number) ((Object[]) result.get(0))[0]).floatValue();
            int nbvotes = ((Number) ((Object[]) result.get(0))[1]).intValue();

            AverageRating avgr = new MemoryAverageRating(null, nbvotes, vote / (float) nbvotes, method);
            return avgr;
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public boolean removeRating(Rating rating) throws RatingsException
    {
        return rating.remove();
    }

    /**
     * Get the reputation algorithm class. Make sure the version is checked when it is a groovy script
     */
    @Override
    public ReputationAlgorithm getReputationAlgorythm() throws RatingsException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        String groovyPage = getReputationAlgorithmGroovyPage();
        if (reputationAlgorithm != null) {
            if ((reputationAlgorithmVersion == null) || (groovyPage == null)) {
                return reputationAlgorithm;
            } else {
                XWikiDocument groovyDoc = null;
                try {
                    groovyDoc = xwiki.getDocument(groovyPage, context);
                    String groovyVersion = groovyDoc.getVersion();
                    // version is the same let's use the already loaded one
                    if (reputationAlgorithmVersion.equals(groovyVersion)) {
                        return reputationAlgorithm;
                    }
                } catch (XWikiException e) {
                    if (logger.isErrorEnabled()) {
                        logger
                            .error(
                                "Could not check if the reputation algorithm loaded from the groovy page '{}' needs reloading. Assuming no.",
                                groovyPage, e);
                    }

                    // Assume that the page did not change.
                    return reputationAlgorithm;
                }
            }
        }

        // If the algorithm is specified in a Groovy page, make sure it is executed and registered before trying to load
        // it.
        if (groovyPage != null) {
            try {
                XWikiDocument groovyDoc = xwiki.getDocument(groovyPage, context);
                // Execute the Groovy script that causes the defined component to get registered.
                groovyDoc.getRenderedContent(context);
                // Update the version that was used so that we know next time if we need to reload it because it was
                // changed.
                reputationAlgorithmVersion = groovyDoc.getVersion();
            } catch (XWikiException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Could not read reputation algorythm from groovy page {}.", groovyPage, e);
                }
            }
        }

        String reputationAlgorithmHint = getReputationAlgorithmComponentHint();
        try {
            reputationAlgorithm = componentManager.getInstance(ReputationAlgorithm.class, reputationAlgorithmHint);
        } catch (ComponentLookupException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Could not initialize reputation algorithm for hint '{}'. Using default instead.",
                    reputationAlgorithmHint, e);
            }

            // Use default instead.
            try {
                reputationAlgorithm = componentManager.getInstance(ReputationAlgorithm.class);
            } catch (ComponentLookupException e1) {
                // Fatal, but unlikely since the default is bundled with the module itself.
                throw new RatingsException(RatingsException.MODULE_PLUGIN_RATINGS,
                    RatingsException.ERROR_RATINGS_NO_REPUTATION_ALGORITHM, "No reputation algorithm available.", e1);
            }
        }

        return reputationAlgorithm;
    }

    private String getReputationAlgorithmGroovyPage()
    {
        XWikiContext context = getXWikiContext();

        String groovyPage = context.getWiki().Param("xwiki.ratings.reputation.groovypage", "");
        groovyPage = context.getWiki().getXWikiPreference("ratings_reputation_groovypage", groovyPage, context);
        return groovyPage;
    }

    private String getReputationAlgorithmComponentHint()
    {
        XWikiContext context = getXWikiContext();

        String groovyPage = context.getWiki().Param("xwiki.ratings.reputation.component", "");
        groovyPage = context.getWiki().getXWikiPreference("ratings_reputation_component", groovyPage, context);
        return groovyPage;
    }

    @Override
    public AverageRating calcAverageRating(String documentName, String method) throws RatingsException
    {
        int nbVotes = 0;
        int balancedNbVotes = 0;
        float totalVote = 0;
        float averageVote = 0;
        List<Rating> ratings = getRatings(documentName, 0, 0, true);
        if (ratings == null) {
            return null;
        }
        for (Rating rating : ratings) {
            if (method.equals(RATING_REPUTATION_METHOD_BALANCED)) {
                String author = rating.getAuthor();
                // in case we are evaluating the average rating of a user
                // we should not include votes of himself to a user
                if (!author.equals(documentName)) {
                    AverageRating reputation = getUserReputation(author);
                    if ((reputation == null) || (reputation.getAverageVote() == 0)) {
                        totalVote += rating.getVote();
                        balancedNbVotes++;
                    } else {
                        totalVote += rating.getVote() * reputation.getAverageVote();
                        balancedNbVotes += reputation.getAverageVote();
                    }
                }
            } else {
                totalVote += rating.getVote();
                balancedNbVotes++;
            }
            nbVotes++;
        }

        if (balancedNbVotes != 0) {
            averageVote = totalVote / balancedNbVotes;
        }
        return new MemoryAverageRating(documentName, nbVotes, averageVote, method);
    }

    @Override
    public void updateAverageRating(String documentName, Rating rating, int oldVote, String method)
        throws RatingsException
    {
        // we only update if we are in stored mode and if the vote changed
        if (isAverageRatingStored() && oldVote != rating.getVote()) {
            XWikiContext context = getXWikiContext();

            AverageRating aRating = calcAverageRating(documentName, method);
            AverageRating averageRating = getAverageRating(documentName, method, true);
            averageRating.setAverageVote(aRating.getAverageVote());
            averageRating.setNbVotes(aRating.getNbVotes());
            averageRating.save(context);
            /*
             * StoredAverageRating averageRating = (StoredAverageRating) getAverageRating(container, method, true,
             * context); int diffTotal = rating.getVote() - oldVote; int diffNbVotes = (oldVote==0) ? 1 : 0; int
             * oldNbVotes = averageRating.getNbVotes(); averageRating.setNbVotes(oldNbVotes + diffNbVotes);
             * averageRating.setAverageVote((averageRating.getAverageVote()*oldNbVotes + diffTotal) / (oldNbVotes +
             * diffNbVotes));
             */
        }
    }

    @Override
    public void updateReputation(String documentName, Rating rating, int oldVote) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        // we only update if we are in stored mode and if the vote changed
        if (hasReputation() && isReputationStored() && oldVote != rating.getVote()) {
            ReputationAlgorithm ralgo = getReputationAlgorythm();
            // voter reputation. This will give points to the voter
            try {
                AverageRating voterRating =
                    ralgo.calcNewVoterReputation(rating.getAuthor(), documentName, rating, oldVote);
                // we need to save this reputation if it has changed
                updateUserReputation(rating.getAuthor(), voterRating);
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while calculating voter reputation " + rating.getAuthor()
                            + " for document " + documentName, e);
                    }
                }
            }

            // author reputation. This will be giving points to the creator of a document or comment
            try {
                XWikiDocument doc = context.getWiki().getDocument(documentName, context);
                AverageRating authorRating =
                    ralgo.calcNewContributorReputation(doc.getCreator(), documentName, rating, oldVote);
                // we need to save the author reputation
                updateUserReputation(doc.getCreator(), authorRating);
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while calculating author reputation for document " + documentName, e);
                    }
                }
            } catch (XWikiException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error while calculating author reputation for document " + documentName, e);
                }
            }

            // all authors reputation. This will be used to give points to all participants to a document
            try {
                Map<String, AverageRating> authorsRatings =
                    ralgo.calcNewAuthorsReputation(documentName, rating, oldVote);
                // TODO this is not implemented yet
            } catch (ReputationException e) {
                if (e.getCode() != ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                    // we should log this error
                    if (logger.isErrorEnabled()) {
                        logger.error("Error while calculating authors reputation for document " + documentName, e);
                    }
                }
            } catch (XWikiException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error while calculating authors for document " + documentName, e);
                }
            }
        }
    }

    private void updateUserReputation(String author, AverageRating voterRating) throws RatingsException
    {
        try {
            XWikiContext context = getXWikiContext();

            // We should update the user rating
            AverageRating rating = getAverageRating(author, voterRating.getMethod(), true);
            rating.setAverageVote(voterRating.getAverageVote());
            rating.setMethod(voterRating.getMethod());
            rating.setNbVotes(voterRating.getNbVotes());
            rating.save(context);
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public AverageRating getUserReputation(String username) throws RatingsException
    {
        try {
            return getReputationAlgorythm().getUserReputation(username);
        } catch (ReputationException e) {
            if (e.getCode() == ReputationException.ERROR_REPUTATION_NOT_IMPLEMENTED) {
                return getAverageRating(username);
            } else {
                throw e;
            }
        }
    }

    @Override
    public AverageRating getAverageRating(String documentName, String method) throws RatingsException
    {
        return getAverageRating(documentName, method, false);
    }

    @Override
    public AverageRating getAverageRating(String documentName, String method, boolean create) throws RatingsException
    {
        XWikiContext context = getXWikiContext();

        try {
            if (isAverageRatingStored()) {
                String className = getAverageRatingsClassName();
                XWikiDocument doc = context.getWiki().getDocument(documentName, context);
                BaseObject averageRatingObject =
                    doc.getObject(className, RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD, method,
                        false);
                if (averageRatingObject == null) {
                    if (!create) {
                        return calcAverageRating(documentName, method);
                    }

                    // initiate a new average rating object
                    averageRatingObject = doc.newObject(className, context);
                    averageRatingObject.setStringValue(RatingsManager.AVERAGERATING_CLASS_FIELDNAME_AVERAGEVOTE_METHOD,
                        method);
                }

                return new StoredAverageRating(doc, averageRatingObject);
            } else {
                return calcAverageRating(documentName, method);
            }
        } catch (XWikiException e) {
            throw new RatingsException(e);
        }
    }

    @Override
    public Rating getRating(String documentName, String author) throws RatingsException
    {
        if (author == null) {
            logger.warn("No author specified. Returning null rating.");
            return null;
        }

        for (Rating rating : getRatings(documentName, 0, 0, false)) {
            if (author.equals(rating.getAuthor())) {
                return rating;
            }
        }

        return null;
    }

    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    public void fireRatingActivityEvent(String ratingEventType, Rating rating)
    {
        String documentName = rating.getDocumentName();
        DocumentReference documentReference = resolver.resolve(documentName);
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        DocumentReference ratingsClassReference = resolver.resolve(getRatingsClassName());

        Event event = factory.createEvent();
        event.setUser(context.getUserReference());
        event.setApplication("Ratings");
        event.setType(ratingEventType);
        event.setTitle(String.format("A rating has been assigned for the document \"{}\"", documentName));

        event.setStream(serializer.serialize(documentReference));

        event.setWiki(documentReference.getWikiReference());
        try {
            event.setUrl(new URL(xwiki.getExternalURL(documentName, "view", context)));
        } catch (Exception e) {
            logger.error("Failed to get the rating event's URL.", e);
        }
        event.setSpace(documentReference.getLastSpaceReference());
        event.setDocument(documentReference);
        try {
            XWikiDocument document = xwiki.getDocument(documentReference, context);
            event.setDocumentVersion(document.getVersion());
            event.setDocumentTitle(document.getRenderedTitle(Syntax.PLAIN_1_0, context));
        } catch (Exception e) {
            logger.error("Failed to set the rating event's document information", e);
        }

        try {
            event.setRelatedEntity(new BaseObjectReference(ratingsClassReference, rating.getAsObject().getNumber(),
                documentReference));
        } catch (Exception e) {
            logger.error("Failed to set the rating event's related information", e);
        }

        event.setBody(String.valueOf(rating.getVote()));
        event.setImportance(Event.Importance.MINOR);
        eventStream.addEvent(event);
    }
}
