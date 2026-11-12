/*
 * Copyright 2026 Fraunhofer AISEC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.aisec.glue

import de.fraunhofer.aisec.glue.config.ConfigManager
import io.ktor.server.config.MapApplicationConfig
import java.nio.file.Path

fun emptyConfigManager(): ConfigManager = ConfigManager(MapApplicationConfig())

/**
 * A Config manager that moves the data store to the specified directory
 */
fun tempDataStoreConfigManager(dir: Path): ConfigManager =
    ConfigManager(MapApplicationConfig("datastore.location" to dir.resolve("datastore.db").toString()))

/**
 * Example ODRL offer shared by the test cases.
 */
val rawExampleOffer =
    """
    {
      "@context": {
        "odrl": "http://www.w3.org/ns/odrl/2/",
        "glue": "http://fhg.de/glue/"
      },
      "@type": "http://www.w3.org/ns/odrl/2/Offer",
      "@id": "http://dataowner.com/policy:6163",
      "odrl:permission": [
        {
          "odrl:target": "http://dataowner.com/document:1234",
          "odrl:assigner": "http://dataowner.com/org",
          "odrl:assignee": "http://dataconsumer.com/org",
          "odrl:action": "http://www.w3.org/ns/odrl/2/use",
          "odrl:duty": {
            "odrl:action": {
              "@id": "http://fhg.de/glue/deployPipeline",
              "odrl:refinement": [
                {
                  "odrl:leftOperand": "http://fhg.de/glue/containerPipeline",
                  "odrl:operator": "http://www.w3.org/ns/odrl/2/eq",
                  "odrl:rightOperand": {
                    "@type": "http://fhg.de/glue/ContainerPipeline",
                    "glue:container": [
                      {
                        "@type": "http://fhg.de/glue/Container",
                        "glue:image": "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/proxy:latest",
                        "glue:hash": "${'$'}{PROXY_HASH}",
                        "glue:parameter": {
                          "@type": "http://fhg.de/glue/RestParameter",
                          "glue:httpUrl": "http://0.0.0.0:1234/dockerEndpoint?key1=value1",
                          "glue:httpType": "GET"
                        },
                        "glue:allowNetwork": true
                      },
                      {
                        "@type": "http://fhg.de/glue/Container",
                        "glue:image": "container-registry.gitlab.cc-asp.fraunhofer.de/ccit-glue/usage-control/container-pipeline/http-echo:latest",
                        "glue:hash": "${'$'}{HTTP_ECHO_HASH}",
                        "glue:parameter": {
                          "@type": "http://fhg.de/glue/RestParameter",
                          "glue:httpUrl": "http://0.0.0.0:1234/dockerEndpoint?key1=value1",
                          "glue:httpType": "GET"
                        },
                        "glue:allowNetwork": true
                      }
                    ]
                  }
                }
              ]
            }
          },
          "odrl:constraint": [
            {
              "odrl:leftOperand": "http://www.w3.org/ns/odrl/2/dateTime",
              "odrl:operator": "http://www.w3.org/ns/odrl/2/lteq",
              "odrl:rightOperand": {
                "@value": "3000-12-21",
                "@type": "http://www.w3.org/2001/XMLSchema#date"
              }
            }
          ]
        },
        {
          "odrl:target": "http://dataowner.com/document:1234",
          "odrl:assigner": "http://dataowner.com/org",
          "odrl:assignee": "http://dataconsumer.com/org",
          "odrl:action": "http://www.w3.org/ns/odrl/2/distribute",
          "odrl:duty": {
            "odrl:action": {
              "@id": "http://www.w3.org/ns/odrl/2/nextPolicy",
              "odrl:refinement": [
                {
                  "odrl:leftOperand": "http://fhg.de/glue/policyTemplate",
                  "odrl:operator": "http://www.w3.org/ns/odrl/2/eq",
                  "odrl:rightOperand": {
                    "@type": "http://fhg.de/glue/Value",
                    "glue:json": {
                      "@context": "http://www.w3.org/ns/odrl.jsonld",
                      "@type": "Policy",
                      "uid": "http://dataowner.com/policy:987",
                      "permission": [
                        {
                          "target": "http://dataowner.com/document:1234",
                          "assigner": "http://dataconsumer.com/org",
                          "assignee": "http://fhg.de/glue/ParameterPlaceholder?key=nextDataReceiverUid",
                          "action": "use",
                          "constraint": {
                            "@type": "http://fhg.de/glue/ParameterPlaceholder",
                            "glue:key": "nextDataReceiverConstraint"
                          }
                        }
                      ]
                    }
                  }
                }
              ]
            }
          }
        }
      ]
    }
    """.trimIndent()
