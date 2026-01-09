The `schema/` directory contains the json schemas for the CMC Json API.
The original files can be found in the [CMC repository](https://github.com/Fraunhofer-AISEC/cmc/tree/4b2617524120e18f06933c8855fe5228a8bf7d68/doc/api/json/api).
Documentation for the API is located [here](https://github.com/Fraunhofer-AISEC/cmc/blob/4b2617524120e18f06933c8855fe5228a8bf7d68/doc/cmcd-api.md).

Note that we have to modify the schema files slightly to generate working model classes.
The required modifications are listed in the following patch.

```diff
diff --git a/../cmc/doc/api/json/api/VerificationResponse.json b/src/main/resources/schema/VerificationResponse.json
index 7445f6a..2e072d2 100644
--- a/../cmc/doc/api/json/api/VerificationResponse.json
+++ b/src/main/resources/schema/VerificationResponse.json
@@ -1186,8 +1186,7 @@
         "type",
         "summary",
         "freshness",
-        "signature",
-        "artifacts"
+        "signature"
       ]
     },
     "MetadataResult": {
@@ -1306,9 +1305,7 @@
         "name",
         "version",
         "validity",
-        "SgxValues",
-        "summary",
-        "signatureValidation"
+        "summary"
       ]
     },
     "MetadataSummary": {
@@ -1338,8 +1335,6 @@
       "type": "object",
       "required": [
         "devDescResult",
-        "manifestResults",
-        "unknownResults",
         "compatibilityResult"
       ]
     },
```