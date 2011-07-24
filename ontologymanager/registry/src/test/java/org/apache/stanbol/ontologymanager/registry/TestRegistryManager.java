package org.apache.stanbol.ontologymanager.registry;

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.CachingPolicy;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * TODO: send them offline
 */
public class TestRegistryManager {

    private RegistryManager regman;

    @Test
    public void testDistributedCaching() {
        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.CROSS_REGISTRY);
        configuration.put(RegistryManager.REGISTRY_LOCATIONS,
            new String[] {getClass().getResource("/ontologies/registry/onmtest.owl").toString(),
                          getClass().getResource("/ontologies/registry/onmtest_additions.owl").toString()});
        regman = new RegistryManagerImpl(configuration);
        assertNotNull(regman);
        assertSame(CachingPolicy.CROSS_REGISTRY, regman.getCachingPolicy());
        Iterator<Registry> it = regman.getRegistries().iterator();
        OWLOntologyManager cache = it.next().getCache();
        while (it.hasNext())
            assertSame(cache, it.next().getCache());
    }

}
