/*
 * Copyright 2012, Stuart Douglas, and individual contributors as indicated
 * by the @authors tag.
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

package org.fakereplace.integration.jbossas.hibernate4;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fakereplace.api.ChangedClass;
import org.fakereplace.classloading.ClassIdentifier;
import org.fakereplace.data.InstanceTracker;
import org.fakereplace.integration.hibernate4.FakereplaceEntityManagerFactoryProxy;
import org.fakereplace.integration.jbossas.JbossasExtension;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.server.CurrentServiceContainer;

/**
 * @author Stuart Douglas
 */
public class JBossASHibernate4Replacer {

    public static void handleHibernateReplacement(List<ChangedClass> changed, List<ClassIdentifier> added) {
        final Set<Class<?>> changedClasses = new HashSet<Class<?>>();
        for (ChangedClass changedClass : changed) {
            changedClasses.add(changedClass.getChangedClass());
        }
        final Set<PersistenceUnitServiceImpl> puServices = (Set<PersistenceUnitServiceImpl>) InstanceTracker.get(JbossasExtension.PERSISTENCE_UNIT_SERVICE);

        try {
            final Field puField = PersistenceUnitServiceImpl.class.getDeclaredField("pu");
            puField.setAccessible(true);
            final Field persistenceProviderAdaptorField = PersistenceUnitServiceImpl.class.getField("persistenceProviderAdaptor");
            persistenceProviderAdaptorField.setAccessible(true);


            WritableServiceBasedNamingStore.pushOwner(CurrentServiceContainer.getServiceContainer());
            try {
                for (PersistenceUnitServiceImpl puService : puServices) {
                    final FakereplaceEntityManagerFactoryProxy proxy = (FakereplaceEntityManagerFactoryProxy) puService.getEntityManagerFactory();
                    final PersistenceProviderAdaptor adaptor = (PersistenceProviderAdaptor) persistenceProviderAdaptorField.get(puService);
                    final PersistenceUnitMetadata pu = (PersistenceUnitMetadata) puField.get(puService);
                    adaptor.beforeCreateContainerEntityManagerFactory(pu);
                    try {
                        proxy.reload();
                    } finally {
                        adaptor.afterCreateContainerEntityManagerFactory(pu);
                    }

                }
            } finally {
                WritableServiceBasedNamingStore.popOwner();
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
