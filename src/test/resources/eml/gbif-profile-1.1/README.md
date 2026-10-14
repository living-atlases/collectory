# GBIF EML profile 1.1 schemas (test only)

Unmodified copies, so `EmlRenderServiceSpec` can validate the EML that collectory renders (#303) without network access.
`EmlRenderService` declares `http://rs.gbif.org/schema/eml-gbif-profile/1.1/eml-gbif-profile.xsd` as its schemaLocation.

| File | Source | SHA-256 |
|---|---|---|
| eml-gbif-profile.xsd | http://rs.gbif.org/schema/eml-gbif-profile/1.1/eml-gbif-profile.xsd | 450abde6626d1a3a6c8c1410d31a6e163b05c63437bd36bf0c84c27dc121170f |
| eml.xsd | http://rs.gbif.org/schema/eml-gbif-profile/1.1/eml.xsd | c1306dc6e5cf3bd2dea2422d2b4ff177133cd598bb1989545b7a96260d46ea31 |
| dc.xsd | http://rs.gbif.org/schema/eml-gbif-profile/1.1/dc.xsd | 47b663ca11b893a24632f3e8568929d0e7720746198561c2a4f53d61f6990442 |
| xml.xsd | http://rs.gbif.org/schema/xml.xsd | 2eafaa6bab254e61051901082d9bc7703c932c1c1675ec3b8dd9ce7140076397 |

The schemas import `xml.xsd` from `http://rs.gbif.org/schema/xml.xsd` and `http://www.w3.org/2001/xml.xsd`.
The test resolves both to the local `xml.xsd` and refuses any other remote location.
