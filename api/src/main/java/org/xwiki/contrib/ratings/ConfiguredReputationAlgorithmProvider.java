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
public class ConfiguredReputationAlgorithmProvider implements Provider<ReputationAlgorithm> 
{
    
    @Inject 
    Logger logger;   

    @Inject
    Execution execution;

    @Inject
    ComponentManager componentManager;
    
    @Inject
    Provider<RatingsManager> ratingsManagerProvider;

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
    public ReputationAlgorithm get() 
    {
        // TODO implement
        String reputationAlgorithmHint = getXWiki().Param(RatingsManager.RATINGS_CONFIG_PARAM_PREFIX + RatingsManager.RATINGS_CONFIG_FIELDNAME_REPUTATIONALGORITHM_HINT, "default");
        
        try {
            XWikiDocument configDoc = getXWiki().getDocument(RatingsManager.RATINGS_CONFIG_PAGE, getXWikiContext());
            if (configDoc!=null && !configDoc.isNew() && configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME)!=null) {
                BaseProperty prop = (BaseProperty) configDoc.getObject(RatingsManager.RATINGS_CONFIG_CLASSNAME).get(RatingsManager.RATINGS_CONFIG_FIELDNAME_REPUTATIONALGORITHM_HINT);
                String hint = (prop==null) ? null : (String) prop.getValue();
                reputationAlgorithmHint = (hint==null) ? reputationAlgorithmHint : hint;
            }
        } catch(Exception e) {
            logger.error("Cannot read reputation algorithm config", e);
        }
        
        // if the reputation algorithm hint is a page let's try to get the instance from groovy
        if (reputationAlgorithmHint.contains(".")) { 
            try {
             ReputationAlgorithmGroovy reputationInstance = (ReputationAlgorithmGroovy) getXWiki().parseGroovyFromPage(reputationAlgorithmHint, getXWikiContext());
             
             if (reputationInstance!=null) {            
                 reputationInstance.setComponentManager(componentManager);
                 reputationInstance.setExecution(execution);
                 reputationInstance.setXWikiContext(getXWikiContext());
                 reputationInstance.setRatingsManager(ratingsManagerProvider.get());
                 return reputationInstance;
             }
            } catch (Throwable e) {
                logger.error("Cannot instanciate Reputation algorithm from page " + reputationAlgorithmHint, e);                
            }
        } 
         
        try {
            return componentManager.getInstance(ReputationAlgorithm.class, reputationAlgorithmHint);
        } catch (ComponentLookupException e) {
            // TODO Auto-generated catch block
            logger.error("Error loading ratings manager component for hint " + reputationAlgorithmHint, e);
            try {
                return componentManager.getInstance(ReputationAlgorithm.class, "default");
            } catch (ComponentLookupException e1) {
                return null;
            }
        }
    }
}
