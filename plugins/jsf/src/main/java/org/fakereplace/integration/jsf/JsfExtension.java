/*
 * Copyright 2016, Stuart Douglas, and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.fakereplace.integration.jsf;

import java.util.Collections;
import java.util.Set;

import org.fakereplace.Extension;

public class JsfExtension implements Extension {

    @Override
    public String getClassChangeAwareName() {
        return "org.fakereplace.integration.jsf.JSFClassChangeAware";
    }

    @Override
    public Set<String> getIntegrationTriggerClassNames() {
        return Collections.singleton("javax.faces.webapp.FacesServlet");
    }

    @Override
    public String getEnvironment() {
        return null;
    }

    @Override
    public Set<String> getTrackedInstanceClassNames() {
        return Collections.singleton("javax.el.BeanELResolver");
    }
}
