# List of Containers for Container-based and pipelined Usage-Control (CoPi-UC)

> [!NOTE]
> Wenn wir auf GHCR umziehen, dann brauchts hoffentlich keinen speziellen Zugriff mehr und wir können hier einfach die URL reinschreiben.

The GLUE Connector provides numerous precompiled instances of example containers to be used inside processing chains. 
More information about the container registry and how to get access can be found in [Container Registry](container-registry.md).

## Available Containers

This section details containers which are already available of usage in a pipeline processing network.

### Data-Retrieval-Container (Start-Container)

Start container that handles the reception of data and also provides buffer options for larger amounts of data.
- Serves as only accepted input to a processing network.

### Data-Release-Container (End-Container)

End container that enables the final output of the data to clearly predefined interfaces.
- Visualization output
- Data output
- Binary output

### Filter-Container

The contianer is configured via a configurable regular expression To dynamically delete special records.
Example: records for a specific group must be removed.

### Anonymizer-Container

The container deletes personal data.
Example: reliable deletion of first name or last name fields.

### Attribute-Reference-Protection-Container

If certain data records are personally identifiable, they must be deleted or the property must be removed.
Example: only one female entry is only one woman in the data set.

Typical technics for protection against attribute inference make use of: 
- K-Anonimity
- L-Diversity
- T-Closeness
- and more privacy configuration factors

## Future Containers

This section details containers which are planned for usage in a pipeline processing network and will be implemented soon.

### Loop-Container

Repeats processes in a flow of programs until a certain condition is met on the data sets.

### Jump-Container

Executes program jumps to configurable positions in a container chain.

### Label-Container

A container that can provide datasets with an additional label as meta information.

### JSON-Modification-Container

A container that can change the format of the data in a configurable manner.

### Database-Container / a.k.a. Aggregations-Container

Extraction tester: e.g. At least 10 data sets (protection against extraction)
Selectable mass operation on fields (e.g. average, sum, variance)

## More Information

Next can be seen the [Container Registry](container-registry.md).