package org.xwiki.contrib.ratings;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;

@Component
@Singleton
public class ConfiguredRatingsManagerProvider implements Provider<RatingsManager> 
{
        
    @Inject 
    Logger logger;   

    @Inject
    Execution execution;

    @Inject
    ComponentManager componentManager;

    /**
     * <p>
     * Retrieve the XWiki context from the current execution context
     * </p>
     * 
     * @return The XWiki context.
     * @throws RuntimeException If there was an error retrieving the context.
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }

    /**
     * <p>
     * Retrieve the XWiki private API object
     * </p>
     * 
     * @return The XWiki private API object.
     */
    protected XWiki getXWiki()
    {
        return getXWikiContext().getWiki();
    }



    @Override
    public RatingsManager get() 
    {
        // TODO implement
        String ratingsHint = getXWiki().Param(RatingsManager.RATINGS_CONFIG_PARAM_PREFIX + RatingsManager.RATINGS_CONFIG_FIELDNAME_MANAGER_HINT, "default");
        
        try {
            XWikiDocument configDoc = getXWiki().getDocument(RatingsManager.RATINGS_CONFIG_PAGE, getXWikiContext());
            if (!configDoc.isNew() && configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME)!=null) {
                BaseProperty prop = (BaseProperty) configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME).get(RatingsManager.RATINGS_CONFIG_FIELDNAME_MANAGER_HINT);
                String hint = (prop==null) ? null : (String) prop.getValue();
                ratingsHint = (hint==null) ? ratingsHint : hint;
            }
        } catch(Exception e) {
            logger.error("Cannot read ratings config", e);
        }

        try {
            return componentManager.getInstance(RatingsManager.class, ratingsHint);
        } catch (ComponentLookupException e) {
            // TODO Auto-generated catch block
            logger.error("Error loading ratings manager component for hint " + ratingsHint, e);
            try {
                return componentManager.getInstance(RatingsManager.class, "default");
            } catch (ComponentLookupException e1) {
                return null;
            }
        }
    }
}
