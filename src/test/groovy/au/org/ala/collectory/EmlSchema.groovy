package au.org.ala.collectory

import org.w3c.dom.ls.LSInput
import org.w3c.dom.ls.LSResourceResolver

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory
import javax.xml.validation.Validator

/**
 * The GBIF EML profile 1.1 schema, loaded from src/test/resources/eml/gbif-profile-1.1 without network access.
 */
class EmlSchema {

    static final String DIR = '/eml/gbif-profile-1.1/'

    /**
     * Remote locations imported by the schemas, mapped to the local copies.
     * Both import the xml namespace, so usually only the first one found is read.
     */
    static final Map<String, String> LOCAL = [
            'http://rs.gbif.org/schema/xml.xsd': 'xml.xsd',
            'http://www.w3.org/2001/xml.xsd'   : 'xml.xsd'
    ]

    /** Only local files may be read; a remote location without a local copy fails instead of being downloaded. */
    static final String LOCAL_PROTOCOLS = 'file,jar'

    /** The remote locations the schemas asked for, to check that the local copies were used. */
    static final Set<String> resolved = Collections.synchronizedSet(new HashSet<String>())

    private static final LSResourceResolver RESOLVER = { String type, String namespace, String publicId, String systemId, String baseURI ->
        String file = LOCAL[systemId]
        if (!file) {
            return null // relative imports resolve next to the local schema; anything else is refused by LOCAL_PROTOCOLS
        }
        resolved << systemId
        URL url = EmlSchema.getResource(DIR + file)
        new LocalInput(publicId: publicId, systemId: url.toString(), baseURI: baseURI, byteStream: url.openStream())
    } as LSResourceResolver

    private static final Schema SCHEMA = load()

    /** Throws SAXException with the first schema error. */
    static void validate(String xml) {
        Validator validator = SCHEMA.newValidator()
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, LOCAL_PROTOCOLS)
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, '')
        validator.resourceResolver = RESOLVER
        validator.validate(new StreamSource(new StringReader(xml)))
    }

    private static Schema load() {
        SchemaFactory factory = SchemaFactory.newDefaultInstance() // the JDK one; the Xerces on the classpath ignores the access properties
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, LOCAL_PROTOCOLS)
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, '')
        factory.resourceResolver = RESOLVER
        URL profile = EmlSchema.getResource(DIR + 'eml-gbif-profile.xsd')
        factory.newSchema(new StreamSource(profile.openStream(), profile.toString()))
    }

    private static class LocalInput implements LSInput {
        Reader characterStream
        InputStream byteStream
        String stringData
        String systemId
        String publicId
        String baseURI
        String encoding
        boolean certifiedText
    }
}
