package au.org.ala.collectory

import grails.testing.services.ServiceUnitTest
import org.apache.tools.zip.ZipFile
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Reading the archive of a GBIF download ("Upload GBIF file").
 */
class GbifServiceSpec extends Specification implements ServiceUnitTest<GbifService> {

    File dir

    def setup() {
        dir = File.createTempDir()
    }

    def cleanup() {
        dir?.deleteDir()
    }

    private ZipFile archive(Map<String, String> entries) {
        File zip = new File(dir, 'download.zip')
        new ZipOutputStream(new FileOutputStream(zip)).withCloseable { out ->
            entries.each { name, text ->
                out.putNextEntry(new ZipEntry(name))
                out.write(text.getBytes('UTF-8'))
                out.closeEntry()
            }
        }
        new ZipFile(zip)
    }

    def "#293 the details of a GBIF download are read from its EML"() {
        given:
        def eml = '''<?xml version="1.0" encoding="UTF-8"?>
<eml:eml xmlns:eml="eml://ecoinformatics.org/eml-2.1.1" packageId="4a3d9c2e-1111-2222-3333-444455556666">
  <dataset>
    <title>Global Roadkill Data – Ñandú</title>
    <abstract><para>A description</para></abstract>
    <contact><phone>+61 2 0000 0000</phone><electronicMailAddress>a@example.org</electronicMailAddress></contact>
  </dataset>
  <additionalMetadata><metadata><gbif>
    <citation>Smith et al. 2024</citation>
    <rights>Dataset: Global Roadkill Data</rights>
  </gbif></metadata></additionalMetadata>
</eml:eml>'''

        when:
        def json = service.extractDataResourceJSON(archive(['dataset/4a3d9c2e.xml': eml]), dir)

        then:
        json.guid == '4a3d9c2e-1111-2222-3333-444455556666'
        json.name == 'Global Roadkill Data – Ñandú'
        json.email == 'a@example.org'
        json.citation == 'Smith et al. 2024'
        json.rights == 'Dataset: Global Roadkill Data'
    }
}
