/**
 * Copyright (C) 2011 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 */
package au.org.ala.collectory

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import java.text.DateFormat
import java.text.SimpleDateFormat

class ExternalIdentifierService {

    static transactional = true

    /**
     * Add an external identifier to this object
     *
     * @param identifier The identifier
     * @param source The identifier source (eg. 'GBIF')
     * @param link A link to the oreiginal source
     * @return
     */
    ExternalIdentifier addExternalIdentifier(String uid, String identifier, String source, String link) {
        ExternalIdentifier ext = new ExternalIdentifier(entityUid: uid, identifier: identifier, source: source, uri: link)
        ExternalIdentifier.withTransaction {
            ext.save(flush: true)
        }

        if (uid[0..1] == 'co') {
            Collection c = Collection.findByUid(uid)
            if (c.externalIdentifiers == null) c.externalIdentifiers = new HashSet<>();
            c.externalIdentifiers.add(ext)
            Collection.withTransaction { c.save(flush: true) }
        } else if (uid[0..1] == 'in') {
            Institution c = Institution.findByUid(uid)
            if (c.externalIdentifiers == null) c.externalIdentifiers = new HashSet<>();
            c.externalIdentifiers.add(ext)
            Institution.withTransaction { c.save(flush: true) }
        } else if (uid[0..1] == 'dp') {
            DataProvider c = DataProvider.findByUid(uid)
            if (c.externalIdentifiers == null) c.externalIdentifiers = new HashSet<>();
            c.externalIdentifiers.add(ext)
            DataProvider.withTransaction { c.save(flush: true) }
        } else if (uid[0..1] == 'dr') {
            DataResource c = DataResource.findByUid(uid)
            if (c.externalIdentifiers == null) c.externalIdentifiers = new HashSet<>();
            c.externalIdentifiers.add(ext)
            DataResource.withTransaction { c.save(flush: true) }
        }
        return ext
    }
}
