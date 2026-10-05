# Introduction

The GLUE connector was built for usage control of container pipelines. 
The following sections provide insights about the motivation, the concepts used, and a simplified explanation of our approach. 
More technical details are explained in the following sections [Concept Overview](concept-overview.md) and [Concept Deed-Dive](concept-details.md).

## Motivation

Nowadays, more and more workloads are moved to remote cloud systems for several reasons including lower cost, dynamic load balancing capabilities, managed administration, or for financial benefits, as no self-hosted hardware is needed.
However, using a remote system for data processing requires trusting the cloud provider.
To enable trust our system makes use of two security paradigms which are **Confidential Computing** and **Usage Control**.

Confidential Computing is a term which states that an execution remains private and protected and only visible to a remote peer controlling the process. 
It typically makes use of Trusted Execution Environments (TEEs) to execute code in a protected realm on a remote computer while still making it impossible for even root-level administrators to interfere with the sandboxed execution.

Usage control can control the flow of data beyond traditional access control.
Access control determines who is allowed to access data, but once access is granted, it typically does not control what the recipient can do with it.
Usage control is therefore needed to enforce ongoing constraints on how data may be processed, transformed, forwarded, or shared after access has been granted.
Constraints for data processing are kept not just during the moment of transmission, but even after the sets of data have left the data provider.
The most typical constraints include:
- Time frames: allowing the usage of data during a certain period
- Purposes: allowing only a pre-defined set of actions, which the data may be used for after its transmission.
- Geolocation: restricting the processing of data for certain countries or areas

Using these paradigm numerous new use cases can be realized, which are detailed in our section about [Use Cases](use-cases.md). 
The ultimate aim is to enable the processing of data on a remote cloud system according to accepted policies among multiple peers without having security drawbacks, such as data leaks, while still profiting from cloud benefits.
We address this scenario using our GLUE connector which is capable to control and enforce usage control policies.

## Our Approach

In our design we provide a usage control framework for enforcing a policy-bound pipeline of containers for data processing. 
On the one hand, we gain dynamic usage control, since we can plug together any container as desired.
On the other hand we remain in full control of the enforcement of the usage control due to the precisely defined acceptable data flows within the container network, which are bound in a corresponding policy.

For a practical guide to set up the GLUE Connector, see the [Getting Started Guide](getting-started.md).
To gain a deeper insight into the underlying concepts and design decisions, see [Concept Overview Section](concept-overview.md).
