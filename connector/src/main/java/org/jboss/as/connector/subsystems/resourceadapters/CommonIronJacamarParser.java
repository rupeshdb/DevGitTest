/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.connector.subsystems.resourceadapters;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CLASS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.INTERLEAVING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.NO_RECOVERY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.POOL_NAME_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_CLASSNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERLUGIN_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_PASSWORD;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_USERNAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_CCM;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WRAP_XA_RESOURCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.connector.util.AbstractParser;
import org.jboss.as.connector.util.ParserException;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.CommonBundle;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.v10.CommonConnDef;
import org.jboss.jca.common.api.metadata.ds.v11.DataSource;
import org.jboss.jca.common.api.metadata.ds.v11.XaDataSource;
import org.jboss.jca.common.api.metadata.resourceadapter.v10.ResourceAdapter;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.logging.Messages;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * A CommonIronJacamarParser.
 *
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public abstract class CommonIronJacamarParser extends AbstractParser {
    /**
     * The bundle
     */
    private static CommonBundle bundle = Messages.getBundle(CommonBundle.class);


    protected void parseConfigProperties(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map) throws XMLStreamException, ParserException {
        String name = rawAttributeText(reader, "name");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        String value = rawElementText(reader);
        CONFIG_PROPERTY_VALUE.parseAndSetParameter(value, operation, reader);

        if (map.containsKey(name)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        map.put(name, operation);
    }

    /**
     * parse a single connection-definition tag
     *
     * @param reader the reader
     * @throws javax.xml.stream.XMLStreamException
     *                         XMLStreamException
     * @throws ParserException ParserException
     * @throws org.jboss.jca.common.api.validator.ValidateException
     *                         ValidateException
     */
    protected void parseConnectionDefinitions(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map,
                                              final Map<String, HashMap<String, ModelNode>> configMap, final boolean isXa)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode connectionDefinitionNode = new ModelNode();
        connectionDefinitionNode.get(OP).set(ADD);

        String poolName = null;
        String jndiName = null;
        int attributeSize = reader.getAttributeCount();
        boolean poolDefined = Boolean.FALSE;

        for (int i = 0; i < attributeSize; i++) {
            CommonConnDef.Attribute attribute = CommonConnDef.Attribute.forName(reader.getAttributeLocalName(i));
            String value = reader.getAttributeValue(i);
            switch (attribute) {
                case ENABLED: {
                    ENABLED.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }
                case JNDI_NAME: {
                    jndiName = value;
                    JNDINAME.parseAndSetParameter(jndiName, connectionDefinitionNode, reader);
                    break;
                }
                case POOL_NAME: {
                    poolName = value;
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    USE_JAVA_CONTEXT.parseAndSetParameter(value, connectionDefinitionNode, reader);

                    break;
                }

                case USE_CCM: {
                    USE_CCM.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }

                case CLASS_NAME: {
                    CLASS_NAME.parseAndSetParameter(value, connectionDefinitionNode, reader);
                    break;
                }
                default:
                    break;
            }
        }
        if (poolName == null || poolName.trim().equals("")) {
            if (jndiName != null && jndiName.trim().length() != 0) {
                if (jndiName.contains("/")) {
                    poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                } else {
                    poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                }
            } else {
                throw ParseUtils.missingRequired(reader, EnumSet.of(CommonConnDef.Attribute.JNDI_NAME));
            }
        }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ResourceAdapter.Tag.forName(reader.getLocalName()) == ResourceAdapter.Tag.CONNECTION_DEFINITION) {

                        map.put(poolName, connectionDefinitionNode);
                        return;
                    } else {
                        if (CommonConnDef.Tag.forName(reader.getLocalName()) == CommonConnDef.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonConnDef.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (!configMap.containsKey(poolName)) {
                                configMap.put(poolName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(poolName));
                            break;
                        }
                        case SECURITY: {
                            parseSecuritySettings(reader, connectionDefinitionNode);
                            break;
                        }
                        case TIMEOUT: {
                            parseTimeOut(reader, isXa, connectionDefinitionNode);
                            break;
                        }
                        case VALIDATION: {
                            parseValidation(reader, connectionDefinitionNode);
                            break;
                        }
                        case XA_POOL: {
                            if (!isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parseXaPool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case POOL: {
                            if (isXa) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            if (poolDefined) {
                                throw new ParserException(bundle.multiplePools());
                            }
                            parsePool(reader, connectionDefinitionNode);
                            poolDefined = true;
                            break;
                        }
                        case RECOVERY: {
                            parseRecovery(reader, connectionDefinitionNode);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);

    }

    private void parseValidation(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (CommonConnDef.Tag.forName(reader.getLocalName()) == CommonConnDef.Tag.VALIDATION) {

                        return;
                    } else {
                        if (CommonValidation.Tag.forName(reader.getLocalName()) == CommonValidation.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonValidation.Tag.forName(reader.getLocalName())) {
                        case BACKGROUND_VALIDATION: {
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case BACKGROUND_VALIDATION_MILLIS: {
                            String value = rawElementText(reader);
                            BACKGROUNDVALIDATIONMILLIS.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USE_FAST_FAIL: {
                            String value = rawElementText(reader);
                            USE_FAST_FAIL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);

                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseTimeOut(XMLExtendedStreamReader reader, Boolean isXa, ModelNode node) throws XMLStreamException,
            ParserException, ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (CommonConnDef.Tag.forName(reader.getLocalName()) == CommonConnDef.Tag.TIMEOUT) {

                        return;
                    } else {
                        if (CommonTimeOut.Tag.forName(reader.getLocalName()) == CommonTimeOut.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    String value = rawElementText(reader);
                    switch (CommonTimeOut.Tag.forName(reader.getLocalName())) {
                        case ALLOCATION_RETRY: {
                            ALLOCATION_RETRY.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case ALLOCATION_RETRY_WAIT_MILLIS: {
                            ALLOCATION_RETRY_WAIT_MILLIS.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case BLOCKING_TIMEOUT_MILLIS: {
                            BLOCKING_TIMEOUT_WAIT_MILLIS.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case IDLE_TIMEOUT_MINUTES: {
                            IDLETIMEOUTMINUTES.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case XA_RESOURCE_TIMEOUT: {
                            XA_RESOURCE_TIMEOUT.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }


    protected void parseAdminObjects(final XMLExtendedStreamReader reader, final Map<String, ModelNode> map, final Map<String, HashMap<String, ModelNode>> configMap)
            throws XMLStreamException, ParserException, ValidateException {


        final ModelNode adminObjectNode = new ModelNode();
        adminObjectNode.get(OP).set(ADD);
        int attributeSize = reader.getAttributeCount();


        String poolName = null;
        String jndiName = null;
        for (int i = 0; i < attributeSize; i++) {
            CommonAdminObject.Attribute attribute = CommonAdminObject.Attribute.forName(reader
                    .getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    String value = rawAttributeText(reader, ENABLED.getXmlName());
                    if (value != null) {
                        ENABLED.parseAndSetParameter(value, adminObjectNode, reader);
                    }
                    break;
                }
                case JNDI_NAME: {
                    jndiName = rawAttributeText(reader, JNDINAME.getXmlName());
                    if (jndiName != null) {
                        JNDINAME.parseAndSetParameter(jndiName, adminObjectNode, reader);
                    }
                    break;
                }
                case POOL_NAME: {
                    poolName = rawAttributeText(reader, POOL_NAME_NAME);
                    break;
                }
                case USE_JAVA_CONTEXT: {
                    String value = rawAttributeText(reader, USE_JAVA_CONTEXT.getXmlName());
                    if (value != null) {
                        USE_JAVA_CONTEXT.parseAndSetParameter(value, adminObjectNode, reader);
                    }
                    break;
                }
                case CLASS_NAME: {
                    String value = rawAttributeText(reader, CLASS_NAME.getXmlName());
                    if (value != null) {
                        CLASS_NAME.parseAndSetParameter(value, adminObjectNode, reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        if (poolName == null || poolName.trim().equals("")) {
            if (jndiName != null && jndiName.trim().length() != 0) {
                if (jndiName.contains("/")) {
                    poolName = jndiName.substring(jndiName.lastIndexOf("/") + 1);
                } else {
                    poolName = jndiName.substring(jndiName.lastIndexOf(":") + 1);
                }
            } else {
                throw ParseUtils.missingRequired(reader, EnumSet.of(CommonAdminObject.Attribute.JNDI_NAME));

            }
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (ResourceAdapter.Tag.forName(reader.getLocalName()) == ResourceAdapter.Tag.ADMIN_OBJECT) {

                        map.put(poolName, adminObjectNode);
                        return;
                    } else {
                        if (CommonAdminObject.Tag.forName(reader.getLocalName()) == CommonAdminObject.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonAdminObject.Tag.forName(reader.getLocalName())) {
                        case CONFIG_PROPERTY: {
                            if (!configMap.containsKey(poolName)) {
                                configMap.put(poolName, new HashMap<String, ModelNode>(0));
                            }
                            parseConfigProperties(reader, configMap.get(poolName));
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    /**
     * parse a {@link org.jboss.jca.common.api.metadata.common.CommonXaPool} object
     *
     * @param reader reader
     * @throws XMLStreamException XMLStreamException
     * @throws ParserException
     * @throws ValidateException  ValidateException
     */
    protected void parseXaPool(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException, ValidateException {


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.XA_POOL) {

                        return;

                    } else {
                        if (CommonXaPool.Tag.forName(reader.getLocalName()) == CommonXaPool.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    String value = rawElementText(reader);
                    switch (CommonXaPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            MAX_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            MIN_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }

                        case PREFILL: {
                            POOL_PREFILL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case INTERLEAVING: {
                            //just presence means true
                            value = value == null ? "true" : value;
                            INTERLEAVING.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case IS_SAME_RM_OVERRIDE: {
                            SAME_RM_OVERRIDE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case NO_TX_SEPARATE_POOLS: {
                            //just presence means true
                            value = value == null ? "true" : value;
                            NOTXSEPARATEPOOL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case PAD_XID: {
                            PAD_XID.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case WRAP_XA_RESOURCE: {
                            WRAP_XA_RESOURCE.parseAndSetParameter(value, node, reader);
                            break;
                        }

                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }


    protected void parsePool(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.POOL) {

                        return;

                    } else {
                        if (CommonPool.Tag.forName(reader.getLocalName()) == CommonPool.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    String value = rawElementText(reader);
                    switch (CommonPool.Tag.forName(reader.getLocalName())) {
                        case MAX_POOL_SIZE: {
                            MAX_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case MIN_POOL_SIZE: {
                            MIN_POOL_SIZE.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case PREFILL: {
                            POOL_PREFILL.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USE_STRICT_MIN: {
                            POOL_USE_STRICT_MIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case FLUSH_STRATEGY: {
                            POOL_FLUSH_STRATEGY.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }


    protected void parseRecovery(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {


        for (Recovery.Attribute attribute : Recovery.Attribute.values()) {
            switch (attribute) {
                case NO_RECOVERY: {
                    String value = rawAttributeText(reader, NO_RECOVERY.getXmlName());
                    if (value != null) {
                        NO_RECOVERY.parseAndSetParameter(value, node, reader);
                    }
                    break;
                }
                default:
                    break;
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (XaDataSource.Tag.forName(reader.getLocalName()) == XaDataSource.Tag.RECOVERY) {
                        return;
                    } else {
                        if (Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    Recovery.Tag tag = Recovery.Tag.forName(reader.getLocalName());
                    switch (tag) {
                        case RECOVER_CREDENTIAL: {
                            parseRecoveryCredential(reader, node);
                            break;
                        }
                        case RECOVER_PLUGIN: {
                            parseExtension(reader, tag.getLocalName(), node, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }



    private void parseSecuritySettings(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {

        boolean securtyDomainMatched = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY) {

                        return;
                    } else {
                        if (CommonSecurity.Tag.forName(reader.getLocalName()) == CommonSecurity.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (CommonSecurity.Tag.forName(reader.getLocalName())) {

                        case SECURITY_DOMAIN: {
                            if (securtyDomainMatched) {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN.parseAndSetParameter(value, node, reader);
                            securtyDomainMatched = true;
                            break;
                        }
                        case SECURITY_DOMAIN_AND_APPLICATION: {
                            String value = rawElementText(reader);
                            SECURITY_DOMAIN_AND_APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case APPLICATION: {
                            String value = rawElementText(reader);
                            //just presence means true
                            value = value == null ? "true" : value;
                            APPLICATION.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }

    private void parseRecoveryCredential(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException, ParserException,
            ValidateException {


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT: {
                    if (DataSource.Tag.forName(reader.getLocalName()) == DataSource.Tag.SECURITY ||
                            Recovery.Tag.forName(reader.getLocalName()) == Recovery.Tag.RECOVER_CREDENTIAL) {

                        return;
                    } else {
                        if (Credential.Tag.forName(reader.getLocalName()) == Credential.Tag.UNKNOWN) {
                            throw ParseUtils.unexpectedEndElement(reader);
                        }
                    }
                    break;
                }
                case START_ELEMENT: {
                    switch (Credential.Tag.forName(reader.getLocalName())) {
                        case PASSWORD: {
                            String value = rawElementText(reader);
                            RECOVERY_PASSWORD.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case USER_NAME: {
                            String value = rawElementText(reader);
                            RECOVERY_USERNAME.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        case SECURITY_DOMAIN: {
                            String value = rawElementText(reader);
                            RECOVERY_SECURITY_DOMAIN.parseAndSetParameter(value, node, reader);
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
            }
        }
        throw ParseUtils.unexpectedEndElement(reader);
    }
}
