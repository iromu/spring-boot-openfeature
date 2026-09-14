## ADDED Requirements

### Requirement: The build executes the real-endpoint provider gate
The `verify` phase SHALL execute the Unleash integration test that drives a live Unleash server over HTTP, and SHALL fail when that test's assertions are not satisfied. The build SHALL NOT satisfy this gate by skipping the test: an execution in which the test did not contact a real endpoint does not count as a passing gate.

#### Scenario: Server-authored flag is evaluated correctly
- **WHEN** the gate authors a flag on a live Unleash endpoint and reads it back through the auto-configured provider
- **THEN** the enabled flag evaluates true, the disabled flag evaluates false, and an absent flag evaluates false
- **AND** the build records the test as executed rather than skipped

#### Scenario: Endpoint cannot be brought up
- **WHEN** the endpoint the gate needs cannot be started or does not become healthy within its deadline
- **THEN** the `verify` phase fails

#### Scenario: Assertion does not hold
- **WHEN** a flag authored on the endpoint does not evaluate as authored
- **THEN** the `verify` phase fails and names the offending flag

#### Scenario: No container runtime is available
- **WHEN** the build runs where the endpoint cannot be provisioned
- **THEN** the `verify` phase fails rather than passing with the gate unexecuted

### Requirement: The endpoint gate provisions its own dependencies reproducibly
The gate SHALL provision everything it needs to contact a real endpoint as part of its own execution, with no manual or CI-specific pre-step, and SHALL reference each container image by an immutable pin rather than a mutable tag. Each execution SHALL begin from endpoint state in which the gate's credentials are actually created, so a reused or leftover state cannot silently leave the gate without them.

#### Scenario: Clean checkout runs the gate without a manual step
- **WHEN** the build runs on a host with a working container runtime and no endpoint was started beforehand
- **THEN** the gate still executes against a real endpoint and passes

#### Scenario: Upstream re-tags a mutable tag
- **WHEN** an image publisher moves a mutable tag such as `latest` to a different build
- **THEN** what the gate runs does not change, because the image is referenced by an immutable pin

#### Scenario: Endpoint state is reused from an earlier run
- **WHEN** endpoint data or a database volume survives from a previous run and would cause credential seeding to be skipped
- **THEN** the gate does not proceed into a state where its credentials are absent and its assertions pass vacuously

### Requirement: Endpoint failure is diagnosable from the build output
When the gate fails, the build output SHALL include enough of the endpoint and client interaction to attribute the cause without reproducing the failure by hand.

#### Scenario: Gate failure reports endpoint logs
- **WHEN** the gate fails
- **THEN** the build output includes the endpoint's own log output for the failing run

#### Scenario: Endpoint never becomes healthy
- **WHEN** the endpoint does not reach a healthy state within its deadline
- **THEN** the reported failure identifies the endpoint as the cause and includes its log output
