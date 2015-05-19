package org.sakaiproject.pasystem.impl.rest;

import java.util.List;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.EntityProvider;
import org.sakaiproject.entitybroker.entityprovider.EntityProviderManager;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.EntityView;


public class PASystemEntityProvider implements EntityProvider, AutoRegisterEntityProvider, ActionsExecutable, Outputable, Describeable {

    @Override
    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON };
    }

    @Override
    public String getEntityPrefix() {
        return "pasystem";
    }


    @EntityCustomAction(action = "hello", viewKey = EntityView.VIEW_LIST)
    public String sayHello(EntityView view) {
        return "Hello, world";
    }


    private EntityProviderManager entityProviderManager;

    public void setEntityProviderManager(EntityProviderManager entityProviderManager) {
        this.entityProviderManager = entityProviderManager;
    }

   protected DeveloperHelperService developerHelperService;

   public void setDeveloperHelperService(DeveloperHelperService developerHelperService) {
      this.developerHelperService = developerHelperService;
   }
}
